package com.yee.launcher.data

import android.content.Context
import com.yee.launcher.data.db.AppDatabase
import com.yee.launcher.data.model.ItemInfo


class DesktopRepository {


    fun loadAllItemInfo(context: Context): MutableList<ItemInfo> {
        return AppDatabase.getInstance(context).desktopDao().getAll()
    }

    fun updateItem(context: Context, item: List<ItemInfo>) {
        AppDatabase.getInstance(context).desktopDao().update(item)
    }

    fun updateItem(context: Context, item: ItemInfo) {
        AppDatabase.getInstance(context).desktopDao().update(item)
    }

    fun deleteItem(context: Context, item: ItemInfo) {
        AppDatabase.getInstance(context).desktopDao().delete(item)
    }

    fun deleteItem(context: Context, item: List<ItemInfo>) {
        AppDatabase.getInstance(context).desktopDao().delete(item)
    }

    fun addItem(context: Context, item: ItemInfo): Long {
        return AppDatabase.getInstance(context).desktopDao().insert(item)
    }

}