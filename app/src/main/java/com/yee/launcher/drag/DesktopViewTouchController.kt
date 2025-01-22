package com.yee.launcher.drag

import android.content.Context
import android.graphics.PointF
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import com.yee.launcher.ui.adapter.DesktopAdapter
import com.yee.launcher.widget.DesktopRecyclerView

class DesktopViewTouchController(
    private val mContext: Context,
    private val recyclerView: DesktopRecyclerView,
    private val desktopAdapter: DesktopAdapter,
    private val selectRangeController: SelectRangeController,
    private val dragController: DragLayout
) : OnItemTouchListener {
    private val gestureListener: SimpleOnGestureListener
    private val gestureDetector: GestureDetector
    private var currentState = STATE_IDLE
    private var startMotion: MotionEvent? = null
    private var disallowIntercept: Boolean = false

    init {
        gestureListener = object : SimpleOnGestureListener() {
            var isSecondaryPressed = false
            override fun onDown(e: MotionEvent): Boolean {
                isSecondaryPressed = (e.buttonState and MotionEvent.BUTTON_SECONDARY) == MotionEvent.BUTTON_SECONDARY
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (!isSecondaryPressed) {
                    selectRangeController.handlerClick(recyclerView, e)
                }
                return false
            }

            //长按后，既不抬起也不移动触发
            //            setIsLongpressEnabled:false 不触发该事件   true:触发该事件，但是触发后移动鼠标不会触发onScroll事件
            override fun onLongPress(e: MotionEvent) {
                super.onLongPress(e)
            }

            /**
             * @param e1 手指按下时的事件.
             * @param e2 当前事件
             * @param distanceX 距离上次事件的移动距离.
             * @param distanceY
             * @return
             */
            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float
            ): Boolean {
                if (disallowIntercept) {
                    return false
                }
                var e1 = e1
                if (e1 == null) {
                    e1 = startMotion
                }
                if (currentState == STATE_IDLE) {
                    val viewHolder = getViewHolderUnder(recyclerView, e1) ?: return false
                    currentState = if (desktopAdapter.handleDrag(viewHolder, e1!!)) {
                        STATE_DRAG
                    } else {
                        STATE_SELECT
                    }
                }
                if (currentState == STATE_SELECT) {
                    selectRangeController.handleMove(recyclerView, e1!!, e2)
                } else if (currentState == STATE_DRAG) {
                    dragController.handleMove(e1!!, e2)
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                return false
            }

            //单击确认
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
//                if ((e.buttonState and MotionEvent.BUTTON_SECONDARY) == MotionEvent.BUTTON_SECONDARY) {
//                    val viewHolder = getViewHolderUnder(recyclerView, e)
//                    if (viewHolder != null) {
//                        DesktopPopup.show(viewHolder.itemView.context, viewHolder, e)
//                    }
//                }
                return false
            }
        }
        gestureDetector = GestureDetector(mContext, gestureListener)
        gestureDetector.setIsLongpressEnabled(false)
    }


    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (desktopAdapter.interceptTouchEvent(e)) {
            return false
        }
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                startMotion = MotionEvent.obtain(e)
                gestureDetector.onTouchEvent(e)
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                gestureDetector.onTouchEvent(e)
                return false
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                gestureDetector.onTouchEvent(e)
                if (!disallowIntercept) {
                    if (currentState == STATE_SELECT) {
                        selectRangeController.handleEnd(recyclerView)
                    } else if (currentState == STATE_DRAG) {
                        dragController.handleEnd(PointF(e.x, e.y))
                    }
                    currentState = STATE_IDLE
                }
                return false
            }
        }
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        this.disallowIntercept = disallowIntercept
    }


    private fun getViewHolderUnder(rv: RecyclerView, e: MotionEvent?): DesktopAdapter.DeskTopViewHolder? {
        val view = rv.findChildViewUnder(e!!.x, e.y)
        if (view != null) {
            val viewHolder = rv.getChildViewHolder(view)
            return viewHolder as DesktopAdapter.DeskTopViewHolder
        }
        return null
    }

    companion object {
        private const val STATE_IDLE = 0
        private const val STATE_SELECT = 1
        private const val STATE_DRAG = 2
    }
}
