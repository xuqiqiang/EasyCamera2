package com.snailstudio2010.camera2.module;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;
import android.net.Uri;
import android.text.TextUtils;

import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.Properties;
import com.snailstudio2010.camera2.callback.CameraUiEvent;
import com.snailstudio2010.camera2.callback.PictureListener;
import com.snailstudio2010.camera2.callback.RequestCallback;
import com.snailstudio2010.camera2.manager.CameraSettings;
import com.snailstudio2010.camera2.manager.Controller;
import com.snailstudio2010.camera2.manager.DeviceManager;
import com.snailstudio2010.camera2.manager.FocusOverlayManager;
import com.snailstudio2010.camera2.manager.Session;
import com.snailstudio2010.camera2.manager.SingleDeviceManager;
import com.snailstudio2010.camera2.ui.CameraBaseUI;
import com.snailstudio2010.camera2.ui.GLCameraUI;
import com.snailstudio2010.camera2.utils.FileSaver;
import com.snailstudio2010.camera2.utils.Logger;
import com.snailstudio2010.camera2.utils.MediaFunc;

/**
 * Created by xuqiqiang on 16-3-8.
 */
public abstract class SingleCameraModule extends CameraModule implements FileSaver.FileListener {

    private static final String TAG = Config.getTag(SingleCameraModule.class);
    protected SurfaceTexture mSurfaceTexture;
    protected CameraBaseUI mUI;
    protected Session mSession;
    protected SingleDeviceManager mDeviceMgr;
    protected FocusOverlayManager mFocusManager;
    protected String mCameraKey;
    protected PictureListener mPictureListener;
    private String mFilter;
    private RequestCallback mRequestCallback;
    private PreviewCallback mPreviewCallback;
    //    private Properties mProperties;
    private DeviceManager.CameraEvent mCameraEvent = new DeviceManager.CameraEvent() {
//        @Override
//        public void onDeviceOpened(Camera device) {
//            super.onDeviceOpened(device);
//            Logger.d(TAG, "camera opened");
//            mSession.applyRequest(Session.RQ_SET_CAMERA_DEVICE, device, mDeviceMgr.getCameraId());
//            enableState(Controller.CAMERA_STATE_OPENED);
//            if (stateEnabled(Controller.CAMERA_STATE_UI_READY)) {
//                mSession.applyRequest(Session.RQ_START_PREVIEW, mSurfaceTexture, mRequestCallback);
//            }
//        }

        @Override
        public void onDeviceOpened(Object device) {
//            super.onDeviceOpened(device);
            Logger.d(TAG, "camera opened");
            mSession.applyRequest(Session.RQ_SET_DEVICE, device, mDeviceMgr.getCameraId());
            enableState(Controller.CAMERA_STATE_OPENED);
            if (stateEnabled(Controller.CAMERA_STATE_UI_READY)) {
                mSession.applyRequest(Session.RQ_START_PREVIEW, mSurfaceTexture, mRequestCallback);
            }
        }

        @Override
        public void onDeviceClosed() {
            super.onDeviceClosed();
            disableState(Controller.CAMERA_STATE_OPENED);
            if (mUI != null) {
                mUI.resetFrameCount();
            }
            Logger.d(TAG, "camera closed");
        }
    };
    private CameraUiEvent mCameraUiEvent = new CameraUiEvent() {

        @Override
        public void onPreviewUiReady(SurfaceTexture mainSurface, SurfaceTexture auxSurface) {
            Logger.d(TAG, "onSurfaceTextureAvailable");
            mSurfaceTexture = mainSurface;
            enableState(Controller.CAMERA_STATE_UI_READY);
            if (stateEnabled(Controller.CAMERA_STATE_OPENED)) {
                mSession.applyRequest(Session.RQ_START_PREVIEW, mSurfaceTexture, mRequestCallback);
//                if (mProperties != null && mProperties.isUseCameraV1()) {
//                    mSession.applyRequest(Session.RQ_START_PREVIEW, mSurfaceTexture, mRequestCallback);
//                } else {
//                    mSession.applyRequest(Session.RQ_START_PREVIEW, mSurfaceTexture, mRequestCallback);
//                }
            }
        }

        @Override
        public void onPreviewUiDestroy() {
            disableState(Controller.CAMERA_STATE_UI_READY);
            Logger.d(TAG, "onSurfaceTextureDestroyed");
        }

//        @Override
//        public void onTouchToFocus(float x, float y) {
//            // close all menu when touch to focus
//            getBaseUI().closeMenu();
//            mFocusManager.startFocus(x, y);
//            MeteringRectangle focusRect = mFocusManager.getFocusArea(x, y, true);
//            MeteringRectangle meterRect = mFocusManager.getFocusArea(x, y, false);
//            mSession.applyRequest(Session.RQ_AF_AE_REGIONS, focusRect, meterRect);
//        }

        @Override
        public void resetTouchToFocus() {
            if (stateEnabled(Controller.CAMERA_MODULE_RUNNING)) {
                mSession.applyRequest(Session.RQ_FOCUS_MODE,
                        mProperties != null && mProperties.isUseCameraV1() ? "continuous-picture" :
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//                mSession.applyRequest(Session.RQ_CAMERA_FOCUS_MODE,
//                        "continuous-picture");
            }
        }

//        @Override
//        public <T> void onSettingChange(CaptureRequest.Key<T> key, T value) {
//            if (key == CaptureRequest.LENS_FOCUS_DISTANCE) {
//                mSession.applyRequest(Session.RQ_FOCUS_DISTANCE, value);
//            }
//        }

        @Override
        public <T> void onAction(String type, T value) {
            // close all menu when ui click
            getBaseUI().closeMenu();
            switch (type) {
//                case CameraUiEvent.ACTION_CHANGE_MODULE:
//                    getBaseUI().changeModule((Integer) value);
//                    break;
                case CameraUiEvent.ACTION_SWITCH_CAMERA:
                    break;
                case CameraUiEvent.ACTION_PREVIEW_READY:
                    if (getCoverView() != null)
                        getCoverView().hideWithAnimation();
                    break;
                default:
                    break;
            }
        }
    };

    public SingleCameraModule() {
        super();
    }

    public SingleCameraModule(Properties properties) {
        super(properties);
    }

    protected abstract CameraBaseUI getUI(CameraUiEvent mCameraUiEvent);

    protected abstract Session getSession();

    protected abstract String getCameraKey();

    protected abstract RequestCallback getRequestCallback();

    public void setPreviewCallback(PreviewCallback previewCallback) {
        if (this.mPreviewCallback == previewCallback) return;
        this.mPreviewCallback = previewCallback;
        if (mSession != null) {
            mSession.setPreviewCallback(mPreviewCallback);
            if (stateEnabled(Controller.CAMERA_STATE_OPENED)
                    && stateEnabled(Controller.CAMERA_STATE_UI_READY)) {
                mSession.applyRequest(Session.RQ_START_PREVIEW, mSurfaceTexture, mRequestCallback);
            }
        }
    }

    @Override
    protected void init() {
        mCameraKey = getCameraKey();
        mRequestCallback = getRequestCallback();
        mUI = getUI(mCameraUiEvent);
        mUI.setCoverView(getCoverView());
        mDeviceMgr = new SingleDeviceManager(appContext, mProperties, getExecutor(), mCameraEvent);
        mFocusManager = new FocusOverlayManager(getBaseUI().getFocusView(), mainHandler.getLooper());
        mFocusManager.setListener(mCameraUiEvent);
        mSession = getSession();
        mSession.setPreviewCallback(mPreviewCallback);
    }

    @Override
    public void start() {
        String cameraId = getSettings().getGlobalPref(
                mCameraKey, mDeviceMgr.getDefaultCameraId());//mDeviceMgr.getCameraIdList()[0]);
        mDeviceMgr.setCameraId(cameraId);
        mDeviceMgr.openCamera(mainHandler);
        // when module changed , need update listener
        fileSaver.setFileListener(this);
        addModuleView(mUI.getRootView());
        Logger.d(TAG, "start module");
        if (!TextUtils.isEmpty(mFilter)) {
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setFilterConfig(mFilter);
                }
            }, 300);
        }
    }

    @Override
    public void stop() {
        getBaseUI().closeMenu();
        if (getCoverView() != null)
            getCoverView().show();
        mFocusManager.removeDelayMessage();
        mFocusManager.hideFocusUI();
        mSession.release();
        mDeviceMgr.releaseCamera();
        Logger.d(TAG, "stop module");
    }

    @Override
    public DeviceManager getDeviceManager() {
        return mDeviceMgr;
    }

    /**
     * FileSaver.FileListener
     *
     * @param uri       image file uri
     * @param path      image file path
     * @param thumbnail image thumbnail
     */
    @Override
    public void onFileSaved(Uri uri, String path, Bitmap thumbnail) {
        MediaFunc.setCurrentUri(uri);
//        mUI.setUIClickable(true);
        getBaseUI().setUIClickable(true);
        Logger.d(TAG, "uri:" + uri.toString());
        Logger.d(TAG, "path:" + path);
        if (mPictureListener != null) {
            mPictureListener.onComplete(uri, path, thumbnail);
            mPictureListener = null;
        }
    }

    /**
     * callback for file save error
     *
     * @param msg error msg
     */
    @Override
    public void onFileSaveError(String msg) {
//        mUI.setUIClickable(true);
        getBaseUI().setUIClickable(true);
        if (mPictureListener != null) {
            mPictureListener.onError(msg);
            mPictureListener = null;
        }
    }

    public boolean isFrontCamera() {
        return mDeviceMgr.isFrontCamera();
//        Integer face = mDeviceMgr.getCharacteristics(mDeviceMgr.getCameraId())
//                .get(CameraCharacteristics.LENS_FACING);
//        return face != null && CameraCharacteristics.LENS_FACING_FRONT == face;
    }

