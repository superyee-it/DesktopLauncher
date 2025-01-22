package com.yee.launcher.widget;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.recyclerview.widget.RecyclerView;

import java.nio.charset.StandardCharsets;

public class TitleView extends AppCompatEditText {

    private RecyclerView recyclerview;
    private int maxByte = 255;

    public TitleView(@NonNull Context context) {
        this(context, null);
    }

    public TitleView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextStyle);
    }

    public TitleView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        post(new Runnable() {
            @Override
            public void run() {
                init();
            }
        });
    }

    void init() {
        ViewParent parent = getParent();
        while (parent != null) {
            if (parent instanceof RecyclerView) {
                recyclerview = (RecyclerView) parent;
                break;
            }
            parent = parent.getParent();
        }
        //鼠标事件返回true避免adapter重复监听到鼠标事件弹出右键弹窗和文本复制弹窗冲突
        setOnGenericMotionListener((v, event) -> true);
//        setMovementMethod(ScrollingMovementMethod.getInstance());
        this.setCustomSelectionActionModeCallback(new ActionModeCallbackInterceptor());
        //setMovementMethod后保持复制功能可用
        setTextIsSelectable(false);
        //在recyclerview中有时会无法选中，长按没有粘贴
//        setOnLongClickListener(v -> {
////            setEnabled(false);
////            setEnabled(true);
////            setTextIsSelectable(false);
//            setTextIsSelectable(true);
////            selectAll();
//            return false;
//        });

        addTextChangedListener(mInputTextWatcher);
        enableEdit(false);
    }


    private TextWatcher mInputTextWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            String string = s.toString().trim();

            //限定输入框最多输入255个字节
            int byteLength = string.getBytes(StandardCharsets.UTF_8).length;
            if (byteLength >= maxByte) {
                removeTextChangedListener(this);
                while (string.getBytes(StandardCharsets.UTF_8).length > maxByte) {
                    s.delete(s.length() - 1, s.length());
                    string = s.toString().trim();
                }
                addTextChangedListener(this);
//                setFilters(new InputFilter[]{new InputFilter.LengthFilter(string.length())});
            } else {
//                setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxByte)});
            }
        }
    };

    private DesktopItemView itemView;

    int defaultHeight = 0;

    public TitleView enableEdit(boolean enable) {
        if (itemView == null) {
            itemView = getParentView(DesktopItemView.class);
            if(itemView != null){
                defaultHeight = itemView.getHeight();
            }
        }
        if (enable) {
            setEnabled(false);
            setEnabled(true);
            setTextIsSelectable(false);
            setTextIsSelectable(true);
            setMaxLines(3);
            int index = getText().toString().lastIndexOf(".");
            if (index < 0) {
                index = getText().length();
            }
            setSelection(0, index);
            if (itemView != null) {
                ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
                layoutParams.height = defaultHeight + 100;
                itemView.setLayoutParams(layoutParams);
            }
        } else {
            setEnabled(false);
            setTextIsSelectable(false);
            setMaxLines(2);
            if (itemView != null) {
                ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
                layoutParams.height = defaultHeight;
                itemView.setLayoutParams(layoutParams);
            }
        }
        return this;
    }


    public <T extends View> T getParentView(Class<T> cla) {
        ViewParent parent = getParent();
        while (parent != null && !(cla.isInstance(parent))){
            parent = parent.getParent();
        }
        if (parent != null) {
            return (T) parent;
        }
        return null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                    || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                    || keyCode == KeyEvent.KEYCODE_DPAD_UP
                    || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                super.onKeyDown(keyCode, event);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        if (recyclerview != null) {
            //禁止recyclerview处理touch事件
            recyclerview.requestDisallowInterceptTouchEvent(true);
        }
        if (event.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
            //禁止右键菜单
            return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Prevents the action bar (top horizontal bar with cut, copy, paste, etc.) from appearing
     * by intercepting the callback that would cause it to be created, and returning false.
     */
    public static class ActionModeCallbackInterceptor implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.removeItem(android.R.id.shareText);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }
    }


}
