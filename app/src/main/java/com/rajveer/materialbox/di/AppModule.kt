package com.rajveer.materialbox.di

import android.content.Context
import androidx.room.Room
import com.rajveer.materialbox.data.AppDatabase
import com.rajveer.materialbox.data.dao.CachedVideoDao
import com.rajveer.materialbox.data.dao.MaterialDao
import com.rajveer.materialbox.data.dao.SubjectDao
import com.rajveer.materialbox.data.dao.TopicDao
import com.rajveer.materialbox.data.dao.WatchedVideoDao
import com.rajveer.materialbox.data.dao.YoutubeFeedDao
import com.rajveer.materialbox.data.repository.MaterialRepository
import com.rajveer.materialbox.data.repository.SubjectRepository
import com.rajveer.materialbox.data.repository.TopicRepository
import com.rajveer.materialbox.data.repository.VideoCacheRepository
import com.rajveer.materialbox.data.repository.WatchedVideoRepository
import com.rajveer.materialbox.data.repository.YoutubeFeedRepository
import com.rajveer.materialbox.util.FeedSyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "materialbox_database"
        )
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5
        )
        .build()
    }

    @Provides @Singleton
    fun provideSubjectDao(db: AppDatabase): SubjectDao = db.subjectDao()

    @Provides @Singleton
    fun provideTopicDao(db: AppDatabase): TopicDao = db.topicDao()

    @Provides @Singleton
    fun provideMaterialDao(db: AppDatabase): MaterialDao = db.materialDao()

    @Provides @Singleton
    fun provideYoutubeFeedDao(db: AppDatabase): YoutubeFeedDao = db.youtubeFeedDao()

    @Provides @Singleton
    fun provideCachedVideoDao(db: AppDatabase): CachedVideoDao = db.cachedVideoDao()

    @Provides @Singleton
    fun provideWatchedVideoDao(db: AppDatabase): WatchedVideoDao = db.watchedVideoDao()

    @Provides @Singleton
    fun provideSubjectRepository(dao: SubjectDao): SubjectRepository = SubjectRepository(dao)

    @Provides @Singleton
    fun provideTopicRepository(dao: TopicDao): TopicRepository = TopicRepository(dao)

    @Provides @Singleton
    fun provideMaterialRepository(dao: MaterialDao): MaterialRepository = MaterialRepository(dao)

    @Provides @Singleton
    fun provideYoutubeFeedRepository(dao: YoutubeFeedDao): YoutubeFeedRepository = YoutubeFeedRepository(dao)

    @Provides @Singleton
    fun provideVideoCacheRepository(dao: CachedVideoDao): VideoCacheRepository = VideoCacheRepository(dao)

    @Provides @Singleton
    fun provideWatchedVideoRepository(dao: WatchedVideoDao): WatchedVideoRepository = WatchedVideoRepository(dao)

    @Provides @Singleton
    fun provideFeedSyncManager(
        feedRepository: YoutubeFeedRepository,
        cacheRepository: VideoCacheRepository
    ): FeedSyncManager = FeedSyncManager(feedRepository, cacheRepository)
}