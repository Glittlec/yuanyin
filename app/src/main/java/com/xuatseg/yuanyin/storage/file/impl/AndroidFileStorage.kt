package com.xuatseg.yuanyin.storage.file.impl

import android.content.Context
import com.xuatseg.yuanyin.storage.file.FileFilter
import com.xuatseg.yuanyin.storage.file.FileInfo
import com.xuatseg.yuanyin.storage.file.FileStorageError
import com.xuatseg.yuanyin.storage.file.IFileStorage
import com.xuatseg.yuanyin.storage.file.WriteOptions
import com.xuatseg.yuanyin.storage.file.util.FilePathValidator
import com.xuatseg.yuanyin.storage.file.util.FileUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import java.util.UUID

/**
 * Android文件存储实现
 */
class AndroidFileStorage(private val context: Context) : IFileStorage {
    
    // 主根目录
    private val rootDir: File = context.filesDir
    
    // 常用子目录
    private val tempDir: File = File(rootDir, TEMP_DIR)
    private val logDir: File = File(rootDir, LOG_DIR)
    private val mediaDir: File = File(rootDir, MEDIA_DIR)
    private val imageDir: File = File(mediaDir, IMAGE_DIR)
    private val audioDir: File = File(mediaDir, AUDIO_DIR)
    
    /**
     * 初始化创建所需目录
     */
    fun ensureDirectoriesExist() {
        tempDir.mkdirs()
        logDir.mkdirs()
        mediaDir.mkdirs()
        imageDir.mkdirs()
        audioDir.mkdirs()
    }
    
    /**
     * 写入文件
     */
    override suspend fun writeFile(path: String, content: ByteArray, options: WriteOptions?): Unit = try {
        // 验证路径安全性
        FilePathValidator.validatePath(path)
        
        val file = File(rootDir, path)
        
        // 确保父目录存在
        if (options?.createDirectories != false) {
            file.parentFile?.mkdirs()
        }
        
        // 检查文件是否存在且覆盖选项
        if (file.exists() && options?.overwrite == false) {
            throw FileStorageError.IOError("File already exists and overwrite is not allowed: $path")
        }
        
        // 写入文件
        if (options?.append == true && file.exists()) {
            FileOutputStream(file, true).use { it.write(content) }
        } else {
            file.writeBytes(content)
        }
    } catch (e: IOException) {
        throw FileStorageError.IOError("Failed to write file: ${e.message}")
    } catch (e: SecurityException) {
        throw FileStorageError.SecurityError("Security error writing file: ${e.message}")
    } catch (e: FileStorageError) {
        throw e
    } catch (e: Exception) {
        throw FileStorageError.IOError("Unexpected error writing file: ${e.message}")
    }
    
    /**
     * 读取文件
     */
    override suspend fun readFile(path: String): ByteArray = try {
        // 验证路径安全性
        FilePathValidator.validatePath(path)
        
        val file = File(rootDir, path)
        if (!file.exists()) {
            throw FileStorageError.IOError("File does not exist: $path")
        }
        
        if (!file.isFile) {
            throw FileStorageError.InvalidPathError("Path is not a file: $path")
        }
        
        file.readBytes()
    } catch (e: IOException) {
        throw FileStorageError.IOError("Failed to read file: ${e.message}")
    } catch (e: SecurityException) {
        throw FileStorageError.SecurityError("Security error reading file: ${e.message}")
    } catch (e: OutOfMemoryError) {
        throw FileStorageError.IOError("File too large to read into memory: $path")
    } catch (e: FileStorageError) {
        throw e
    } catch (e: Exception) {
        throw FileStorageError.IOError("Unexpected error reading file: ${e.message}")
    }
    
    /**
     * 删除文件
     */
    override suspend fun deleteFile(path: String): Boolean {
        try {
            // 验证路径安全性
            FilePathValidator.validatePath(path)
            
            val file = File(rootDir, path)
            if (!file.exists()) {
                return false
            }
            
            if (!file.isFile) {
                throw FileStorageError.InvalidPathError("Path is not a file: $path")
            }
            
            return file.delete()
        } catch (e: SecurityException) {
            throw FileStorageError.SecurityError("Security error deleting file: ${e.message}")
        } catch (e: FileStorageError) {
            throw e
        } catch (e: Exception) {
            throw FileStorageError.IOError("Unexpected error deleting file: ${e.message}")
        }
    }
    
    /**
     * 检查文件是否存在
     */
    override fun fileExists(path: String): Boolean {
        try {
            // 验证路径安全性
            FilePathValidator.validatePath(path)
            
            val file = File(rootDir, path)
            return file.exists() && file.isFile
        } catch (e: FileStorageError) {
            throw e
        } catch (e: Exception) {
            throw FileStorageError.IOError("Error checking if file exists: ${e.message}")
        }
    }
    
