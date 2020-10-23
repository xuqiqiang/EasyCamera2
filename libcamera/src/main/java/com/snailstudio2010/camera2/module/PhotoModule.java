package com.snailstudio2010.camera2.module;

import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.Properties;
import com.snailstudio2010.camera2.callback.PictureListener;
import com.snailstudio2010.camera2.callback.RequestCallback;
import com.snailstudio2010.camera2.manager.Camera2PhotoSession;
import com.snailstudio2010.camera2.manager.CameraPhotoSession;
import com.snailstudio2010.camera2.manager.CameraSettings;
import com.snailstudio2010.camera2.manager.Controller;
import com.snailstudio2010.camera2.manager.Session;
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
                    if (!isDestroyed()) {
                        getBaseUI().updateUiSize(width, height);
                    }
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
            if (!isDestroyed()) {
                getBaseUI().onZoomChanged(currentZoom, maxZoom);
            }
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
    protected Session getSession() {
        return mProperties != null && mProperties.isUseCameraV1() ?
                new CameraPhotoSession(appContext, mainHandler, getSettings()) :
                new Camera2PhotoSession(appContext, mainHandler, getToolKit().getBackgroundHandler(),
                        getSettings(), mProperties);
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
                || stateEnabled(Controller.CAMERA_STATE_CAPTURE)
                || stateEnabled(Controller.CAMERA_MODULE_DESTROY)) {
            if (listener != null)
                listener.onError("state error");
            return;
        }
        enableState(Controller.CAMERA_STATE_CAPTURE);
        mPictureListener = listener;
        getBaseUI().setUIClickable(false);
        mSession.applyRequest(Session.RQ_TAKE_PICTURE, getToolKit().getOrientation(), mPictureListener);
    }
}
