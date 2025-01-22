package com.yee.launcher.data.glide;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.yee.launcher.data.model.DesktopItemInfo;

import java.io.InputStream;

public class AppModelLoaderFactory implements ModelLoaderFactory<DesktopItemInfo, InputStream> {
    private Context context;

    public AppModelLoaderFactory(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ModelLoader<DesktopItemInfo, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {

        return new AppIconModelLoader(context);
    }

    @Override
    public void teardown() {

    }
}