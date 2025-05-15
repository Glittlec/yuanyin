package com.xuatseg.yuanyin.storage.file.impl

import android.content.Context
import com.google.gson.Gson
import com.xuatseg.yuanyin.storage.file.FileStorageError
import com.xuatseg.yuanyin.storage.file.ILogStorage
import com.xuatseg.yuanyin.storage.file.LogCleanupCriteria
import com.xuatseg.yuanyin.storage.file.LogEntry
import com.xuatseg.yuanyin.storage.file.LogLevel
import com.xuatseg.yuanyin.storage.file.LogQuery
import com.xuatseg.yuanyin.storage.file.util.FilePathValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

/**
 * 日志存储实现
 */
class LogStorageImpl(private val context: Context) : ILogStorage {
    
    private val gson = Gson()
    private val fileStorage = AndroidFileStorage(context)
    private val logDir = fileStorage.getDirectory(AndroidFileStorage.DirectoryType.LOG)
    
    init {
        // 确保日志目录存在
        logDir.mkdirs()
    }
    
    /**
     * 将Instant转换为LocalDate的兼容性方法
     */
    private fun instantToLocalDate(instant: Instant): LocalDate {
        return Date(instant.toEpochMilli())
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }
    
    /**
     * 写入日志
     */
    override suspend fun writeLog(entry: LogEntry) = withContext(Dispatchers.IO) {
        val logFile = getLogFileForDate(entry.timestamp)
        
        try {
            // 序列化日志条目
            val json = gson.toJson(entry)
            
            // 写入到文件
            FileWriter(logFile, true).use { writer ->
                PrintWriter(writer).println(json)
            }
        } catch (e: Exception) {
            throw FileStorageError.IOError("Failed to write log: ${e.message}")
        }
    }
    
    /**
     * 批量写入日志
     */
    override suspend fun writeLogBatch(entries: List<LogEntry>) = withContext(Dispatchers.IO) {
        if (entries.isEmpty()) return@withContext
        
        // 按日期分组日志条目
        val entriesByDate = entries.groupBy { entry ->
            instantToLocalDate(entry.timestamp)
        }
        
        // 为每个日期批量写入日志
        for ((date, dateEntries) in entriesByDate) {
            val logFile = getLogFileForDate(dateEntries.first().timestamp)
            
            try {
                FileWriter(logFile, true).use { writer ->
                    val printWriter = PrintWriter(writer)
                    
                    for (entry in dateEntries) {
                        val json = gson.toJson(entry)
                        printWriter.println(json)
                    }
                }
            } catch (e: Exception) {
                throw FileStorageError.IOError("Failed to write log batch: ${e.message}")
            }
        }
    }
    
    /**
     * 查询日志
     */
    override fun queryLogs(query: LogQuery): Flow<List<LogEntry>> = flow {
        val results = mutableListOf<LogEntry>()
        var count = 0
        
        // 确定日期范围
        val startDate = if (query.timeRange != null) {
            instantToLocalDate(query.timeRange.start)
        } else {
            LocalDate.now().minusDays(30) // 默认查询最近30天
        }
        
        val endDate = if (query.timeRange != null) {
            instantToLocalDate(query.timeRange.endInclusive)
        } else {
            LocalDate.now()
        }
        
        var currentDate = startDate
        
        // 遍历日期范围内的每个日志文件
        while (!currentDate.isAfter(endDate) && (query.limit == null || count < query.limit)) {
            val logFile = getLogFileForDate(
                currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            )
            
            if (logFile.exists()) {
                try {
                    BufferedReader(FileReader(logFile)).use { reader ->
                        var line: String?
                        
                        while (reader.readLine().also { line = it } != null && 
                               (query.limit == null || count < query.limit)) {
                            
                            try {
                                val entry = gson.fromJson(line, LogEntry::class.java)
                                
                                // 应用过滤条件
                                if (isEntryMatchingQuery(entry, query)) {
                                    if (count >= query.offset) {
                                        results.add(entry)
                                    }
                                    count++
                                }
                            } catch (e: Exception) {
                                // 跳过无法解析的行
                                continue
                            }
                        }
                    }
                } catch (e: Exception) {
                    throw FileStorageError.IOError("Failed to read log file: ${e.message}")
                }
            }
            
            currentDate = currentDate.plusDays(1)
        }
        
        emit(results)
    }.flowOn(Dispatchers.IO)
    
