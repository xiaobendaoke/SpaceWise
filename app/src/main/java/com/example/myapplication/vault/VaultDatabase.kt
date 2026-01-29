/**
 * 保险箱独立数据库
 */
package com.example.myapplication.vault

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VaultItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    
    abstract fun vaultDao(): VaultDao
    
    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null
        
        fun getInstance(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "vault.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
