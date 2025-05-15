package com.xuatseg.yuanyin.storage.file.impl

import android.content.Context
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import com.xuatseg.yuanyin.storage.file.FileFilter
import com.xuatseg.yuanyin.storage.file.FileInfo
import com.xuatseg.yuanyin.storage.file.FileStorageError
import com.xuatseg.yuanyin.storage.file.FileSystemEvent
import com.xuatseg.yuanyin.storage.file.IFileSystemMonitor
import com.xuatseg.yuanyin.storage.file.StorageStats
import com.xuatseg.yuanyin.storage.file.util.FilePathValidator
import com.xuatseg.yuanyin.storage.file.util.FileUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 文件系统监控实现
 */
class FileSystemMonitorImpl(private val context: Context) : IFileSystemMonitor {
    
    private val fileStorage = AndroidFileStorage(context)
    private val rootDir = fileStorage.getDirectory(AndroidFileStorage.DirectoryType.ROOT)
    private val observers = ConcurrentHashMap<String, MonitorFileObserver>()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    /**
     * 开始监控指定路径
     */
    override fun startMonitoring(path: String, filter: FileFilter?): Flow<FileSystemEvent> = callbackFlow {
        try {
            // 验证路径安全性
            FilePathValidator.validatePath(path)
            
            val targetDir = File(rootDir, path)
            if (!targetDir.exists() || !targetDir.isDirectory) {
                throw FileStorageError.InvalidPathError("Path is not a valid directory: $path")
            }
            
            // 创建文件观察者
            val observer = MonitorFileObserver(
                targetDir.absolutePath,
                FileObserver.ALL_EVENTS,
                targetDir,
                filter
            ) { event ->
                // 将事件发送到Flow
                trySend(event)
            }
            
            // 存储观察者
            observers[path] = observer
            
            // 开始监控
            observer.startWatching()
            
            // 当Flow收集结束时关闭观察者
            awaitClose {
                observer.stopWatching()
                observers.remove(path)
            }
        } catch (e: Exception) {
            // 转换异常
            val error = when (e) {
                is FileStorageError -> e
                else -> FileStorageError.IOError("Error starting file monitoring: ${e.message}")
            }
            throw error
        }
    }
    
    /**
     * 停止监控
     */
    override fun stopMonitoring() {
        observers.forEach { (_, observer) ->
            observer.stopWatching()
        }
        observers.clear()
    }
    
