package com.snailstudio2010.camera2.ui;

import android.view.View;
import android.widget.RelativeLayout;

import com.snailstudio2010.camera2.callback.CameraUiEvent;

/**
 * Created by xuqiqiang on 3/3/17.
 */
public abstract class CameraBaseUI {//implements GestureTextureView.GestureListener {
    private final String TAG = this.getClass().getSimpleName();
    CameraUiEvent uiEvent;
    int frameCount = 0;
    private ICoverView mCoverView;

    CameraBaseUI(CameraUiEvent event) {
        uiEvent = event;
    }

    public abstract View getRootView();

//    public void setUIClickable(boolean clickable) {
//    }

    public void resetFrameCount() {
        frameCount = 0;
    }

    public void setCoverView(ICoverView coverView) {
        mCoverView = coverView;
    }

    /* GestureTextureView.GestureListener */
//    @Override
//    public void onClick(float x, float y) {
////        uiEvent.onTouchToFocus(x, y);
//    }
//
//    @Override
//    public void onSwipeLeft() {
////        int newIndex = ModuleManager.getCurrentIndex() + 1;
////        if (ModuleManager.isValidIndex(newIndex)) {
////            mCoverView.setAlpha(1.0f);
////            uiEvent.onAction(CameraUiEvent.ACTION_CHANGE_MODULE, newIndex);
////        }
//        uiEvent.onAction(CameraUiEvent.ACTION_CHANGE_MODULE, 0);
//    }
//
//    @Override
//    public void onSwipeRight() {
////        int newIndex = ModuleManager.getCurrentIndex() - 1;
////        if (ModuleManager.isValidIndex(newIndex)) {
////            mCoverView.setAlpha(1.0f);
////            uiEvent.onAction(CameraUiEvent.ACTION_CHANGE_MODULE, newIndex);
////        }
//        uiEvent.onAction(CameraUiEvent.ACTION_CHANGE_MODULE, 1);
//    }
//
//    @Override
//    public void onSwipe(float percent) {
////        int newIndex;
////        if (percent < 0) {
////            newIndex = ModuleManager.getCurrentIndex() + 1;
////        } else {
////            newIndex = ModuleManager.getCurrentIndex() - 1;
////        }
////        if (ModuleManager.isValidIndex(newIndex)) {
////            mCoverView.setMode(newIndex);
////            mCoverView.setAlpha(Math.abs(percent));
////            mCoverView.setVisibility(View.VISIBLE);
////        }
//    }
//
//    @Override
//    public void onCancel() {
//        if (mCoverView != null)
//            mCoverView.hide();
//    }

    public void switchCamera() {
    }
}
