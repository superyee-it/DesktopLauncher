package com.yee.launcher.ui.adapter

import android.graphics.Bitmap
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.signature.ObjectKey
import com.yee.launcher.R
import com.yee.launcher.data.glide.GlideApp
import com.yee.launcher.data.model.DesktopItemInfo
import com.yee.launcher.data.model.ItemType
import com.yee.launcher.data.model.MenuBean
import com.yee.launcher.popup.DesktopPopup
import com.yee.launcher.ui.AppViewModel
import com.yee.launcher.utils.BitmapUtil
import com.yee.launcher.utils.ViewUtils
import com.yee.launcher.utils.IntentUtils
import com.yee.launcher.utils.ScreenUtils
import com.yee.launcher.widget.DesktopItemView
import java.util.concurrent.ConcurrentHashMap


class DesktopAdapter(var columns: Int, var spanCount: Int) :
    RecyclerView.Adapter<DesktopAdapter.DeskTopViewHolder>() {


    private var itemList: ConcurrentHashMap<Int, DesktopItemInfo>? = null
    private var selectedPositionArray = HashSet<Int>()
    var reNameViewHolder: DeskTopViewHolder? = null
    private var screentWidth: Int = ScreenUtils.getScreenWidth()

    class DeskTopViewHolder : RecyclerView.ViewHolder {
        val rootView: DesktopItemView
        var itemInfo: DesktopItemInfo? = null

        constructor(view: DesktopItemView) : super(view) {
            rootView = view
        }

        fun initListener(adapter: DesktopAdapter, doubleClickCallback: OnItemClickListener<MotionEvent>, popupCallback: OnItemClickListener<MenuBean>) {
            val gestureListener: GestureDetector.SimpleOnGestureListener =
                object : GestureDetector.SimpleOnGestureListener() {
                    var isSecondaryPressed = false
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        doubleClickCallback?.onItemClick(itemView, e)
                        itemInfo?.let {
                            IntentUtils.startFromLauncher(itemView.context, itemView, it)
                        }
                        return false
                    }

                    override fun onDown(e: MotionEvent): Boolean {
                        isSecondaryPressed = (e.buttonState and MotionEvent.BUTTON_SECONDARY) == MotionEvent.BUTTON_SECONDARY
                        return super.onDown(e)
                    }

                    override fun onSingleTapUp(e: MotionEvent): Boolean {
                        //这里判断不出是哪个按键,所以根据down事件来判断是哪个按键
                        if (isSecondaryPressed) {
                            isSecondaryPressed = false
                            //右键菜单
                            if (isNull()) {
                                //右键空白处清空所有选中
                                adapter.updateSelectedPosition(HashSet())
                            } else {
                                //当前没有选中的话，则清空其他选中，只选中当前
                                if (!adapter.selectedPositionArray.contains(bindingAdapterPosition)) {
                                    adapter.selectedPositionArray.clear()
                                    adapter.selectedPositionArray.add(bindingAdapterPosition)
                                    adapter.updateSelectedPosition(adapter.selectedPositionArray)
                                }
                            }
                            DesktopPopup.show(adapter, itemView.context, this@DeskTopViewHolder, e, popupCallback)
                        }
                        return super.onSingleTapUp(e)
                    }

                }
            val gestureDetector = GestureDetector(itemView.context, gestureListener)
//            itemView.setOnLongClickListener {
//                //实现这个方法，gestureDetector才会正常执行
//                true
//            }
//          需要给itemview设置个事件，才能保证setOnTouchListener事件起作用
            itemView.setOnClickListener {}
            itemView.setOnTouchListener { _, event ->
                return@setOnTouchListener gestureDetector.onTouchEvent(event)
            }
        }

        //恢复选择状态
        fun restoreSelectState() {
            rootView.isSelected = rootView.tag as Boolean
        }

        //鼠标拖拽文件移动到该位置时，背景高亮显示
        fun showMoveState() {
            rootView.isSelected = true
        }

        fun isNull() = run { itemInfo == null }

        fun updateSelectState(isSelected: Boolean) {
            rootView.tag = isSelected
            rootView.isSelected = isSelected
        }

        fun isSelected(): Boolean {
            return rootView.tag as Boolean
        }

        fun bindData(itemInfo: DesktopItemInfo?, isSelected: Boolean) {
            if (this.itemInfo == itemInfo) {
                updateSelectState(isSelected)
                return
            }
            this.itemInfo = itemInfo
            if (itemInfo != null) {
                rootView.visibility = View.VISIBLE
                rootView.getBinding().tvTitle.setText(itemInfo.getTitle())
                if (itemInfo.getItemType() == ItemType.ITEM_TYPE_FILE) {
                    GlideApp.with(rootView.context).load(itemInfo.itemInfo.data)
                        .placeholder(R.drawable.icon_unkonw_file)
                        .fallback(R.drawable.icon_unkonw_file)
                        .signature(ObjectKey(itemInfo.itemInfo.lastUpdated)).into(rootView.getBinding().ivIcon)
                } else {
                    GlideApp.with(rootView.context).load(itemInfo).into(rootView.getBinding().ivIcon)
                }
            } else {
                rootView.getBinding()?.ivIcon?.setImageDrawable(null)
                rootView.visibility = View.INVISIBLE
            }
            updateSelectState(isSelected)
        }

        fun createBitmap(): Bitmap? {
            rootView.getBinding().apply {
                bgView.visibility = View.GONE
                //如果不禁止缓存，调用draw方法，会出现app图标错位情况
                ivIcon.destroyDrawingCache()
                ivIcon.isDrawingCacheEnabled = false
                val maxLines = tvTitle.maxLines
                tvTitle.maxLines = 2
                val bitmap = BitmapUtil.getBitmapFromView(rootView)
                tvTitle.maxLines = maxLines
                bgView.visibility = View.VISIBLE
                return bitmap
            }

        }
    }

    fun interceptTouchEvent(e: MotionEvent): Boolean {
        if (isRenaming()) {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!ViewUtils.isClickInView(reNameViewHolder!!.rootView.getBinding().tvTitle, e.rawX, e.rawY)) {
                        cancelRename()
                    }
                }
            }
            return true
        }
        return false
    }

    private fun isRenaming(): Boolean {
        return reNameViewHolder != null
    }

    private fun cancelRename() {
        reNameViewHolder?.let {
            it.rootView.getBinding().tvTitle.enableEdit(false)
            val newTitle = it.rootView.getBinding().tvTitle.text.toString()
            if (it.itemInfo!!.getTitle() != newTitle) {
                AppViewModel.getInstance().reName(reNameViewHolder!!, newTitle)
            }
            it.itemView.parent?.requestDisallowInterceptTouchEvent(false)
            reNameViewHolder = null
        }
    }

    //    是否执行拖拽操作,判断逻辑是光标处在icon区域内
    fun handleDrag(viewHolder: DeskTopViewHolder, e: MotionEvent): Boolean {
        if (viewHolder.isNull()) {
            return false
        }
        if (selectedPositionArray.contains(viewHolder.bindingAdapterPosition)) {
            //如果已经选中，直接判断光标是否在高亮背景内
            if (ViewUtils.isClickInView(viewHolder.rootView.getBinding().bgView, e.rawX, e.rawY)) {
                return true
            }
        }
        //没有选中，判断光标是否在icon范围内
        val isClickInsideView = ViewUtils.isClickInView(viewHolder.rootView.getBinding().ivIcon, e.rawX, e.rawY)
        if (isClickInsideView && !selectedPositionArray.contains(viewHolder.bindingAdapterPosition)) {
            selectedPositionArray.clear()
            selectedPositionArray.add(viewHolder.bindingAdapterPosition)
            updateSelectedPosition(selectedPositionArray)
        }
        return isClickInsideView
    }

    fun getItemData(): ConcurrentHashMap<Int, DesktopItemInfo> {
        return itemList ?: ConcurrentHashMap()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DeskTopViewHolder {
//        val binding = ItemDesktopViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        binding.root.layoutParams.width = parent.width / columns
        val itemView = DesktopItemView(parent.context)
        ScreenUtils.getScreenWidth()
        val layoutParams = RecyclerView.LayoutParams(screentWidth / columns, RecyclerView.LayoutParams.MATCH_PARENT)
        itemView.layoutParams = layoutParams
        val viewHolder = DeskTopViewHolder(itemView)
        viewHolder.initListener(this@DesktopAdapter, object : OnItemClickListener<MotionEvent> {
            override fun onItemClick(view: View, item: MotionEvent) {
                cancelRename()
            }
        }, object : OnItemClickListener<MenuBean> {
            override fun onItemClick(view: View, item: MenuBean) {
                when (item.itemType) {
                    MenuBean.MENU_TYPE_RENAME -> {
                        //重命名
                        reNameViewHolder = viewHolder
//                        reNameViewHolder?.itemView?.parent?.let {
//                            (it as ViewGroup).invalidate()
//                        }
                        reNameViewHolder?.itemView?.parent?.requestDisallowInterceptTouchEvent(true)
                        reNameViewHolder!!.rootView.getBinding().tvTitle.enableEdit(true).requestFocus()
//                        reNameViewHolder!!.itemView.requestLayout()
                    }
                }
            }
        })
        return viewHolder
    }

    override fun getItemCount(): Int {
        return columns * spanCount
    }

    fun updateSelectedPosition(selectedArray: HashSet<Int>) {
        selectedPositionArray = HashSet(selectedArray)
        notifyItemRangeChanged(0, itemCount, "updateSelect")
    }

    fun updateData(list: ConcurrentHashMap<Int, DesktopItemInfo>?) {
        itemList = list
        //最后面参数一定要设置一个非空值，否则会重复调用onCreateHolder，造成屏幕闪烁
        notifyItemRangeChanged(0, itemCount, "updateData")
    }

    fun updateSpanCount(spanCount: Int, columns: Int) {
        this.spanCount = spanCount
        this.columns = columns
        notifyDataSetChanged()
    }

    fun getSelectedPositionArray(): HashSet<Int> {
        return selectedPositionArray
    }


    fun getSelectedItemList(): ArrayList<DesktopItemInfo> {
        val list = ArrayList<DesktopItemInfo>()

        selectedPositionArray.forEach { pos ->
            itemList?.get(pos)?.let { item ->
                list.add(item)
            }
        }
        return list
    }


    override fun onBindViewHolder(
        holder: DeskTopViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else if ("updateSelect" == payloads[0]) {
            holder.updateSelectState(selectedPositionArray.contains(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(
        holder: DeskTopViewHolder,
        position: Int
    ) {
        itemList?.get(position)?.let { itemInfo ->
            holder.bindData(itemInfo, selectedPositionArray.contains(position))
        } ?: run {
            holder.bindData(null, false)
        }
    }
}