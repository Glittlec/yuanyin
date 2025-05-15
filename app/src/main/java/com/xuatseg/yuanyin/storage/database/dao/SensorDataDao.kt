package com.xuatseg.yuanyin.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.xuatseg.yuanyin.storage.database.entity.SensorDataEntity

/**
 * 传感器数据数据访问对象
 */
@Dao
interface SensorDataDao {
    /**
     * 插入单条传感器数据
     */
    @Insert
    suspend fun insertSensorData(data: SensorDataEntity)
    
    /**
     * 批量插入传感器数据
     */
    @Insert
    suspend fun insertSensorDataBatch(dataList: List<SensorDataEntity>)
    
    /**
     * 按条件查询传感器数据
     */
    @Query(
        """
        SELECT * FROM sensor_data 
        WHERE (:sensorTypes IS NULL OR sensorType IN (:sensorTypes))
        AND timestamp BETWEEN :startTime AND :endTime
        ORDER BY timestamp DESC 
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun querySensorData(
        sensorTypes: List<String>?, 
        startTime: Long, 
        endTime: Long, 
        limit: Int, 
        offset: Int
    ): List<SensorDataEntity>
    
    /**
     * 删除指定时间之前的传感器数据
     */
    @Query("DELETE FROM sensor_data WHERE timestamp < :timestamp")
    suspend fun deleteSensorDataOlderThan(timestamp: Long): Int
    
    /**
     * 获取传感器数据总数
     */
    @Query("SELECT COUNT(*) FROM sensor_data")
    suspend fun getSensorDataCount(): Long
    
    /**
     * 按传感器类型获取最新数据
     */
    @Query("SELECT * FROM sensor_data WHERE sensorType = :sensorType ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSensorData(sensorType: String): SensorDataEntity?
}