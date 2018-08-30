package com.dtnguy.camerachathead;

/**
 * Created by dtngu_000 on 2/16/2017.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by dtngu_000 on 2/16/2017.
 */

public class RoundFrameLayout extends FrameLayout{
    //private Path clippingPath;

    public RoundFrameLayout(Context context) {

        this(context, null);
    }

    public RoundFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundFrameLayout(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
    }
//    @Override
//    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
//        if (w != oldw || h != oldh) {
//            int radius = Math.min(w, h)/2;
//            clippingPath = new Path();
//            clippingPath.addCircle(w/2, h/2, radius, Path.Direction.CW);
//        }
//    }
//    @Override
//    protected void dispatchDraw(Canvas canvas) {
//        int count = canvas.save();
//        canvas.clipPath(clippingPath);
//        super.dispatchDraw(canvas);
//        canvas.restoreToCount(count);
//    }
//   @Override
//    protected void onDraw(Canvas canvas) {
//        int count = canvas.save();
//        canvas.clipPath(clippingPath);
//        super.dispatchDraw(canvas);
//        canvas.restoreToCount(count);
//    }

}
