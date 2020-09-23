package com.snailstudio2010.camera2.manager;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CaptureResult;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.WindowManager;

import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.callback.RequestCallback;
import com.snailstudio2010.camera2.utils.Logger;
import com.snailstudio2010.camera2.utils.NonNull;
import com.snailstudio2010.camera2.utils.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.snailstudio2010.camera2.Properties.toSize;
import static com.snailstudio2010.camera2.manager.FocusOverlayManager.IMAP_STORE_RESPONSE;

public class CameraPhotoSession extends Session {

    private final String TAG = Config.getTag(CameraPhotoSession.class);

    private Handler mMainHandler;
    private RequestManager mRequestMgr;
    private RequestCallback mCallback;
    private int mDeviceRotation;

    private Camera mCamera;
    private String mCameraId;

    private Camera.Parameters mParameters;
    private int mPictureWidth;
    private int mPictureHeight;
    private Size mPreviewSize;

    public CameraPhotoSession(Context context, Handler mainHandler, CameraSettings settings) {
        super(context, settings);
        mMainHandler = mainHandler;
        mRequestMgr = new RequestManager();
    }

    @Override
    public void applyRequest(int msg, Object value1, Object value2) {
        switch (msg) {
            case RQ_SET_DEVICE: {
                mCamera = (Camera) value1;
                mCameraId = (String) value2;
                Logger.d(TAG, "RQ_SET_CAMERA_DEVICE");
                break;
            }
            case RQ_START_PREVIEW: {
                createCameraPreviewSession((SurfaceTexture) value1, (RequestCallback) value2);
                break;
            }
            case RQ_AF_AE_REGIONS: {
                Rect focusRect = (Rect) value1;
                Rect meteringRect = (Rect) value2;
                Logger.d("rect_focusRect", focusRect.toString());
                Logger.d("rect_meteringRect", meteringRect.toString());
                if (this.mParameters.getMaxNumFocusAreas() > 0) {
                    List<Camera.Area> focusAreas = new ArrayList();
                    focusAreas.add(new Camera.Area(focusRect, IMAP_STORE_RESPONSE));
                    this.mParameters.setFocusAreas(focusAreas);
                }
                if (this.mParameters.getMaxNumMeteringAreas() > 0) {
                    List<Camera.Area> meteringAreas = new ArrayList();
                    meteringAreas.add(new Camera.Area(meteringRect, IMAP_STORE_RESPONSE));
                    this.mParameters.setMeteringAreas(meteringAreas);
                }

                setFocusMode(this.mParameters, Camera.Parameters.FOCUS_MODE_AUTO);
                this.mCamera.setParameters(this.mParameters);
                this.mCamera.autoFocus(new Camera.AutoFocusCallback() {

                    public void onAutoFocus(boolean success, Camera camera) {
                        Logger.d(TAG, "onAutoFocus:" + success);
                        mCallback.onAFStateChanged(success ? CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED :
                                CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED);
                    }
                });
//                sendControlAfAeRequest((MeteringRectangle) value1, (MeteringRectangle) value2);
                break;
            }
            case RQ_FOCUS_MODE: {
                setFocusMode(this.mParameters, (String) value1);
//                sendControlFocusModeRequest((int) value1);
                break;
            }
            case RQ_FLASH_MODE: {
//                sendFlashRequest((String) value1);
                this.mParameters.setFlashMode((String) value1);
                this.mCamera.setParameters(this.mParameters);
                this.mCamera.startPreview();
                break;
            }
            case RQ_RESTART_PREVIEW: {
                this.mCamera.startPreview();
                break;
            }
            case RQ_TAKE_PICTURE: {
                mDeviceRotation = (Integer) value1;
                runCaptureStep((Camera.ShutterCallback) value2);
                break;
            }
            case RQ_CAMERA_ZOOM: {
                setCameraZoom((float) value1);
                break;
            }
            default: {
                Logger.e(TAG, "invalid request code " + msg);
                break;
            }
        }
    }

    @Override
    public void setRequest(int msg, @Nullable Object value1, @Nullable Object value2) {
    }

    @Override
    public void release() {
//        if (cameraSession != null) {
//            cameraSession.close();
//            cameraSession = null;
//        }
//        if (mImageReader != null) {
//            mImageReader.close();
//            mImageReader = null;
//        }
    }

