package com.yee.launcher.popup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PointF
import android.media.MediaScannerConnection
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BasePopupView
import com.lxj.xpopup.enums.PopupPosition
import com.lxj.xpopup.util.KeyboardUtils
import com.yee.launcher.R
import com.yee.launcher.constants.AppConstants
import com.yee.launcher.data.model.DesktopItemInfo
import com.yee.launcher.data.model.ItemType
import com.yee.launcher.data.model.MenuBean
import com.yee.launcher.ui.AppViewModel
import com.yee.launcher.ui.adapter.DesktopAdapter
import com.yee.launcher.ui.adapter.OnItemClickListener
import com.yee.launcher.utils.FileUtils
import com.yee.launcher.utils.IntentUtils
import com.yee.launcher.widget.DesktopRecyclerView


class DesktopPopup(val adapter: DesktopAdapter, private val viewHolder: DesktopAdapter.DeskTopViewHolder, private val menuItem: MenuBean?, private var callBack: OnItemClickListener<MenuBean>?) : FixAttachPopupView(viewHolder.itemView.context),
    OnItemClickListener<MenuBean> {


    override fun doAfterDismiss() {
        // PartShadowPopupView要等到完全关闭再关闭输入法，不然有问题
        if (popupInfo != null && popupInfo.autoOpenSoftInput)
            KeyboardUtils.hideSoftInput(this)
        removeCallbacks(doAfterDismissTask)
        postDelayed(doAfterDismissTask, animationDuration.toLong())
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(doAfterShowTask)
    }

    companion object {
        fun show(adapter: DesktopAdapter, activity: Context, viewHolder: DesktopAdapter.DeskTopViewHolder, event: MotionEvent, callBack: OnItemClickListener<MenuBean>) {
            val desktopRecyclerView = viewHolder.itemView.parent as DesktopRecyclerView
            desktopRecyclerView.requestDisallowInterceptTouchEvent(true)
            val layout = DesktopPopup(adapter, viewHolder, null, callBack)
            XPopup.Builder(activity)
                .isViewMode(true)
                .hasShadowBg(false)
                .moveUpToKeyboard(false)
                .shadowBgColor(Color.TRANSPARENT)
                .autoDismiss(false)
                .isClickThrough(true)
                .isTouchThrough(true)
                .isDestroyOnDismiss(true)
                .animationDuration(200)
                .dismissOnTouchOutside(true)
                .atPoint(PointF(event.rawX, event.rawY))
                .isRequestFocus(false)
                .setPopupCallback(object : SimplePopupCallback() {
                    override fun beforeDismiss(popupView: BasePopupView?) {
                        layout.childPopup?.dismiss()
                    }

                    override fun onDismiss(popupView: BasePopupView?) {
                        super.onDismiss(popupView)
                        desktopRecyclerView.requestDisallowInterceptTouchEvent(false)
                    }
                })
                .asCustom(layout)
                .show()
        }
    }

    override fun getPopupWidth(): Int {
        if (!viewHolder.isNull()) {
            return context.resources.getDimensionPixelSize(R.dimen.dp_174)
        } else if (menuItem == null) {
            return context.resources.getDimensionPixelSize(R.dimen.dp_238)
        } else if (menuItem.itemType == MenuBean.MENU_TYPE_VIEW || menuItem.itemType == MenuBean.MENU_TYPE_SORT) {
            return context.resources.getDimensionPixelSize(R.dimen.dp_174)
        }
        return context.resources.getDimensionPixelSize(R.dimen.dp_238)
    }

    override fun getImplLayoutId(): Int {
        return R.layout.layout_popup_window
    }

    private lateinit var mAdapter: PopMenuAdapter
    private var parentPopup: DesktopPopup? = null
    private var childPopup: DesktopPopup? = null


    override fun onCreate() {
        super.onCreate()
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        mAdapter = PopMenuAdapter(this)
        mAdapter.hoverListener = object : OnItemClickListener<MenuBean> {
            override fun onItemClick(view: View, item: MenuBean) {
                onHover(view, item)
            }
        }
        recyclerView.adapter = mAdapter


        val list = ArrayList<MenuBean>()
        if (menuItem == null) {
            //点击桌面
            if (viewHolder.isNull()) {
                //点击空白处
                list.add(MenuBean(R.drawable.ic_view, context.getString(R.string.menu_view), MenuBean.MENU_TYPE_VIEW, R.drawable.ic_arrow_right, true))
                list.add(MenuBean(R.drawable.ic_menu_sort, context.getString(R.string.menu_sort), MenuBean.MENU_TYPE_SORT, R.drawable.ic_arrow_right, true))
                list.add(MenuBean(R.drawable.ic_menu_choose_wallpaper, context.getString(R.string.menu_choose_wallpaper), MenuBean.MENU_TYPE_WALLPAPER, 0, true))
                var lastMenu = MenuBean(R.drawable.ic_new_folder, context.getString(R.string.menu_new_folder), MenuBean.MENU_TYPE_NEW_FOLDER, 0, false)
                list.add(lastMenu)
                AppViewModel.getInstance().canPaste { canPaste ->
                    if (canPaste) {
                        lastMenu.showDivider = true
                        list.add(MenuBean(R.drawable.ic_menu_paste, context.getString(R.string.menu_paste), MenuBean.MENU_TYPE_PASTE, 0, false))
                        mAdapter.notifyItemChanged(list.size - 2)
                        mAdapter.notifyItemInserted(list.size - 1)
                    }
                }
            } else {
                val selectItemList = getSelectedItemList()
                var isAllFile = true
                selectItemList.forEach {
                    if (it.getItemType() != ItemType.ITEM_TYPE_FILE && it.getItemType() != ItemType.ITEM_TYPE_FILE_FOLDER) {
                        isAllFile = false
                    }
                }
                list.add(MenuBean(0, context.getString(R.string.menu_open), MenuBean.MENU_TYPE_OPEN, 0))
//                if (viewHolder.itemInfo!!.getItemType() == ItemType.ITEM_TYPE_FILE) {
//                    list.add(MenuBean(0, context.getString(R.string.menu_default_app), MenuBean.MENU_TYPE_DEFAULT_APP, 0))
//                }
                var lastMenu = MenuBean(0, context.getString(R.string.menu_rename), MenuBean.MENU_TYPE_RENAME, 0)
                list.add(lastMenu)
                if (viewHolder.itemInfo!!.getItemType() == ItemType.ITEM_TYPE_TRASH) { //回收站
                    lastMenu = MenuBean(0, context.getString(R.string.menu_clean_trash), MenuBean.MENU_TYPE_CLEAN_TRASH, 0)
                    list.add(lastMenu)
                } else {
                    if (isAllFile) {
                        list.add(MenuBean(0, context.getString(R.string.menu_copy), MenuBean.MENU_TYPE_COPY, 0))
                        list.add(MenuBean(0, context.getString(R.string.menu_cut), MenuBean.MENU_TYPE_CUT, 0))
                    }
                    lastMenu = MenuBean(0, context.getString(R.string.menu_remove), MenuBean.MENU_TYPE_REMOVE, 0)
                    list.add(lastMenu)
                }
                if (selectItemList.size == 1 && viewHolder.itemInfo!!.getItemType() == ItemType.ITEM_TYPE_APPLICATION && !viewHolder.itemInfo!!.itemInfo.isSystemApp) {
                    lastMenu = MenuBean(0, context.getString(R.string.menu_uninstall), MenuBean.MENU_TYPE_UNINSTALL, 0)
                    list.add(lastMenu)
                }
                lastMenu.showDivider = false
            }
        } else if (menuItem.itemType == MenuBean.MENU_TYPE_SORT) {
            list.add(MenuBean(0, context.getString(R.string.menu_sort_by_name), MenuBean.SORT_BY_NAME, 0))
            list.add(MenuBean(0, context.getString(R.string.menu_sort_by_type), MenuBean.SORT_BY_TYPE, 0))
            list.add(MenuBean(0, context.getString(R.string.menu_sort_by_date), MenuBean.SORT_BY_DATE, 0, false))
        } else if (menuItem.itemType == MenuBean.MENU_TYPE_VIEW) {
            list.add(MenuBean(0, context.getString(R.string.menu_view_18x8), MenuBean.VIEW_18X8, 0))
            list.add(MenuBean(0, context.getString(R.string.menu_view_18x9), MenuBean.VIEW_18X9, 0))
            list.add(MenuBean(0, context.getString(R.string.menu_view_19x8), MenuBean.VIEW_19X8, 0))
            list.add(MenuBean(0, context.getString(R.string.menu_view_19x9), MenuBean.VIEW_19X9, 0))
            list.add(MenuBean(0, context.getString(R.string.menu_view_20x10), MenuBean.VIEW_20X10, 0, false))
        }

        mAdapter.mData = list
        mAdapter.notifyDataSetChanged()
    }

    fun onHover(view: View, item: MenuBean) {
        when (item.itemType) {
            MenuBean.MENU_TYPE_SORT, MenuBean.MENU_TYPE_VIEW -> {
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({
                    run {
                        showChildPopup(view, item)
                    }
                }, 200)
            }

            else -> {
                handler.removeCallbacksAndMessages(null)
                childPopup?.dismiss()
            }
        }
    }

    fun getSelectedItemList(): ArrayList<DesktopItemInfo> {
        val selectedItemList = adapter.getSelectedItemList()
        if (selectedItemList.isEmpty() && viewHolder.itemInfo != null) {
            selectedItemList.add(viewHolder.itemInfo!!)
        }
        return selectedItemList
    }

    override fun onItemClick(view: View, item: MenuBean) {
        when (item.itemType) {
            MenuBean.MENU_TYPE_SORT, MenuBean.MENU_TYPE_VIEW -> {
                showChildPopup(view, item)
            }

            MenuBean.SORT_BY_NAME, MenuBean.SORT_BY_TYPE, MenuBean.SORT_BY_DATE -> {
                dismissAll()
                AppViewModel.getInstance().sortBy(item.itemType)
            }

            MenuBean.MENU_TYPE_OPEN -> {
                dismissAll()
                viewHolder.itemInfo?.let {
                    IntentUtils.startFromLauncher(context, viewHolder.itemView, it)
                }
            }

            MenuBean.MENU_TYPE_UNINSTALL -> {
                dismissAll { IntentUtils.getUninstallAppIntent(viewHolder.itemInfo?.getPackageName()) }
            }
            //清空回收站
            MenuBean.MENU_TYPE_CLEAN_TRASH -> {
                dismissAll()
                AppViewModel.getInstance().cleanTrash()
            }
            //删除
            MenuBean.MENU_TYPE_REMOVE -> {
                dismissAll {
                    val selectedItemList = getSelectedItemList()
                    AppViewModel.getInstance().removeItem(selectedItemList)
                }
            }
            //复制
            MenuBean.MENU_TYPE_COPY -> {
                dismissAll {
                    val selectedItemList = getSelectedItemList()
                    val fileList = selectedItemList.filter { it.getItemType() == ItemType.ITEM_TYPE_FILE || it.getItemType() == ItemType.ITEM_TYPE_FILE_FOLDER }
                        .mapNotNull { it.itemInfo.data }
                    AppViewModel.getInstance().ctrlC(fileList)
                }
            }
            //剪切
            MenuBean.MENU_TYPE_CUT -> {
                dismissAll {
                    val selectedItemList = getSelectedItemList()
                    val fileList = selectedItemList.filter { it.getItemType() == ItemType.ITEM_TYPE_FILE || it.getItemType() == ItemType.ITEM_TYPE_FILE_FOLDER }
                        .mapNotNull { it.itemInfo.data }
                    AppViewModel.getInstance().ctrlX(fileList)
                }
            }
            //粘贴
            MenuBean.MENU_TYPE_PASTE -> {
                dismissAll {
                    AppViewModel.getInstance().ctrlV(viewHolder.bindingAdapterPosition)
                }
            }
            //新建文件夹
            MenuBean.MENU_TYPE_NEW_FOLDER -> {
                dismissAll()
                val filePath = FileUtils.createNewFile(AppConstants.DESKTOP_FOLDER_PATH, true, AppConstants.NEW_FOLDER_NAME)
                if (filePath != null) {
                    MediaScannerConnection.scanFile(context, arrayOf<String>(filePath), arrayOf<String>("")) { path1: String?, url: Uri? ->
                    }
                    AppViewModel.getInstance().addFile(viewHolder.bindingAdapterPosition, filePath, true)
                }
            }

            MenuBean.VIEW_18X8, MenuBean.VIEW_18X9, MenuBean.VIEW_19X8, MenuBean.VIEW_19X9, MenuBean.VIEW_20X10 -> {
                dismissAll {
                    AppViewModel.getInstance().changeView(item.itemType)
                }
            }
            //重命名操作在adapter中处理
            MenuBean.MENU_TYPE_RENAME -> {
                dismissAll()
            }
            //更换壁纸
            MenuBean.MENU_TYPE_WALLPAPER -> {
                dismissAll {
                    val intent: Intent = Intent(Intent.ACTION_PICK).apply {
                        type = "image/*"
                    }
//                    val options = ActivityOptions.makeBasic()
//                    try {
//                        val clazz = Class.forName("android.app.ActivityOptions")
//                        val method = clazz.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
//                        method.isAccessible = true
//                        method.invoke(options, WindowConfiguration.WINDOWING_MODE_FREEFORM)
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    }
//
//                    //        options.setLaunchWindowingMode(WindowConfiguration.WINDOWING_MODE_FREEFORM);
//                    options.setLaunchBounds(FreeFormConfiguration.freeformModeVerticalRect)
                    try {
//                        (context as Activity).startActivityForResult(intent, AppConstants.RequestConstant.REQUEST_CHOOSE_WALLPAPER, options.toBundle())
                        (context as Activity).startActivityForResult(intent, AppConstants.RequestConstant.REQUEST_CHOOSE_WALLPAPER)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            else -> {
                dismissAll()
            }
        }
        callBack?.onItemClick(view, item)
    }


    private fun showChildPopup(view: View, item: MenuBean) {
        childPopup?.let {
            if (it.isShow) {
                if (it.menuItem == item) {
                    return
                }
                it.dismiss()
            }
        }
        var popupLayout = DesktopPopup(adapter, viewHolder, item, callBack)
        childPopup = XPopup.Builder(context)
            .hasShadowBg(false)
            .isViewMode(true)
            .hasShadowBg(false)
            .autoDismiss(true)
            .isDestroyOnDismiss(true)
            .isClickThrough(true)
            .isTouchThrough(true)
            .atView(view)
            .animationDuration(200)
            .dismissOnTouchOutside(false)
            .popupPosition(PopupPosition.Right)
            .offsetX(popupWidth + context.resources.getDimensionPixelSize(R.dimen.dp_10))
            .offsetY(-context.resources.getDimensionPixelSize(R.dimen.dp_67))
            .isRequestFocus(true)
            .asCustom(popupLayout) as DesktopPopup
        popupLayout.parentPopup = this
        childPopup?.show()
    }

    private fun dismissAll(runnable: Runnable? = null) {
        parentPopup?.dismiss()
        dismissWith(runnable)
    }

}
