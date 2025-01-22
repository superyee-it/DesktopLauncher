package com.yee.launcher.utils;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

public class ViewUtils {

    /**
     * Set the enabled state of this view.
     *
     * @param view    The view.
     * @param enabled True to enabled, false otherwise.
     */
    public static void setViewEnabled(View view, boolean enabled) {
        setViewEnabled(view, enabled, (View) null);
    }

    /**
     * Set the enabled state of this view.
     *
     * @param view     The view.
     * @param enabled  True to enabled, false otherwise.
     * @param excludes The excludes.
     */
    public static void setViewEnabled(View view, boolean enabled, View... excludes) {
        if (view == null) return;
        if (excludes != null) {
            for (View exclude : excludes) {
                if (view == exclude) return;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                setViewEnabled(viewGroup.getChildAt(i), enabled, excludes);
            }
        }
        view.setEnabled(enabled);
    }


    /**
     * Fix the problem of topping the ScrollView nested ListView/GridView/WebView/RecyclerView.
     *
     * @param view The root view inner of ScrollView.
     */
    public static void fixScrollViewTopping(View view) {
        view.setFocusable(false);
        ViewGroup viewGroup = null;
        if (view instanceof ViewGroup) {
            viewGroup = (ViewGroup) view;
        }
        if (viewGroup == null) {
            return;
        }
        for (int i = 0, n = viewGroup.getChildCount(); i < n; i++) {
            View childAt = viewGroup.getChildAt(i);
            childAt.setFocusable(false);
            if (childAt instanceof ViewGroup) {
                fixScrollViewTopping(childAt);
            }
        }
    }

    /**
     * 坐标是否在view范围内，小窗或全屏模式下都可用
     *
     * @param view
     * @param x
     * @param y
     * @return
     */
    public static boolean isClickInView(View view, float x, float y) {
        int[] location = new int[2];
        //当前item已选中，或者鼠标在title或icon范围内，就启动拖拽
        Rect ivRect = new Rect();
        view.getLocationOnScreen(location);
        ivRect.set(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
        return ivRect.contains((int) x, (int) y);
    }
}
