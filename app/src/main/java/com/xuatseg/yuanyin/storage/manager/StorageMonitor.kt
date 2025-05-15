package com.xuatseg.yuanyin.storage.manager

import com.xuatseg.yuanyin.storage.DatabaseStatus
import com.xuatseg.yuanyin.storage.FileSystemStatus
import com.xuatseg.yuanyin.storage.IStorageMonitor
import com.xuatseg.yuanyin.storage.PerformanceMetrics
import com.xuatseg.yuanyin.storage.StorageMetrics
import com.xuatseg.yuanyin.storage.database.IDatabase
import com.xuatseg.yuanyin.storage.file.IFileStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 存储监控器实现
 */
class StorageMonitor(
    private val database: IDatabase,
    private val fileStorage: IFileStorage
) : IStorageMonitor {
    
    // 监控作用域
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 监控job
    private var monitoringJob: Job? = null
    
    // 监控状态
    private val isMonitoring = AtomicBoolean(false)
    
    // 性能指标流
    private val _performanceFlow = MutableSharedFlow<PerformanceMetrics>(replay = 1)
    
    // 存储指标
    private var currentMetrics = StorageMetrics(
        databaseMetrics = emptyMap(),
        fileSystemMetrics = emptyMap(),
        errorMetrics = emptyMap(),
        performanceMetrics = PerformanceMetrics(0, 0, 0f, 0f)
    )
    
    // 最后采集时间
    private var lastCollectionTime = System.currentTimeMillis()
    
    /**
     * 开始监控
     */
    override fun startMonitoring() {
        if (isMonitoring.getAndSet(true)) {
            // 已经在监控中
            return
        }
        
        monitoringJob = scope.launch {
            // 初始采集
            collectMetrics()
            
            // 定期采集
            while (isActive) {
                // 等待监控间隔
                delay(MONITORING_INTERVAL)
                
                // 采集指标
                collectMetrics()
            }
        }
    }
    
    /**
     * 停止监控
     */
    override fun stopMonitoring() {
        if (!isMonitoring.getAndSet(false)) {
            // 未在监控
            return
        }
        
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    /**
     * 获取监控指标
     */
    override fun getMetrics(): StorageMetrics {
        return currentMetrics
    }
    
    /**
     * 观察性能指标
     */
    override fun observePerformance(): Flow<PerformanceMetrics> {
        return _performanceFlow.asSharedFlow()
    }
    
    /**
     * 采集监控指标
     */
    private suspend fun collectMetrics() {
        try {
            // 计算采集间隔
            val now = System.currentTimeMillis()
            val interval = now - lastCollectionTime
            lastCollectionTime = now
            
            // 采集数据库指标
            val dbMetrics = collectDatabaseMetrics()
            
            // 采集文件系统指标
            val fsMetrics = collectFileSystemMetrics()
            
            // 更新错误指标
            val errorMetrics = currentMetrics.errorMetrics // 保持当前错误指标
            
            // 更新性能指标
            val performanceMetrics = PerformanceMetrics(
                readLatency = calculateAverageReadLatency(dbMetrics, fsMetrics),
                writeLatency = calculateAverageWriteLatency(dbMetrics, fsMetrics),
                operationsPerSecond = calculateOperationsPerSecond(dbMetrics, fsMetrics, interval),
                errorRate = calculateErrorRate(errorMetrics)
            )
            
            // 更新存储指标
            currentMetrics = StorageMetrics(
                databaseMetrics = dbMetrics,
                fileSystemMetrics = fsMetrics,
                errorMetrics = errorMetrics,
                performanceMetrics = performanceMetrics
            )
            
            // 发布性能指标
            _performanceFlow.emit(performanceMetrics)
            
        } catch (e: Exception) {
            // 记录错误，但不中断监控
            val errorMetrics = currentMetrics.errorMetrics.toMutableMap()
            val errorType = e.javaClass.simpleName
            errorMetrics[errorType] = (errorMetrics[errorType] ?: 0) + 1
            
            // 更新错误指标
            currentMetrics = currentMetrics.copy(errorMetrics = errorMetrics)
        }
    }
    
    /**
     * 采集数据库指标
     */
    private fun collectDatabaseMetrics(): Map<String, Float> {
        // 实际实现会查询数据库统计信息
        // 简化版只返回基本指标
        return mapOf(
            "queryLatency" to 10f,
            "writeLatency" to 20f,
            "readCount" to 100f,
            "writeCount" to 50f,
            "cacheHitRate" to 0.8f
        )
    }
    
    /**
     * 采集文件系统指标
     */
    private fun collectFileSystemMetrics(): Map<String, Float> {
        // 实际实现会查询文件系统统计信息
        // 简化版只返回基本指标
        return mapOf(
            "readLatency" to 5f,
            "writeLatency" to 15f,
            "readCount" to 80f,
            "writeCount" to 40f,
            "freeSpacePercentage" to 0.7f
        )
    }
    
    /**
     * 计算平均读取延迟
     */
    private fun calculateAverageReadLatency(
        dbMetrics: Map<String, Float>,
        fsMetrics: Map<String, Float>
    ): Long {
        val dbLatency = dbMetrics["queryLatency"] ?: 0f
        val fsLatency = fsMetrics["readLatency"] ?: 0f
        
        return ((dbLatency + fsLatency) / 2).toLong()
    }
    
    /**
     * 计算平均写入延迟
     */
    private fun calculateAverageWriteLatency(
        dbMetrics: Map<String, Float>,
        fsMetrics: Map<String, Float>
    ): Long {
        val dbLatency = dbMetrics["writeLatency"] ?: 0f
        val fsLatency = fsMetrics["writeLatency"] ?: 0f
        
        return ((dbLatency + fsLatency) / 2).toLong()
    }
    
    /**
     * 计算每秒操作数
     */
    private fun calculateOperationsPerSecond(
        dbMetrics: Map<String, Float>,
        fsMetrics: Map<String, Float>,
        intervalMs: Long
    ): Float {
        val dbReadCount = dbMetrics["readCount"] ?: 0f
        val dbWriteCount = dbMetrics["writeCount"] ?: 0f
        val fsReadCount = fsMetrics["readCount"] ?: 0f
        val fsWriteCount = fsMetrics["writeCount"] ?: 0f
        
        val totalOperations = dbReadCount + dbWriteCount + fsReadCount + fsWriteCount
        val intervalSeconds = intervalMs / 1000f
        
        return if (intervalSeconds > 0) totalOperations / intervalSeconds else 0f
    }
    
    /**
     * 计算错误率
     */
    private fun calculateErrorRate(errorMetrics: Map<String, Int>): Float {
        val totalErrors = errorMetrics.values.sum()
        
        // 总操作数估计值 (简化版)
        val totalOperations = 1000
        
        return if (totalOperations > 0) 
            totalErrors.toFloat() / totalOperations
        else 
            0f
    }
    
    companion object {
        // 监控间隔 (30秒)
        private const val MONITORING_INTERVAL = 30 * 1000L
    }
} 