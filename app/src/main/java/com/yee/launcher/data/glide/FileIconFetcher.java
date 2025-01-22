package com.yee.launcher.data.glide;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.yee.launcher.R;
import com.yee.launcher.data.model.DesktopItemInfo;
import com.yee.launcher.data.model.ItemType;
import com.yee.launcher.utils.BitmapUtil;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * 获取文件图标
 */
public class FileIconFetcher implements DataFetcher<InputStream> {
    private DesktopItemInfo appInfo;
    private Context context;
    private final PackageManager packageManager;
    private InputStream inputStream;

    public FileIconFetcher(Context context, DesktopItemInfo pkgName) {
        this.context = context;
        this.appInfo = pkgName;
        packageManager = context.getPackageManager();
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
        Drawable drawable = null;
        if (appInfo.getItemInfo().getItemType() == ItemType.ITEM_TYPE_TRASH) {
            drawable = ContextCompat.getDrawable(context, R.drawable.icon_trash);
        } else {
            drawable = ContextCompat.getDrawable(context, R.drawable.icon_folder);
        }
        Bitmap bitmap = BitmapUtil.drawableToBitamp(drawable);
        inputStream = BitmapUtil.bitmap2InputStream(bitmap);
        bitmap.recycle();
        callback.onDataReady(inputStream);
        closeQuietly(inputStream);
        inputStream = null;
    }

    void closeQuietly(Closeable inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {

            }
        }
    }


    @Override
    public void cleanup() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                // Ignored.
            }
        }
    }

    @Override
    public void cancel() {

    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.LOCAL;
    }
}
