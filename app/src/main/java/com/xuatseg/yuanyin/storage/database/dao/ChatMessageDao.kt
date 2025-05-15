package com.xuatseg.yuanyin.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xuatseg.yuanyin.storage.database.entity.ChatMessageEntity

/**
 * 聊天消息数据访问对象
 */
@Dao
interface ChatMessageDao {
    /**
     * 插入单条消息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)
    
    /**
     * 批量插入消息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)
    
    /**
     * 按时间倒序获取消息
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessages(limit: Int, offset: Int): List<ChatMessageEntity>
    
    /**
     * 按发送者类型筛选消息
     */
    @Query("SELECT * FROM chat_messages WHERE senderType = :senderType ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesBySender(senderType: String, limit: Int, offset: Int): List<ChatMessageEntity>
    
    /**
     * 删除消息
     */
    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String): Int
    
    /**
     * 更新消息状态
     */
    @Query("UPDATE chat_messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String): Int
    
    /**
     * 按时间范围查询消息
     */
    @Query("SELECT * FROM chat_messages WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getMessagesByTimeRange(startTime: Long, endTime: Long): List<ChatMessageEntity>
}