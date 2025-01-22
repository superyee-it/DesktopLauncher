package com.yee.launcher.constants

import android.os.Environment
import java.io.File

object AppConstants {
    val DESKTOP_FOLDER_PATH: String
        get() = Environment.getExternalStorageDirectory().absolutePath + File.separator + "Desktop"


    const val NEW_FOLDER_NAME = "新建文件夹"

    object SP {
        const val VIEW_SPANCOUNT_COLUMN = "view_spancount_column"

    }

    object RequestConstant {
        // 选择壁纸
        const val REQUEST_CHOOSE_WALLPAPER = 100
    }
}
