package com.cmtech.android.bledeviceapp.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;

/**
 *  CircleImageView: 圆形ImageView
 *  Created by bme on 2019/3/19.
 */

public class CircleImageView extends android.support.v7.widget.AppCompatImageView{

    private float width,height;
    private final Path path = new Path();

    public CircleImageView(Context context) {
        this(context, null);
    }

    public CircleImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        width = getWidth();
        height = getHeight();
        //ViseLog.e(" " + width + " " + height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //ViseLog.e(" " + width + " " + height);
        if (width > 12 && height  > 12) {
            path.reset();
            float radius = (width > height) ? height/2 : width/2;
            path.addCircle(width/2, height/2, radius, Path.Direction.CW);
            canvas.clipPath(path);
        }

        super.onDraw(canvas);
    }
}
