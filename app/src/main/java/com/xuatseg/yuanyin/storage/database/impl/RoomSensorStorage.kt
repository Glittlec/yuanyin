package com.xuatseg.yuanyin.storage.database.impl

import com.xuatseg.yuanyin.robot.SensorData
import com.xuatseg.yuanyin.storage.database.DatabaseError
import com.xuatseg.yuanyin.storage.database.DeleteCriteria
import com.xuatseg.yuanyin.storage.database.ISensorStorage
import com.xuatseg.yuanyin.storage.database.SensorDataQuery
import com.xuatseg.yuanyin.storage.database.SortOrder
import com.xuatseg.yuanyin.storage.database.converter.SensorDataConverter
import com.xuatseg.yuanyin.storage.database.dao.SensorDataDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Room传感器存储实现
 */
class RoomSensorStorage(private val sensorDataDao: SensorDataDao) : ISensorStorage {
    
    /**
     * 保存传感器数据
     */
    override suspend fun saveSensorData(data: SensorData) = withContext(Dispatchers.IO) {
        try {
            val entity = SensorDataConverter.toEntity(data)
            sensorDataDao.insertSensorData(entity)
        } catch (e: Exception) {
            throw DatabaseError.StorageError("Failed to save sensor data: ${e.message}")
        }
    }
    
    /**
     * 批量保存传感器数据
     */
    override suspend fun saveSensorDataBatch(dataList: List<SensorData>) = withContext(Dispatchers.IO) {
        if (dataList.isEmpty()) return@withContext
        
        try {
            val entities = SensorDataConverter.toEntityList(dataList)
            sensorDataDao.insertSensorDataBatch(entities)
        } catch (e: Exception) {
            throw DatabaseError.StorageError("Failed to save sensor data batch: ${e.message}")
        }
    }
    
    /**
     * 查询传感器数据
     */
    override fun querySensorData(query: SensorDataQuery): Flow<List<SensorData>> = flow {
        try {
            // 准备查询参数
            val startTime = query.timeRange?.start?.toEpochMilli() ?: 0
            val endTime = query.timeRange?.endInclusive?.toEpochMilli() ?: System.currentTimeMillis()
            val sensorTypes = query.sensorTypes?.toList()
            val limit = query.limit ?: 100
            val offset = query.offset
            
            // 执行查询
            val entities = sensorDataDao.querySensorData(
                sensorTypes,
                startTime,
                endTime,
                limit,
                offset
            )
            
            // 转换结果
            val result = SensorDataConverter.toSensorDataList(entities)
            emit(result)
        } catch (e: Exception) {
            throw DatabaseError.QueryError("Failed to query sensor data: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 删除传感器数据
     */
    override suspend fun deleteSensorData(criteria: DeleteCriteria): Int = withContext(Dispatchers.IO) {
        try {
            // 按时间范围删除
            if (criteria.timeRange != null) {
                val endTime = criteria.timeRange.endInclusive.toEpochMilli()
                return@withContext sensorDataDao.deleteSensorDataOlderThan(endTime)
            }
            
            // 如果没有指定删除条件，不执行任何操作
            0
        } catch (e: Exception) {
            throw DatabaseError.StorageError("Failed to delete sensor data: ${e.message}")
        }
    }
}