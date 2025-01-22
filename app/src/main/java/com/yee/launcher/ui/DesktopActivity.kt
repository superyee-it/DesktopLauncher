package com.yee.launcher.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.yee.launcher.R
import com.yee.launcher.constants.AppConstants
import com.yee.launcher.data.glide.GlideApp
import com.yee.launcher.data.model.ItemType
import com.yee.launcher.databinding.ActivityDesktopBinding
import com.yee.launcher.ui.adapter.DesktopAdapter
import com.yee.launcher.widget.DesktopLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class DesktopActivity : AppCompatActivity() {

    private lateinit var viewModel: AppViewModel
    private lateinit var binding: ActivityDesktopBinding
    private lateinit var adapter: DesktopAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDesktopBinding.inflate(layoutInflater)
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            val windowInsetsController = window.insetsController
            windowInsetsController!!.hide(WindowInsets.Type.systemBars())
        }
        viewModel = AppViewModel.getInstance()

        viewModel.allItem.observe(this@DesktopActivity, Observer {
            val data = it ?: return@Observer
            adapter.updateData(data)
        })
        initView()
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.loadALlItem()
        }
    }

    private fun initView() {
        loadWallpaper()
        val value = AppViewModel.getInstance().getSpanCountColumn()
        val spanCount = value[0]
        val column = value[1]
        adapter = DesktopAdapter(column, spanCount)
        binding.listView.layoutManager = DesktopLayoutManager(this, spanCount, RecyclerView.HORIZONTAL, false)
        binding.listView.adapter = adapter
        binding.listView.setChildDrawingOrderCallback(object : RecyclerView.ChildDrawingOrderCallback {
            override fun onGetChildDrawingOrder(childCount: Int, i: Int): Int {
                var position: Int = adapter.reNameViewHolder?.bindingAdapterPosition ?: -1
                if (position < 0) {
                    return i
                } else {
                    if (i == childCount - 1) {
                        if (position > i) {
                            position = i
                        }
                        return position
                    }
                    if (i == position) {
                        return childCount - 1
                    }
                }
                return i
            }
        })
        viewModel.init(binding.listView, adapter, binding.dragView)
        viewModel.selectRangeController.selectedItemLiveData.observe(this@DesktopActivity) { selectedItems ->
            adapter.updateSelectedPosition(selectedItems)
        }
        viewModel.wallpaperChanged.observe(this@DesktopActivity) {
            loadWallpaper()
        }
        viewModel.viewType.observe(this@DesktopActivity) {
            val value = AppViewModel.getInstance().getSpanCountColumn()
            val spanCount = value[0]
            val column = value[1]
            adapter.updateSpanCount(spanCount, column)
            (binding.listView.layoutManager as GridLayoutManager).spanCount = spanCount
        }
    }

    private fun loadWallpaper() {
        GlideApp.with(this).load(viewModel.getWallpaperFilePath()).error(R.mipmap.bg_desktop).fallback(R.mipmap.bg_desktop).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(binding.ivWallpaper)
    }

    override fun onKeyUp(keyCode: Int, keyEvent: KeyEvent?): Boolean {
        keyEvent?.let { event ->
            if (event.isCtrlPressed) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_COPY, KeyEvent.KEYCODE_C -> {
                        val selectedItemList = adapter.getSelectedItemList()
                        val fileList = selectedItemList.filter { it.getItemType() == ItemType.ITEM_TYPE_FILE || it.getItemType() == ItemType.ITEM_TYPE_FILE_FOLDER }.toMutableList().mapNotNull { it -> it.itemInfo.data }
                        viewModel.ctrlC(fileList)
                    }

                    KeyEvent.KEYCODE_X -> {
                        val selectedItemList = adapter.getSelectedItemList()
                        val fileList = selectedItemList.filter { it.getItemType() == ItemType.ITEM_TYPE_FILE || it.getItemType() == ItemType.ITEM_TYPE_FILE_FOLDER }.toMutableList().mapNotNull { it -> it.itemInfo.data }
                        viewModel.ctrlX(fileList)
                    }

                    KeyEvent.KEYCODE_V -> {
                        viewModel.ctrlV()
                    }
                }
            }
        }
        return super.onKeyUp(keyCode, keyEvent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AppConstants.RequestConstant.REQUEST_CHOOSE_WALLPAPER) {
            if (resultCode == Activity.RESULT_OK) {
                data?.clipData?.let {
                    for (i in 0 until it.itemCount) {
                        val item = it.getItemAt(i)
                        val uri = item.uri
                        viewModel.saveWallpaperFile(uri)
                        break
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}