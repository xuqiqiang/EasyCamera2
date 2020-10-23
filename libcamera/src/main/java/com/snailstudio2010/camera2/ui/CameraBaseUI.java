package com.snailstudio2010.camera2.ui;

import android.view.View;

import com.snailstudio2010.camera2.callback.CameraUiEvent;

/**
 * Created by xuqiqiang on 3/3/17.
 */
public abstract class CameraBaseUI {
    CameraUiEvent uiEvent;
    int frameCount = 0;

    CameraBaseUI(CameraUiEvent event) {
        uiEvent = event;
    }

    public abstract View getRootView();

    public void resetFrameCount() {
        frameCount = 0;
    }

    public void switchCamera() {
    }
}
