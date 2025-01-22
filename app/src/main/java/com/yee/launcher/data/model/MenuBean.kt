package com.yee.launcher.data.model

class MenuBean(val leftIcon: Int, val title: String, val itemType: Int, val rightIcon: Int, var showDivider: Boolean = true) {

    companion object {
        const val MENU_TYPE_VIEW: Int = 1
        const val MENU_TYPE_SORT: Int = 2
        const val MENU_TYPE_NEW_FOLDER: Int = 3
        const val MENU_TYPE_REMOVE: Int = 4
        const val MENU_TYPE_OPEN: Int = 5
        const val MENU_TYPE_DEFAULT_APP: Int = 6
        const val MENU_TYPE_RENAME: Int = 7
        const val MENU_TYPE_CLEAN_TRASH: Int = 8
        const val MENU_TYPE_PASTE: Int = 9
        const val MENU_TYPE_COPY: Int = 10
        const val MENU_TYPE_CUT: Int = 11
        const val MENU_TYPE_UNINSTALL: Int = 12
        const val MENU_TYPE_WALLPAPER: Int = 13

        const val SORT_BY_NAME: Int = 0xFF01
        const val SORT_BY_TYPE: Int = 0xFF02
        const val SORT_BY_DATE: Int = 0xFF03


        const val VIEW_18X8: Int = 0xee01
        const val VIEW_18X9: Int = 0xee02
        const val VIEW_19X8: Int = 0xee03
        const val VIEW_19X9: Int = 0xee04
        const val VIEW_20X10: Int = 0xee05
    }
}