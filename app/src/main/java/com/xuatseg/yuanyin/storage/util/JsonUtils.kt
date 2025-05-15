package com.xuatseg.yuanyin.storage.util

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * JSON工具类
 * 提供通用的JSON序列化和反序列化方法
 */
object JsonUtils {
    
    // 提升为公共API，这样inline函数可以访问
    val gson: Gson = GsonProvider.gson
    
    /**
     * 将对象转换为JSON字符串
     */
    fun <T> toJson(obj: T): String {
        return gson.toJson(obj)
    }
    
    /**
     * 将对象转换为JSON字符串(美化输出)
     */
    fun <T> toPrettyJson(obj: T): String {
        return GsonProvider.prettyGson.toJson(obj)
    }
    
    /**
     * 将JSON字符串转换为对象
     */
    @Throws(JsonSyntaxException::class)
    inline fun <reified T> fromJson(json: String): T {
        return gson.fromJson(json, object : TypeToken<T>() {}.type)
    }
    
    /**
     * 将JSON字符串转换为对象
     */
    @Throws(JsonSyntaxException::class)
    fun <T> fromJson(json: String, type: Type): T {
        return gson.fromJson(json, type)
    }
    
    /**
     * 将JSON字符串转换为对象
     */
    @Throws(JsonSyntaxException::class)
    fun <T> fromJson(json: String, classOfT: Class<T>): T {
        return gson.fromJson(json, classOfT)
    }
    
    /**
     * 将Map转换为JSON字符串
     */
    fun mapToJson(map: Map<String, Any?>): String {
        return gson.toJson(map)
    }
    
    /**
     * 将JSON字符串转换为Map
     */
    @Throws(JsonSyntaxException::class)
    fun jsonToMap(json: String): Map<String, Any?> {
        val type = object : TypeToken<Map<String, Any?>>() {}.type
        return gson.fromJson(json, type)
    }
    
    /**
     * 将列表转换为JSON字符串
     */
    fun <T> listToJson(list: List<T>): String {
        return gson.toJson(list)
    }
    
    /**
     * 将JSON字符串转换为列表
     */
    @Throws(JsonSyntaxException::class)
    inline fun <reified T> jsonToList(json: String): List<T> {
        val type = object : TypeToken<List<T>>() {}.type
        return gson.fromJson(json, type)
    }
    
    /**
     * 将JSON字符串转换为指定类型的列表
     */
    @Throws(JsonSyntaxException::class)
    fun <T> jsonToList(json: String, classOfT: Class<T>): List<T> {
        val type = TypeToken.getParameterized(List::class.java, classOfT).type
        return gson.fromJson(json, type)
    }
    
    /**
     * 检查字符串是否为有效的JSON
     */
    fun isValidJson(json: String): Boolean {
        return try {
            gson.fromJson(json, Any::class.java)
            true
        } catch (e: JsonSyntaxException) {
            false
        }
    }
}