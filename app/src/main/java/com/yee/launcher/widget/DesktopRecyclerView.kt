package com.yee.launcher.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import com.lxj.xpopup.util.XPopupUtils.dp2px

class DesktopRecyclerView : RecyclerView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    private var showMask: Boolean = false
    private var maskRect: RectF? = null

    private val maskPaint = Paint().apply {
        color = Color.parseColor("#165287FF")
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint().apply {
        color = Color.parseColor("#5287FF")
        style = Paint.Style.STROKE
        strokeWidth = dp2px(context, 1f).toFloat()
    }

    fun setMaskRect(rect: RectF?): DesktopRecyclerView {
        this.maskRect = rect
        if (showMask) {
            invalidate()
        }
        return this
    }

    fun setShowMask(show: Boolean): DesktopRecyclerView {
        if (this.showMask != show) {
            this.showMask = show
            invalidate()
        }
        return this
    }


    override fun onDrawForeground(c: Canvas) {
        super.onDrawForeground(c)
        if (maskRect != null && showMask) {
            maskRect?.let {
                c.drawRect(it, maskPaint)
                c.drawRect(it, borderPaint)
            }
        }
    }
}
