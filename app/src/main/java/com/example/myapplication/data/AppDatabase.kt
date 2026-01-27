package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SpaceEntity::class,
        SpotEntity::class,
        ItemEntity::class,
        TagEntity::class,
        ItemTagCrossRef::class,
        PackingListEntity::class,
        PackingListItemEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myapplication.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
