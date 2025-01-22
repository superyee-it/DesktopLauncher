package com.yee.launcher.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.yee.launcher.databinding.ItemDesktopViewBinding

/**
 * 按需延时初始化，桌面空白处只加载空布局
 */
class DesktopItemView(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    private lateinit var binding: ItemDesktopViewBinding

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : this(context, attrs, defStyleAttr, 0)

    override fun setVisibility(visibility: Int) {
//        super.setVisibility(visibility);
        if (!::binding.isInitialized && visibility == VISIBLE) {
            clipChildren = false
            clipToPadding = false
            initView()
        } else {
            if (::binding.isInitialized) {
                binding.root.visibility = visibility
            }
        }
    }

    fun isVisible(): Boolean {
        return ::binding.isInitialized && binding.root.visibility == VISIBLE
    }

    fun getBinding(): ItemDesktopViewBinding {
        if (!::binding.isInitialized) {
            throw RuntimeException("请先调用setVisibility(View.VISIBLE)")
        }

        return binding
    }


    private fun initView() {
        binding = ItemDesktopViewBinding.inflate(LayoutInflater.from(context), this, false)
        val mInflateView: View = binding.root
        val layoutParams = mInflateView.layoutParams as LayoutParams
        layoutParams.width = getLayoutParams().width
        layoutParams.height = getLayoutParams().height
        if (layoutParams.gravity == LayoutParams.UNSPECIFIED_GRAVITY) {
            layoutParams.gravity = Gravity.CENTER
        }
        mInflateView.layoutParams = layoutParams
        addView(mInflateView)
    }
}