    /**
     * 获取文件信息
     */
    override fun getFileInfo(path: String): FileInfo = try {
        // 验证路径安全性
        FilePathValidator.validatePath(path)
        
        val file = File(rootDir, path)
        if (!file.exists()) {
            throw FileStorageError.IOError("File does not exist: $path")
        }
        
        FileInfo(
            path = path,
            name = file.name,
            size = file.length(),
            createdAt = Instant.ofEpochMilli(FileUtils.getCreationTime(file)),
            modifiedAt = Instant.ofEpochMilli(file.lastModified()),
            isDirectory = file.isDirectory,
            attributes = mapOf(
                "readable" to file.canRead(),
                "writable" to file.canWrite(),
                "hidden" to file.isHidden(),
                "mimeType" to FileUtils.getMimeType(file)
            )
        )
    } catch (e: FileStorageError) {
        throw e
    } catch (e: Exception) {
        throw FileStorageError.IOError("Error getting file info: ${e.message}")
    }
    
    /**
     * 列出目录内容
     */
    override fun listDirectory(path: String, filter: FileFilter?): List<FileInfo> = try {
        // 验证路径安全性
        FilePathValidator.validatePath(path)
        
        val dir = File(rootDir, path)
        if (!dir.exists()) {
            throw FileStorageError.IOError("Directory does not exist: $path")
        }
        
        if (!dir.isDirectory) {
            throw FileStorageError.InvalidPathError("Path is not a directory: $path")
        }
        
        val files = dir.listFiles() ?: return emptyList()
        
        files.filter { file ->
            if (filter == null) return@filter true
            
            // 应用过滤条件
            val matchesExtension = filter.extensions == null || 
                filter.extensions.contains(file.extension)
            
            val matchesSize = (filter.minSize == null || file.length() >= filter.minSize) &&
                (filter.maxSize == null || file.length() <= filter.maxSize)
            
            val fileModified = Instant.ofEpochMilli(file.lastModified())
            val matchesModified = (filter.modifiedAfter == null || 
                fileModified.isAfter(filter.modifiedAfter)) &&
                (filter.modifiedBefore == null || 
                fileModified.isBefore(filter.modifiedBefore))
            
            val matchesName = filter.namePattern == null || 
                filter.namePattern.matches(file.name)
            
            matchesExtension && matchesSize && matchesModified && matchesName
        }.map { file ->
            FileInfo(
                path = if (path.isNotEmpty() && !path.endsWith("/")) "$path/${file.name}" else "${path}${file.name}",
                name = file.name,
                size = file.length(),
                createdAt = Instant.ofEpochMilli(FileUtils.getCreationTime(file)),
                modifiedAt = Instant.ofEpochMilli(file.lastModified()),
                isDirectory = file.isDirectory,
                attributes = mapOf(
                    "readable" to file.canRead(),
                    "writable" to file.canWrite(),
                    "hidden" to file.isHidden(),
                    "mimeType" to FileUtils.getMimeType(file)
                )
            )
        }
    } catch (e: FileStorageError) {
        throw e
    } catch (e: Exception) {
        throw FileStorageError.IOError("Error listing directory: ${e.message}")
    }
    
    /**
     * 创建目录
     */
    fun createDirectory(path: String): Boolean {
        try {
            // 验证路径安全性
            FilePathValidator.validatePath(path)
            
            val dir = File(rootDir, path)
            if (dir.exists()) {
                return dir.isDirectory
            }
            
            return dir.mkdirs()
        } catch (e: SecurityException) {
            throw FileStorageError.SecurityError("Security error creating directory: ${e.message}")
        } catch (e: FileStorageError) {
            throw e
        } catch (e: Exception) {
            throw FileStorageError.IOError("Unexpected error creating directory: ${e.message}")
        }
    }
    
    /**
     * 删除目录
     */
    fun removeDirectory(path: String, recursive: Boolean = false): Boolean {
        try {
            // 验证路径安全性
            FilePathValidator.validatePath(path)
            
            val dir = File(rootDir, path)
            if (!dir.exists()) {
                return false
            }
            
            if (!dir.isDirectory) {
                throw FileStorageError.InvalidPathError("Path is not a directory: $path")
            }
            
            return if (recursive) {
                dir.deleteRecursively()
            } else {
                dir.delete()
            }
        } catch (e: SecurityException) {
            throw FileStorageError.SecurityError("Security error removing directory: ${e.message}")
        } catch (e: FileStorageError) {
            throw e
        } catch (e: Exception) {
            throw FileStorageError.IOError("Unexpected error removing directory: ${e.message}")
        }
    }
    
