package com.xuatseg.yuanyin.storage.database.impl

import com.xuatseg.yuanyin.chat.ChatMessage
import com.xuatseg.yuanyin.storage.database.ChatMessageQuery
import com.xuatseg.yuanyin.storage.database.DatabaseError
import com.xuatseg.yuanyin.storage.database.DeleteCriteria
import com.xuatseg.yuanyin.storage.database.IChatStorage
import com.xuatseg.yuanyin.storage.database.converter.ChatMessageConverter
import com.xuatseg.yuanyin.storage.database.dao.ChatMessageDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Room聊天存储实现
 */
class RoomChatStorage(private val chatMessageDao: ChatMessageDao) : IChatStorage {
    
    /**
     * 保存聊天消息
     */
    override suspend fun saveMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        try {
            val entity = ChatMessageConverter.toEntity(message)
            chatMessageDao.insertMessage(entity)
        } catch (e: Exception) {
            throw DatabaseError.StorageError("Failed to save message: ${e.message}")
        }
    }
    
    /**
     * 批量保存聊天消息
     */
    override suspend fun saveMessageBatch(messages: List<ChatMessage>) = withContext(Dispatchers.IO) {
        if (messages.isEmpty()) return@withContext
        
        try {
            val entities = ChatMessageConverter.toEntityList(messages)
            chatMessageDao.insertMessages(entities)
        } catch (e: Exception) {
            throw DatabaseError.StorageError("Failed to save message batch: ${e.message}")
        }
    }
    
    /**
     * 查询聊天消息
     */
    override fun queryMessages(query: ChatMessageQuery): Flow<List<ChatMessage>> = flow {
        try {
            val entities = if (query.timeRange != null) {
                // 按时间范围查询
                val startTime = query.timeRange.start.toEpochMilli()
                val endTime = query.timeRange.endInclusive.toEpochMilli()
                chatMessageDao.getMessagesByTimeRange(startTime, endTime)
            } else if (query.senderIds != null && query.senderIds.isNotEmpty()) {
                // 按发送者查询 - 目前只支持一种发送者类型
                val senderType = query.senderIds.first()
                chatMessageDao.getMessagesBySender(
                    senderType,
                    query.limit ?: 100,
                    query.offset
                )
            } else {
                // 默认查询
                chatMessageDao.getMessages(
                    query.limit ?: 100,
                    query.offset
                )
            }
            
            // 转换结果
            val result = ChatMessageConverter.toChatMessageList(entities)
            emit(result)
        } catch (e: Exception) {
            throw DatabaseError.QueryError("Failed to query messages: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 删除聊天消息
     */
    override suspend fun deleteMessages(criteria: DeleteCriteria): Int = withContext(Dispatchers.IO) {
        try {
            var deletedCount = 0
            
            // 按ID删除
            if (criteria.ids != null && criteria.ids.isNotEmpty()) {
                for (id in criteria.ids) {
                    deletedCount += chatMessageDao.deleteMessage(id)
                }
            }
            
            deletedCount
        } catch (e: Exception) {
            throw DatabaseError.StorageError("Failed to delete messages: ${e.message}")
        }
    }
}