package com.yee.launcher.data.model

object ItemType {
    //这里的设置会影响类型排序顺序，修改设置后需要卸载重装
    const val ITEM_TYPE_APPLICATION: Int = 0
    const val ITEM_TYPE_FILE: Int = 1
    const val ITEM_TYPE_FILE_FOLDER: Int = 2

    //回收站
    const val ITEM_TYPE_TRASH: Int = 3
}
