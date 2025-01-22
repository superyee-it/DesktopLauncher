package com.yee.launcher.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class BitmapUtil {


    public static Bitmap getBitmapFromView(View view) {
        // 确保视图已经测量和布局
//        view.measure(View.MeasureSpec.makeMeasureSpec(view.getWidth(), View.MeasureSpec.EXACTLY),
//                View.MeasureSpec.makeMeasureSpec(view.getHeight(), View.MeasureSpec.EXACTLY));
//        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        // 创建一个与视图大小相同的Bitmap
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        // 创建一个Canvas对象，将Bitmap作为绘制目标
        Canvas canvas = new Canvas(bitmap);

        // 将视图的内容绘制到Canvas上
        view.draw(canvas);

        return bitmap;
    }

    // 将Bitmap转换成InputStream
    public static InputStream bitmap2InputStream(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }


    public static Bitmap drawableToBitamp(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            //创建个副本，避免bitmap回收影响drawable（如果drawable被缓存下来，再次getBitmap会出现isRecycled异常）
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            return bitmap.copy(bitmap.getConfig(), bitmap.isMutable());
        }
        //声明将要创建的bitmap
        Bitmap bitmap = null;
        //获取图片宽度
        int width = drawable.getIntrinsicWidth();
        //获取图片高度
        int height = drawable.getIntrinsicHeight();
        //图片位深，PixelFormat.OPAQUE代表没有透明度，RGB_565就是没有透明度的位深，否则就用ARGB_8888。详细见下面图片编码知识。
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        //创建一个空的Bitmap
        bitmap = Bitmap.createBitmap(width, height, config);
        //在bitmap上创建一个画布
        Canvas canvas = new Canvas(bitmap);
        //设置画布的范围
        drawable.setBounds(0, 0, width, height);
        //将drawable绘制在canvas上
        drawable.draw(canvas);
        return bitmap;
    }


    public static Bitmap scaleBitmap(Bitmap bitmap, int width, int height) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scalewidth = (float) width / w;
        float scaleheigh = (float) height / h;
        matrix.postScale(scalewidth, scaleheigh);
//        int size = Math.min(w, h);
//        int x = (w - size)/2;
//        int y = (h - size)/2;
//        Bitmap newBmp = Bitmap.createBitmap(bitmap, x, y, size, size, matrix, true);
        Bitmap newBmp = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
        return newBmp;
    }

    //居中裁剪正方形区域
    public static Bitmap cropSquareBitmap(Bitmap bitmap) {
        if (bitmap.getWidth() == bitmap.getHeight()) {
            return bitmap;
        }
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        // 创建一个新的bitmap，其中包含裁剪的图像。
        Bitmap croppedBitmap = Bitmap.createBitmap(size, size, bitmap.getConfig());
        int x = (bitmap.getWidth() - size) / 2;
        int y = (bitmap.getHeight() - size) / 2;
        // 用Canvas绘制原始位图的一部分
        Canvas canvas = new Canvas(croppedBitmap);
        Rect srcRect = new Rect(x, y, x + size, y + size);
        Rect dstRect = new Rect(0, 0, size, size);
        canvas.drawBitmap(bitmap, srcRect, dstRect, null);

        return croppedBitmap;
    }

    // 9. drawable进行缩放 ---> bitmap 然后比对bitmap进行缩放
    public static Drawable scaleDrawable(Drawable drawable, int w, int h) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        // 调用5 中 drawable转换成bitmap
        Bitmap oldbmp = drawableToBitamp(drawable);
        // 创建操作图片用的Matrix对象
        Matrix matrix = new Matrix();
        // 计算缩放比例
        float sx = ((float) w / width);
        float sy = ((float) h / height);
        // 设置缩放比例
        matrix.postScale(sx, sy);
        // 建立新的bitmap，其内容是对原bitmap的缩放后的图
        Bitmap newbmp = Bitmap.createBitmap(oldbmp, 0, 0, width, height,
                matrix, true);
        return new BitmapDrawable(newbmp);
    }

    public static Bitmap setRoundCornerBitmap(Bitmap bitmap, float roundPx) {
        int width = bitmap.getWidth();
        int heigh = bitmap.getHeight();
        // 创建输出bitmap对象
        Bitmap outmap = Bitmap.createBitmap(width, heigh, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outmap);
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, width, heigh);
        final RectF rectf = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectf, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return outmap;
    }

}
