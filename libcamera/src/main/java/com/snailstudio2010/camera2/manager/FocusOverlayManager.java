package com.snailstudio2010.camera2.manager;

import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.callback.CameraUiEvent;
import com.snailstudio2010.camera2.ui.IFocusView;
import com.snailstudio2010.camera2.utils.CoordinateTransformer;

import java.lang.ref.WeakReference;

/**
 * Created by xuqiqiang on 5/2/17.
 */
public class FocusOverlayManager {

    public static final int IMAP_STORE_RESPONSE = 1000;
    private static final String TAG = Config.getTag(FocusOverlayManager.class);
    private static final int HIDE_FOCUS_DELAY = 4000;
    private static final int MSG_HIDE_FOCUS = 0x10;
    private IFocusView mFocusView;
    private MainHandler mHandler;
    private CameraUiEvent mListener;
    private float currentX;
    private float currentY;
    private CoordinateTransformer mTransformer;
    private Rect mPreviewRect;
    private Rect mFocusRect;

    public FocusOverlayManager(IFocusView focusView, Looper looper) {
        mFocusView = focusView;
        mHandler = new MainHandler(this, looper);
        if (mFocusView != null)
            mFocusView.resetToDefaultPosition();
        mFocusRect = new Rect();
    }

    private static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    public void setListener(CameraUiEvent listener) {
        mListener = listener;
    }

    public void onPreviewChanged(int width, int height, CameraCharacteristics c) {
        mPreviewRect = new Rect(0, 0, width, height);
        mTransformer = new CoordinateTransformer(c, rectToRectF(mPreviewRect));
    }

    /* just set focus view position, not start animation*/
    public void startFocus(float x, float y) {
        currentX = x;
        currentY = y;
        mHandler.removeMessages(MSG_HIDE_FOCUS);
        if (mFocusView != null)
            mFocusView.moveToPosition(x, y);
        //mFocusView.startFocus();
        mHandler.sendEmptyMessageDelayed(MSG_HIDE_FOCUS, HIDE_FOCUS_DELAY);
    }

    /* show focus view by af state */
    public void startFocus() {
        mHandler.removeMessages(MSG_HIDE_FOCUS);
        if (mFocusView != null)
            mFocusView.startFocus();
        mHandler.sendEmptyMessageDelayed(MSG_HIDE_FOCUS, HIDE_FOCUS_DELAY);
    }

    public void autoFocus() {
        mHandler.removeMessages(MSG_HIDE_FOCUS);
        if (mFocusView != null) {
            mFocusView.resetToDefaultPosition();
            mFocusView.startFocus();
        }
        mHandler.sendEmptyMessageDelayed(MSG_HIDE_FOCUS, 1000);
    }

    public void focusSuccess() {
        if (mFocusView != null)
            mFocusView.focusSuccess();
    }

    public void focusError() {
        if (mFocusView != null)
            mFocusView.focusError();
    }

    public void hideFocusUI() {
        //mFocusView.resetToDefaultPosition();
        if (mFocusView != null)
            mFocusView.hide();
    }

    public void removeDelayMessage() {
        mHandler.removeMessages(MSG_HIDE_FOCUS);
    }

    public MeteringRectangle getFocusArea(float x, float y, boolean isFocusArea) {
        currentX = x;
        currentY = y;
        if (isFocusArea) {
            return calcTapAreaForCamera2(mPreviewRect.width() / 5, 1000);
        } else {
            return calcTapAreaForCamera2(mPreviewRect.width() / 4, 1000);
        }
    }

    private MeteringRectangle calcTapAreaForCamera2(int areaSize, int weight) {
        int left = clamp((int) currentX - areaSize / 2,
                mPreviewRect.left, mPreviewRect.right - areaSize);
        int top = clamp((int) currentY - areaSize / 2,
                mPreviewRect.top, mPreviewRect.bottom - areaSize);
        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        toFocusRect(mTransformer.toCameraSpace(rectF));
        return new MeteringRectangle(mFocusRect, weight);
    }

//    public Rect calcTapArea2(float x, float y, float coefficient) {
//        float focusAreaSize = 300;
//        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
//        int centerY = (int) (x / (float) mPreviewRect.right * 2000 - 1000);
//        int centerX = (int) (y / (float) mPreviewRect.bottom * 2000 - 1000);
//        int left = clamp(centerX - areaSize / 2, -1000, 1000);
//        int top = clamp(centerY - areaSize / 2, -1000, 1000);
//
//        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
//        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
//    }

    public Rect calcTapArea(float x, float y, float coefficient) {
        int areaSize = Float.valueOf(300.0f * coefficient).intValue();

        int centerY = (int) (((x / ((float) mPreviewRect.right)) * 2000.0f) - 1000.0f);
        int centerX = (int) (((y / ((float) mPreviewRect.bottom)) * 2000.0f) - 1000.0f);

        int left = clamp(centerX - (areaSize / 2), -1000, 1000 - areaSize);
        int right = clamp(left + areaSize, areaSize - 1000, IMAP_STORE_RESPONSE);
        int top = clamp(centerY - (areaSize / 2), -1000, 1000 - areaSize);
        int bottom = clamp(top + areaSize, areaSize - 1000, IMAP_STORE_RESPONSE);
        return new Rect(left, top, right, bottom);
    }

    private RectF rectToRectF(Rect rect) {
        return new RectF(rect);
    }

    private void toFocusRect(RectF rectF) {
        mFocusRect.left = Math.round(rectF.left);
        mFocusRect.top = Math.round(rectF.top);
        mFocusRect.right = Math.round(rectF.right);
        mFocusRect.bottom = Math.round(rectF.bottom);
    }

    private static class MainHandler extends Handler {
        final WeakReference<FocusOverlayManager> mManager;

        MainHandler(FocusOverlayManager manager, Looper looper) {
            super(looper);
            mManager = new WeakReference<>(manager);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (mManager.get() == null) {
                return;
            }
            switch (msg.what) {
                case MSG_HIDE_FOCUS:
                    if (mManager.get().mFocusView != null)
                        mManager.get().mFocusView.resetToDefaultPosition();
                    mManager.get().hideFocusUI();
                    mManager.get().mListener.resetTouchToFocus();
                    break;
            }
        }
    }
}
