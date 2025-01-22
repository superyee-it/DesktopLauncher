package com.yee.launcher.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.yee.launcher.data.model.ItemInfo

@Dao
interface DesktopItemDao {

    @Query("SELECT * FROM itemInfo")
    fun getAll(): MutableList<ItemInfo>

    @Insert
    fun insert(item: ItemInfo): Long

    @Update
    fun update(item: ItemInfo)

    @Update
    fun update(itemList: List<ItemInfo>)

    @Delete
    fun delete(item: ItemInfo)

    @Delete
    fun delete(itemList: List<ItemInfo>)

}