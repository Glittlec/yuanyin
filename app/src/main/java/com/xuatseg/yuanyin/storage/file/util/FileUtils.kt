package com.xuatseg.yuanyin.storage.file.util

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import com.xuatseg.yuanyin.storage.file.FileStorageError
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.Checksum

/**
 * 文件工具类
 */
object FileUtils {
    
    // 常用MIME类型映射
    private val MIME_TYPE_MAP = mapOf(
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "gif" to "image/gif",
        "webp" to "image/webp",
        "mp3" to "audio/mpeg",
        "wav" to "audio/wav",
        "ogg" to "audio/ogg",
        "mp4" to "video/mp4",
        "txt" to "text/plain",
        "html" to "text/html",
        "json" to "application/json",
        "pdf" to "application/pdf",
        "zip" to "application/zip"
    )
    
    /**
     * 获取文件MIME类型
     */
    fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        return MIME_TYPE_MAP[extension] ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }
    
    /**
     * 获取文件的创建时间
     * 由于Java File API限制，使用lastModified作为备选
     */
    fun getCreationTime(file: File): Long {
        return file.lastModified() // 在Android上通常只能获取到最后修改时间
    }
    
    /**
     * 计算文件的MD5哈希值
     */
    fun calculateMD5(file: File): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            var read: Int
            
            FileInputStream(file).use { fis ->
                while (fis.read(buffer).also { read = it } != -1) {
                    md.update(buffer, 0, read)
                }
            }
            
            val md5Bytes = md.digest()
            val md5String = StringBuilder()
            
            for (b in md5Bytes) {
                md5String.append(String.format("%02x", b.toInt() and 0xff))
            }
            
            md5String.toString()
        } catch (e: Exception) {
            throw FileStorageError.IOError("Error calculating MD5: ${e.message}")
        }
    }
    
    /**
     * 计算文件的CRC32校验和
     */
    fun calculateCRC32(file: File): Long {
        return try {
            val checksum: Checksum = CRC32()
            val buffer = ByteArray(8192)
            var read: Int
            
            FileInputStream(file).use { fis ->
                while (fis.read(buffer).also { read = it } != -1) {
                    checksum.update(buffer, 0, read)
                }
            }
            
            checksum.value
        } catch (e: Exception) {
            throw FileStorageError.IOError("Error calculating CRC32: ${e.message}")
        }
    }
    
    /**
     * 复制流
     */
    @Throws(IOException::class)
    fun copyStream(input: InputStream, output: OutputStream): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesCopied: Long = 0
        var bytes = input.read(buffer)
        while (bytes >= 0) {
            output.write(buffer, 0, bytes)
            bytesCopied += bytes
            bytes = input.read(buffer)
        }
        return bytesCopied
    }
    
    /**
     * 获取文件大小（递归计算目录大小）
     */
    fun getFileSize(file: File): Long {
        if (!file.exists()) return 0
        
        if (file.isFile) return file.length()
        
        var size: Long = 0
        file.listFiles()?.forEach { childFile ->
            size += getFileSize(childFile)
        }
        
        return size
    }
    
    /**
     * 判断是否为媒体文件
     */
    fun isMediaFile(file: File): Boolean {
        val mimeType = getMimeType(file)
        return mimeType.startsWith("image/") || 
               mimeType.startsWith("audio/") || 
               mimeType.startsWith("video/")
    }
    
    /**
     * 创建空文件，包括所需的目录
     */
    fun createEmptyFile(file: File): Boolean {
        if (file.exists()) return file.isFile
        
        file.parentFile?.mkdirs()
        return file.createNewFile()
    }
    
    /**
     * 将Uri转换为文件路径
     */
    fun getPathFromUri(contentResolver: ContentResolver, uri: Uri): String? {
        // 实现因Android版本而异
        // 这里是一个简化版本，实际应用中需要处理不同的Uri格式
        if (uri.scheme == "file") {
            return uri.path
        }
        
        // 内容Uri需要使用ContentResolver查询
        return null
    }
    
    // 常量
    private const val DEFAULT_BUFFER_SIZE = 8192
}