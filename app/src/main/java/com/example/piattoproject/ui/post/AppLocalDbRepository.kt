package com.example.piattoproject.ui.post

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Post::class], version = 4)
abstract class AppLocalDbRepository : RoomDatabase() {
    abstract fun postDao(): PostDao

    companion object {
        @Volatile
        private var instance: AppLocalDbRepository? = null

        fun getInstance(context: Context): AppLocalDbRepository {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppLocalDbRepository::class.java,
                    "piatto_db.db"
                )
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build().also { instance = it }
            }
        }
    }
}
