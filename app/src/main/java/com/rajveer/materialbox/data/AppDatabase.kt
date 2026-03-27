package com.rajveer.materialbox.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.rajveer.materialbox.data.converter.DateConverter
import com.rajveer.materialbox.data.converter.ListConverter
import com.rajveer.materialbox.data.converter.MaterialTypeConverter
import com.rajveer.materialbox.data.dao.CachedVideoDao
import com.rajveer.materialbox.data.dao.MaterialDao
import com.rajveer.materialbox.data.dao.SubjectDao
import com.rajveer.materialbox.data.dao.TopicDao
import com.rajveer.materialbox.data.dao.WatchedVideoDao
import com.rajveer.materialbox.data.dao.YoutubeFeedDao
import com.rajveer.materialbox.data.entity.CachedVideo
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.entity.Subject
import com.rajveer.materialbox.data.entity.Topic
import com.rajveer.materialbox.data.entity.WatchedVideo
import com.rajveer.materialbox.data.entity.YoutubeFeed

@Database(
    entities = [
        Subject::class, Topic::class, Material::class,
        YoutubeFeed::class, CachedVideo::class, WatchedVideo::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(DateConverter::class, MaterialTypeConverter::class, ListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun topicDao(): TopicDao
    abstract fun materialDao(): MaterialDao
    abstract fun youtubeFeedDao(): YoutubeFeedDao
    abstract fun cachedVideoDao(): CachedVideoDao
    abstract fun watchedVideoDao(): WatchedVideoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE materials ADD COLUMN originalFileUri TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS youtube_feeds (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        subjectId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        channelUrls TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(subjectId) REFERENCES subjects(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cached_videos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        feedId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        videoUrl TEXT NOT NULL,
                        thumbnailUrl TEXT NOT NULL,
                        publishedAt INTEGER NOT NULL,
                        channelName TEXT NOT NULL,
                        cachedAt INTEGER NOT NULL,
                        FOREIGN KEY(feedId) REFERENCES youtube_feeds(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cached_videos_feedId ON cached_videos(feedId)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS watched_videos (
                        videoUrl TEXT NOT NULL PRIMARY KEY,
                        watchedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "materialbox_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}