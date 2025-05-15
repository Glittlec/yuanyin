package com.xuatseg.yuanyin.storage.manager

import android.content.Context
import com.xuatseg.yuanyin.storage.IStorageManager
import com.xuatseg.yuanyin.storage.StorageError
import com.xuatseg.yuanyin.storage.StorageEvent
import com.xuatseg.yuanyin.storage.StorageStatus
import com.xuatseg.yuanyin.storage.DatabaseStatus
import com.xuatseg.yuanyin.storage.FileSystemStatus
import com.xuatseg.yuanyin.storage.StorageUsage
import com.xuatseg.yuanyin.storage.PerformanceMetrics
import com.xuatseg.yuanyin.storage.EventType
import com.xuatseg.yuanyin.storage.database.IDatabase
import com.xuatseg.yuanyin.storage.database.impl.RoomDatabaseImpl
import com.xuatseg.yuanyin.storage.file.IFileStorage
import com.xuatseg.yuanyin.storage.file.impl.AndroidFileStorage
import com.xuatseg.yuanyin.storage.file.impl.FileSystemMonitorImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 存储管理器实现
 */
class StorageManager(private val context: Context) : IStorageManager {
    
    // 协程作用域，用于异步操作
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 初始化标志
    private val initialized = AtomicBoolean(false)
    
    // 最后维护时间
    private var lastMaintenanceTime: Instant = Instant.EPOCH
    
    // 事件流
    private val _storageEvents = MutableSharedFlow<StorageEvent>(replay = 0)
    
    // 懒加载数据库实例 - 重命名为_database避免与getDatabase()方法冲突
    private val _database: IDatabase by lazy {
        RoomDatabaseImpl(context)
    }
    
    // 懒加载文件存储实例 - 重命名为_fileStorage避免与getFileStorage()方法冲突
    private val _fileStorage: IFileStorage by lazy {
        AndroidFileStorage(context)
    }
    
    // 文件系统监视器
    private val fileSystemMonitor by lazy {
        FileSystemMonitorImpl(context)
    }
    
    /**
     * 初始化存储系统
     */
    override suspend fun initialize() = withContext(Dispatchers.IO) {
        if (initialized.getAndSet(true)) {
            // 已经初始化过，忽略
            return@withContext
        }
        
        try {
            // 初始化数据库
            _database.initialize()
            
            // 初始化文件存储
            (_fileStorage as AndroidFileStorage).ensureDirectoriesExist()
            
            // 记录初始化时间
            lastMaintenanceTime = Instant.now()
            
            // 发布初始化事件
            emitEvent(StorageEvent.DatabaseEvent(EventType.INITIALIZED, "Database initialized"))
            emitEvent(StorageEvent.FileSystemEvent(EventType.INITIALIZED, "File system initialized"))
            
        } catch (e: Exception) {
            // 重置初始化标志
            initialized.set(false)
            
            // 转换为StorageError并发布错误事件
            val error = convertToStorageError(e)
            emitEvent(StorageEvent.ErrorEvent(error, "Failed to initialize storage system"))
            
            // 重新抛出原始异常
            throw e
        }
    }
    
    /**
     * 关闭存储系统
     */
    override suspend fun shutdown() = withContext(Dispatchers.IO) {
        if (!initialized.getAndSet(false)) {
            // 未初始化，忽略
            return@withContext
        }
        
        try {
            // 关闭数据库
            _database.close()
            
            // 停止文件系统监控
            fileSystemMonitor.stopMonitoring()
            
            // 发布关闭事件
            emitEvent(StorageEvent.DatabaseEvent(EventType.DISCONNECTED, "Database closed"))
            emitEvent(StorageEvent.FileSystemEvent(EventType.DISCONNECTED, "File system disconnected"))
            
        } catch (e: Exception) {
            // 转换为StorageError并发布错误事件
            val error = convertToStorageError(e)
            emitEvent(StorageEvent.ErrorEvent(error, "Failed to shutdown storage system"))
            
            // 重新抛出原始异常
            throw e
        }
    }
    
    /**
     * 获取数据库实例
     */
    override fun getDatabase(): IDatabase {
        checkInitialized()
        return _database
    }
    
