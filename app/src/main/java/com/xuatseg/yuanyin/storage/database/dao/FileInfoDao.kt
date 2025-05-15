package com.xuatseg.yuanyin.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xuatseg.yuanyin.storage.database.entity.FileInfoEntity

/**
 * 文件信息数据访问对象
 */
@Dao
interface FileInfoDao {
    /**
     * 插入/更新文件信息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFileInfo(fileInfo: FileInfoEntity)
    
    /**
     * 批量插入文件信息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFileInfoBatch(fileInfoList: List<FileInfoEntity>)
    
    /**
     * 根据路径获取文件信息
     */
    @Query("SELECT * FROM file_info WHERE path = :path")
    suspend fun getFileInfo(path: String): FileInfoEntity?
    
    /**
     * 按路径模式查找文件
     */
    @Query("SELECT * FROM file_info WHERE path LIKE :pathPattern")
    suspend fun findFilesByPath(pathPattern: String): List<FileInfoEntity>
    
    /**
     * 删除文件信息
     */
    @Query("DELETE FROM file_info WHERE path = :path")
    suspend fun deleteFileInfo(path: String): Int
    
    /**
     * 获取所有目录
     */
    @Query("SELECT * FROM file_info WHERE isDirectory = 1")
    suspend fun getAllDirectories(): List<FileInfoEntity>
}