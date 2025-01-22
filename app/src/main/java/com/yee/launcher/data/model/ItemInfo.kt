package com.yee.launcher.data.model

import android.content.Intent
import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
@Entity(tableName = ItemInfo.TABLE_NAME)
class ItemInfo {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    //只对ItemType.ITEM_TYPE_APPLICATION有效
    var intent: Intent? = null
    var itemType: Int = ItemType.ITEM_TYPE_APPLICATION
    var position: Int = 0
    var title: String? = null

    //文件路径：只对文件和文件夹有效
    var data: String? = null

    // TODO: 暂未用到，后续入需实现多屏，用于记录位于哪个屏幕上
    var screenId: Int = 0

    /// TODO: 暂未用到，后续入需实现应用文件夹，可以用于记录应用属于哪个文件夹
    var container: String? = null

    //是否是系统应用
    var isSystemApp: Boolean = false
    var lastUpdated: Long = 0


    companion object {
        const val TABLE_NAME = "itemInfo"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ItemInfo

        if (id != other.id) return false
        if (intent != other.intent) return false
        if (itemType != other.itemType) return false
        if (position != other.position) return false
        if (title != other.title) return false
        if (screenId != other.screenId) return false
        if (container != other.container) return false
        if (isSystemApp != other.isSystemApp) return false
        if (lastUpdated != other.lastUpdated) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (intent?.hashCode() ?: 0)
        result = 31 * result + itemType
        result = 31 * result + position
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + screenId
        result = 31 * result + (container?.hashCode() ?: 0)
        result = 31 * result + isSystemApp.hashCode()
        result = 31 * result + lastUpdated.hashCode()
        return result
    }
}