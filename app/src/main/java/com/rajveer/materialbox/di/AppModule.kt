package com.rajveer.materialbox.di

import android.content.Context
import androidx.room.Room
import com.rajveer.materialbox.data.AppDatabase
import com.rajveer.materialbox.data.dao.MaterialDao
import com.rajveer.materialbox.data.dao.SubjectDao
import com.rajveer.materialbox.data.dao.TopicDao
import com.rajveer.materialbox.data.repository.MaterialRepository
import com.rajveer.materialbox.data.repository.SubjectRepository
import com.rajveer.materialbox.data.repository.TopicRepository
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
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideSubjectDao(database: AppDatabase): SubjectDao {
        return database.subjectDao()
    }

    @Provides
    @Singleton
    fun provideTopicDao(database: AppDatabase): TopicDao {
        return database.topicDao()
    }

    @Provides
    @Singleton
    fun provideMaterialDao(database: AppDatabase): MaterialDao {
        return database.materialDao()
    }

    @Provides
    @Singleton
    fun provideSubjectRepository(subjectDao: SubjectDao): SubjectRepository {
        return SubjectRepository(subjectDao)
    }

    @Provides
    @Singleton
    fun provideTopicRepository(topicDao: TopicDao): TopicRepository {
        return TopicRepository(topicDao)
    }

    @Provides
    @Singleton
    fun provideMaterialRepository(materialDao: MaterialDao): MaterialRepository {
        return MaterialRepository(materialDao)
    }
} 