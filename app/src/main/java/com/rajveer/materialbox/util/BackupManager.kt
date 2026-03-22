package com.rajveer.materialbox.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {

    private const val DB_NAME = "materialbox_db"

    suspend fun exportDatabaseToMbox(context: Context, destUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(destUri)?.use { destStream ->
                ZipOutputStream(destStream).use { zipOut ->
                    
                    // 1. Export Database Files
                    val dbFile = context.getDatabasePath(DB_NAME)
                    val dbWalFile = context.getDatabasePath("$DB_NAME-wal")
                    val dbShmFile = context.getDatabasePath("$DB_NAME-shm")

                    val databases = listOf(dbFile, dbWalFile, dbShmFile)
                    for (db in databases) {
                        if (db.exists()) {
                            zipOut.putNextEntry(ZipEntry("db/${db.name}"))
                            FileInputStream(db).use { it.copyTo(zipOut) }
                            zipOut.closeEntry()
                        }
                    }

                    // 2. Export Internal Files (Legacy scanner files and cached images)
                    val filesDir = context.filesDir
                    filesDir.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            zipOut.putNextEntry(ZipEntry("files/${file.name}"))
                            FileInputStream(file).use { it.copyTo(zipOut) }
                            zipOut.closeEntry()
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importDatabaseFromMbox(context: Context, sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { sourceStream ->
                ZipInputStream(sourceStream).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val fileName = entry.name
                        
                        // Security check: strictly only allow db/ or files/ prefixes
                        if (fileName.startsWith("db/")) {
                            val dbName = fileName.removePrefix("db/")
                            val destFile = context.getDatabasePath(dbName)
                            destFile.parentFile?.mkdirs()
                            FileOutputStream(destFile).use { out ->
                                zipIn.copyTo(out)
                            }
                        } else if (fileName.startsWith("files/")) {
                            val localFileName = fileName.removePrefix("files/")
                            val destFile = File(context.filesDir, localFileName)
                            FileOutputStream(destFile).use { out ->
                                zipIn.copyTo(out)
                            }
                        }
                        
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
