package com.yee.launcher.data.model

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import com.yee.launcher.utils.FileUtils

class DesktopItemInfo(var itemInfo: ItemInfo) {


    fun getPackageName(): String {
        return itemInfo.intent?.component?.packageName ?: ""
    }

    fun getClassName(): String {
        return itemInfo.intent?.component?.className ?: ""
    }

    fun getIntent(): Intent? {
        if (getItemType() == ItemType.ITEM_TYPE_FILE || getItemType() == ItemType.ITEM_TYPE_FILE_FOLDER || getItemType() == ItemType.ITEM_TYPE_TRASH) {
            return Intent(Intent.ACTION_VIEW).apply {
                var fileName = ""
                if (getItemType() != ItemType.ITEM_TYPE_TRASH) {
                    fileName = FileUtils.getFileName(itemInfo?.data)
                }
                data = Uri.parse("")
            }
        }
        return itemInfo.intent
    }

    fun getTitle(): String {
        return itemInfo.title ?: ""
    }

    fun getPosition(): Int {
        return itemInfo.position ?: -1
    }

    fun getItemType(): Int {
        return itemInfo.itemType ?: ItemType.ITEM_TYPE_APPLICATION
    }

    fun appIconKey(): String {
        return "id=${itemInfo.id}, packageName=${getPackageName()}, className=${getClassName()}, lastUpdated=${itemInfo.lastUpdated})"
    }

    fun canRemove(): Boolean {
        return getItemType() != ItemType.ITEM_TYPE_TRASH
    }

    fun changePosition(position: Int) {
        itemInfo.position = position
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DesktopItemInfo

        return itemInfo == other.itemInfo
    }

    override fun hashCode(): Int {
        return itemInfo.hashCode() ?: 0
    }
}
