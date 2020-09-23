package com.snailstudio2010.camera2.module;

import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.Properties;
import com.snailstudio2010.camera2.callback.CameraUiEvent;
import com.snailstudio2010.camera2.callback.PictureListener;
import com.snailstudio2010.camera2.callback.RequestCallback;
import com.snailstudio2010.camera2.manager.Camera2PhotoSession;
import com.snailstudio2010.camera2.manager.CameraPhotoSession;
import com.snailstudio2010.camera2.manager.CameraSettings;
import com.snailstudio2010.camera2.manager.Controller;
import com.snailstudio2010.camera2.manager.Session;
import com.snailstudio2010.camera2.ui.CameraBaseUI;
import com.snailstudio2010.camera2.ui.GLPhotoUI;
import com.snailstudio2010.camera2.ui.PhotoUI;
import com.snailstudio2010.camera2.utils.Logger;

/**
 * Created by xuqiqiang on 16-3-8.
 */
public class PhotoModule extends SingleCameraModule {

    private static final String TAG = Config.getTag(PhotoModule.class);

    private RequestCallback mRequestCallback = new RequestCallback() {
        @Override
        public void onDataBack(byte[] data, int width, int height) {
            super.onDataBack(data, width, height);
            saveFile(data, width, height, mDeviceMgr.getCameraId(),
                    CameraSettings.KEY_PICTURE_FORMAT, "CAMERA");
            mSession.applyRequest(Session.RQ_RESTART_PREVIEW);
        }

        @Override
        public void onViewChange(final int width, final int height) {
            super.onViewChange(width, height);
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    getBaseUI().updateUiSize(width, height);
                }
            });
            mFocusManager.onPreviewChanged(width, height, mDeviceMgr.getCharacteristics());
        }

        @Override
        public void onAFStateChanged(int state) {
            super.onAFStateChanged(state);
            updateAFState(state, mFocusManager);
        }

        @Override
        public void onZoomChanged(float currentZoom, float maxZoom) {
            getBaseUI().onZoomChanged(currentZoom, maxZoom);
        }

        @Override
        public void onRequestComplete() {
            disableState(Controller.CAMERA_STATE_CAPTURE);
        }
    };

    public PhotoModule() {
        super();
    }

    public PhotoModule(Properties properties) {
        super(properties);
    }

    @Override
    protected CameraBaseUI getUI(CameraUiEvent mCameraUiEvent) {
        return mProperties != null && mProperties.isUseGPUImage() ?
                new GLPhotoUI(appContext, mainHandler, mCameraUiEvent) :
                new PhotoUI(appContext, mainHandler, mCameraUiEvent);
//        return new GLPhotoUI(appContext, mainHandler, mCameraUiEvent);
    }

    @Override
    protected Session getSession() {
        return mProperties != null && mProperties.isUseCameraV1() ?
                new CameraPhotoSession(appContext, mainHandler, getSettings()) :
                new Camera2PhotoSession(appContext, mainHandler, getToolKit().getBackgroundHandler(), getSettings());
//        return new Camera2PhotoSession(appContext, mainHandler, getSettings());
    }

    @Override
    protected String getCameraKey() {
        return CameraSettings.KEY_CAMERA_ID;
    }

    @Override
    protected RequestCallback getRequestCallback() {
        return mRequestCallback;
    }

    public void takePicture(PictureListener listener) {
        Logger.d(TAG, "CAMERA_STATE_CAPTURE:" + stateEnabled(Controller.CAMERA_STATE_CAPTURE));
        if (!stateEnabled(Controller.CAMERA_MODULE_RUNNING)
                || !stateEnabled(Controller.CAMERA_STATE_OPENED)
                || !stateEnabled(Controller.CAMERA_STATE_UI_READY)
                || stateEnabled(Controller.CAMERA_STATE_CAPTURE)) {
            if (listener != null)
                listener.onError("state error");
            return;
        }
        enableState(Controller.CAMERA_STATE_CAPTURE);
        mPictureListener = listener;
//        setPictureListener(listener);
//        mUI.setUIClickable(false);
        getBaseUI().setUIClickable(false);
        mSession.applyRequest(Session.RQ_TAKE_PICTURE, getToolKit().getOrientation(), mPictureListener);
//        mSession.applyRequest(Session.RQ_CAMERA_TAKE_PICTURE, getToolKit().getOrientation());
    }
}
