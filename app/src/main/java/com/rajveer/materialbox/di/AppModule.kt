package com.rajveer.materialbox.di

import android.content.Context
import androidx.room.Room
import com.rajveer.materialbox.data.AppDatabase
import com.rajveer.materialbox.data.dao.CachedVideoDao
import com.rajveer.materialbox.data.dao.MaterialDao
import com.rajveer.materialbox.data.dao.SubjectDao
import com.rajveer.materialbox.data.dao.TopicDao
import com.rajveer.materialbox.data.dao.YoutubeFeedDao
import com.rajveer.materialbox.data.repository.MaterialRepository
import com.rajveer.materialbox.data.repository.SubjectRepository
import com.rajveer.materialbox.data.repository.TopicRepository
import com.rajveer.materialbox.data.repository.VideoCacheRepository
import com.rajveer.materialbox.data.repository.YoutubeFeedRepository
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
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "materialbox_database"
        )
        // ⚠️ NEVER use fallbackToDestructiveMigration() — it silently wipes ALL user data
        // when the DB version changes. Instead, use explicit migrations so data is preserved.
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4
        )
        .build()
    }

    @Provides @Singleton
    fun provideSubjectDao(database: AppDatabase): SubjectDao = database.subjectDao()

    @Provides @Singleton
    fun provideTopicDao(database: AppDatabase): TopicDao = database.topicDao()

    @Provides @Singleton
    fun provideMaterialDao(database: AppDatabase): MaterialDao = database.materialDao()

    @Provides @Singleton
    fun provideYoutubeFeedDao(database: AppDatabase): YoutubeFeedDao = database.youtubeFeedDao()

    @Provides @Singleton
    fun provideCachedVideoDao(database: AppDatabase): CachedVideoDao = database.cachedVideoDao()

    @Provides @Singleton
    fun provideSubjectRepository(subjectDao: SubjectDao): SubjectRepository =
        SubjectRepository(subjectDao)

    @Provides @Singleton
    fun provideTopicRepository(topicDao: TopicDao): TopicRepository =
        TopicRepository(topicDao)

    @Provides @Singleton
    fun provideMaterialRepository(materialDao: MaterialDao): MaterialRepository =
        MaterialRepository(materialDao)

    @Provides @Singleton
    fun provideYoutubeFeedRepository(youtubeFeedDao: YoutubeFeedDao): YoutubeFeedRepository =
        YoutubeFeedRepository(youtubeFeedDao)

    @Provides @Singleton
    fun provideVideoCacheRepository(cachedVideoDao: CachedVideoDao): VideoCacheRepository =
        VideoCacheRepository(cachedVideoDao)
}