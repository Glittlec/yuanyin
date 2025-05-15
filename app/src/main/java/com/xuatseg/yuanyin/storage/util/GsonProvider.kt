package com.xuatseg.yuanyin.storage.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.time.Instant

/**
 * Gson提供者单例
 * 统一管理JSON序列化配置
 */
object GsonProvider {
    /**
     * 获取默认配置的Gson实例
     */
    val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .serializeNulls() // 序列化null值
            .disableHtmlEscaping() // 禁用HTML转义
            .create()
    }
    
    /**
     * 获取紧凑输出的Gson实例
     */
    val compactGson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .serializeNulls()
            .disableHtmlEscaping()
            .create()
    }
    
    /**
     * 获取美化输出的Gson实例
     */
    val prettyGson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .serializeNulls()
            .disableHtmlEscaping()
            .setPrettyPrinting() // 美化JSON输出
            .create()
    }
}