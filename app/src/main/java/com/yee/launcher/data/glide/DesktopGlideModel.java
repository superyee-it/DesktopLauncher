package com.yee.launcher.data.glide;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;
import com.yee.launcher.data.model.DesktopItemInfo;

import java.io.File;
import java.io.InputStream;

@GlideModule
public class DesktopGlideModel extends AppGlideModule {

    private static final int IMAGE_DISK_CACHE_MAX_SIZE = 500 * 1024 * 1024;

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        //根据包名获取apk图标
        registry.prepend(DesktopItemInfo.class, InputStream.class, new AppModelLoaderFactory(context));
    }

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        builder.setLogLevel(Log.VERBOSE);
        // 读写外部缓存目录不需要申请存储权限
        File diskCacheFile = new File(context.getCacheDir(), "glide");
        // 如果这个路径是一个文件
        if (diskCacheFile.exists() && diskCacheFile.isFile()) {
            // 执行删除操作
            diskCacheFile.delete();
        }
        // 如果这个路径不存在
        if (!diskCacheFile.exists()) {
            // 创建多级目录
            diskCacheFile.mkdirs();
        }
        builder.setDiskCache(() -> DiskLruCacheWrapper.create(diskCacheFile, IMAGE_DISK_CACHE_MAX_SIZE));

        MemorySizeCalculator calculator = new MemorySizeCalculator.Builder(context).setMemoryCacheScreens(3).build();
        int defaultMemoryCacheSize = calculator.getMemoryCacheSize();
        int defaultBitmapPoolSize = calculator.getBitmapPoolSize();

        int customMemoryCacheSize = (int) (1.2 * defaultMemoryCacheSize);
        int customBitmapPoolSize = (int) (1.2 * defaultBitmapPoolSize);

        builder.setDefaultRequestOptions(
                new RequestOptions()
                        .format(DecodeFormat.PREFER_RGB_565));
        builder.setMemoryCache(new LruResourceCache(customMemoryCacheSize));
        builder.setBitmapPool(new LruBitmapPool(customBitmapPoolSize));
//        builder.setSourceExecutor(GlideExecutor.newSourceExecutor(1, "GlideExecutor", GlideExecutor.UncaughtThrowableStrategy.DEFAULT));
    }


}
