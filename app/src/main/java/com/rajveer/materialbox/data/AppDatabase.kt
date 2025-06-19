package com.rajveer.materialbox.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rajveer.materialbox.data.converter.DateConverter
import com.rajveer.materialbox.data.converter.MaterialTypeConverter
import com.rajveer.materialbox.data.dao.MaterialDao
import com.rajveer.materialbox.data.dao.SubjectDao
import com.rajveer.materialbox.data.dao.TopicDao
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.entity.Subject
import com.rajveer.materialbox.data.entity.Topic

@Database(
    entities = [Subject::class, Topic::class, Material::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class, MaterialTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun topicDao(): TopicDao
    abstract fun materialDao(): MaterialDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "materialbox_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}