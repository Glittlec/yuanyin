package com.xuatseg.yuanyin.storage.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 聊天消息实体
 */
@Entity(
    tableName = "chat_messages",
    indices = [Index("timestamp")]
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val contentType: String,  // TEXT, VOICE, ERROR
    val textContent: String?,
    val audioFilePath: String?,
    val duration: Long?,
    val transcript: String?,
    val senderType: String,  // USER, BOT, SYSTEM
    val timestamp: Long,
    val status: String,  // SENDING, SENT, DELIVERED, READ, FAILED
    val metadata: String?  // JSON格式的元数据
)