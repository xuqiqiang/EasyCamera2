package com.snailstudio2010.camera2.manager;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Size;
import android.view.Surface;

import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.Properties;
import com.snailstudio2010.camera2.callback.RequestCallback;
import com.snailstudio2010.camera2.utils.CameraUtil;
import com.snailstudio2010.camera2.utils.Logger;
import com.snailstudio2010.camera2.utils.MediaFunc;
import com.snailstudio2010.camera2.utils.NonNull;
import com.snailstudio2010.camera2.utils.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Camera2VideoSession extends Camera2Session {
    private final String TAG = Config.getTag(Camera2VideoSession.class);

    //    private Handler mMainHandler;
//    private Handler mBackgroundHandler;
//    private RequestManager mRequestMgr;
    private RequestCallback mCallback;
    private SurfaceTexture mTexture;
    private Surface mSurface;
    private MediaRecorder mMediaRecorder;
    private CaptureRequest.Builder mPreviewBuilder;
    private CaptureRequest.Builder mVideoBuilder;
    private int mLatestAfState = -1;
    private Size mVideoSize;
    private Size mPreviewSize;
    private File mCurrentRecordFile;
    private Properties mProperties;
    private OnImageAvailableListenerImpl mOnImageAvailableListener;
    private CameraCaptureSession.CaptureCallback mPreviewCallback = new CameraCaptureSession
            .CaptureCallback() {

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull
                CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            updateAfState(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull
                CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            updateAfState(result);
            mCallback.onRequestComplete();
        }
    };
    //session callback
    private CameraCaptureSession.StateCallback sessionStateCb = new CameraCaptureSession
            .StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Logger.d(TAG, " session onConfigured id:" + session.getDevice().getId());
            cameraSession = session;
            sendPreviewRequest();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Logger.d(TAG, "create session error id:" + session.getDevice().getId());
        }
    };
    //session callback
    private CameraCaptureSession.StateCallback videoSessionStateCb = new CameraCaptureSession
            .StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Logger.d(TAG, " session onConfigured id:" + session.getDevice().getId());
            cameraSession = session;
            sendVideoPreviewRequest();
            vibrate(50);
            try {
                mMediaRecorder.start();
                mCallback.onRecordStarted(true);
            } catch (RuntimeException e) {
                mCallback.onRecordStarted(false);
                Logger.e(TAG, "start record error msg", e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Logger.d(TAG, "create session error id:" + session.getDevice().getId());
        }
    };

    public Camera2VideoSession(Context context, Handler mainHandler, Handler backgroundThread,
                               CameraSettings settings, Properties properties) {
        super(context, mainHandler, backgroundThread, settings);
        mProperties = properties;
    }

