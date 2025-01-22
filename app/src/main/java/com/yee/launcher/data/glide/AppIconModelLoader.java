package com.yee.launcher.data.glide;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;
import com.yee.launcher.data.model.DesktopItemInfo;
import com.yee.launcher.data.model.ItemType;

import java.io.InputStream;

public class AppIconModelLoader implements ModelLoader<DesktopItemInfo, InputStream> {
    private Context context;

    public AppIconModelLoader(Context context) {
        this.context = context;
    }

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull DesktopItemInfo appInfo, int width, int height, @NonNull Options options) {
        if (appInfo.getItemType() == ItemType.ITEM_TYPE_APPLICATION) {
            return new LoadData<>(new ObjectKey(appInfo.appIconKey()), new AppIconFetcher(context, appInfo));
        } else {
            return new LoadData<>(new ObjectKey(appInfo.appIconKey()), new FileIconFetcher(context, appInfo));
        }
    }

    @Override
    public boolean handles(@NonNull DesktopItemInfo apkIconModel) {
        return true;
    }
}