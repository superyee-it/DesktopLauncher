package com.yee.launcher.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;


public class GlobalApplication extends Application implements ViewModelStoreOwner {

    private static GlobalApplication instance;
    private ViewModelStore viewModelStore;

    public static GlobalApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        if (viewModelStore == null) {
            viewModelStore = new ViewModelStore();
        }
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        return viewModelStore;
    }
}