//    public Camera2VideoSession(Context context, Handler mainHandler, Handler backgroundThread, CameraSettings settings) {
//        super(context, settings);
//        mMainHandler = mainHandler;
//        mBackgroundHandler = backgroundThread;
//        mRequestMgr = new RequestManager();
//    }

    @Override
    public void applyRequest(int msg, Object value1, Object value2) {
        switch (msg) {
            case RQ_SET_DEVICE: {
                setCameraDevice((CameraDevice) value1);
                break;
            }
            case RQ_START_PREVIEW: {
                createPreviewSession((SurfaceTexture) value1, (RequestCallback) value2);
                break;
            }
            case RQ_AF_AE_REGIONS: {
                sendControlAfAeRequest((MeteringRectangle) value1, (MeteringRectangle) value2);
                break;
            }
            case RQ_FOCUS_MODE: {
                sendControlFocusModeRequest((int) value1);
                break;
            }
            case RQ_FOCUS_DISTANCE: {
                //sendControlFocusDistanceRequest((float) value1);
                break;
            }
            case RQ_FLASH_MODE: {
                sendFlashRequest((String) value1);
                break;
            }
            case RQ_RESTART_PREVIEW: {
                sendRestartPreviewRequest();
                break;
            }
            case RQ_START_RECORD: {
                createVideoSession((Integer) value1);
                break;
            }
            case RQ_STOP_RECORD: {
                if (mMediaRecorder != null) {
                    handleStopMediaRecorder();
                }
                createPreviewSession(mTexture, mCallback);
                break;
            }
            case RQ_PAUSE_RECORD: {
                // TODO pause feature
                break;
            }
            case RQ_RESUME_RECORD: {
                // TODO resume feature
                break;
            }
            case RQ_CAMERA_ZOOM: {
                sendCameraZoomRequest((float) value1);
                break;
            }
            default: {
                Logger.e(TAG, "not used request code " + msg);
                break;
            }
        }
    }

    @Override
    public void setRequest(int msg, @Nullable Object value1, @Nullable Object value2) {
        switch (msg) {
            case RQ_SET_DEVICE: {
                break;
            }
            case RQ_START_PREVIEW: {
                break;
            }
            case RQ_AF_AE_REGIONS: {
                break;
            }
            case RQ_FOCUS_MODE: {
                break;
            }
            case RQ_FOCUS_DISTANCE: {
                break;
            }
            case RQ_FLASH_MODE: {
                mRequestMgr.applyFlashRequest(getPreviewBuilder(), (String) value1);
                break;
            }
            case RQ_RESTART_PREVIEW: {
                break;
            }
            case RQ_TAKE_PICTURE: {
                break;
            }
            default: {
                Logger.e(TAG, "not used set request code " + msg);
                break;
            }
        }
    }

    @Override
    public void release() {
        if (cameraSession != null) {
            cameraSession.close();
            cameraSession = null;
        }
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    private void handleStopMediaRecorder() {
        try {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mCallback.onRecordStopped(mCurrentRecordFile.getPath(),
                    mVideoSize.getWidth(), mVideoSize.getHeight());
            vibrate(50);
        } catch (Exception e) {
            mMediaRecorder.reset();
            if (mCurrentRecordFile.exists() && mCurrentRecordFile.delete()) {
                Logger.e(TAG, "video file delete success");
            }
            Logger.e(TAG, "handleStopMediaRecorder", e);
        }
    }

    private void sendFlashRequest(String value) {
        Logger.d(TAG, "flash value:" + value);
        CaptureRequest request = mRequestMgr.getFlashRequest(getPreviewBuilder(), value);
        sendRepeatingRequest(request, mPreviewCallback, mMainHandler);
    }

    private void setCameraDevice(CameraDevice device) {
        cameraDevice = device;
        // device changed, get new Characteristics
        initCharacteristics();
        mRequestMgr.setCharacteristics(characteristics);
        // camera device may change, reset builder
        mPreviewBuilder = null;
        mVideoBuilder = null;
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
    }

    /* need call after surface is available, after session configured
     * send preview request in callback */
    private void createPreviewSession(@NonNull SurfaceTexture texture, RequestCallback callback) {
        if (cameraSession != null) {
            cameraSession.close();
            cameraSession = null;
        }
        mVideoBuilder = null;
        mCallback = callback;
        mTexture = texture;
//        if (mSurface == null)
//            mSurface = new Surface(mTexture);
        mRequestMgr.setRequestCallback(callback);
        mRequestMgr.setProperties(mProperties);
        getPreviewBuilder();
        try {
            cameraDevice.createCaptureSession(setPreviewOutputSize(cameraDevice.getId(), mTexture),
                    sessionStateCb, mMainHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createVideoSession(int deviceRotation) {
        if (cameraSession != null) {
            cameraSession.close();
            cameraSession = null;
        }
        setUpMediaRecorder(deviceRotation);
        try {
            cameraDevice.createCaptureSession(setVideoOutputSize(
                    mTexture, mMediaRecorder.getSurface()), videoSessionStateCb, mMainHandler);
        } catch (CameraAccessException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void sendPreviewRequest() {
        CaptureRequest.Builder builder = getPreviewBuilder();
        updateRequestFromSetting(builder);
        CaptureRequest request = mRequestMgr.getPreviewRequest(builder);
        sendRepeatingRequest(request, mPreviewCallback, mMainHandler);
    }

    private void sendVideoPreviewRequest() {
        CaptureRequest.Builder builder = getVideoBuilder();
        updateRequestFromSetting(builder);
        CaptureRequest request = mRequestMgr.getPreviewRequest(builder);
        sendRepeatingRequest(request, mPreviewCallback, mMainHandler);
    }

    private void sendControlAfAeRequest(MeteringRectangle focusRect,
                                        MeteringRectangle meteringRect) {
        CaptureRequest.Builder builder = getPreviewBuilder();
        CaptureRequest request = mRequestMgr
                .getTouch2FocusRequest(builder, focusRect, meteringRect);
        sendRepeatingRequest(request, mPreviewCallback, mMainHandler);
        // trigger af
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        sendCaptureRequest(builder.build(), null, mMainHandler);
    }

    private void sendControlFocusModeRequest(int focusMode) {
        Logger.d(TAG, "focusMode:" + focusMode);
        CaptureRequest request = mRequestMgr.getFocusModeRequest(getPreviewBuilder(), focusMode);
        sendRepeatingRequest(request, mPreviewCallback, mMainHandler);
    }

    private void sendRestartPreviewRequest() {
        Logger.d(TAG, "need start preview :" + cameraSettings.needStartPreview());
        if (cameraSettings.needStartPreview()) {
            sendPreviewRequest();
        }
    }

    private void sendCameraZoomRequest(float value) {
        CaptureRequest request = mRequestMgr.getCameraZoomRequest(getPreviewBuilder(), value);
        sendRepeatingRequest(request, mPreviewCallback, mMainHandler);
    }

    private CaptureRequest.Builder getPreviewBuilder() {
        // if is in video recording, request send to use VideoBuilder
        if (mVideoBuilder != null) {
            return mVideoBuilder;
        }
        if (mPreviewBuilder == null) {
            if (mSurface == null) mSurface = new Surface(mTexture);
            mPreviewBuilder = createBuilder(CameraDevice.TEMPLATE_PREVIEW, mSurface);
        }
        return mPreviewBuilder;
    }

    private CaptureRequest.Builder getVideoBuilder() {
        try {
            if (mSurface == null) mSurface = new Surface(mTexture);
            mVideoBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mVideoBuilder.addTarget(mSurface);
            mVideoBuilder.addTarget(mMediaRecorder.getSurface());
        } catch (CameraAccessException | IllegalStateException e) {
            e.printStackTrace();
        }
        return mVideoBuilder;
    }

    private void updateRequestFromSetting(CaptureRequest.Builder builder) {
        String flashValue = cameraSettings.getGlobalPref(CameraSettings.KEY_FLASH_MODE);
        mRequestMgr.applyFlashRequest(builder, flashValue);
    }

    //config picture size and preview size
    private List<Surface> setPreviewOutputSize(String id, SurfaceTexture texture) {
        StreamConfigurationMap map = characteristics
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        // parameters key
        mVideoSize = cameraSettings.getVideoSize(id, map);
        double videoRatio = mVideoSize.getWidth() / (double) (mVideoSize.getHeight());
        mPreviewSize = cameraSettings.getPreviewSizeByRatio(map.getOutputSizes(SurfaceTexture.class),
                CameraSettings.KEY_VIDEO_PREVIEW_SIZE, videoRatio);
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Logger.d(TAG, "previewSize:" + mPreviewSize);
        Logger.d(TAG, "videoSize:" + mVideoSize);
        // config surface
//        Surface surface = new Surface(texture);
        Size uiSize = CameraUtil.getPreviewUiSize(appContext, mPreviewSize);
        mCallback.onViewChange(uiSize.getHeight(), uiSize.getWidth());

        if (mOnImageAvailableListener == null) {
            mOnImageAvailableListener = new OnImageAvailableListenerImpl(mBackgroundHandler);
        }
        Surface previewReaderSurface = mOnImageAvailableListener
                .handleImage(mPreviewSize, mPreviewBuilder, mCameraPreviewCallback);
        if (previewReaderSurface != null)
            return Arrays.asList(mSurface, previewReaderSurface);
//            list.add(previewReaderSurface);

        return Collections.singletonList(mSurface);
    }

    // config video record size
    private List<Surface> setVideoOutputSize(SurfaceTexture texture, Surface videoSurface) {
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        // config surface
//        Surface surface = new Surface(texture);
        if (mSurface == null) mSurface = new Surface(mTexture);
        return Arrays.asList(mSurface, videoSurface);
    }

    private void setUpMediaRecorder(int deviceRotation) {
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }
        mCurrentRecordFile = MediaFunc.getOutputMediaFile(MediaFunc.MEDIA_TYPE_VIDEO, "VIDEO",
                mProperties != null ? mProperties.getSavePath() : null);
        if (mCurrentRecordFile == null) {
            Logger.e(TAG, " get video file error");
            return;
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mCurrentRecordFile.getPath());
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = CameraUtil.getJpgRotation(characteristics, deviceRotation);
        mMediaRecorder.setOrientationHint(rotation);
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Logger.e(TAG, "error prepare video record:" + e.getMessage());
        }
    }

    private void updateAfState(CaptureResult result) {
        Integer state = result.get(CaptureResult.CONTROL_AF_STATE);
        if (state != null && mLatestAfState != state) {
            mLatestAfState = state;
            mCallback.onAFStateChanged(state);
        }
    }

}