    /**
     * 复制文件
     */
    fun copyFile(sourcePath: String, destinationPath: String, overwrite: Boolean = false): Boolean = try {
        // 验证路径安全性
        FilePathValidator.validatePath(sourcePath)
        FilePathValidator.validatePath(destinationPath)
        
        val sourceFile = File(rootDir, sourcePath)
        if (!sourceFile.exists() || !sourceFile.isFile) {
            throw FileStorageError.IOError("Source file does not exist or is not a file: $sourcePath")
        }
        
        val destFile = File(rootDir, destinationPath)
        if (destFile.exists() && !overwrite) {
            throw FileStorageError.IOError("Destination file already exists and overwrite is not allowed: $destinationPath")
        }
        
        // 确保目标文件的父目录存在
        destFile.parentFile?.mkdirs()
        
        sourceFile.copyTo(destFile, overwrite)
        true
    } catch (e: IOException) {
        throw FileStorageError.IOError("Failed to copy file: ${e.message}")
    } catch (e: SecurityException) {
        throw FileStorageError.SecurityError("Security error copying file: ${e.message}")
    } catch (e: FileStorageError) {
        throw e
    } catch (e: Exception) {
        throw FileStorageError.IOError("Unexpected error copying file: ${e.message}")
    }
    
    /**
     * 移动文件
     */
    fun moveFile(sourcePath: String, destinationPath: String, overwrite: Boolean = false): Boolean = try {
        // 验证路径安全性
        FilePathValidator.validatePath(sourcePath)
        FilePathValidator.validatePath(destinationPath)
        
        val sourceFile = File(rootDir, sourcePath)
        if (!sourceFile.exists() || !sourceFile.isFile) {
            throw FileStorageError.IOError("Source file does not exist or is not a file: $sourcePath")
        }
        
        val destFile = File(rootDir, destinationPath)
        if (destFile.exists() && !overwrite) {
            throw FileStorageError.IOError("Destination file already exists and overwrite is not allowed: $destinationPath")
        }
        
        // 确保目标文件的父目录存在
        destFile.parentFile?.mkdirs()
        
        sourceFile.renameTo(destFile)
    } catch (e: IOException) {
        throw FileStorageError.IOError("Failed to move file: ${e.message}")
    } catch (e: SecurityException) {
        throw FileStorageError.SecurityError("Security error moving file: ${e.message}")
    } catch (e: FileStorageError) {
        throw e
    } catch (e: Exception) {
        throw FileStorageError.IOError("Unexpected error moving file: ${e.message}")
    }
    
    /**
     * 重命名文件
     */
    fun renameFile(path: String, newName: String): Boolean = try {
        // 验证路径安全性
        FilePathValidator.validatePath(path)
        
        if (newName.contains("/") || newName.contains("\\")) {
            throw FileStorageError.InvalidPathError("New name cannot contain path separators: $newName")
        }
        
        val file = File(rootDir, path)
        if (!file.exists() || !file.isFile) {
            throw FileStorageError.IOError("File does not exist or is not a file: $path")
        }
        
        val parent = file.parentFile
        val newFile = File(parent, newName)
        
        if (newFile.exists()) {
            throw FileStorageError.IOError("File with new name already exists: $newName")
        }
        
        file.renameTo(newFile)
    } catch (e: IOException) {
        throw FileStorageError.IOError("Failed to rename file: ${e.message}")
    } catch (e: SecurityException) {
        throw FileStorageError.SecurityError("Security error renaming file: ${e.message}")
    } catch (e: FileStorageError) {
        throw e
    } catch (e: Exception) {
        throw FileStorageError.IOError("Unexpected error renaming file: ${e.message}")
    }
    
    /**
     * 创建临时文件
     */
    fun createTempFile(prefix: String, suffix: String): File {
        tempDir.mkdirs()
        val tempFileName = "$prefix${UUID.randomUUID()}$suffix"
        return File(tempDir, tempFileName)
    }
    
    /**
     * 获取目录
     */
    fun getDirectory(type: DirectoryType): File {
        return when (type) {
            DirectoryType.ROOT -> rootDir
            DirectoryType.TEMP -> tempDir
            DirectoryType.LOG -> logDir
            DirectoryType.MEDIA -> mediaDir
            DirectoryType.IMAGE -> imageDir
            DirectoryType.AUDIO -> audioDir
        }
    }
    
    companion object {
        // 目录常量
        private const val TEMP_DIR = "temp"
        private const val LOG_DIR = "logs"
        private const val MEDIA_DIR = "media"
        private const val IMAGE_DIR = "images"
        private const val AUDIO_DIR = "audio"
    }
    
    /**
     * 目录类型
     */
    enum class DirectoryType {
        ROOT,
        TEMP,
        LOG,
        MEDIA,
        IMAGE,
        AUDIO
    }
}