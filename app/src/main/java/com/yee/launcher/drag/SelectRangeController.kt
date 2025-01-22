package com.yee.launcher.drag

import android.graphics.RectF
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.yee.launcher.ui.adapter.DesktopAdapter
import com.yee.launcher.widget.DesktopRecyclerView
import kotlin.math.max
import kotlin.math.min

/**
 * 鼠标框选控制器
 */
class SelectRangeController {
    private val selectionRect = RectF()
    private val selectedItems = HashSet<Int>()
    var selectedItemLiveData: MutableLiveData<HashSet<Int>> = MutableLiveData()


    fun handleMove(
        recyclerView: DesktopRecyclerView,
        startMotion: MotionEvent,
        currentMotion: MotionEvent
    ) {
        selectionRect.right = max(startMotion.x.toDouble(), currentMotion.x.toDouble()).toFloat()
        selectionRect.bottom = max(startMotion.y.toDouble(), currentMotion.y.toDouble()).toFloat()
        selectionRect.left = min(startMotion.x.toDouble(), currentMotion.x.toDouble()).toFloat()
        selectionRect.top = min(startMotion.y.toDouble(), currentMotion.y.toDouble()).toFloat()
        recyclerView.setShowMask(true)
        recyclerView.setMaskRect(selectionRect)
        findRangeView(recyclerView)
    }

    fun handleEnd(recyclerView: DesktopRecyclerView) {
        recyclerView.setShowMask(false)
        recyclerView.setMaskRect(null)
    }

    private fun findRangeView(recyclerView: RecyclerView) {
        var endPosition = recyclerView.adapter!!.itemCount
        selectedItems.clear()
        for (i in 0..endPosition) {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(i) as DesktopAdapter.DeskTopViewHolder?
            if (viewHolder != null && !viewHolder.isNull() && selectionRect.intersects(
                    viewHolder.itemView.left.toFloat(),
                    viewHolder.itemView.top.toFloat(),
                    viewHolder.itemView.right.toFloat(),
                    viewHolder.itemView.bottom.toFloat()
                )
            ) {
                selectedItems.add(i)
            }
        }
        selectedItemLiveData.value = selectedItems
    }

    fun handlerClick(recyclerView: RecyclerView, event: MotionEvent): Boolean {
        if (isCtrlPress(recyclerView, event) || isShiftPress(recyclerView, event)) {
            return true
        }
        val clickPosition = getItemPositionUnder(recyclerView, event)
        selectedItems.clear()
        if (clickPosition != RecyclerView.NO_POSITION) {
            selectedItems.add(clickPosition)
        }
        selectedItemLiveData.value = selectedItems
        return true
    }

    private fun isCtrlPress(recyclerView: RecyclerView, event: MotionEvent): Boolean {
        if ((event.metaState and KeyEvent.META_CTRL_ON) == 0) {
            return false
        }
        val position = getItemPositionUnder(recyclerView, event)
        if (position != RecyclerView.NO_POSITION) {
            if (selectedItems.contains(position)) {
                selectedItems.remove(position)
            } else {
                selectedItems.add(position)
            }
            selectedItemLiveData.value = selectedItems
        }
        return true
    }


    private fun getItemPositionUnder(recyclerView: RecyclerView, event: MotionEvent): Int {
        val view = recyclerView.findChildViewUnder(event.x, event.y)
        if (view != null) {
            val viewHolder = recyclerView.getChildViewHolder(view)
            if (viewHolder is DesktopAdapter.DeskTopViewHolder) {
                if (!viewHolder.isNull()) {
                    return viewHolder.getAdapterPosition()
                }
            }
        }
        return RecyclerView.NO_POSITION
    }

    private fun isShiftPress(recyclerView: RecyclerView, event: MotionEvent): Boolean {
        if ((event.metaState and KeyEvent.META_SHIFT_ON) == 0) {
            return false
        }
        val position = getItemPositionUnder(recyclerView, event)
        val min = getEdgePosition(0)
        val max = getEdgePosition(1)
        if (position != RecyclerView.NO_POSITION) {
            selectedItems.clear()
            val start: Int
            val end: Int
            if (position < min) {
                start = position
                end = max
            } else if (position < max) {
                start = position
                end = max
            } else {
                start = min
                end = position
            }
            for (i in start..end) {
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(i)
                if (viewHolder is DesktopAdapter.DeskTopViewHolder) {
                    if (!viewHolder.isNull()) {
                        selectedItems.add(i)
                    }
                }
            }
            selectedItemLiveData.value = selectedItems
        }
        return true
    }

    /**
     * 获取最小值或最大值
     *
     * @param type 0最小值 非0最大值
     * @return
     */
    private fun getEdgePosition(type: Int): Int {
        return if (type == 0) {
            selectedItems.stream().min { o1: Int, o2: Int -> o1 - o2 }.get()
        } else {
            selectedItems.stream().max { o1: Int, o2: Int -> o1 - o2 }.get()
        }
    }
}
