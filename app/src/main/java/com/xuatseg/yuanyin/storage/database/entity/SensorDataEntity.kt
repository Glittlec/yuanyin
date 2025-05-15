package com.xuatseg.yuanyin.storage.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 传感器数据实体
 */
@Entity(
    tableName = "sensor_data",
    indices = [Index(value = ["sensorType", "timestamp"])]
)
data class SensorDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sensorType: String,  // 使用SensorType的name
    val timestamp: Long,     // 存储毫秒时间戳
    val reliability: Float,  // 可靠性评分 0.0-1.0
    val dataJson: String     // 传感器数据的JSON序列化
)