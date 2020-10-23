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
import android.util.Size;
import android.view.TextureView;

import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.Properties;
import com.snailstudio2010.camera2.callback.CameraUiEvent;
import com.snailstudio2010.camera2.callback.PictureListener;
import com.snailstudio2010.camera2.callback.RequestCallback;
import com.snailstudio2010.camera2.callback.TakeShotListener;
import com.snailstudio2010.camera2.manager.CameraSettings;
import com.snailstudio2010.camera2.manager.Controller;
import com.snailstudio2010.camera2.manager.DeviceManager;
import com.snailstudio2010.camera2.manager.FocusOverlayManager;
import com.snailstudio2010.camera2.manager.Session;
import com.snailstudio2010.camera2.manager.SingleDeviceManager;
import com.snailstudio2010.camera2.ui.CameraBaseUI;
import com.snailstudio2010.camera2.ui.CameraUI;
import com.snailstudio2010.camera2.ui.GLCameraUI;
import com.snailstudio2010.camera2.ui.gl.CameraGLSurfaceViewWithTexture;
import com.snailstudio2010.camera2.utils.FileSaver;
import com.snailstudio2010.camera2.utils.JobExecutor;
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
    private DeviceManager.CameraEvent mCameraEvent = new DeviceManager.CameraEvent() {

        @Override
        public void onDeviceOpened(Object device) {
            super.onDeviceOpened(device);
            Logger.d(TAG, "camera opened");
            presetRecordingSize();
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
            }
        }

        @Override
        public void onPreviewUiDestroy() {
            disableState(Controller.CAMERA_STATE_UI_READY);
            Logger.d(TAG, "onSurfaceTextureDestroyed");
        }

        @Override
        public void resetTouchToFocus() {
            if (stateEnabled(Controller.CAMERA_MODULE_RUNNING)) {
                mSession.applyRequest(Session.RQ_FOCUS_MODE,
                        mProperties != null && mProperties.isUseCameraV1() ? "continuous-picture" :
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            }
        }

        @Override
        public <T> void onAction(String type, T value) {
            // close all menu when ui click
//            getBaseUI().closeMenu();
            switch (type) {
                case CameraUiEvent.ACTION_SWITCH_CAMERA:
                    break;
                case CameraUiEvent.ACTION_PREVIEW_READY:
//                    if (getCoverView() != null)
//                        getCoverView().hideWithAnimation();
                    getBaseUI().onUIReady(SingleCameraModule.this,
                            (SurfaceTexture) value, null);
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

    private void presetRecordingSize() {
        if (mProperties != null && mProperties.isUseCameraV1() && mProperties.isUseGPUImage()) {
            Camera.Parameters parameters = getDeviceManager().getParameters();
            if (parameters != null) {
                Size recordSize = null;
                if (SingleCameraModule.this instanceof PhotoModule) {
                    // for takeShot
                    recordSize = getSettings().getPreviewSize(getDeviceManager().getCameraId(),
                            CameraSettings.KEY_PREVIEW_SIZE,
                            parameters);
                } else {
                    // for recording
                    recordSize = getSettings().getVideoSize(getDeviceManager().getCameraId(),
                            parameters);
                }
                ((GLCameraUI) mUI).presetRecordingSize(recordSize.getHeight(), recordSize.getWidth());
            }
        }
    }

    protected CameraBaseUI getUI(CameraUiEvent cameraUiEvent) {
        if (mProperties != null && mProperties.isUseGPUImage()) {
            Size recordSize = null;
            if (mProperties != null && mProperties.isUseCameraV1()) {
                // 此时无法配置recordSize参数
                // see presetRecordingSize()
            } else {
                if (this instanceof PhotoModule) {
                    // for takeShot
                    recordSize = getSettings().getPreviewSize(getDeviceManager().getCameraId(),
                            CameraSettings.KEY_PREVIEW_SIZE,
                            getDeviceManager().getConfigMap());
                } else {
                    // for recording
                    recordSize = getSettings().getVideoSize(getDeviceManager().getCameraId(),
                            getDeviceManager().getConfigMap());
                }
            }
            return new GLCameraUI(appContext, cameraUiEvent, recordSize, mProperties);
        } else {
            return new CameraUI(appContext, cameraUiEvent);
        }
    }

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
        mDeviceMgr = new SingleDeviceManager(appContext, mProperties, getExecutor(), mCameraEvent);
        mUI = getUI(mCameraUiEvent);
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
//        getBaseUI().closeMenu();
//        if (getCoverView() != null)
//            getCoverView().show();
        getBaseUI().onUIDestroy(this);
        mFocusManager.removeDelayMessage();
        mFocusManager.hideFocusUI();

        if (stateEnabled(Controller.CAMERA_STATE_CAPTURE)
                || stateEnabled(Controller.CAMERA_STATE_START_RECORD)) {
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mSession.release();
                    mDeviceMgr.releaseCamera();
                }
            }, 2000);
        } else {
            mSession.release();
            mDeviceMgr.releaseCamera();
        }
        Logger.d(TAG, "stop module");
    }

    @Override
    public SingleDeviceManager getDeviceManager() {
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
        if (!isDestroyed()) {
            getBaseUI().setUIClickable(true);
        }
        Logger.d(TAG, "onFileSaved uri:" + uri.toString() + ", path:" + path);
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
        if (!isDestroyed()) {
            getBaseUI().setUIClickable(true);
        }
        if (mPictureListener != null) {
            mPictureListener.onError(msg);
            mPictureListener = null;
        }
    }

    public boolean isFrontCamera() {
        return mDeviceMgr.isFrontCamera();
    }

    public boolean switchCamera() {
        boolean ret = false;
        if (mProperties != null) {
            Object o = mProperties.get(CameraSettings.KEY_CAMERA_ID);
            if (o instanceof Boolean) {
                mProperties.cameraDevice(!Boolean.TRUE.equals(o));
                ret = true;
            }
        }

        if (!ret) {
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
            if (TextUtils.isEmpty(switchId)) return false;
            mDeviceMgr.setCameraId(switchId);
            ret = getSettings().setGlobalPref(mCameraKey, switchId);
        }

        if (ret) {
            stopModule();
            startModule();
            mUI.switchCamera();
        } else {
            Logger.e(TAG, "set camera id pref error");
        }
        return ret;
    }

    public String getFlashMode() {
        return getSettings().getGlobalPref(CameraSettings.KEY_FLASH_MODE);
    }

    public void setFlashMode(String mode) {
        if (mProperties != null && mProperties.get(CameraSettings.KEY_FLASH_MODE) != null) {
            mProperties.flashMode(mode);
        } else {
            getSettings().setPrefValueById(mDeviceMgr.getCameraId(), CameraSettings.KEY_FLASH_MODE, mode);
        }
        mSession.applyRequest(Session.RQ_FLASH_MODE, mode);
    }

    public void setFocusDistance(float value) {
        mSession.applyRequest(Session.RQ_FOCUS_DISTANCE, value);
    }

    /**
     * 设置相机缩放比例
     *
     * @param value 相机当前缩放值(范围: 0.0 - 1.0，默认为0.0，最大值为1.0)
     */
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

    public Bitmap takeShot() {
        return takeShot(0, 0);
    }

    public Bitmap takeShot(int width, int height) {
        if (mUI == null) return null;
        if (mUI instanceof GLCameraUI) {
            final TakeShotLock takeShotLock = new TakeShotLock();
            CameraGLSurfaceViewWithTexture textureView = ((GLCameraUI) mUI).getGLSurfaceView();
            textureView.takeShot(width, height, new TakeShotListener() {
                @Override
                public void onTakeShot(Bitmap bmp) {
                    takeShotLock.bitmap = bmp;
                    synchronized (takeShotLock) {
                        takeShotLock.notifyAll();
                    }
                }
            });
            synchronized (takeShotLock) {
                try {
                    takeShotLock.wait(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return takeShotLock.bitmap;
        } else {
            TextureView textureView = ((CameraUI) mUI).getTextureView();
            if (width > 0 && height > 0) return textureView.getBitmap(width, height);
            else return textureView.getBitmap();
        }
    }

    public void takeShotAsync(TakeShotListener listener) {
        takeShotAsync(0, 0, listener);
    }

    public void takeShotAsync(final int width, final int height, final TakeShotListener listener) {
        if (mUI == null) {
//            if (listener != null) mainHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    listener.onTakeShot(null);
//                }
//            });
            listener.onTakeShot(null);
            return;
        }
        if (mUI instanceof GLCameraUI) {
            CameraGLSurfaceViewWithTexture textureView = ((GLCameraUI) mUI).getGLSurfaceView();
            textureView.takeShot(width, height, listener);
        } else {
            getToolKit().getExecutor().executeMust(new JobExecutor.Task<Bitmap>() {
                @Override
                public Bitmap run() {
                    TextureView textureView = ((CameraUI) mUI).getTextureView();
                    if (width > 0 && height > 0) return textureView.getBitmap(width, height);
                    else return textureView.getBitmap();
                }

                @Override
                public void onJobThread(Bitmap result) {
                    if (listener != null) listener.onTakeShot(result);
                }
            });
//            TextureView textureView = ((CameraUI) mUI).getTextureView();
//            if (listener != null) listener.onTakeShot(textureView.getBitmap());
        }
    }

    public void onTouchToFocus(float x, float y) {
        // close all menu when touch to focus
        if (stateEnabled(Controller.CAMERA_MODULE_STOP)
                || stateEnabled(Controller.CAMERA_MODULE_DESTROY)) return;
//        getBaseUI().closeMenu();
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

    public CameraBaseUI getUI() {
        return mUI;
    }

    public interface PreviewCallback {
        void onPreviewFrame(byte[] data, android.util.Size size);
    }

    private static class TakeShotLock {
        private Bitmap bitmap;
    }
}
