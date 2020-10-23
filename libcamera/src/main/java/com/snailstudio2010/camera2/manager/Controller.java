package com.snailstudio2010.camera2.manager;

import com.snailstudio2010.camera2.CameraView;

public interface Controller {
    int CAMERA_MODULE_STOP = 1;
    int CAMERA_MODULE_RUNNING = 1 << 1;
    int CAMERA_STATE_OPENED = 1 << 2;
    int CAMERA_STATE_UI_READY = 1 << 3;
    int CAMERA_STATE_START_RECORD = 1 << 4;
    int CAMERA_STATE_PAUSE_RECORD = 1 << 5;
    int CAMERA_STATE_CAPTURE = 1 << 6;
    int CAMERA_MODULE_DESTROY = 1 << 7;

//    void changeModule(int module);

    CameraToolKit getToolKit();

//    void showSetting();

//    CameraSettings getCameraSettings(Context context);

    CameraView getBaseUI();
}
