package com.yee.launcher.drag

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import android.util.SparseArray
import android.view.MotionEvent
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.yee.launcher.data.model.DesktopItemInfo
import com.yee.launcher.data.model.ItemType
import com.yee.launcher.ui.AppViewModel
import com.yee.launcher.ui.adapter.DesktopAdapter
import com.yee.launcher.utils.FileUtils
import java.util.function.Consumer

open class DragLayout : ConstraintLayout {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        visibility = GONE
    }

    protected lateinit var recyclerView: RecyclerView
    protected lateinit var desktopAdapter: DesktopAdapter
    protected lateinit var viewModel: AppViewModel
    protected var lastFolderViewHolder: DesktopAdapter.DeskTopViewHolder? = null

    //拖拽的内容是否可以移动到文件夹（逻辑是拖拽内容中有文件夹或文件）
    protected var canMoveToFolder = false

    fun init(recyclerView: RecyclerView, adapter: DesktopAdapter) {
        this.recyclerView = recyclerView
        this.desktopAdapter = adapter
        viewModel = AppViewModel.getInstance()
    }


    //处理图标跟随鼠标移动逻辑
    open fun handleMove(startMotion: MotionEvent, currentMotion: MotionEvent) {
        if (visibility != VISIBLE) {
            removeAllViews()
            visibility = VISIBLE
            addDragView(startMotion)
        }
        translationX = currentMotion.rawX - startMotion.rawX
        translationY = currentMotion.rawY - startMotion.rawY
        if (canMoveToFolder) {
            //移动的内容中有文件或文件夹，如果鼠标移动到文件上时，让文件夹高亮，意味着可以将内容移动到文件夹内
            val view = recyclerView.findChildViewUnder(currentMotion.x, currentMotion.y)
            view?.let {
                val viewHolder = recyclerView.getChildViewHolder(it) as DesktopAdapter.DeskTopViewHolder
                if (!viewHolder.isNull() && !isMoving(viewHolder.bindingAdapterPosition) && viewHolder.itemInfo?.getItemType() == ItemType.ITEM_TYPE_FILE_FOLDER) {
                    //文件或文件夹
                    if (lastFolderViewHolder != viewHolder) {
                        //如果当前选中的文件夹与上一个文件夹不同，则将上一个文件夹的选中状态取消
                        lastFolderViewHolder?.restoreSelectState()
                        lastFolderViewHolder = viewHolder
                        viewHolder.showMoveState()
                    }
                } else {
                    lastFolderViewHolder?.restoreSelectState()
                    lastFolderViewHolder = null
                }
            } ?: {
                lastFolderViewHolder?.restoreSelectState()
                lastFolderViewHolder = null
            }
        }
    }

    open fun handleEnd(point: PointF) {
        if (lastFolderViewHolder != null) {
            lastFolderViewHolder?.restoreSelectState()
            lastFolderViewHolder = null
        }
        move(point)
        removeAllViews()
        visibility = GONE
        canMoveToFolder = false
        lastFolderViewHolder = null
        translationY = 0f
        translationX = 0f
    }

    //处理图标排列逻辑
    fun move(event: PointF) {
        val view = recyclerView.findChildViewUnder(event.x, event.y) ?: return
        val selectedPositionArray = desktopAdapter.getSelectedPositionArray()
        val newDataList = viewModel.getDataList()
        var currViewHolder = recyclerView.getChildViewHolder(view) as DesktopAdapter.DeskTopViewHolder
        val mousePosition = currViewHolder.bindingAdapterPosition
        if (canMoveToFolder && !currViewHolder.isNull() && !isMoving(currViewHolder.bindingAdapterPosition)
            && (currViewHolder.itemInfo?.getItemType() == ItemType.ITEM_TYPE_FILE_FOLDER || currViewHolder.itemInfo?.getItemType() == ItemType.ITEM_TYPE_TRASH)
        ) {
            val toRemoveItemList = arrayListOf<DesktopItemInfo>()
            //将拖拽的文件移动到回收站或文件夹
            for (i in 0 until childCount) {
                val childView = getChildAt(i)
                val itemInfo = childView.tag as DesktopItemInfo
                if (itemInfo.canRemove() && (itemInfo.getItemType() == ItemType.ITEM_TYPE_FILE || itemInfo.getItemType() == ItemType.ITEM_TYPE_FILE_FOLDER)) {
                    if (currViewHolder.itemInfo?.getItemType() == ItemType.ITEM_TYPE_FILE_FOLDER) {
                        val fromPath = itemInfo.itemInfo?.data
                        val toPath = currViewHolder.itemInfo?.itemInfo?.data
                        FileUtils.moveTo(fromPath, toPath)
                    }
                    toRemoveItemList.add(itemInfo)
                }
            }
            //这里除了移除桌面图标外，还会将文件或文件及移入回收站
            AppViewModel.getInstance().removeItem(toRemoveItemList)
            selectedPositionArray.clear()
            return
        }

        selectedPositionArray.clear()
        //图标冲突，需要等待移动的原始位置图标
        val toMove = SparseArray<DesktopItemInfo>()
        //拖拽的图标未匹配到合适的位置，需要重新定位
        val noMatchPosition: MutableList<DesktopItemInfo> = ArrayList()
        val rect = Rect()
        val childCount = childCount
        //先将拖动的图标从原位置移除
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            val itemInfo = childView.tag as DesktopItemInfo
            newDataList.remove(itemInfo.getPosition())
        }
        //已经更换成了刚拖拽过来的图标了，不再摆放其它图标
        val excludePosition: MutableList<Int> = ArrayList()
        for (index in 0 until childCount) {
            val childView = getChildAt(index)
            val itemInfo = childView.tag as DesktopItemInfo
            childView.getGlobalVisibleRect(rect)
            val viewHolder = findMostOverlappingItem(rect, excludePosition)
            if (viewHolder == null) {
                //拖拽的图标未匹配到合适的位置，等待重新定位
                noMatchPosition.add(itemInfo)
            } else {
                val newPos = viewHolder.bindingAdapterPosition
                excludePosition.add(newPos)
                val originalItem = newDataList[newPos]
                if (originalItem != null) {
                    //位置被占用，需要重新摆放
                    toMove.put(newPos, originalItem)
                }
                itemInfo.changePosition(newPos)
                newDataList.put(newPos, itemInfo)
                //选中
                selectedPositionArray.add(newPos)
            }
        }
        //被替换的原始图标，先尝试移动到后面的空白位置，如果后面没有位置，再往前找空白位置
        for (i in 0 until toMove.size()) {
            val oldPos = toMove.keyAt(i)
            var moveItem = toMove[oldPos]
            for (newPos in oldPos + 1 until desktopAdapter.itemCount) {
                val item = newDataList[newPos]
                if (item == null) {
                    moveItem!!.changePosition(newPos)
                    newDataList.put(newPos, moveItem)
                    moveItem = null
                    //选中
//                    selectedPositionArray.add(newPos);
                    break
                }
            }

            //后面没有找到空白处，尝试往前找
            if (moveItem != null) {
                for (newPos in oldPos - 1 downTo 0) {
                    val item = newDataList[newPos]
                    if (item == null) {
                        moveItem.changePosition(newPos)
                        newDataList.put(newPos, moveItem)
                        //选中
//                        selectedPositionArray.add(newPos);
                        break
                    }
                }
            }
        }
        //拖拽的图标未匹配到合适的位置，从鼠标位置往后找空白处
        for (index in noMatchPosition.indices) {
            var moveItem: DesktopItemInfo? = noMatchPosition[index]
            for (newPos in mousePosition until desktopAdapter.itemCount) {
                val item = newDataList[newPos]
                if (item == null) {
                    moveItem!!.changePosition(newPos)
                    newDataList.put(newPos, moveItem)
                    //选中
                    selectedPositionArray.add(newPos)
                    moveItem = null
                    break
                }
            }
            //再往前找
            if (moveItem != null) {
                for (newPos in mousePosition downTo 0) {
                    val item = newDataList[newPos]
                    if (item == null) {
                        moveItem.changePosition(newPos)
                        newDataList.put(newPos, moveItem)
                        //选中
                        selectedPositionArray.add(newPos)
                        break
                    }
                }
            }
        }
        viewModel.notifyDataSetChanged(newDataList)
        viewModel.updateDesktopItem(newDataList)
    }

    /**
     * 计算recyclerview中与targetRect重叠区域面积最大的item
     *
     * @param targetRect
     * @return
     */
    private fun findMostOverlappingItem(
        targetRect: Rect?,
        excludePosition: List<Int>
    ): RecyclerView.ViewHolder? {
        val childCount = recyclerView.childCount
        var mostOverlappingItem: RecyclerView.ViewHolder? = null
        var maxOverlapArea = 0

        for (i in 0 until childCount) {
            val itemView = recyclerView.getChildAt(i)
            val holder = recyclerView.getChildViewHolder(itemView)
            if (excludePosition.contains(holder.bindingAdapterPosition)) {
                continue
            }
            val itemRect = Rect()
            itemView.getGlobalVisibleRect(itemRect)

            // 计算交集区域
            // 创建一个新的 Rect 对象来存储交集区域
            val intersectionRect = Rect(targetRect)
            if (intersectionRect.intersect(itemRect)) {
                val overlapArea = intersectionRect.width() * intersectionRect.height()
                if (overlapArea > maxOverlapArea) {
                    maxOverlapArea = overlapArea
                    mostOverlappingItem = recyclerView.getChildViewHolder(itemView)
                }
            }
        }

        return mostOverlappingItem
    }


    protected fun addDragView(event: MotionEvent) {
        val hashSet = desktopAdapter.getSelectedPositionArray()
        val dataSet = desktopAdapter.getItemData()
        val copy = HashSet(hashSet)
        if (copy.isEmpty()) {
            val view = recyclerView.findChildViewUnder(event.x, event.y)
            if (view != null) {
                val viewHolder = recyclerView.getChildViewHolder(view)
                if (viewHolder != null) {
                    copy.add(viewHolder.bindingAdapterPosition)
                }
            }
        }
        val location = IntArray(2)
        copy.forEach(Consumer<Int> { position: Int? ->
            val itemInfo = dataSet[position!!] ?: return@Consumer
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) as DesktopAdapter.DeskTopViewHolder?
            val imageView = ImageView(context)
            imageView.id = generateViewId()
            val bitmap = viewHolder!!.createBitmap()
            imageView.setImageBitmap(bitmap)
            val layoutParams = LayoutParams(bitmap!!.width, bitmap.height)
            layoutParams.leftToLeft = LayoutParams.PARENT_ID
            layoutParams.topToTop = LayoutParams.PARENT_ID
            //获取当前窗口的坐标，保证小窗模式下正常
            viewHolder.rootView.getLocationInWindow(location)
            layoutParams.leftMargin = location[0]
            layoutParams.topMargin = location[1]
            imageView.layoutParams = layoutParams
            imageView.tag = itemInfo
            addView(imageView, layoutParams)
            if (itemInfo.getItemType() == ItemType.ITEM_TYPE_FILE_FOLDER || itemInfo.getItemType() == ItemType.ITEM_TYPE_FILE) {
                canMoveToFolder = true
            }
        })
    }

    //position否是正在拖动的内容
    fun isMoving(position: Int): Boolean {
        return desktopAdapter.getSelectedPositionArray().contains(position)
    }
}