    /**
     * 获取存储统计信息
     */
    override fun getStorageStats(): StorageStats {
        var fileCount = 0L
        var directoryCount = 0L
        
        // 计算文件和目录数量
        fun calculateFileCounts(dir: File) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    directoryCount++
                    calculateFileCounts(file)
                } else {
                    fileCount++
                }
            }
        }
        
        // 执行计算
        calculateFileCounts(rootDir)
        
        // 返回统计信息
        return StorageStats(
            totalSpace = rootDir.totalSpace,
            usedSpace = rootDir.totalSpace - rootDir.freeSpace,
            freeSpace = rootDir.freeSpace,
            fileCount = fileCount,
            directoryCount = directoryCount
        )
    }
    
    /**
     * 文件观察者内部类
     */
    private inner class MonitorFileObserver(
        path: String,
        mask: Int,
        private val rootFile: File,
        private val filter: FileFilter?,
        private val onEvent: (FileSystemEvent) -> Unit
    ) : FileObserver(path, mask) {
        
        override fun onEvent(event: Int, path: String?) {
            if (path == null) return
            
            val file = File(rootFile, path)
            
            // 如果有过滤器且不匹配，则跳过
            if (filter != null && !isFileMatchingFilter(file, filter)) {
                return
            }
            
            // 主线程处理事件
            mainHandler.post {
                when (event) {
                    FileObserver.CREATE -> {
                        try {
                            val fileInfo = createFileInfo(file)
                            onEvent(FileSystemEvent.Created(fileInfo))
                        } catch (e: Exception) {
                            // 如果无法获取文件信息，忽略该事件
                        }
                    }
                    FileObserver.MODIFY -> {
                        try {
                            val fileInfo = createFileInfo(file)
                            onEvent(FileSystemEvent.Modified(fileInfo))
                        } catch (e: Exception) {
                            // 如果无法获取文件信息，忽略该事件
                        }
                    }
                    FileObserver.DELETE -> {
                        val relativePath = getRelativePath(file)
                        onEvent(FileSystemEvent.Deleted(relativePath))
                    }
                    FileObserver.MOVED_FROM -> {
                        // 保存源路径，等待MOVED_TO事件
                        val relativePath = getRelativePath(file)
                        lastMovedFrom = relativePath
                    }
                    FileObserver.MOVED_TO -> {
                        val toPath = getRelativePath(file)
                        
                        if (lastMovedFrom != null) {
                            // 如果有上一个MOVED_FROM事件，则发送移动事件
                            onEvent(FileSystemEvent.Moved(lastMovedFrom!!, toPath))
                            lastMovedFrom = null
                        } else {
                            // 否则作为创建事件处理
                            try {
                                val fileInfo = createFileInfo(file)
                                onEvent(FileSystemEvent.Created(fileInfo))
                            } catch (e: Exception) {
                                // 如果无法获取文件信息，忽略该事件
                            }
                        }
                    }
                }
            }
        }
        
        private var lastMovedFrom: String? = null
        
        /**
         * 获取相对路径
         */
        private fun getRelativePath(file: File): String {
            val rootPath = rootFile.absolutePath
            val filePath = file.absolutePath
            
            return if (filePath.startsWith(rootPath)) {
                val relativePath = filePath.substring(rootPath.length)
                if (relativePath.startsWith("/")) relativePath.substring(1) else relativePath
            } else {
                file.name
            }
        }
        
        /**
         * 创建文件信息对象
         */
        private fun createFileInfo(file: File): FileInfo {
            if (!file.exists()) {
                throw FileStorageError.IOError("File does not exist: ${file.absolutePath}")
            }
            
            return FileInfo(
                path = getRelativePath(file),
                name = file.name,
                size = file.length(),
                createdAt = java.time.Instant.ofEpochMilli(FileUtils.getCreationTime(file)),
                modifiedAt = java.time.Instant.ofEpochMilli(file.lastModified()),
                isDirectory = file.isDirectory,
                attributes = mapOf(
                    "readable" to file.canRead(),
                    "writable" to file.canWrite(),
                    "hidden" to file.isHidden(),
                    "mimeType" to FileUtils.getMimeType(file)
                )
            )
        }
        
        /**
         * 检查文件是否匹配过滤器
         */
        private fun isFileMatchingFilter(file: File, filter: FileFilter): Boolean {
            if (!file.exists()) return false
            
            // 应用过滤条件
            val matchesExtension = filter.extensions == null || 
                filter.extensions.contains(file.extension)
            
            val matchesSize = (filter.minSize == null || file.length() >= filter.minSize) &&
                (filter.maxSize == null || file.length() <= filter.maxSize)
            
            val fileModified = java.time.Instant.ofEpochMilli(file.lastModified())
            val matchesModified = (filter.modifiedAfter == null || 
                fileModified.isAfter(filter.modifiedAfter)) &&
                (filter.modifiedBefore == null || 
                fileModified.isBefore(filter.modifiedBefore))
            
            val matchesName = filter.namePattern == null || 
                filter.namePattern.matches(file.name)
            
            return matchesExtension && matchesSize && matchesModified && matchesName
        }
    }
}

/**
 * 扩展StorageStats类以支持可变属性
 */
private var StorageStats.fileCount: Long
    get() = this.fileCount
    set(value) {
        val field = this::class.java.getDeclaredField("fileCount")
        field.isAccessible = true
        field.set(this, value)
    }

private var StorageStats.directoryCount: Long
    get() = this.directoryCount
    set(value) {
        val field = this::class.java.getDeclaredField("directoryCount")
        field.isAccessible = true
        field.set(this, value)
    }