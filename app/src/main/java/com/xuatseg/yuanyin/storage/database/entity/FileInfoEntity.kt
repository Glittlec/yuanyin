package com.xuatseg.yuanyin.storage.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 文件信息实体
 */
@Entity(
    tableName = "file_info",
    indices = [Index(value = ["path"], unique = true)]
)
data class FileInfoEntity(
    @PrimaryKey
    val path: String,        // 文件路径作为主键
    val name: String,        // 文件名
    val size: Long,          // 文件大小
    val createdAt: Long,     // 创建时间
    val modifiedAt: Long,    // 修改时间
    val isDirectory: Boolean, // 是否为目录
    val attributes: String?   // JSON格式的文件属性
)