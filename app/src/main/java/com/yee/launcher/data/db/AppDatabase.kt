package com.yee.launcher.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yee.launcher.data.model.ItemInfo

@Database(entities = [ItemInfo::class], version = 1, exportSchema = false)
@TypeConverters(IntentConvert::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun desktopDao(): DesktopItemDao

    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }


}