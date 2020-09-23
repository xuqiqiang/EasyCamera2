package com.snailstudio2010.camera2.manager;

import android.content.Context;
import android.hardware.Camera;
import com.snailstudio2010.camera2.utils.Nullable;

import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.module.SingleCameraModule;

public abstract class Session {

    public static final int RQ_SET_DEVICE = 1;
    public static final int RQ_START_PREVIEW = 2;
    public static final int RQ_AF_AE_REGIONS = 3;
    public static final int RQ_FOCUS_MODE = 4;
    public static final int RQ_FOCUS_DISTANCE = 5;
    public static final int RQ_FLASH_MODE = 6;
    public static final int RQ_RESTART_PREVIEW = 7;
    public static final int RQ_TAKE_PICTURE = 8;
    public static final int RQ_START_RECORD = 9;
    public static final int RQ_STOP_RECORD = 10;
    public static final int RQ_PAUSE_RECORD = 11;
    public static final int RQ_RESUME_RECORD = 12;
    public static final int RQ_CAMERA_ZOOM = 13;

    private final String TAG = Config.getTag(Session.class);
    Context appContext;
    CameraSettings cameraSettings;

    SingleCameraModule.PreviewCallback mCameraPreviewCallback;

    Session(Context context, CameraSettings settings) {
        cameraSettings = settings;
        appContext = context;
    }

    public void applyRequest(int msg) {
        applyRequest(msg, null, null);
    }

    public void applyRequest(int msg, Object value) {
        applyRequest(msg, value, null);
    }

    public abstract void applyRequest(int msg, @Nullable Object value1, @Nullable Object value2);

    public void setRequest(int msg) {
        applyRequest(msg, null, null);
    }

    public void setRequest(int msg, Object value) {
        setRequest(msg, value, null);
    }

    public abstract void setRequest(int msg, @Nullable Object value1, @Nullable Object value2);

    public abstract void release();

    public void setPreviewCallback(SingleCameraModule.PreviewCallback previewCallback) {
        this.mCameraPreviewCallback = previewCallback;
    }
}
