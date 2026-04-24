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
import com.rajveer.materialbox.data.dao.TopicChecklistDao
import com.rajveer.materialbox.data.dao.WatchedVideoDao
import com.rajveer.materialbox.data.dao.YoutubeFeedDao
import com.rajveer.materialbox.data.dao.RoadmapDao
import com.rajveer.materialbox.data.dao.SubjectStreakDao
import com.rajveer.materialbox.data.entity.RoadmapItem
import com.rajveer.materialbox.data.entity.SubjectStreak
import com.rajveer.materialbox.data.entity.CachedVideo
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.entity.Subject
import com.rajveer.materialbox.data.entity.Topic
import com.rajveer.materialbox.data.entity.TopicChecklistItem
import com.rajveer.materialbox.data.entity.WatchedVideo
import com.rajveer.materialbox.data.entity.YoutubeFeed

@Database(
    entities = [
        Subject::class, Topic::class, Material::class,
        YoutubeFeed::class, CachedVideo::class, WatchedVideo::class,
        RoadmapItem::class, SubjectStreak::class, TopicChecklistItem::class
    ],
    version = 12,
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
    abstract fun roadmapDao(): RoadmapDao
    abstract fun topicChecklistDao(): TopicChecklistDao
    abstract fun subjectStreakDao(): SubjectStreakDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE_NAME = "materialbox_database"

        private fun createMissingForeignKeyIndices(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_topics_subjectId ON topics(subjectId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_youtube_feeds_subjectId ON youtube_feeds(subjectId)")
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createMissingForeignKeyIndices(db)
            }
        }

        val MIGRATION_9_11 = object : Migration(9, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_9_10.migrate(db)
                MIGRATION_10_11.migrate(db)
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recover from previously released schemas that could miss these FK indices.
                createMissingForeignKeyIndices(db)
            }
        }

        val MIGRATION_10_12 = object : Migration(10, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_10_11.migrate(db)
                MIGRATION_11_12.migrate(db)
            }
        }

        val MIGRATION_9_12 = object : Migration(9, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_9_10.migrate(db)
                MIGRATION_10_11.migrate(db)
                MIGRATION_11_12.migrate(db)
            }
        }

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
                db.execSQL("CREATE INDEX IF NOT EXISTS index_youtube_feeds_subjectId ON youtube_feeds(subjectId)")
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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS roadmap_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        subjectId INTEGER NOT NULL,
                        text TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(subjectId) REFERENCES subjects(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_roadmap_items_subjectId ON roadmap_items(subjectId)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS subject_streaks (
                        subjectId INTEGER NOT NULL PRIMARY KEY,
                        currentStreak INTEGER NOT NULL,
                        longestStreak INTEGER NOT NULL,
                        lastActiveDate TEXT NOT NULL,
                        FOREIGN KEY(subjectId) REFERENCES subjects(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // To add a foreign key, we have to recreate the table in SQLite
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS roadmap_items_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        subjectId INTEGER NOT NULL,
                        parentId INTEGER,
                        text TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(subjectId) REFERENCES subjects(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(parentId) REFERENCES roadmap_items(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Copy data
                db.execSQL("""
                    INSERT INTO roadmap_items_new (id, subjectId, text, isCompleted, position, createdAt)
                    SELECT id, subjectId, text, isCompleted, position, createdAt FROM roadmap_items
                """.trimIndent())
                
                // Drop old table and rename new
                db.execSQL("DROP TABLE roadmap_items")
                db.execSQL("ALTER TABLE roadmap_items_new RENAME TO roadmap_items")
                
                // Recreate indices
                db.execSQL("CREATE INDEX IF NOT EXISTS index_roadmap_items_subjectId ON roadmap_items(subjectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_roadmap_items_parentId ON roadmap_items(parentId)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE roadmap_items ADD COLUMN isCollapsed INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS topic_checklist_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        topicId INTEGER NOT NULL,
                        parentId INTEGER,
                        text TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        isCollapsed INTEGER NOT NULL DEFAULT 0,
                        position INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(topicId) REFERENCES topics(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(parentId) REFERENCES topic_checklist_items(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_topic_checklist_items_topicId ON topic_checklist_items(topicId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_topic_checklist_items_parentId ON topic_checklist_items(parentId)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE cached_videos ADD COLUMN sourceUrl TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create the new table with updated schema
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS materials_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        topicId INTEGER,
                        subjectId INTEGER,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        pathOrUrl TEXT NOT NULL,
                        originalFileUri TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        viewCount INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(topicId) REFERENCES topics(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(subjectId) REFERENCES subjects(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                // 2. Copy data from the old table to the new table
                db.execSQL("""
                    INSERT INTO materials_new (id, topicId, type, title, pathOrUrl, originalFileUri, createdAt, updatedAt, viewCount)
                    SELECT id, topicId, type, title, pathOrUrl, originalFileUri, createdAt, updatedAt, viewCount FROM materials
                """.trimIndent())

                // 3. Drop the old table
                db.execSQL("DROP TABLE materials")

                // 4. Rename the new table to the original table name
                db.execSQL("ALTER TABLE materials_new RENAME TO materials")

                // 5. Recreate indices
                db.execSQL("CREATE INDEX IF NOT EXISTS index_materials_topicId ON materials(topicId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_materials_subjectId ON materials(subjectId)")
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                    MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8,
                    MIGRATION_8_9, MIGRATION_9_10, MIGRATION_9_11,
                    MIGRATION_10_11, MIGRATION_9_12, MIGRATION_10_12, MIGRATION_11_12
                )
                .build()
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
