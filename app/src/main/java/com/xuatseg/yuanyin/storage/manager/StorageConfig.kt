package com.xuatseg.yuanyin.storage.manager

import android.content.Context
import com.xuatseg.yuanyin.storage.BackupPolicy
import com.xuatseg.yuanyin.storage.CleanupPolicy
import com.xuatseg.yuanyin.storage.DatabaseConfig
import com.xuatseg.yuanyin.storage.FileSystemConfig
import com.xuatseg.yuanyin.storage.IStorageConfig
import com.xuatseg.yuanyin.storage.MaintenanceConfig
import com.xuatseg.yuanyin.storage.MaintenanceSchedule
import com.xuatseg.yuanyin.storage.RetryPolicy
import java.io.File

/**
 * 存储配置实现
 */
class StorageConfig(private val context: Context) : IStorageConfig {
    
    /**
     * 获取数据库配置
     */
    override fun getDatabaseConfig(): DatabaseConfig {
        return DatabaseConfig(
            connectionString = "yuanyin_database",
            maxConnections = 5,
            timeout = 30000, // 30秒
            retryPolicy = RetryPolicy(
                maxAttempts = 3,
                initialDelay = 1000,
                maxDelay = 10000,
                backoffMultiplier = 1.5f
            )
        )
    }
    
    /**
     * 获取文件系统配置
     */
    override fun getFileSystemConfig(): FileSystemConfig {
        val rootPath = context.filesDir.absolutePath
        
        return FileSystemConfig(
            rootPath = rootPath,
            maxFileSize = 50 * 1024 * 1024L, // 50MB
            quotaSize = 500 * 1024 * 1024L, // 500MB
            encryptionEnabled = false // 简化版不启用加密
        )
    }
    
    /**
     * 获取维护配置
     */
    override fun getMaintenanceConfig(): MaintenanceConfig {
        return MaintenanceConfig(
            schedule = MaintenanceSchedule(
                interval = 24 * 60 * 60 * 1000L, // 每24小时
                startTime = "03:00", // 凌晨3点
                maxDuration = 30 * 60 * 1000L // 最长30分钟
            ),
            cleanupPolicy = CleanupPolicy(
                maxAge = 30 * 24 * 60 * 60 * 1000L, // 30天
                maxSize = 400 * 1024 * 1024L, // 400MB
                priorities = mapOf(
                    "logs" to 1, // 日志最先清理
                    "temp" to 2, // 临时文件次之
                    "cache" to 3  // 缓存文件再次
                )
            ),
            backupPolicy = BackupPolicy(
                enabled = false, // 简化版不启用备份
                interval = 7 * 24 * 60 * 60 * 1000L, // 每7天
                retention = 3, // 保留3个备份
                location = "${context.getExternalFilesDir(null)?.absolutePath}/backups"
            )
        )
    }
} 