package com.rajveer.materialbox.util.update

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

data class AppReleaseInfo(
    val versionName: String,
    val title: String,
    val notes: String,
    val publishedAt: String,
    val releasePageUrl: String,
    val apkAssetUrl: String?,
    val apkFileName: String?
)

sealed interface AppUpdateUiState {
    data object Hidden : AppUpdateUiState
    data object Checking : AppUpdateUiState
    data class Downloading(
        val release: AppReleaseInfo,
        val progressPercent: Int?
    ) : AppUpdateUiState
    data class ReadyToInstall(
        val release: AppReleaseInfo,
        val apkFilePath: String
    ) : AppUpdateUiState
    data class ActionRequired(
        val release: AppReleaseInfo,
        val message: String,
        val canDownloadInApp: Boolean
    ) : AppUpdateUiState
}

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isReceiverRegistered = AtomicBoolean(false)
    private val _uiState = MutableStateFlow<AppUpdateUiState>(AppUpdateUiState.Hidden)
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            val trackedId = prefs.getLong(KEY_DOWNLOAD_ID, -1L)
            if (completedId != trackedId || trackedId == -1L) return
            scope.launch {
                handleDownloadCompletion(completedId)
            }
        }
    }

    init {
        registerReceiverIfNeeded()
        restorePendingState()
    }

    fun checkForUpdates() {
        scope.launch {
            _uiState.value = AppUpdateUiState.Checking
            val latestRelease = runCatching { fetchLatestRelease() }.getOrElse {
                _uiState.value = AppUpdateUiState.Hidden
                return@launch
            }

            if (latestRelease == null || !isRemoteVersionNewer(latestRelease.versionName, currentAppVersionName())) {
                clearTrackedDownload()
                _uiState.value = AppUpdateUiState.Hidden
                return@launch
            }

            val existingFile = getTrackedApkFile(latestRelease.apkFileName)
            if (existingFile?.exists() == true) {
                _uiState.value = AppUpdateUiState.ReadyToInstall(latestRelease, existingFile.absolutePath)
                return@launch
            }

            if (latestRelease.apkAssetUrl.isNullOrBlank() || latestRelease.apkFileName.isNullOrBlank()) {
                _uiState.value = AppUpdateUiState.ActionRequired(
                    release = latestRelease,
                    message = "A new version is live on GitHub, but this release does not include an APK asset for in-app download.",
                    canDownloadInApp = false
                )
                return@launch
            }

            startDownload(latestRelease)
        }
    }

    fun retryDownload() {
        val currentState = _uiState.value
        val release = when (currentState) {
            is AppUpdateUiState.ActionRequired -> currentState.release
            is AppUpdateUiState.Downloading -> currentState.release
            is AppUpdateUiState.ReadyToInstall -> currentState.release
            else -> null
        } ?: return

        if (release.apkAssetUrl.isNullOrBlank() || release.apkFileName.isNullOrBlank()) {
            openReleasePage()
            return
        }

        scope.launch {
            startDownload(release)
        }
    }

    fun openReleasePage() {
        val releaseUrl = when (val state = _uiState.value) {
            is AppUpdateUiState.ActionRequired -> state.release.releasePageUrl
            is AppUpdateUiState.Downloading -> state.release.releasePageUrl
            is AppUpdateUiState.ReadyToInstall -> state.release.releasePageUrl
            else -> GITHUB_RELEASES_URL
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun installUpdate() {
        val state = _uiState.value as? AppUpdateUiState.ReadyToInstall ?: return
        val apkFile = File(state.apkFilePath)
        if (!apkFile.exists()) {
            _uiState.value = AppUpdateUiState.ActionRequired(
                release = state.release,
                message = "The downloaded APK is missing. Please download the update again.",
                canDownloadInApp = true
            )
            clearTrackedDownload()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(installIntent)
        } catch (_: ActivityNotFoundException) {
            _uiState.value = AppUpdateUiState.ActionRequired(
                release = state.release,
                message = "No installer was available to open the APK. Please use the GitHub release page instead.",
                canDownloadInApp = false
            )
        }
    }

    private suspend fun fetchLatestRelease(): AppReleaseInfo? = withContext(Dispatchers.IO) {
        val connection = (URL(GITHUB_LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "MaterialBox/${currentAppVersionName()}")
        }

        try {
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                throw IOException("GitHub release check failed with HTTP $statusCode")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val tagName = json.optString("tag_name").ifBlank { json.optString("name") }
            val versionName = normalizeVersionName(tagName)
            if (versionName.isBlank()) return@withContext null

            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null
            var apkFileName: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val assetName = asset.optString("name")
                    val contentType = asset.optString("content_type")
                    if (assetName.endsWith(".apk", ignoreCase = true) ||
                        contentType.equals(APK_MIME_TYPE, ignoreCase = true)
                    ) {
                        apkUrl = asset.optString("browser_download_url")
                        apkFileName = assetName
                        break
                    }
                }
            }

            AppReleaseInfo(
                versionName = versionName,
                title = json.optString("name").ifBlank { "MaterialBox $versionName" },
                notes = json.optString("body").orEmpty().trim(),
                publishedAt = json.optString("published_at").orEmpty(),
                releasePageUrl = json.optString("html_url").ifBlank { GITHUB_RELEASES_URL },
                apkAssetUrl = apkUrl,
                apkFileName = apkFileName
            )
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun startDownload(release: AppReleaseInfo) {
        val apkUrl = release.apkAssetUrl
        val apkFileName = release.apkFileName
        if (apkUrl.isNullOrBlank() || apkFileName.isNullOrBlank()) {
            _uiState.value = AppUpdateUiState.ActionRequired(
                release = release,
                message = "A new version was found, but there is no APK file attached to the GitHub release.",
                canDownloadInApp = false
            )
            return
        }

        deleteTrackedApkIfPresent()

        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle("MaterialBox update")
            setDescription("Downloading version ${release.versionName}")
            setMimeType(APK_MIME_TYPE)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                apkFileName
            )
        }

        val downloadId = downloadManager.enqueue(request)
        prefs.edit()
            .putLong(KEY_DOWNLOAD_ID, downloadId)
            .putString(KEY_RELEASE_VERSION, release.versionName)
            .putString(KEY_APK_FILE_NAME, apkFileName)
            .apply()

        _uiState.value = AppUpdateUiState.Downloading(release, progressPercent = 0)
        monitorDownload(downloadId, release)
    }

    private fun monitorDownload(downloadId: Long, release: AppReleaseInfo) {
        scope.launch {
            while (true) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager.query(query).use { cursor ->
                    if (!cursor.moveToFirst()) {
                        _uiState.value = AppUpdateUiState.ActionRequired(
                            release = release,
                            message = "The update download could not be tracked anymore. Please try again.",
                            canDownloadInApp = true
                        )
                        clearTrackedDownload()
                        return@launch
                    }

                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val progress = if (downloaded >= 0 && total > 0) {
                        ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                    } else {
                        null
                    }

                    when (status) {
                        DownloadManager.STATUS_PENDING,
                        DownloadManager.STATUS_PAUSED,
                        DownloadManager.STATUS_RUNNING -> {
                            _uiState.value = AppUpdateUiState.Downloading(release, progress)
                        }

                        DownloadManager.STATUS_SUCCESSFUL -> {
                            handleDownloadCompletion(downloadId)
                            return@launch
                        }

                        DownloadManager.STATUS_FAILED -> {
                            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                            _uiState.value = AppUpdateUiState.ActionRequired(
                                release = release,
                                message = "The update download failed (reason code $reason). You can retry or open GitHub manually.",
                                canDownloadInApp = true
                            )
                            clearTrackedDownload()
                            return@launch
                        }
                    }
                }

                delay(750)
            }
        }
    }

    private suspend fun handleDownloadCompletion(downloadId: Long) {
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return

            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val releaseVersion = prefs.getString(KEY_RELEASE_VERSION, null) ?: return
            val release = resolveTrackedRelease(releaseVersion) ?: return
            val apkFile = getTrackedApkFile(prefs.getString(KEY_APK_FILE_NAME, null))

            if (status == DownloadManager.STATUS_SUCCESSFUL && apkFile?.exists() == true) {
                _uiState.value = AppUpdateUiState.ReadyToInstall(release, apkFile.absolutePath)
            } else {
                _uiState.value = AppUpdateUiState.ActionRequired(
                    release = release,
                    message = "The update finished downloading, but the APK could not be opened. Please retry or download manually.",
                    canDownloadInApp = true
                )
                clearTrackedDownload()
            }
        }
    }

    private fun restorePendingState() {
        val apkFile = getTrackedApkFile(prefs.getString(KEY_APK_FILE_NAME, null))
        if (apkFile?.exists() == true) {
            val releaseVersion = prefs.getString(KEY_RELEASE_VERSION, null) ?: return
            scope.launch {
                val release = resolveTrackedRelease(releaseVersion) ?: return@launch
                _uiState.value = AppUpdateUiState.ReadyToInstall(release, apkFile.absolutePath)
            }
            return
        }

        val downloadId = prefs.getLong(KEY_DOWNLOAD_ID, -1L)
        if (downloadId != -1L) {
            scope.launch {
                val releaseVersion = prefs.getString(KEY_RELEASE_VERSION, null) ?: return@launch
                val release = resolveTrackedRelease(releaseVersion) ?: return@launch
                monitorDownload(downloadId, release)
            }
        }
    }

    private fun registerReceiverIfNeeded() {
        if (!isReceiverRegistered.compareAndSet(false, true)) return
        ContextCompat.registerReceiver(
            context,
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun getTrackedApkFile(fileName: String?): File? {
        if (fileName.isNullOrBlank()) return null
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return null
        return File(directory, fileName)
    }

    private fun deleteTrackedApkIfPresent() {
        getTrackedApkFile(prefs.getString(KEY_APK_FILE_NAME, null))?.takeIf { it.exists() }?.delete()
    }

    private fun clearTrackedDownload() {
        prefs.edit()
            .remove(KEY_DOWNLOAD_ID)
            .remove(KEY_RELEASE_VERSION)
            .remove(KEY_APK_FILE_NAME)
            .apply()
    }

    private suspend fun resolveTrackedRelease(releaseVersion: String): AppReleaseInfo? {
        val currentStateRelease = when (val state = _uiState.value) {
            is AppUpdateUiState.ActionRequired -> state.release
            is AppUpdateUiState.Downloading -> state.release
            is AppUpdateUiState.ReadyToInstall -> state.release
            else -> null
        }
        if (currentStateRelease?.versionName == releaseVersion) {
            return currentStateRelease
        }
        return fetchLatestRelease()?.takeIf { it.versionName == releaseVersion }
    }

    private fun normalizeVersionName(raw: String): String {
        return raw.trim().removePrefix("v").removePrefix("V")
    }

    private fun currentAppVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "0"
        } catch (_: Exception) {
            "0"
        }
    }

    private fun isRemoteVersionNewer(remoteVersion: String, localVersion: String): Boolean {
        val remoteParts = VERSION_PART_REGEX.findAll(remoteVersion).map { it.value.toInt() }.toList()
        val localParts = VERSION_PART_REGEX.findAll(localVersion).map { it.value.toInt() }.toList()
        val maxSize = maxOf(remoteParts.size, localParts.size)
        for (index in 0 until maxSize) {
            val remote = remoteParts.getOrElse(index) { 0 }
            val local = localParts.getOrElse(index) { 0 }
            if (remote != local) return remote > local
        }
        return false
    }

    companion object {
        private const val PREFS_NAME = "app_update_prefs"
        private const val KEY_DOWNLOAD_ID = "download_id"
        private const val KEY_RELEASE_VERSION = "release_version"
        private const val KEY_APK_FILE_NAME = "apk_file_name"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        private const val GITHUB_RELEASES_URL = "https://github.com/raj-Dcoder/MaterialBox/releases"
        private const val GITHUB_LATEST_RELEASE_API =
            "https://api.github.com/repos/raj-Dcoder/MaterialBox/releases/latest"
        private val VERSION_PART_REGEX = Regex("\\d+")
    }
}
