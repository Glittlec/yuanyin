package com.xuatseg.yuanyin.storage

import com.xuatseg.yuanyin.storage.database.IDatabase
import com.xuatseg.yuanyin.storage.database.DatabaseError
import com.xuatseg.yuanyin.storage.file.IFileStorage
import com.xuatseg.yuanyin.storage.file.FileStorageError
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 存储管理器接口
 */
interface IStorageManager {
    /**
     * 初始化存储系统
     */
    suspend fun initialize()

    /**
     * 关闭存储系统
     */
    suspend fun shutdown()

    /**
     * 获取数据库实例
     */
    fun getDatabase(): IDatabase

    /**
     * 获取文件存储实例
     */
    fun getFileStorage(): IFileStorage

    /**
     * 执行存储维护
     */
    suspend fun performMaintenance()

    /**
     * 获取存储状态
     */
    fun getStorageStatus(): StorageStatus

    /**
     * 观察存储事件
     */
    fun observeStorageEvents(): Flow<StorageEvent>
}

/**
 * 存储状态
 */
data class StorageStatus(
    val databaseStatus: DatabaseStatus,
    val fileSystemStatus: FileSystemStatus,
    val totalStorageUsage: StorageUsage,
    val lastMaintenance: Instant
)

/**
 * 数据库状态
 */
data class DatabaseStatus(
    val isConnected: Boolean,
    val version: Int,
    val tableCount: Int,
    val totalRecords: Long,
    val storageUsage: StorageUsage,
    val performance: PerformanceMetrics
)

/**
 * 文件系统状态
 */
data class FileSystemStatus(
    val isAvailable: Boolean,
    val rootDirectory: String,
    val fileCount: Long,
    val storageUsage: StorageUsage,
    val performance: PerformanceMetrics
)

/**
 * 存储使用情况
 */
data class StorageUsage(
    val used: Long,
    val available: Long,
    val total: Long,
    val usagePercentage: Float
)

/**
 * 性能指标
 */
data class PerformanceMetrics(
    val readLatency: Long,
    val writeLatency: Long,
    val operationsPerSecond: Float,
    val errorRate: Float
)

/**
 * 存储事件
 */
sealed class StorageEvent {
    data class DatabaseEvent(val type: EventType, val details: String) : StorageEvent()
    data class FileSystemEvent(val type: EventType, val details: String) : StorageEvent()
    data class MaintenanceEvent(val type: EventType, val details: String) : StorageEvent()
    data class ErrorEvent(val error: StorageError, val details: String) : StorageEvent()
}

/**
 * 事件类型
 */
enum class EventType {
    INITIALIZED,
    CONNECTED,
    DISCONNECTED,
    MAINTENANCE_STARTED,
    MAINTENANCE_COMPLETED,
    ERROR_OCCURRED,
    WARNING_OCCURRED,
    QUOTA_EXCEEDED,
    RECOVERY_STARTED,
    RECOVERY_COMPLETED
}

/**
 * 存储错误
 */
sealed class StorageError {
    data class DatabaseError(val error: com.xuatseg.yuanyin.storage.database.DatabaseError) : StorageError()
    data class FileSystemError(val error: com.xuatseg.yuanyin.storage.file.FileStorageError) : StorageError()
    data class MaintenanceError(val message: String) : StorageError()
    data class ConfigurationError(val message: String) : StorageError()
}

/**
 * 存储配置接口
 */
interface IStorageConfig {
    /**
     * 获取数据库配置
     */
    fun getDatabaseConfig(): DatabaseConfig

    /**
     * 获取文件系统配置
     */
    fun getFileSystemConfig(): FileSystemConfig

    /**
     * 获取维护配置
     */
    fun getMaintenanceConfig(): MaintenanceConfig
}

/**
 * 数据库配置
 */
data class DatabaseConfig(
    val connectionString: String,
    val maxConnections: Int,
    val timeout: Long,
    val retryPolicy: RetryPolicy
)

/**
 * 文件系统配置
 */
data class FileSystemConfig(
    val rootPath: String,
    val maxFileSize: Long,
    val quotaSize: Long,
    val encryptionEnabled: Boolean
)

/**
 * 维护配置
 */
data class MaintenanceConfig(
    val schedule: MaintenanceSchedule,
    val cleanupPolicy: CleanupPolicy,
    val backupPolicy: BackupPolicy
)

/**
 * 维护计划
 */
data class MaintenanceSchedule(
    val interval: Long,
    val startTime: String,
    val maxDuration: Long
)

/**
 * 清理策略
 */
data class CleanupPolicy(
    val maxAge: Long,
    val maxSize: Long,
    val priorities: Map<String, Int>
)

/**
 * 备份策略
 */
data class BackupPolicy(
    val enabled: Boolean,
    val interval: Long,
    val retention: Int,
    val location: String
)

/**
 * 重试策略
 */
data class RetryPolicy(
    val maxAttempts: Int,
    val initialDelay: Long,
    val maxDelay: Long,
    val backoffMultiplier: Float
)

/**
 * 存储监控接口
 */
interface IStorageMonitor {
    /**
     * 开始监控
     */
    fun startMonitoring()

    /**
     * 停止监控
     */
    fun stopMonitoring()

    /**
     * 获取监控指标
     */
    fun getMetrics(): StorageMetrics

    /**
     * 观察性能指标
     */
    fun observePerformance(): Flow<PerformanceMetrics>
}

/**
 * 存储指标
 */
data class StorageMetrics(
    val databaseMetrics: Map<String, Float>,
    val fileSystemMetrics: Map<String, Float>,
    val errorMetrics: Map<String, Int>,
    val performanceMetrics: PerformanceMetrics
)

/**
 * 存储恢复接口
 */
interface IStorageRecovery {
    /**
     * 检查存储完整性
     */
    suspend fun checkIntegrity(): IntegrityResult

    /**
     * 执行恢复操作
     */
    suspend fun performRecovery(strategy: RecoveryStrategy)

    /**
     * 验证恢复结果
     */
    suspend fun validateRecovery(): ValidationResult
}

/**
 * 完整性结果
 */
data class IntegrityResult(
    val isValid: Boolean,
    val issues: List<StorageIssue>,
    val recommendations: List<String>
)

/**
 * 存储问题
 */
data class StorageIssue(
    val type: IssueType,
    val location: String,
    val description: String,
    val severity: IssueSeverity
)

/**
 * 问题类型
 */
enum class IssueType {
    CORRUPTION,
    INCONSISTENCY,
    MISSING_DATA,
    INVALID_FORMAT
}

/**
 * 问题严重程度
 */
enum class IssueSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * 恢复策略
 */
sealed class RecoveryStrategy {
    object QuickRepair : RecoveryStrategy()
    object FullRecovery : RecoveryStrategy()
    data class Custom(val options: Map<String, Any>) : RecoveryStrategy()
}

/**
 * 验证结果
 */
data class ValidationResult(
    val success: Boolean,
    val details: Map<String, Any>,
    val warnings: List<String>
)
