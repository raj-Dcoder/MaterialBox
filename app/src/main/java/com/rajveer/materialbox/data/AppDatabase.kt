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
import com.rajveer.materialbox.data.dao.MaterialDao
import com.rajveer.materialbox.data.dao.SubjectDao
import com.rajveer.materialbox.data.dao.TopicDao
import com.rajveer.materialbox.data.dao.YoutubeFeedDao
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.entity.Subject
import com.rajveer.materialbox.data.entity.Topic
import com.rajveer.materialbox.data.entity.YoutubeFeed

@Database(
    entities = [Subject::class, Topic::class, Material::class, YoutubeFeed::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(DateConverter::class, MaterialTypeConverter::class, ListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun topicDao(): TopicDao
    abstract fun materialDao(): MaterialDao
    abstract fun youtubeFeedDao(): YoutubeFeedDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE materials ADD COLUMN originalFileUri TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS youtube_feeds (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        subjectId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        channelUrls TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(subjectId) REFERENCES subjects(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "materialbox_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}