//    public void switchCamera(boolean isFront, Properties properties) {
//        if(isFront == isFrontCamera()) return;
//        switchCamera(properties);
//    }

    public void switchCamera() {
//        this.mProperties = properties;
//        if (properties != null) {
//            Object o = mProperties.get(CameraSettings.KEY_CAMERA_ID);
//            boolean isFront = o instanceof Boolean && (Boolean) o;
//            if (isFront == mDeviceMgr.isFrontCamera()) return;
//        }
        boolean ret;
        Object o = mProperties.get(CameraSettings.KEY_CAMERA_ID);
        if (o instanceof Boolean) {
            mProperties.cameraDevice(!Boolean.TRUE.equals(o));
            ret = true;
        } else {
            String[] idList = mDeviceMgr.getCameraIdList();

            String switchId = null;

            if (mProperties != null && mProperties.isUseCameraV1()) {
                Logger.d(TAG, "switchCamera 0");
                int numberOfCameras = Camera.getNumberOfCameras();
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                if (mDeviceMgr.isFrontCamera()) {
                    for (int i = 0; i < numberOfCameras; i++) {
                        Camera.getCameraInfo(i, cameraInfo);
                        Logger.d(TAG, "switchCamera 0.1:" + cameraInfo.facing);
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                            switchId = i + "";
                            break;
                        }
                    }
                } else {
                    for (int i = 0; i < numberOfCameras; i++) {
                        Camera.getCameraInfo(i, cameraInfo);
                        Logger.d(TAG, "switchCamera 0.2:" + cameraInfo.facing);
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                            switchId = i + "";
                            break;
                        }
                    }
                }
            } else {
                Logger.d(TAG, "switchCamera 0");
                if (mDeviceMgr.isFrontCamera()) {
                    for (String id : idList) {
                        Integer f = mDeviceMgr.getCharacteristics(id)
                                .get(CameraCharacteristics.LENS_FACING);
                        if (f != null && CameraCharacteristics.LENS_FACING_BACK == f) {
                            switchId = id;
                            break;
                        }
                    }
                } else {
                    for (String id : idList) {
                        Integer f = mDeviceMgr.getCharacteristics(id)
                                .get(CameraCharacteristics.LENS_FACING);
                        if (f != null && CameraCharacteristics.LENS_FACING_FRONT == f) {
                            switchId = id;
                            break;
                        }
                    }
                }
            }
            Logger.d(TAG, "switchCamera 2:" + switchId);
            if (TextUtils.isEmpty(switchId)) return;
