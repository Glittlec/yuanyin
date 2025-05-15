package com.xuatseg.yuanyin.storage.database.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xuatseg.yuanyin.chat.MessageContent
import com.xuatseg.yuanyin.chat.MessageSender
import com.xuatseg.yuanyin.chat.MessageStatus
import com.xuatseg.yuanyin.robot.SensorType
import com.xuatseg.yuanyin.robot.Vector3D
import com.xuatseg.yuanyin.storage.file.LogLevel
import com.xuatseg.yuanyin.storage.util.GsonProvider
import java.time.Instant

/**
 * Room数据库类型转换器
 */
class Converters {
    private val gson = GsonProvider.gson
    
    /**
     * Instant转Long
     */
    @TypeConverter
    fun instantToLong(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }
    
    /**
     * Long转Instant
     */
    @TypeConverter
    fun longToInstant(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }
    
    /**
     * Map转String
     */
    @TypeConverter
    fun mapToString(map: Map<String, Any>?): String? {
        return map?.let { gson.toJson(it) }
    }
    
    /**
     * String转Map
     */
    @TypeConverter
    fun stringToMap(value: String?): Map<String, Any>? {
        if (value == null) return null
        val type = object : TypeToken<Map<String, Any>>() {}.type
        return gson.fromJson(value, type)
    }
    
    /**
     * SensorType转String
     */
    @TypeConverter
    fun sensorTypeToString(sensorType: SensorType?): String? {
        return sensorType?.name
    }
    
    /**
     * String转SensorType
     */
    @TypeConverter
    fun stringToSensorType(value: String?): SensorType? {
        return value?.let { SensorType.valueOf(it) }
    }
    
    /**
     * LogLevel转String
     */
    @TypeConverter
    fun logLevelToString(logLevel: LogLevel?): String? {
        return logLevel?.name
    }
    
    /**
     * String转LogLevel
     */
    @TypeConverter
    fun stringToLogLevel(value: String?): LogLevel? {
        return value?.let { LogLevel.valueOf(it) }
    }
    
    /**
     * Vector3D转String
     */
    @TypeConverter
    fun vector3dToString(vector: Vector3D?): String? {
        return vector?.let { gson.toJson(it) }
    }
    
    /**
     * String转Vector3D
     */
    @TypeConverter
    fun stringToVector3d(value: String?): Vector3D? {
        return value?.let { gson.fromJson(it, Vector3D::class.java) }
    }
    
    /**
     * MessageContent转String
     */
    @TypeConverter
    fun messageContentToString(content: MessageContent?): String? {
        return content?.let { gson.toJson(it) }
    }
    
    /**
     * String转MessageContent
     */
    @TypeConverter
    fun stringToMessageContent(value: String?): MessageContent? {
        if (value == null) return null
        return try {
            gson.fromJson(value, MessageContent::class.java)
        } catch (e: Exception) {
            MessageContent.Error("Failed to parse message content: ${e.message}")
        }
    }
    
    /**
     * MessageSender转String
     */
    @TypeConverter
    fun messageSenderToString(sender: MessageSender?): String? {
        return sender?.let { gson.toJson(it) }
    }
    
    /**
     * String转MessageSender
     */
    @TypeConverter
    fun stringToMessageSender(value: String?): MessageSender? {
        if (value == null) return null
        return try {
            gson.fromJson(value, MessageSender::class.java)
        } catch (e: Exception) {
            MessageSender.System("unknown")
        }
    }
    
    /**
     * MessageStatus转String
     */
    @TypeConverter
    fun messageStatusToString(status: MessageStatus?): String? {
        return status?.let { gson.toJson(it) }
    }
    
    /**
     * String转MessageStatus
     */
    @TypeConverter
    fun stringToMessageStatus(value: String?): MessageStatus? {
        if (value == null) return null
        return try {
            gson.fromJson(value, MessageStatus::class.java)
        } catch (e: Exception) {
            MessageStatus.Failed("Failed to parse message status: ${e.message}")
        }
    }
}