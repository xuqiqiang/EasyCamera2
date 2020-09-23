package com.snailstudio2010.camera2.ui;

import com.snailstudio2010.camera2.callback.CameraUiEvent;

/**
 * Created by xuqiqiang on 16-3-18.
 */
public abstract class GLCameraUI extends CameraBaseUI {
    GLCameraUI(CameraUiEvent event) {
        super(event);
    }

    public abstract void setFilterConfig(String config);
}
