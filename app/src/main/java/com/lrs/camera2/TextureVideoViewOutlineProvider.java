package com.lrs.camera2;

import android.graphics.Outline;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;

/**
 * @description 作用: 绘制圆
 * @date: 2020/3/11
 * @author: 卢融霜
 */
public class TextureVideoViewOutlineProvider extends ViewOutlineProvider {
    private float mRadius;

    public TextureVideoViewOutlineProvider(float radius) {
        this.mRadius = radius;
    }

    @Override
    public void getOutline(View view, Outline outline) {
        int left = 0;
        int top = (view.getMeasuredHeight() - view.getMeasuredWidth()) / 2;
        int right = view.getMeasuredWidth();
        int bottom = (view.getMeasuredHeight() - view.getMeasuredWidth()) / 2 + view.getMeasuredWidth();
        int other =(view.getMeasuredHeight() - view.getMeasuredWidth()) / 2;
        Rect rect = new Rect(left, top, right, bottom);
        Log.e("Rect", "left = " + left + "top = " + top + "right = " + right + "bottom = " + bottom);
        outline.setRoundRect(rect, mRadius);
    }
}
