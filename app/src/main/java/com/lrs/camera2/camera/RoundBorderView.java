package com.lrs.camera2.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.lrs.camera2.R;


public class RoundBorderView extends View {
    private Paint paint;
    private int radius = 0;
    private int colors = getResources().getColor(R.color.color_red);
    private int roundWidth = 10;
    private boolean stateColor = true;


    public RoundBorderView(Context context) {
        this(context, null);
    }

    public RoundBorderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (paint == null) {
            paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setAntiAlias(true);
            paint.setDither(true);
        }
        if (stateColor) {
            SweepGradient sweepGradient = new SweepGradient(((float) getWidth() / 2), ((float) getHeight() / 2),
                    colors, colors);
            paint.setShader(sweepGradient);
            stateColor = false;
        }
        drawBorder(canvas, roundWidth);
    }


    private void drawBorder(Canvas canvas, int rectThickness) {
        if (canvas == null) {
            return;
        }
        paint.setStrokeWidth(rectThickness);
        Path drawPath = new Path();
        drawPath.addRoundRect(new RectF(0 + (roundWidth / 2), 0 + (roundWidth / 2), getWidth() - (roundWidth / 2), getHeight() - (roundWidth / 2)), radius, radius, Path.Direction.CW);
        canvas.drawPath(drawPath, paint);
    }

    public void turnRound() {
        invalidate();
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void setColors(int colors) {
        this.colors = colors;
        stateColor = true;
    }
}