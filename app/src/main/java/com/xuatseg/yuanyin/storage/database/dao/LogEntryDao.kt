package com.xuatseg.yuanyin.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.xuatseg.yuanyin.storage.database.entity.LogEntryEntity

/**
 * 日志数据访问对象
 */
@Dao
interface LogEntryDao {
    /**
     * 插入单条日志
     */
    @Insert
    suspend fun insertLogEntry(entry: LogEntryEntity)
    
    /**
     * 批量插入日志
     */
    @Insert
    suspend fun insertLogEntries(entries: List<LogEntryEntity>)
    
    /**
     * 按条件查询日志
     */
    @Query(
        """
        SELECT * FROM log_entries 
        WHERE timestamp BETWEEN :startTime AND :endTime
        AND (:levels IS NULL OR level IN (:levels))
        AND (:tags IS NULL OR tag IN (:tags))
        ORDER BY timestamp DESC 
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun queryLogs(
        startTime: Long, 
        endTime: Long, 
        levels: List<String>?, 
        tags: List<String>?, 
        limit: Int, 
        offset: Int
    ): List<LogEntryEntity>
    
    /**
     * 删除指定时间之前的日志
     */
    @Query("DELETE FROM log_entries WHERE timestamp < :timestamp")
    suspend fun deleteLogsOlderThan(timestamp: Long): Int
    
    /**
     * 按日志级别清理日志
     */
    @Query("DELETE FROM log_entries WHERE level IN (:levels)")
    suspend fun deleteLogsByLevel(levels: List<String>): Int
    
    /**
     * 获取日志总数
     */
    @Query("SELECT COUNT(*) FROM log_entries")
    suspend fun getLogCount(): Long
}