    /**
     * 清理日志
     */
    override suspend fun cleanupLogs(criteria: LogCleanupCriteria): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        
        // 按时间清理
        if (criteria.olderThan != null) {
            val cutoffDate = instantToLocalDate(criteria.olderThan)
            val logFiles = logDir.listFiles { file ->
                try {
                    // 尝试从文件名解析日期
                    val fileDate = parseLogFileDate(file)
                    fileDate.isBefore(cutoffDate)
                } catch (e: Exception) {
                    false
                }
            }
            
            logFiles?.forEach { file ->
                if (file.delete()) {
                    deletedCount++
                }
            }
        }
        
        // 如果还需要按级别或标签清理，遍历所有日志文件并重写
        if (criteria.levels != null || criteria.tags != null) {
            val logFiles = logDir.listFiles { file -> file.name.endsWith(".log") }
            
            logFiles?.forEach { file ->
                val tempFile = File(file.parentFile, "${file.name}.temp")
                var entriesDeleted = 0
                
                try {
                    // 读取并重写日志文件，跳过匹配条件的条目
                    BufferedReader(FileReader(file)).use { reader ->
                        PrintWriter(FileWriter(tempFile)).use { writer ->
                            var line: String?
                            
                            while (reader.readLine().also { line = it } != null) {
                                try {
                                    val entry = gson.fromJson(line, LogEntry::class.java)
                                    
                                    // 检查是否需要删除
                                    val shouldDelete = (criteria.levels != null && entry.level in criteria.levels) ||
                                                      (criteria.tags != null && entry.tag in criteria.tags)
                                    
                                    if (!shouldDelete) {
                                        writer.println(line)
                                    } else {
                                        entriesDeleted++
                                    }
                                } catch (e: Exception) {
                                    // 保留无法解析的行
                                    writer.println(line)
                                }
                            }
                        }
                    }
                    
                    // 替换原文件
                    if (entriesDeleted > 0) {
                        file.delete()
                        tempFile.renameTo(file)
                        deletedCount += entriesDeleted
                    } else {
                        tempFile.delete()
                    }
                } catch (e: Exception) {
                    tempFile.delete() // 清理临时文件
                    throw FileStorageError.IOError("Failed to clean up log file: ${e.message}")
                }
            }
        }
        
        // 检查是否需要按大小清理
        if (criteria.maxSize != null) {
            var totalSize = 0L
            val logFiles = logDir.listFiles()?.sortedBy { it.lastModified() } ?: emptyList()
            
            // 计算当前大小
            for (file in logFiles) {
                totalSize += file.length()
            }
            
            // 如果超过最大大小，删除最老的文件
            if (totalSize > criteria.maxSize) {
                for (file in logFiles) {
                    val fileSize = file.length()
                    if (file.delete()) {
                        deletedCount++
                        totalSize -= fileSize
                        
                        if (totalSize <= criteria.maxSize) {
                            break
                        }
                    }
                }
            }
        }
        
        deletedCount
    }
    
    /**
     * 获取日志文件
     */
    override fun getLogFile(date: Instant): File {
        return getLogFileForDate(date)
    }
    
    /**
     * 获取指定日期的日志文件
     */
    private fun getLogFileForDate(timestamp: Instant): File {
        val date = instantToLocalDate(timestamp)
        val fileName = date.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log"
        return File(logDir, fileName)
    }
    
    /**
     * 判断日志条目是否匹配查询条件
     */
    private fun isEntryMatchingQuery(entry: LogEntry, query: LogQuery): Boolean {
        // 时间范围检查
        if (query.timeRange != null && 
            (entry.timestamp.isBefore(query.timeRange.start) || entry.timestamp.isAfter(query.timeRange.endInclusive))) {
            return false
        }
        
        // 日志级别检查
        if (query.levels != null && entry.level !in query.levels) {
            return false
        }
        
        // 标签检查
        if (query.tags != null && entry.tag !in query.tags) {
            return false
        }
        
        // 消息内容检查
        if (query.messagePattern != null && !query.messagePattern.containsMatchIn(entry.message)) {
            return false
        }
        
        return true
    }
    
    /**
     * 从日志文件名解析日期
     */
    private fun parseLogFileDate(file: File): LocalDate {
        val datePart = file.nameWithoutExtension
        return LocalDate.parse(datePart, DateTimeFormatter.ISO_LOCAL_DATE)
    }
    
    companion object {
        private const val LOG_FILE_PREFIX = "app-log-"
        private const val LOG_FILE_EXTENSION = ".log"
    }
}