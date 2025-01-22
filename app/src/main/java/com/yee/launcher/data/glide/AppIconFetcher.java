package com.yee.launcher.data.glide;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.yee.launcher.R;
import com.yee.launcher.data.model.DesktopItemInfo;
import com.yee.launcher.utils.BitmapUtil;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * 根据app包名解析图标
 */
public class AppIconFetcher implements DataFetcher<InputStream> {
    private DesktopItemInfo appInfo;
    private Context context;
    private final PackageManager packageManager;
    private InputStream inputStream;

    public AppIconFetcher(Context context, DesktopItemInfo pkgName) {
        this.context = context;
        this.appInfo = pkgName;
        packageManager = context.getPackageManager();
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
        ApplicationInfo applicationInfo;
        try {
            String packageName = appInfo.getPackageName();
            Drawable drawable = null;
            if (drawable == null) {
                applicationInfo = packageManager.getApplicationInfo(packageName, 0);
                drawable = packageManager.getApplicationIcon(applicationInfo); //xxx根据自己的情况获取drawable
            }
            if (drawable == null) {
                drawable = context.getResources().getDrawable(R.mipmap.ic_launcher);
            }
            Bitmap bitmap = BitmapUtil.drawableToBitamp(drawable);
            inputStream = BitmapUtil.bitmap2InputStream(bitmap);
            bitmap.recycle();
            callback.onDataReady(inputStream);
            closeQuietly(inputStream);
            inputStream = null;
        } catch (Exception e) {
            e.printStackTrace();
            callback.onLoadFailed(e);
            if (inputStream != null) {
                closeQuietly(inputStream);
            }
        }
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
