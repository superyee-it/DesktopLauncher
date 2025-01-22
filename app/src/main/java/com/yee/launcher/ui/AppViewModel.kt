package com.yee.launcher.ui

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yee.launcher.R
import com.yee.launcher.constants.AppConstants
import com.yee.launcher.data.DesktopRepository
import com.yee.launcher.data.model.DesktopItemInfo
import com.yee.launcher.data.model.ItemInfo
import com.yee.launcher.data.model.ItemType
import com.yee.launcher.data.model.MenuBean
import com.yee.launcher.drag.DesktopViewTouchController
import com.yee.launcher.drag.DragLayout
import com.yee.launcher.drag.SelectRangeController
import com.yee.launcher.ui.adapter.DesktopAdapter
import com.yee.launcher.utils.FileUtils
import com.yee.launcher.utils.SPUtils
import com.yee.launcher.utils.ActivityUtils
import com.yee.launcher.utils.AppUtils
import com.yee.launcher.utils.CloseUtils
import com.yee.launcher.widget.DesktopRecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.Collator
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer


class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val _allItem = MutableLiveData<ConcurrentHashMap<Int, DesktopItemInfo>>()
    val allItem: LiveData<ConcurrentHashMap<Int, DesktopItemInfo>> = _allItem
    val viewType: MutableLiveData<Int> = MutableLiveData<Int>()
    val wallpaperChanged: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
    lateinit var selectRangeController: SelectRangeController
    private lateinit var dragController: DragLayout
    private lateinit var desktopViewTouchController: DesktopViewTouchController

    private val desktopRepository by lazy { DesktopRepository() }

    companion object {
        private var instance: AppViewModel? = null

        fun getInstance(): AppViewModel {
            return instance ?: synchronized(this) {
                instance ?: ViewModelProvider(
                    GlobalApplication.getInstance(), ViewModelProvider.AndroidViewModelFactory(GlobalApplication.getInstance())
                )[AppViewModel::class.java].also {
                    instance = it
                }
            }
        }
    }

    fun init(
        recyclerView: DesktopRecyclerView, adapter: DesktopAdapter, dragController: DragLayout
    ) {
        this.dragController = dragController
        dragController.init(recyclerView, adapter)
        selectRangeController = SelectRangeController()
        desktopViewTouchController = DesktopViewTouchController(
            recyclerView.context, recyclerView, adapter, selectRangeController, dragController
        )
        recyclerView.addOnItemTouchListener(desktopViewTouchController)
    }


    fun getDataList(): ConcurrentHashMap<Int, DesktopItemInfo> {
        return _allItem.value ?: ConcurrentHashMap<Int, DesktopItemInfo>()
    }

    fun notifyDataSetChanged(newDataList: ConcurrentHashMap<Int, DesktopItemInfo>) {
        _allItem.postValue(newDataList)
    }

    //删除桌面item，文件会被移入回收站
    fun removeItem(itemList: List<DesktopItemInfo>) {
        viewModelScope.launch(Dispatchers.IO) {
            val filesToMoveTrash = ArrayList<String>()
            val dataList = getDataList()
            itemList.forEach {
                if (it.canRemove()) {
                    it.itemInfo.let { item ->
                        if (item.itemType == ItemType.ITEM_TYPE_FILE_FOLDER || item.itemType == ItemType.ITEM_TYPE_FILE) {
                            item.data?.let { path ->
                                if (File(path).exists()) {
                                    filesToMoveTrash.add(path)
                                }
                            }
                        }
                        desktopRepository.deleteItem(getApplication(), item)
                    }
                    dataList.remove(it.getPosition())
                }
            }
            _allItem.postValue(dataList)
            moveFileToTrash(filesToMoveTrash)
        }
    }


    private fun addTrash() {
        val itemInfo = ItemInfo()
        itemInfo.position = 0
        itemInfo.title = getApplication<Application>().getString(R.string.trash)
        itemInfo.itemType = ItemType.ITEM_TYPE_TRASH
        itemInfo.lastUpdated = System.currentTimeMillis()
        itemInfo.data = "7"
        val id = desktopRepository.addItem(getApplication(), itemInfo)
        itemInfo.id = id.toInt()
        val item = DesktopItemInfo(itemInfo)
        val dataList = getDataList()
        dataList.put(itemInfo.position, item)
        _allItem.postValue(dataList)
    }


    //新建文件夹或文件夹
    fun addFile(position: Int, filePath: String, isFolder: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val itemInfo = ItemInfo()
            itemInfo.position = getAvailablePosition(position)
            if (itemInfo.position < 0) {
                return@launch
            }
            val item = DesktopItemInfo(itemInfo)
            val dataList = getDataList()
            //尽可能早的加入集合中，避免多线程操作时多个图标加入到同一位置
            dataList.put(itemInfo.position, item)

            itemInfo.title = FileUtils.getFileName(filePath)
            itemInfo.itemType = if (isFolder) ItemType.ITEM_TYPE_FILE_FOLDER else ItemType.ITEM_TYPE_FILE
            itemInfo.lastUpdated = System.currentTimeMillis()
            itemInfo.data = filePath
            _allItem.postValue(dataList)
            val id = desktopRepository.addItem(getApplication(), itemInfo)
            itemInfo.id = id.toInt()
        }
    }

    /**
     * 新建app快捷图标
     * @param position 添加到指定位置，如果当前为止已存在图标，则先向后查找空白处，如果后面没有空白处就往前查找空白处，没有空白处不会添加
     *          position为-1，则从头开始查找空白处添加，如果所有位置都存在图标，则不添加
     */
    fun addAppShortcut(position: Int, title: String?, intent: Intent) {
        val packageName = intent.`package` ?: intent.component?.packageName ?: return
        intent.flags = 0
        viewModelScope.launch(Dispatchers.IO) {
            val itemInfo = ItemInfo()
            itemInfo.position = getAvailablePosition(position)
            if (itemInfo.position < 0) {
                return@launch
            }
            val item = DesktopItemInfo(itemInfo)
            val dataList = getDataList()
            dataList.put(itemInfo.position, item)
            itemInfo.title = title
            itemInfo.itemType = ItemType.ITEM_TYPE_APPLICATION
            itemInfo.lastUpdated = System.currentTimeMillis()
            itemInfo.intent = intent
            itemInfo.isSystemApp = AppUtils.isAppSystem(getApplication(), packageName)
            _allItem.postValue(dataList)

            val id = desktopRepository.addItem(getApplication(), itemInfo)
            itemInfo.id = id.toInt()
        }
    }

    /**
     * 移除快捷图标
     */
    fun removeFileShortcut(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dataList = getDataList()
            val deletes = arrayListOf<ItemInfo>()
            dataList.forEach { (key, item) ->
                if (item.itemInfo.data == filePath && item.canRemove() && (item.itemInfo.itemType == ItemType.ITEM_TYPE_FILE || item.itemInfo.itemType == ItemType.ITEM_TYPE_FILE_FOLDER)) {
                    dataList.remove(key)
                    deletes.add(item.itemInfo)
                }
            }
            _allItem.postValue(dataList)
            desktopRepository.deleteItem(getApplication(), deletes)
        }
    }

    /**
     * 刷新桌面图标
     * 如果文件不存在，则删除桌面图标，否则刷新桌面缩略图
     */
    fun refreshFileShortcut(filePath: String?) {
        if (filePath.isNullOrEmpty()) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val dataList = getDataList()
            val deletes = arrayListOf<ItemInfo>()
            dataList.forEach { (key, item) ->
                if (item.itemInfo.data == filePath && item.canRemove() && (item.getItemType() == ItemType.ITEM_TYPE_FILE || item.getItemType() == ItemType.ITEM_TYPE_FILE_FOLDER)) {
                    val file = File(filePath)
                    if (!file.exists()) {
                        dataList.remove(key)
                        deletes.add(item.itemInfo)
                    } else {
                        if (item.itemInfo.lastUpdated != file.lastModified()) {
                            item.itemInfo.lastUpdated = file.lastModified()
                            desktopRepository.updateItem(getApplication(), item.itemInfo)
                        }
                    }
                }
            }
            if (deletes.size > 0) {
                desktopRepository.deleteItem(getApplication(), deletes)
            }
            _allItem.postValue(dataList)
        }
    }

    /**
     * 刷新应用图标，如果应用不存在则删除
     */
    fun refreshAppShortcut(packageName: String?, className: String?) {
        if (packageName.isNullOrEmpty()) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val dataList = getDataList()
            val filterList = arrayListOf<DesktopItemInfo>()
            dataList.forEach { (key, item) ->
                if (item.getItemType() == ItemType.ITEM_TYPE_APPLICATION && item.canRemove()) {
                    val pkg = item.getIntent()?.component?.packageName
                    val claName = item.getIntent()?.component?.className
                    if (packageName == pkg) {
                        if (className != null) {
                            if (className == claName) {
                                filterList.add(item)
                            }
                        } else {
                            filterList.add(item)
                        }
                    }
                }
            }
            val deletes = arrayListOf<ItemInfo>()
            for (item in filterList) {
                val pkg = item.getIntent()?.component?.packageName ?: ""
                val claName = item.getIntent()?.component?.className ?: ""
                if (!ActivityUtils.isActivityExists(getApplication(), pkg, claName)) {
                    dataList.remove(item.getPosition())
                    deletes.add(item.itemInfo)
                } else {
                    val lastUpdateTime = AppUtils.getAppLastUpdateTime(getApplication(), pkg)
                    if (item.itemInfo.lastUpdated != lastUpdateTime) {
                        item.itemInfo.lastUpdated = lastUpdateTime
                        desktopRepository.updateItem(getApplication(), item.itemInfo)
                    }
                }
            }
            if (deletes.size > 0) {
                desktopRepository.deleteItem(getApplication(), deletes)
            }
            _allItem.postValue(dataList)
        }
    }

    fun removeItemByPackageName(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dataList = getDataList()
            val deletes = arrayListOf<ItemInfo>()
            dataList.forEach { (key, item) ->
                if (item.getItemType() == ItemType.ITEM_TYPE_APPLICATION && item.canRemove()) {
                    val pkg = item.getIntent()?.component?.packageName
                    if (packageName == pkg) {
                        dataList.remove(key)
                        deletes.add(item.itemInfo)
                    }
                }
            }
            desktopRepository.deleteItem(getApplication(), deletes)
            _allItem.postValue(dataList)
        }
    }

    /**
     * 移除快捷图标
     */
    fun removeAppShortcut(packageName: String?, className: String?) {
        if (packageName.isNullOrEmpty() && className.isNullOrEmpty()) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val dataList = getDataList()
            val deletes = arrayListOf<ItemInfo>()
            dataList.forEach { (key, item) ->
                if (item.getItemType() == ItemType.ITEM_TYPE_APPLICATION && item.canRemove()) {
                    val pkg = item.getIntent()?.component?.packageName
                    val claName = item.getIntent()?.component?.className
                    if (packageName != null) {
                        if (packageName == pkg) {
                            if (className != null) {
                                if (className == claName) {
                                    dataList.remove(key)
                                    deletes.add(item.itemInfo)
                                }
                            } else {
                                dataList.remove(key)
                                deletes.add(item.itemInfo)
                            }
                        }
                    } else if (className != null) {
                        if (className == claName) {
                            dataList.remove(key)
                            deletes.add(item.itemInfo)
                        }
                    }
                }
            }
            desktopRepository.deleteItem(getApplication(), deletes)
            _allItem.postValue(dataList)
        }
    }

    /**
     * 获取position附近空白的位置
     */
    fun getAvailablePosition(position: Int): Int {
        _allItem.value?.apply {
            var cur = position
            while (containsKey(cur) || cur < 0) {
                cur++
            }
            if (cur >= getTotalItemCount()) {
                cur = position
                while (containsKey(cur)) {
                    cur--
                }
            }
            return cur
        }
        return -1
    }

    fun loadALlItem() {
        val list = desktopRepository.loadAllItemInfo(getApplication())
        val dataSet = ConcurrentHashMap<Int, DesktopItemInfo>()
        for (itemInfo in list) {
            if (itemInfo.itemType == ItemType.ITEM_TYPE_FILE_FOLDER || itemInfo.itemType == ItemType.ITEM_TYPE_FILE) {
                var file = File(itemInfo.data)
                if (!file.exists()) {
                    viewModelScope.launch(Dispatchers.IO) {
                        desktopRepository.deleteItem(getApplication(), itemInfo)
                    }
                    continue
                }
            } else if (itemInfo.itemType == ItemType.ITEM_TYPE_APPLICATION) {
                itemInfo.lastUpdated = AppUtils.getAppLastUpdateTime(getApplication(), itemInfo.intent?.component?.packageName)
                itemInfo.isSystemApp = AppUtils.isAppSystem(getApplication(), itemInfo.intent?.component?.packageName)
            }
            val item = DesktopItemInfo(itemInfo)
            dataSet.put(itemInfo.position, item)
        }
        _allItem.postValue(dataSet)
        if (dataSet.size == 0) {
            //添加回收站
            addTrash()
            //添加临时数据
            val pm: PackageManager = getApplication<Application>().packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null)
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            val activities = pm.queryIntentActivities(mainIntent, 0)
            var position = 0
            for (info in activities) {
                val mMarketInfo = ItemInfo()
                var applicationInfo: ApplicationInfo?
                try {
                    applicationInfo = pm.getApplicationInfo(info.activityInfo.packageName, 0)
                    mMarketInfo.isSystemApp =
                        ((applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) > 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    applicationInfo = null
                }

                val applicationName =
                    (if (applicationInfo != null) pm.getApplicationLabel(applicationInfo) else "unknown") as String

                mMarketInfo.title = (applicationName)


                // 为应用程序的启动Activity 准备Intent
                val launchIntent = Intent()
                launchIntent.setComponent(
                    ComponentName(
                        info.activityInfo.packageName,
                        info.activityInfo.name
                    )
                )
                mMarketInfo.intent = (launchIntent)
                mMarketInfo.position = position
                position++
                if (position < 20) {
                    desktopRepository.addItem(getApplication(), mMarketInfo)
                }
                loadALlItem()
            }
        }
    }

    //保存更新
    fun updateDesktopItem(dataSet: ConcurrentHashMap<Int, DesktopItemInfo>) {
        viewModelScope.launch(Dispatchers.IO) {
            val itemList = ArrayList<ItemInfo>()
            dataSet.forEach { (key, value) ->
                itemList.add(value.itemInfo)
            }
            updateListItem(itemList)
        }
    }

    private fun updateListItem(arrayList: ArrayList<ItemInfo>) {
        desktopRepository.updateItem(getApplication(), arrayList)
    }


    //重命名
    fun reName(viewHolder: DesktopAdapter.DeskTopViewHolder, newName: String) {
        val deskItemInfo = viewHolder.itemInfo
        val itemInfo = deskItemInfo?.itemInfo
        itemInfo?.let {
            it.title = newName
            it.lastUpdated = System.currentTimeMillis()
            viewModelScope.launch(Dispatchers.IO) {
                if (it.itemType == ItemType.ITEM_TYPE_FILE_FOLDER || it.itemType == ItemType.ITEM_TYPE_FILE) {
                    val path = it.data
                    val file = File(path)
                    if (file.exists()) {
                        val newFile = FileUtils.renameFile(path, newName)
                        if (newFile != null) {
                            it.data = newFile.path
                            it.title = newFile.name
                            withContext(Dispatchers.Main) {
                                if (viewHolder.itemInfo == deskItemInfo) {
                                    viewHolder.rootView.getBinding()?.tvTitle?.setText(newFile.name)
                                }
                            }
                        }
                    } else {
                        viewHolder.rootView.getBinding()?.tvTitle?.setText(file.name)
                    }
                }
                updateListItem(arrayListOf(it))
            }
        }
    }

    //排序
    fun sortBy(sortType: Int) {
        val dataList = getDataList()
        val itemList = ArrayList<ItemInfo>()
        dataList.forEach { (key, item) ->
            itemList.add(item.itemInfo)
        }
        when (sortType) {
            MenuBean.SORT_BY_NAME -> {
                val comparator = Collator.getInstance(Locale.CHINA)
                comparator.setStrength(Collator.PRIMARY)
                itemList.sortWith(compareBy(comparator) { it.title })
            }

            MenuBean.SORT_BY_TYPE -> {
                val comparator = Collator.getInstance(Locale.CHINA)
                comparator.setStrength(Collator.PRIMARY)
                itemList.sortWith(compareBy(comparator) { it.title })
                itemList.sortBy { it.itemType }
            }

            MenuBean.SORT_BY_DATE -> itemList.sortBy { it.lastUpdated }
        }
        for (i in 0 until itemList.size) {
            itemList[i].position = i
        }
        val newData = ConcurrentHashMap<Int, DesktopItemInfo>()
        for (itemInfo in itemList) {
            val item = DesktopItemInfo(itemInfo)
            newData.put(itemInfo.position, item)
        }
        _allItem.postValue(newData)
        viewModelScope.launch(Dispatchers.IO) {
            desktopRepository.updateItem(getApplication(), itemList)
        }
    }


    //修改行列数
    fun changeView(type: Int) {
        var value = "9x18"
        when (type) {
            MenuBean.VIEW_18X8 -> value = "8x18"
            MenuBean.VIEW_18X9 -> value = "9x18"
            MenuBean.VIEW_19X8 -> value = "8x19"
            MenuBean.VIEW_19X9 -> value = "9x19"
            MenuBean.VIEW_20X10 -> value = "10x20"
        }
        SPUtils.getInstance().save(AppConstants.SP.VIEW_SPANCOUNT_COLUMN, value)
        viewType.postValue(type)
    }

    fun getSpanCountColumn(): IntArray {
        val value = SPUtils.getInstance().getString(AppConstants.SP.VIEW_SPANCOUNT_COLUMN, "9x18")
        val array = IntArray(2)
        try {
            array[0] = value!!.split("x")[0].toInt()
            array[1] = value!!.split("x")[1].toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            array[0] = 9
            array[1] = 18
        }

        return array
    }

    fun getTotalItemCount(): Int {
        val span = getSpanCountColumn()
        return span[0] * span[1]
    }

    /**
     * 清空回收站
     */
    fun cleanTrash() {
    }

    fun copyFile(position: Int, destFolderPath: String, move: Boolean, fileToCopy: List<String>) {
        if (fileToCopy.isNullOrEmpty()) {
            return
        }
    }

    fun ctrlX(fileToCopy: List<String>?) {
        if (fileToCopy.isNullOrEmpty()) {
            return
        }
    }

    fun ctrlC(fileToCopy: List<String>?) {
        if (fileToCopy.isNullOrEmpty()) {
            return
        }
    }


    fun ctrlV(position: Int = 0) {
    }

    /**
     * 是否可以粘贴
     */
    fun canPaste(consume: Consumer<Boolean>) {
    }

    /**
     * 将文件移入回收站
     */
    fun moveFileToTrash(filesToMoveTrash: ArrayList<String>) {
        if (filesToMoveTrash.isEmpty()) {
            return;
        }
    }

    fun getWallpaperFilePath(): String? {
        return getApplication<Application>().applicationContext.getFileStreamPath("wallpaper.png")?.absolutePath
    }

    fun saveWallpaperFile(uri: Uri) {
        viewModelScope.launch {
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
            val wallpaperFilePath = getWallpaperFilePath()
            var outputStream: FileOutputStream? = null
            try {
                if (inputStream != null && wallpaperFilePath != null) {
                    val wallpaperFile = File(wallpaperFilePath)
                    if (wallpaperFile.exists()) {
                        wallpaperFile.delete()
                    }
                    outputStream = FileOutputStream(wallpaperFile)
                    val buffer = ByteArray(1024)
                    var len: Int
                    while (inputStream.read(buffer).also { len = it } != -1) {
                        outputStream.write(buffer, 0, len)
                    }
                    outputStream.close()
                    inputStream.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                CloseUtils.closeIO(inputStream, outputStream)
            }
            wallpaperChanged.postValue(true)
        }
    }

}