package com.yee.launcher.data.db

import android.content.Intent
import android.util.Log
import androidx.room.TypeConverter

/**
 * 将Intent转换成String保存到数据库中，查询出来再转成Intent
 */
class IntentConvert {
    @TypeConverter
    fun stringToIntent(value: String?): Intent? {
        var intent: Intent? = null
        try {
            intent = Intent.parseUri(value, 0)
            intent.action = null
//            intent.addCategory(Intent.CATEGORY_LAUNCHER)
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return intent
    }

    @TypeConverter
    fun intentToString(value: Intent?): String {
        val res = value?.toUri(0)
        return res ?: ""
    }
}