    /**
     * 获取文件存储实例
     */
    override fun getFileStorage(): IFileStorage {
        checkInitialized()
        return _fileStorage
    }
    
    /**
     * 执行存储维护
     */
    override suspend fun performMaintenance() = withContext(Dispatchers.IO) {
        checkInitialized()
        
        try {
            // 发布维护开始事件
            emitEvent(StorageEvent.MaintenanceEvent(EventType.MAINTENANCE_STARTED, "Storage maintenance started"))
            
            // 执行数据库清理
            cleanupDatabase()
            
            // 执行文件系统清理
            cleanupFileSystem()
            
            // 更新最后维护时间
            lastMaintenanceTime = Instant.now()
            
            // 发布维护完成事件
            emitEvent(StorageEvent.MaintenanceEvent(EventType.MAINTENANCE_COMPLETED, "Storage maintenance completed"))
            
        } catch (e: Exception) {
            // 转换为StorageError并发布错误事件
            val error = convertToStorageError(e)
            emitEvent(StorageEvent.ErrorEvent(error, "Failed to perform maintenance"))
            
            // 重新抛出原始异常
            throw e
        }
    }
    
    /**
     * 获取存储状态
     */
    override fun getStorageStatus(): StorageStatus {
        checkInitialized()
        
        return StorageStatus(
            databaseStatus = getDatabaseStatus(),
            fileSystemStatus = getFileSystemStatus(),
            totalStorageUsage = calculateTotalStorageUsage(),
            lastMaintenance = lastMaintenanceTime
        )
    }
    
    /**
     * 观察存储事件
     */
    override fun observeStorageEvents(): Flow<StorageEvent> {
        return _storageEvents.asSharedFlow()
    }
    
    /**
     * 检查是否已初始化
     */
    private fun checkInitialized() {
        if (!initialized.get()) {
            throw IllegalStateException("Storage manager is not initialized")
        }
    }
    
    /**
     * 发布存储事件
     */
    private fun emitEvent(event: StorageEvent) {
        scope.launch {
            _storageEvents.emit(event)
        }
    }
    
    /**
     * 获取数据库状态
     */
    private fun getDatabaseStatus(): DatabaseStatus {
        try {
            // 这里应该从数据库获取更详细的信息
            // 简化版本只提供基本信息
            
            return DatabaseStatus(
                isConnected = initialized.get(),
                version = 1, // 从数据库获取实际版本
                tableCount = 4, // 估计表数量
                totalRecords = estimateTotalRecords(),
                storageUsage = calculateDatabaseStorageUsage(),
                performance = PerformanceMetrics(
                    readLatency = 0,
                    writeLatency = 0,
                    operationsPerSecond = 0f,
                    errorRate = 0f
                )
            )
        } catch (e: Exception) {
            // 如果获取状态失败，返回断开连接状态
            return DatabaseStatus(
                isConnected = false,
                version = 0,
                tableCount = 0,
                totalRecords = 0,
                storageUsage = StorageUsage(0, 0, 0, 0f),
                performance = PerformanceMetrics(0, 0, 0f, 0f)
            )
        }
    }
    
    /**
     * 获取文件系统状态
     */
    private fun getFileSystemStatus(): FileSystemStatus {
        try {
            val storageStats = fileSystemMonitor.getStorageStats()
            
            return FileSystemStatus(
                isAvailable = true,
                rootDirectory = context.filesDir.absolutePath,
                fileCount = storageStats.fileCount,
                storageUsage = StorageUsage(
                    used = storageStats.usedSpace,
                    available = storageStats.freeSpace,
                    total = storageStats.totalSpace,
                    usagePercentage = if (storageStats.totalSpace > 0) 
                        storageStats.usedSpace.toFloat() / storageStats.totalSpace 
                    else 
                        0f
                ),
                performance = PerformanceMetrics(
                    readLatency = 0,
                    writeLatency = 0,
                    operationsPerSecond = 0f,
                    errorRate = 0f
                )
            )
        } catch (e: Exception) {
            // 如果获取状态失败，返回不可用状态
            return FileSystemStatus(
                isAvailable = false,
                rootDirectory = context.filesDir.absolutePath,
                fileCount = 0,
                storageUsage = StorageUsage(0, 0, 0, 0f),
                performance = PerformanceMetrics(0, 0, 0f, 0f)
            )
        }
    }
    
