package com.xuatseg.yuanyin.storage.database.converter

import com.xuatseg.yuanyin.robot.Point3D
import com.xuatseg.yuanyin.robot.SensorData
import com.xuatseg.yuanyin.robot.SensorType
import com.xuatseg.yuanyin.robot.Vector3D
import com.xuatseg.yuanyin.storage.database.entity.SensorDataEntity
import com.xuatseg.yuanyin.storage.util.GsonProvider
import java.time.Instant

/**
 * 传感器数据转换器
 * 负责SensorData和SensorDataEntity之间的转换
 */
object SensorDataConverter {
    
    private val gson = GsonProvider.gson
    
    /**
     * 将SensorData转换为SensorDataEntity
     */
    fun toEntity(sensorData: SensorData): SensorDataEntity {
        val dataJson = when (sensorData) {
            is SensorData.IMUData -> gson.toJson(sensorData)
            is SensorData.LidarData -> gson.toJson(sensorData)
            is SensorData.CameraData -> gson.toJson(sensorData)
            is SensorData.EnvironmentalData -> gson.toJson(sensorData)
        }
        
        return SensorDataEntity(
            id = 0, // 自动生成的ID
            sensorType = sensorData.sensorType.name,
            timestamp = sensorData.timestamp.toEpochMilli(),
            reliability = sensorData.reliability,
            dataJson = dataJson
        )
    }
    
    /**
     * 将SensorDataEntity转换为SensorData
     */
    fun toSensorData(entity: SensorDataEntity): SensorData {
        val sensorType = SensorType.valueOf(entity.sensorType)
        val timestamp = Instant.ofEpochMilli(entity.timestamp)
        
        return when (sensorType) {
            SensorType.IMU -> toIMUData(entity.dataJson, timestamp, entity.reliability)
            SensorType.LIDAR -> toLidarData(entity.dataJson, timestamp, entity.reliability)
            SensorType.CAMERA -> toCameraData(entity.dataJson, timestamp, entity.reliability)
            else -> toEnvironmentalData(entity.dataJson, timestamp, sensorType, entity.reliability)
        }
    }
    
    /**
     * 将JSON字符串转换为IMUData
     */
    private fun toIMUData(json: String, timestamp: Instant, reliability: Float): SensorData.IMUData {
        try {
            return gson.fromJson(json, SensorData.IMUData::class.java)
        } catch (e: Exception) {
            // 如果解析失败，返回默认值
            return SensorData.IMUData(
                timestamp = timestamp,
                acceleration = Vector3D(0f, 0f, 0f),
                gyroscope = Vector3D(0f, 0f, 0f),
                magnetometer = Vector3D(0f, 0f, 0f),
                reliability = reliability
            )
        }
    }
    
    /**
     * 将JSON字符串转换为LidarData
     */
    private fun toLidarData(json: String, timestamp: Instant, reliability: Float): SensorData.LidarData {
        try {
            return gson.fromJson(json, SensorData.LidarData::class.java)
        } catch (e: Exception) {
            // 如果解析失败，返回默认值
            return SensorData.LidarData(
                timestamp = timestamp,
                pointCloud = emptyList(),
                resolution = 0f,
                reliability = reliability
            )
        }
    }
    
    /**
     * 将JSON字符串转换为CameraData
     */
    private fun toCameraData(json: String, timestamp: Instant, reliability: Float): SensorData.CameraData {
        try {
            return gson.fromJson(json, SensorData.CameraData::class.java)
        } catch (e: Exception) {
            // 如果解析失败，返回默认值
            return SensorData.CameraData(
                timestamp = timestamp,
                imageData = ByteArray(0),
                resolution = com.xuatseg.yuanyin.robot.Resolution(0, 0),
                format = com.xuatseg.yuanyin.robot.ImageFormat.RGB,
                reliability = reliability
            )
        }
    }
    
    /**
     * 将JSON字符串转换为EnvironmentalData
     */
    private fun toEnvironmentalData(
        json: String, 
        timestamp: Instant, 
        sensorType: SensorType, 
        reliability: Float
    ): SensorData.EnvironmentalData {
        try {
            return gson.fromJson(json, SensorData.EnvironmentalData::class.java)
        } catch (e: Exception) {
            // 如果解析失败，返回默认值
            return SensorData.EnvironmentalData(
                timestamp = timestamp,
                sensorType = sensorType,
                value = 0f,
                unit = "",
                reliability = reliability
            )
        }
    }
    
    /**
     * 转换传感器数据列表
     */
    fun toSensorDataList(entities: List<SensorDataEntity>): List<SensorData> {
        return entities.map { toSensorData(it) }
    }
    
    /**
     * 转换为实体列表
     */
    fun toEntityList(sensorDataList: List<SensorData>): List<SensorDataEntity> {
        return sensorDataList.map { toEntity(it) }
    }
}