    private void createCameraPreviewSession(@NonNull SurfaceTexture texture, final RequestCallback callback) {
        mCallback = callback;
        mRequestMgr.setRequestCallback(callback);
        this.mParameters = this.mCamera.getParameters();
        this.mParameters.setPictureFormat(ImageFormat.JPEG);
        setPictureSize(this.mParameters);
        setPreviewSize(this.mParameters);
        setFocusMode(this.mParameters, Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        this.mCamera.setParameters(this.mParameters);

        try {
            this.mCamera.setPreviewTexture(texture);
        } catch (IOException e) {
            Logger.e(TAG, "createCameraPreviewSession", e);
            e.printStackTrace();
        }
        Logger.d(TAG, "createCameraPreviewSession");
        this.mCamera.startPreview();
        if (mCameraPreviewCallback != null) {
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    mCameraPreviewCallback.onPreviewFrame(data, mPreviewSize);
                }
            });
        } else {
            mCamera.setPreviewCallback(null);
        }
    }

    private void setPictureSize(Camera.Parameters parameters) {
        Size size = cameraSettings.getPictureSize(mCameraId, parameters);
        mPictureWidth = size.getWidth();
        mPictureHeight = size.getHeight();
        Logger.d(TAG, "pictureSize:" + size);
        parameters.setPictureSize(mPictureWidth, mPictureHeight);
//        List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
//        if (sizes != null) {
//            int maxSize = 0;
//            int width = 0;
//            int height = 0;
//            for (int i = 0; i < sizes.size(); i++) {
//                Camera.Size size = (Camera.Size) sizes.get(i);
//                int pix = size.width * size.height;
//                if (pix > maxSize) {
//                    maxSize = pix;
//                    width = size.width;
//                    height = size.height;
//                }
//            }
//            Logger.d(TAG, "设置图片的大小：" + width + " height:" + height);
//            parameters.setPictureSize(width, height);
//        }
    }

    private void setFocusMode(Camera.Parameters parameters, String focusMode) {
        if (parameters.getSupportedFocusModes().contains(focusMode)) {
            parameters.setFocusMode(focusMode);
        }
    }

    private boolean setPreviewSize(Camera.Parameters parameters) {

//        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
//        if (sizes != null) {
//            int maxSize = 0;
//            int width = 0;
//            int height = 0;
//            for (int i = 0; i < sizes.size(); i++) {
//                Camera.Size size = (Camera.Size) sizes.get(i);
//                int pix = size.width * size.height;
//                if (pix > maxSize) {
//                    maxSize = pix;
//                    width = size.width;
//                    height = size.height;
//                }
//            }
//            Logger.d(TAG, "设置预览的大小：" + width + " height:" + height);
//            parameters.setPreviewSize(width, height);
//            this.preViewWidth = width;
//            this.preViewHeight = height;
//        }

        Size[] sizes = toSize(parameters.getSupportedPreviewSizes());
        double ratio = mPictureWidth / (double) mPictureHeight;

        mPreviewSize = cameraSettings.getPreviewSizeByRatio(sizes, CameraSettings.KEY_PREVIEW_SIZE, ratio);

//        Size size = cameraSettings.getPreviewSize(mCameraId, CameraSettings.KEY_PREVIEW_SIZE, parameters);
//        this.preViewWidth = size.getWidth();
//        this.preViewHeight = size.getHeight();
        parameters.setPreviewSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        setSurfaceViewSize();
        Logger.d(TAG, "previewSize:" + mPreviewSize);

//        Data.preViewSizes = parameters.getSupportedPreviewSizes();
//        if (Data.defaultPreViewSize == null) {
//            Data.defaultPreViewSize = this.mCamera.getParameters().getPreviewSize();
//        }
//        if (Data.hasSettingPreViewSize) {
//            this.preViewWidth = Data.preViewWidth;
//            this.preViewHeight = Data.preViewHeight;
//            parameters.setPreviewSize(this.preViewWidth, this.preViewHeight);
//            setSurfaceViewSize();
//            return true;
//        }
//        Logger.d(TAG, "初始预览的大小：width:" + Data.defaultPreViewSize.width + " height:" + Data.defaultPreViewSize.height);
//        this.preViewWidth = Data.defaultPreViewSize.width;
//        this.preViewHeight = Data.defaultPreViewSize.height;
//        Data.preViewWidth = Data.defaultPreViewSize.width;
//        Data.preViewHeight = Data.defaultPreViewSize.height;
//        setSurfaceViewSize();
        return false;
    }

    private void setSurfaceViewSize() {

        WindowManager windowManager = (WindowManager) appContext.getSystemService(Context
                .WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        int w = metrics.widthPixels;
        int previewWidth = mPreviewSize.getWidth();
        int previewHeight = mPreviewSize.getHeight();
        if (previewWidth > previewHeight) {
            previewWidth = mPreviewSize.getHeight();
            previewHeight = mPreviewSize.getWidth();
        }
        int h = (int) ((((float) previewHeight) / ((float) previewWidth)) * ((float) w));
//        if (this.width != w || this.height != h) {
//            MainActivity.getInstance().setSize((float) w, (float) h);
        mCallback.onViewChange(w, h);

        Logger.d(TAG, "setSurfaceViewSize：width:" + w + " height:" + h);
//        }
    }

    private void setCameraZoom(float value) {
        float maxZoom = this.mParameters.getMaxZoom();
        float zoom = 1 + (maxZoom - 1) * value;
        Logger.d(TAG, "setCameraZoom maxZoom:" + maxZoom + ",zoom:" + zoom);
        this.mParameters.setZoom((int) zoom);
        this.mCamera.setParameters(this.mParameters);
        this.mCamera.startPreview();
        mCallback.onZoomChanged(zoom, maxZoom);
    }

    private void runCaptureStep(Camera.ShutterCallback onShutter) {
        int rotation = 0;
        if (mDeviceRotation != OrientationEventListener.ORIENTATION_UNKNOWN) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(Integer.parseInt(mCameraId), info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                rotation = (info.orientation - mDeviceRotation + 360) % 360;
            } else { // back-facing camera
                rotation = (info.orientation + mDeviceRotation) % 360;
                Log.d(TAG, "_testo_ getJpegRotation info.orientation:" + info.orientation);
                Log.d(TAG, "_testo_ getJpegRotation orientation:" + mDeviceRotation);
            }
        }
        Log.d(TAG, "_testo_ getJpegRotation rotation:" + rotation);
        mParameters.setRotation(rotation);
        mCamera.setParameters(mParameters);
        this.mCamera.takePicture(onShutter, null, new Camera.PictureCallback() {

            public void onPictureTaken(final byte[] data, Camera camera) {
                mCallback.onRequestComplete();
                new Thread() {
                    public void run() {
                        Camera.Size size = mParameters.getPictureSize();
                        mCallback.onDataBack(data,
                                size.width, size.height);
                    }
                }.start();
            }
        });
    }
}