//        int currentId = Integer.parseInt(mDeviceMgr.getCameraId());
//        int cameraCount = mDeviceMgr.getCameraIdList().length;
//        currentId++;
//        if (cameraCount < 2) {
//            // only one camera, just return
//            return;
//        } else if (currentId >= cameraCount) {
//            currentId = 0;
//        }
//        String switchId = String.valueOf(currentId);
            mDeviceMgr.setCameraId(switchId);
            ret = getSettings().setGlobalPref(mCameraKey, switchId);
        }


        if (ret) {
            stopModule();
            startModule();
        } else {
            Logger.e(TAG, "set camera id pref error");
        }
        mUI.switchCamera();
    }

    public void setFlashMode(String mode) {
        getSettings().setPrefValueById(mDeviceMgr.getCameraId(), CameraSettings.KEY_FLASH_MODE, mode);
        mSession.applyRequest(Session.RQ_FLASH_MODE, mode);
    }

    public void setFocusDistance(float value) {
        mSession.applyRequest(Session.RQ_FOCUS_DISTANCE, value);
    }

    public void setCameraZoom(float value) {
        mSession.applyRequest(Session.RQ_CAMERA_ZOOM, value);
    }

    public void setFilterConfig(String config) {
        mFilter = config;
        if (mUI instanceof GLCameraUI) {
            ((GLCameraUI) mUI).setFilterConfig(config);
            fileSaver.setFilterConfig(config);
        }
    }

    public void onTouchToFocus(float x, float y) {
        // close all menu when touch to focus
        if (stateEnabled(Controller.CAMERA_MODULE_STOP)) return;
        getBaseUI().closeMenu();
        mFocusManager.startFocus(x, y);
        if (mProperties != null && mProperties.isUseCameraV1()) {
            Rect focusRect = mFocusManager.calcTapArea(x, y, 1.0f);
            Rect meterRect = mFocusManager.calcTapArea(x, y, 1.5f);
            mSession.applyRequest(Session.RQ_AF_AE_REGIONS, focusRect, meterRect);
        } else {
            MeteringRectangle focusRect = mFocusManager.getFocusArea(x, y, true);
            MeteringRectangle meterRect = mFocusManager.getFocusArea(x, y, false);
            mSession.applyRequest(Session.RQ_AF_AE_REGIONS, focusRect, meterRect);
        }
    }

//    public CameraCharacteristics getCharacteristics() {
//        if (mSession == null) return null;
//        return mSession.getCharacteristics();
//    }


    public CameraBaseUI getUI() {
        return mUI;
    }

    public interface PreviewCallback {
        void onPreviewFrame(byte[] data, android.util.Size size);
    }
}