    /**
     * 计算总存储使用情况
     */
    private fun calculateTotalStorageUsage(): StorageUsage {
        val dbUsage = calculateDatabaseStorageUsage()
        val fileUsage = getFileSystemStatus().storageUsage
        
        val total = fileUsage.total // 以文件系统总空间为准
        val used = dbUsage.used + fileUsage.used
        val available = total - used
        
        return StorageUsage(
            used = used,
            available = available,
            total = total,
            usagePercentage = if (total > 0) used.toFloat() / total else 0f
        )
    }
    
    /**
     * 计算数据库存储使用情况
     */
    private fun calculateDatabaseStorageUsage(): StorageUsage {
        // 估计数据库文件大小
        val dbFile = getDatabaseFile()
        val used = if (dbFile.exists()) dbFile.length() else 0
        
        // 获取存储卷信息
        val fileSystem = context.filesDir.absoluteFile.toPath().fileSystem
        val fileStore = fileSystem.getFileStores().iterator().next()
        
        val total = fileStore.totalSpace
        val available = fileStore.usableSpace
        
        return StorageUsage(
            used = used,
            available = available,
            total = total,
            usagePercentage = if (total > 0) used.toFloat() / total else 0f
        )
    }
    
    /**
     * 获取数据库文件
     */
    private fun getDatabaseFile(): File {
        // Room数据库文件通常位于应用数据目录下
        return File(context.getDatabasePath("yuanyin_database").absolutePath)
    }
    
    /**
     * 估计总记录数
     */
    private fun estimateTotalRecords(): Long {
        // 在实际实现中，可以从数据库查询各表的记录数
        // 这里返回估计值
        return 0
    }
    
    /**
     * 清理数据库
     */
    private suspend fun cleanupDatabase() {
        try {
            // 清理30天前的数据
            val thirtyDaysAgo = Instant.now().minusSeconds(30 * 24 * 60 * 60)
            
            // 执行数据库清理
            _database.cleanup()
            
            // 日志清理和其他维护操作
        } catch (e: Exception) {
            // 将异常转换为更具体的错误类型
            when (e) {
                is com.xuatseg.yuanyin.storage.database.DatabaseError -> throw e
                else -> throw IllegalStateException("Database cleanup failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * 清理文件系统
     */
    private suspend fun cleanupFileSystem() {
        try {
            // 获取文件存储实例
            val androidFileStorage = _fileStorage as AndroidFileStorage
            
            // 清理临时文件目录
            val tempDir = androidFileStorage.getDirectory(AndroidFileStorage.DirectoryType.TEMP)
            cleanupDirectory(tempDir, 1) // 清理1天前的临时文件
            
            // 清理日志文件
            val logDir = androidFileStorage.getDirectory(AndroidFileStorage.DirectoryType.LOG)
            cleanupDirectory(logDir, 30) // 清理30天前的日志
        } catch (e: Exception) {
            // 将异常转换为更具体的错误类型
            when (e) {
                is com.xuatseg.yuanyin.storage.file.FileStorageError -> throw e
                else -> throw IllegalStateException("File system cleanup failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * 清理目录中指定天数前的文件
     */
    private fun cleanupDirectory(directory: File, days: Int) {
        if (!directory.exists() || !directory.isDirectory) {
            return
        }
        
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)
        
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoffTime) {
                file.delete()
            }
        }
    }
    
    /**
     * 将异常转换为StorageError
     */
    private fun convertToStorageError(e: Exception): StorageError {
        return when (e) {
            is com.xuatseg.yuanyin.storage.database.DatabaseError -> {
                val dbError = e as com.xuatseg.yuanyin.storage.database.DatabaseError
                StorageError.DatabaseError(dbError)
            }
            is com.xuatseg.yuanyin.storage.file.FileStorageError -> {
                val fileError = e as com.xuatseg.yuanyin.storage.file.FileStorageError
                StorageError.FileSystemError(fileError)
            }
            is IllegalStateException -> 
                StorageError.ConfigurationError(e.message ?: "Configuration error")
            else -> 
                StorageError.MaintenanceError("Unexpected error: ${e.message}")
        }
    }
} 