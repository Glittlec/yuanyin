package com.xuatseg.yuanyin.storage.database.impl

import android.content.Context
import com.xuatseg.yuanyin.storage.database.AppDatabase
import com.xuatseg.yuanyin.storage.database.DatabaseError
import com.xuatseg.yuanyin.storage.database.IDatabase
import com.xuatseg.yuanyin.storage.database.IChatStorage
import com.xuatseg.yuanyin.storage.database.ISensorStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Room数据库实现类
 */
class RoomDatabaseImpl(private val context: Context) : IDatabase {
    
    // 懒加载数据库实例
    private val database by lazy { 
        try {
            AppDatabase.getDatabase(context)
        } catch (e: Exception) {
            throw DatabaseError.ConnectionError("Failed to initialize database: ${e.message}")
        }
    }
    
    // 懒加载存储实例
    private val sensorStorage by lazy { RoomSensorStorage(database.sensorDataDao()) }
    private val chatStorage by lazy { RoomChatStorage(database.chatMessageDao()) }
    
    /**
     * 初始化数据库
     */
    override suspend fun initialize() {
        try {
            // 触发数据库实例创建
            database.clearAllTables()
            
            // 预热查询 - 简单查询确保数据库已打开
            withContext(Dispatchers.IO) {
                database.chatMessageDao().getMessages(1, 0)
                database.sensorDataDao().getSensorDataCount()
            }
        } catch (e: Exception) {
            throw DatabaseError.ConnectionError("Failed to initialize database: ${e.message}")
        }
    }
    
    /**
     * 关闭数据库
     */
    override suspend fun close() {
        // Room数据库实例由Android管理，无需显式关闭
    }
    
    /**
     * 清理数据库
     */
    override suspend fun cleanup() {
        withContext(Dispatchers.IO) {
            try {
                // 清理旧数据 - 删除30天前的数据
                val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
                
                // 删除旧的传感器数据
                database.sensorDataDao().deleteSensorDataOlderThan(thirtyDaysAgo)
                
                // 执行数据库压缩
                database.query("VACUUM", null)
            } catch (e: Exception) {
                throw DatabaseError.StorageError("Failed to cleanup database: ${e.message}")
            }
        }
    }
    
    /**
     * 获取传感器数据存储
     */
    override fun getSensorStorage(): ISensorStorage {
        return sensorStorage
    }
    
    /**
     * 获取聊天消息存储
     */
    override fun getChatStorage(): IChatStorage {
        return chatStorage
    }
}