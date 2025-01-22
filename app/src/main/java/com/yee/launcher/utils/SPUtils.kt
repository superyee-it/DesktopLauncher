package com.yee.launcher.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yee.launcher.ui.GlobalApplication

class SPUtils private constructor(context: Context) {

    private val prefs: SharedPreferences
    private val gson: Gson = Gson()

    init {
        prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    }

    companion object {
        @Volatile
        private var instance: SPUtils? = null

        fun getInstance(): SPUtils {
            return instance ?: synchronized(this) {
                instance ?: SPUtils(GlobalApplication.getInstance()).also { instance = it }
            }
        }
    }

    // 保存数据
    fun save(key: String, value: Any) {
        with(prefs.edit()) {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is String -> putString(key, value)
                else -> throw IllegalArgumentException("Unsupported data type")
            }
            apply()
        }
    }

    // 保存对象
    fun <T> saveObject(key: String, obj: T) {
        val json = gson.toJson(obj)
        prefs.edit().putString(key, json).apply()
    }

    // 获取布尔值
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    // 获取整数值
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }

    // 获取长整数值
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return prefs.getLong(key, defaultValue)
    }

    // 获取浮点数值
    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return prefs.getFloat(key, defaultValue)
    }

    // 获取字符串值
    fun getString(key: String, defaultValue: String? = null): String? {
        return prefs.getString(key, defaultValue)
    }

    // 获取对象
    fun <T> getObject(key: String, classOfT: Class<T>): T? {
        val json = prefs.getString(key, null)
        return if (json == null) null else gson.fromJson(json, classOfT)
    }

    // 获取对象列表
    fun <T> getObjectList(key: String, typeToken: TypeToken<List<T>>): List<T>? {
        val json = prefs.getString(key, null)
        return if (json == null) null else gson.fromJson(json, typeToken.type)
    }

    // 删除指定键值对
    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    // 清空所有数据
    fun clear() {
        prefs.edit().clear().apply()
    }

    // 检查是否存在指定键
    fun contains(key: String): Boolean {
        return prefs.contains(key)
    }
}
