package com.xuatseg.yuanyin.storage.database.converter

import com.xuatseg.yuanyin.chat.ChatMessage
import com.xuatseg.yuanyin.chat.MessageContent
import com.xuatseg.yuanyin.chat.MessageSender
import com.xuatseg.yuanyin.chat.MessageStatus
import com.xuatseg.yuanyin.storage.database.entity.ChatMessageEntity
import com.xuatseg.yuanyin.storage.util.GsonProvider
import java.io.File

/**
 * 聊天消息转换器
 * 负责ChatMessage和ChatMessageEntity之间的转换
 */
object ChatMessageConverter {
    
    private val gson = GsonProvider.gson
    
    // 内容类型常量
    private const val CONTENT_TYPE_TEXT = "TEXT"
    private const val CONTENT_TYPE_VOICE = "VOICE"
    private const val CONTENT_TYPE_ERROR = "ERROR"
    
    // 发送者类型常量
    private const val SENDER_TYPE_USER = "USER"
    private const val SENDER_TYPE_BOT = "BOT"
    private const val SENDER_TYPE_SYSTEM = "SYSTEM"
    
    // 消息状态常量
    private const val STATUS_SENDING = "SENDING"
    private const val STATUS_SENT = "SENT"
    private const val STATUS_DELIVERED = "DELIVERED"
    private const val STATUS_READ = "READ"
    private const val STATUS_FAILED = "FAILED"
    
    /**
     * 将ChatMessage转换为ChatMessageEntity
     */
    fun toEntity(message: ChatMessage): ChatMessageEntity {
        // 处理内容类型
        val contentType = when (message.content) {
            is MessageContent.Text -> CONTENT_TYPE_TEXT
            is MessageContent.Voice -> CONTENT_TYPE_VOICE
            is MessageContent.Error -> CONTENT_TYPE_ERROR
        }
        
        // 处理发送者类型
        val senderType = when (message.sender) {
            is MessageSender.User -> SENDER_TYPE_USER
            is MessageSender.Bot -> SENDER_TYPE_BOT
            is MessageSender.System -> SENDER_TYPE_SYSTEM
        }
        
        // 处理状态
        val status = when (message.status) {
            is MessageStatus.Sending -> STATUS_SENDING
            is MessageStatus.Sent -> STATUS_SENT
            is MessageStatus.Delivered -> STATUS_DELIVERED
            is MessageStatus.Read -> STATUS_READ
            is MessageStatus.Failed -> STATUS_FAILED
        }
        
        // 处理文本内容
        val textContent = when (message.content) {
            is MessageContent.Text -> (message.content as MessageContent.Text).text
            is MessageContent.Error -> (message.content as MessageContent.Error).error
            else -> null
        }
        
        // 处理语音内容
        var audioFilePath: String? = null
        var duration: Long? = null
        var transcript: String? = null
        
        if (message.content is MessageContent.Voice) {
            val voiceContent = message.content as MessageContent.Voice
            audioFilePath = voiceContent.audioFile.absolutePath
            duration = voiceContent.duration
            transcript = voiceContent.transcript
        }
        
        // 序列化元数据
        val metadataJson = message.metadata?.let { gson.toJson(it) }
        
        return ChatMessageEntity(
            id = message.id,
            contentType = contentType,
            textContent = textContent,
            audioFilePath = audioFilePath,
            duration = duration,
            transcript = transcript,
            senderType = senderType,
            timestamp = message.timestamp,
            status = status,
            metadata = metadataJson
        )
    }
    
    /**
     * 将ChatMessageEntity转换为ChatMessage
     */
    fun toChatMessage(entity: ChatMessageEntity): ChatMessage {
        // 处理内容
        val content = when (entity.contentType) {
            CONTENT_TYPE_TEXT -> MessageContent.Text(entity.textContent ?: "")
            CONTENT_TYPE_VOICE -> {
                val file = entity.audioFilePath?.let { File(it) } ?: File("")
                MessageContent.Voice(
                    audioFile = file,
                    duration = entity.duration ?: 0L,
                    transcript = entity.transcript
                )
            }
            CONTENT_TYPE_ERROR -> MessageContent.Error(entity.textContent ?: "Unknown error")
            else -> MessageContent.Error("Unknown content type: ${entity.contentType}")
        }
        
        // 处理发送者
        val sender = when (entity.senderType) {
            SENDER_TYPE_USER -> MessageSender.User
            SENDER_TYPE_BOT -> MessageSender.Bot
            SENDER_TYPE_SYSTEM -> MessageSender.System("system")
            else -> MessageSender.System("unknown")
        }
        
        // 处理状态
        val status = when (entity.status) {
            STATUS_SENDING -> MessageStatus.Sending
            STATUS_SENT -> MessageStatus.Sent
            STATUS_DELIVERED -> MessageStatus.Delivered
            STATUS_READ -> MessageStatus.Read
            STATUS_FAILED -> MessageStatus.Failed("Unknown failure")
            else -> MessageStatus.Failed("Unknown status: ${entity.status}")
        }
        
        // 反序列化元数据
        val metadata: Map<String, Any>? = entity.metadata?.let {
            try {
                @Suppress("UNCHECKED_CAST")
                gson.fromJson(it, Map::class.java) as Map<String, Any>
            } catch (e: Exception) {
                null
            }
        }
        
        return ChatMessage(
            id = entity.id,
            content = content,
            sender = sender,
            timestamp = entity.timestamp,
            status = status,
            metadata = metadata
        )
    }
    
    /**
     * 转换聊天消息列表
     */
    fun toChatMessageList(entities: List<ChatMessageEntity>): List<ChatMessage> {
        return entities.map { toChatMessage(it) }
    }
    
    /**
     * 转换为实体列表
     */
    fun toEntityList(messages: List<ChatMessage>): List<ChatMessageEntity> {
        return messages.map { toEntity(it) }
    }
}