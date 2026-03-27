package com.rajveer.materialbox

import android.app.Application
import android.util.Log
import com.rajveer.materialbox.util.FeedSyncManager
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class MaterialBoxApplication : Application() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FeedSyncEntryPoint {
        fun feedSyncManager(): FeedSyncManager
    }

    override fun onCreate() {
        super.onCreate()
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                this,
                FeedSyncEntryPoint::class.java
            )
            entryPoint.feedSyncManager().syncAllFeeds()
            Log.d("MaterialBoxApp", "Background feed sync triggered at startup.")
        } catch (e: Exception) {
            Log.e("MaterialBoxApp", "Failed to trigger feed sync at startup", e)
        }
    }
}
