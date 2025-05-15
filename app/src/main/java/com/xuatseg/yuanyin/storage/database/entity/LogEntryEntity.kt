package com.xuatseg.yuanyin.storage.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 日志实体
 */
@Entity(
    tableName = "log_entries",
    indices = [Index(value = ["timestamp", "level", "tag"])]
)
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,    // 毫秒时间戳
    val level: String,      // 日志级别
    val tag: String,        // 日志标签
    val message: String,    // 日志消息内容
    val metadata: String?   // JSON格式的元数据
)