package com.xuatseg.yuanyin.storage.util

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.Instant

/**
 * Instant类型的Gson适配器
 * 在JSON和Instant之间转换
 */
class InstantTypeAdapter : TypeAdapter<Instant>() {
    
    /**
     * 写入Instant到JSON
     */
    override fun write(out: JsonWriter, value: Instant?) {
        if (value == null) {
            out.nullValue()
        } else {
            // 将Instant转换为毫秒时间戳
            out.value(value.toEpochMilli())
        }
    }
    
    /**
     * 从JSON读取Instant
     */
    override fun read(reader: JsonReader): Instant? {
        if (reader.peek().name == "NULL") {
            reader.nextNull()
            return null
        }
        
        // 从毫秒时间戳创建Instant
        val timestamp = reader.nextLong()
        return Instant.ofEpochMilli(timestamp)
    }
}