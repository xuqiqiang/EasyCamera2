package com.snailstudio2010.camera2.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import com.snailstudio2010.camera2.utils.NonNull;
import com.snailstudio2010.camera2.utils.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by zhaozhibo on 2017/10/31.
 */

public class ContainerView extends FrameLayout {

    private Paint mPaint;
    private PointF[] mLocationPoints;

    public ContainerView(@NonNull Context context) {
        this(context, null);
    }

    public ContainerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ContainerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
//        init();
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.FILL);
    }


    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);


        if (mLocationPoints == null) {
            return;
        }

        for (PointF pointF : mLocationPoints) {
            canvas.drawCircle(pointF.x, pointF.y, 10, mPaint);
        }
        mLocationPoints = null;
        postInvalidateDelayed(2000);
    }

    public void setLocationPoints(PointF[] locationPoints) {
        this.mLocationPoints = locationPoints;
    }
}
