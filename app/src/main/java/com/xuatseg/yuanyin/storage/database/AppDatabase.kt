package com.xuatseg.yuanyin.storage.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.xuatseg.yuanyin.storage.database.converter.Converters
import com.xuatseg.yuanyin.storage.database.dao.ChatMessageDao
import com.xuatseg.yuanyin.storage.database.dao.FileInfoDao
import com.xuatseg.yuanyin.storage.database.dao.LogEntryDao
import com.xuatseg.yuanyin.storage.database.dao.SensorDataDao
import com.xuatseg.yuanyin.storage.database.entity.ChatMessageEntity
import com.xuatseg.yuanyin.storage.database.entity.FileInfoEntity
import com.xuatseg.yuanyin.storage.database.entity.LogEntryEntity
import com.xuatseg.yuanyin.storage.database.entity.SensorDataEntity
import com.xuatseg.yuanyin.storage.database.migration.DatabaseMigrations

/**
 * 应用数据库
 */
@Database(
    entities = [
        ChatMessageEntity::class,
        SensorDataEntity::class,
        LogEntryEntity::class,
        FileInfoEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    /**
     * 获取聊天消息DAO
     */
    abstract fun chatMessageDao(): ChatMessageDao
    
    /**
     * 获取传感器数据DAO
     */
    abstract fun sensorDataDao(): SensorDataDao
    
    /**
     * 获取日志DAO
     */
    abstract fun logEntryDao(): LogEntryDao
    
    /**
     * 获取文件信息DAO
     */
    abstract fun fileInfoDao(): FileInfoDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * 获取数据库实例
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yuanyin_database"
                )
                // 这里可以添加迁移策略，例如：
                // .addMigrations(DatabaseMigrations.MIGRATION_1_2)
                // 如果没有可用的迁移，则回退到破坏性迁移
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}