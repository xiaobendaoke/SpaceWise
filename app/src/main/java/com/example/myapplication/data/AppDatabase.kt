/**
 * Room 数据库定义。
 *
 * 职责：
 * - 定义数据库实体和版本。
 * - 提供数据库实例化的单例方法。
 * - 暴露 `AppDao` 接口。
 *
 * 上层用途：
 * - `SpaceViewModel` 和其他底层组件通过它获取数据操作入口。
 */
package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocationEntity::class,
        FolderEntity::class,
        ItemEntity::class,
        TagEntity::class,
        ItemTagCrossRef::class,
        PackingListEntity::class,
        PackingListItemEntity::class,
    ],
    version = 2,
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
                )
                    .fallbackToDestructiveMigration()  // 销毁性迁移，重新开始
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
