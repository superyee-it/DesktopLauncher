package com.yee.launcher.popup;

import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.lxj.xpopup.core.AttachPopupView;
import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.util.XPopupUtils;
import com.yee.launcher.R;

public abstract class FixAttachPopupView extends AttachPopupView {
    public FixAttachPopupView(@NonNull Context context) {
        super(context);
    }

    private boolean interceptTouchClickThrough = false;

    public void setInterceptTouchClickThrough(boolean interceptTouchClickThrough) {
        this.interceptTouchClickThrough = interceptTouchClickThrough;
    }

    @Override
    protected void applyBg() {
        super.applyBg();
        //需要先设置背景，阴影才能生效
        attachPopupContainer.setBackgroundResource(R.drawable.bg_popupwindow);
        attachPopupContainer.setElevation(XPopupUtils.dp2px(getContext(), 5));
    }

    /**
     * 是否点击在该弹窗内
     *
     * @param event
     * @return
     */
    private boolean isTouchInView(MotionEvent event) {
        Rect rect = new Rect();
        getPopupImplView().getGlobalVisibleRect(rect);
        return XPopupUtils.isInRect(event.getX(), event.getY(), rect);
    }

    @Override
    public void passTouchThrough(MotionEvent event) {
        if (interceptTouchClickThrough) {
            return;
        }
        if (popupInfo != null && (popupInfo.isClickThrough || popupInfo.isTouchThrough)) {
            if (popupInfo.isViewMode) {
                //需要从DecorView分发，并且要排除自己，否则死循环
                ViewGroup decorView = (ViewGroup) getActivity().getWindow().getDecorView();
                boolean handler = false;
                for (int i = 0; i < decorView.getChildCount(); i++) {
                    View view = decorView.getChildAt(i);
                    //自己和兄弟弹窗都不互相分发，否则死循环
                    if (view instanceof BasePopupView) {
                        BasePopupView popupView = (BasePopupView) view;
                        if (popupView != this) {
                            //先禁止兄弟弹窗的点击透传，否则死循环
                            if (popupView instanceof FixAttachPopupView) {
                                FixAttachPopupView fixAttachPopupView = (FixAttachPopupView) popupView;
                                if (fixAttachPopupView.isTouchInView(event)) {
                                    //点击在弹窗区域内，事件已消费，不再继续传递到后面的view
                                    handler = true;
                                }
                                fixAttachPopupView.setInterceptTouchClickThrough(true);
                                popupView.dispatchTouchEvent(event);
                                fixAttachPopupView.setInterceptTouchClickThrough(false);
                            }
                        }
                    }
                }
                for (int i = 0; !handler && i < decorView.getChildCount(); i++) {
                    View view = decorView.getChildAt(i);
                    if (!(view instanceof BasePopupView)) view.dispatchTouchEvent(event);
                }
            } else {
                getActivity().dispatchTouchEvent(event);
            }
        }
    }
}
