package com.snailstudio2010.camera2.widget;

import android.content.Context;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;

import com.snailstudio2010.camera2.utils.CameraUtil;
import com.snailstudio2010.camera2.utils.Logger;

/**
 * Created by xuqiqiang on 9/13/17.
 */
public class GestureTouchListener implements View.OnTouchListener {
    private static final String TAG = "GestureTouchListener";
    private static final long DELAY_TIME = 200;
    private float mClickDistance;
    private float mFlingDistance;
    private float mZoomDistance;
    private float mMaxDistance;
    private GestureListener mListener;
    private float mDownX;
    private float mDownY;
    private long mTouchTime;
    private long mDownTime;

    private boolean isMultiPointer;
    private float mFingerSpacing = -1;

    public GestureTouchListener(Context context, GestureListener listener) {
        mListener = listener;
        Point point = CameraUtil.getDisplaySize(context);
        mClickDistance = point.x / 20f;
        mFlingDistance = point.x / 10f;
        mZoomDistance = point.x;
        mMaxDistance = point.x / 5f;
    }

    // 确定前两个手指之间的空间
    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void detectGesture(float downX, float upX, float downY, float upY) {
        float distanceX = upX - downX;
        float distanceY = upY - downY;
        if (Math.abs(distanceX) < mClickDistance
                && Math.abs(distanceY) < mClickDistance
                && mTouchTime < DELAY_TIME) {
            mListener.onClick(upX, upY);
        }
        if (Math.abs(distanceX) > mMaxDistance) {
            if (distanceX > 0) {
                mListener.onSwipeRight();
            } else {
                mListener.onSwipeLeft();
            }
        } else if (Math.abs(distanceX) > mClickDistance && mTouchTime < DELAY_TIME) {
            if (distanceX > 0) {
                mListener.onSwipeRight();
            } else {
                mListener.onSwipeLeft();
            }
        }
        if (Math.abs(distanceX) < mMaxDistance && mTouchTime > DELAY_TIME) {
            mListener.onCancel();
        }
    }

    private void detectSwipe(float downX, float moveX) {
        float alpha;
        if (Math.abs(moveX - downX) > mClickDistance) {
            alpha = ((int) (moveX - downX)) / mMaxDistance;
            if (alpha > 1f) {
                alpha = 1f;
            }
            if (alpha < -1f) {
                alpha = -1f;
            }
            mListener.onSwipe(alpha);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Logger.d(TAG, "test onTouch:" + event.getAction() + "," + event.getPointerCount());
        if (event.getPointerCount() == 1 && event.getAction() == MotionEvent.ACTION_DOWN) {
            mFingerSpacing = -1;
            isMultiPointer = false;
        }
        if (event.getPointerCount() > 1) {

            float currentFingerSpacing = getFingerSpacing(event);
            if (isMultiPointer) {
                mListener.onScale((currentFingerSpacing - mFingerSpacing) / mZoomDistance);
            }
            mFingerSpacing = currentFingerSpacing;
            isMultiPointer = true;
            return true;
        }
        if (isMultiPointer) return true;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownTime = System.currentTimeMillis();
                mDownX = event.getX();
                mDownY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                detectSwipe(mDownX, event.getX());
                break;
            case MotionEvent.ACTION_UP:
                mTouchTime = System.currentTimeMillis() - mDownTime;
                detectGesture(mDownX, event.getX(), mDownY, event.getY());
                break;
        }
        return true;
    }

    public interface GestureListener {
        void onClick(float x, float y);

        void onScale(float factor);

        void onSwipeLeft();

        void onSwipeRight();

        void onSwipe(float percent);

        void onCancel();
    }
}
