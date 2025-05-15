package com.xuatseg.yuanyin.storage.file.util

import com.xuatseg.yuanyin.storage.file.FileStorageError

/**
 * 文件路径验证工具
 */
object FilePathValidator {
    
    // 不允许的路径字符
    private val INVALID_PATH_CHARS = charArrayOf('<', '>', ':', '"', '|', '?', '*')
    
    // 危险路径模式（路径穿越攻击）
    private val DANGEROUS_PATTERNS = arrayOf(
        "../", "..\\"
    )
    
    /**
     * 验证路径是否安全
     * @param path 需要验证的路径
     * @throws FileStorageError.InvalidPathError 如果路径不安全
     */
    fun validatePath(path: String) {
        // 检查空路径
        if (path.isBlank()) {
            throw FileStorageError.InvalidPathError("Path cannot be empty")
        }
        
        // 检查路径穿越攻击
        for (pattern in DANGEROUS_PATTERNS) {
            if (path.contains(pattern)) {
                throw FileStorageError.SecurityError("Path contains dangerous pattern: $pattern")
            }
        }
        
        // 检查绝对路径
        if (path.startsWith("/") || path.matches(Regex("^[A-Za-z]:[/\\\\].*"))) {
            throw FileStorageError.SecurityError("Absolute paths are not allowed: $path")
        }
        
        // 检查不允许的字符
        for (c in INVALID_PATH_CHARS) {
            if (path.contains(c)) {
                throw FileStorageError.InvalidPathError("Path contains invalid character: $c")
            }
        }
        
        // 检查路径长度
        if (path.length > MAX_PATH_LENGTH) {
            throw FileStorageError.InvalidPathError("Path is too long (max ${MAX_PATH_LENGTH} characters)")
        }
    }
    
    /**
     * 规范化路径
     * 移除重复的分隔符，处理 ./ 等
     */
    fun normalizePath(path: String): String {
        var result = path.replace("\\", "/")
        
        // 移除重复的 /
        while (result.contains("//")) {
            result = result.replace("//", "/")
        }
        
        // 移除开头的 ./
        if (result.startsWith("./")) {
            result = result.substring(2)
        }
        
        // 移除结尾的 /
        if (result.length > 1 && result.endsWith("/")) {
            result = result.substring(0, result.length - 1)
        }
        
        return result
    }
    
    /**
     * 组合路径
     */
    fun combinePath(vararg paths: String): String {
        if (paths.isEmpty()) return ""
        
        val normalizedPaths = paths.map { normalizePath(it) }
        return normalizedPaths.joinToString("/")
    }
    
    /**
     * 获取文件扩展名
     */
    fun getExtension(path: String): String {
        val lastDotIndex = path.lastIndexOf('.')
        return if (lastDotIndex > 0 && lastDotIndex < path.length - 1) {
            path.substring(lastDotIndex + 1)
        } else {
            ""
        }
    }
    
    /**
     * 获取文件名（不含路径）
     */
    fun getFileName(path: String): String {
        val normalizedPath = normalizePath(path)
        val lastSlashIndex = normalizedPath.lastIndexOf('/')
        return if (lastSlashIndex >= 0 && lastSlashIndex < normalizedPath.length - 1) {
            normalizedPath.substring(lastSlashIndex + 1)
        } else {
            normalizedPath
        }
    }
    
    /**
     * 获取目录路径
     */
    fun getDirectoryPath(path: String): String {
        val normalizedPath = normalizePath(path)
        val lastSlashIndex = normalizedPath.lastIndexOf('/')
        return if (lastSlashIndex > 0) {
            normalizedPath.substring(0, lastSlashIndex)
        } else if (lastSlashIndex == 0) {
            "/"
        } else {
            ""
        }
    }
    
    // 常量
    private const val MAX_PATH_LENGTH = 255
}