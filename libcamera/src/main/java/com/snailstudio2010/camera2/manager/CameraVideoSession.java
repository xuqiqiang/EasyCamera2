package com.snailstudio2010.camera2.manager;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CaptureResult;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.Properties;
import com.snailstudio2010.camera2.callback.RequestCallback;
import com.snailstudio2010.camera2.utils.Logger;
import com.snailstudio2010.camera2.utils.MediaFunc;
import com.snailstudio2010.camera2.utils.NonNull;
import com.snailstudio2010.camera2.utils.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.snailstudio2010.camera2.Properties.toSize;
import static com.snailstudio2010.camera2.manager.FocusOverlayManager.IMAP_STORE_RESPONSE;

public class CameraVideoSession extends Session {

    private final String TAG = Config.getTag(CameraVideoSession.class);

    private Handler mMainHandler;
    private RequestManager mRequestMgr;
    private RequestCallback mCallback;
//    private int mDeviceRotation;

    private SurfaceTexture mTexture;

    private Camera mCamera;
    private String mCameraId;

    private Camera.Parameters mParameters;
    private int mPictureWidth;
    private int mPictureHeight;
    private int preViewHeight;
    private int preViewWidth;
    private Size mPreviewSize;

    //    private int mVideoWidth;
//    private int mVideoHeight;
    private CamcorderProfile mCamcorderProfile;

    private MediaRecorder mMediaRecorder;
    private File mCurrentRecordFile;
    private Properties mProperties;

    public CameraVideoSession(Context context, Handler mainHandler, CameraSettings settings, Properties properties) {
        super(context, settings);
        mMainHandler = mainHandler;
        mRequestMgr = new RequestManager();
        mProperties = properties;
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
//                mDeviceRotation = (Integer) value1;
//                runCaptureStep();
                break;
            }
            case RQ_CAMERA_ZOOM: {
                setCameraZoom((float) value1);
                break;
            }
            case RQ_START_RECORD: {
                setUpMediaRecorder((Integer) value1);
                break;
            }
            case RQ_STOP_RECORD: {
                if (mMediaRecorder != null) {
                    mMediaRecorder.stop();
                    mMediaRecorder.release();
                    mMediaRecorder = null;
                    mCallback.onRecordStopped(mCurrentRecordFile.getPath(),
                            mCamcorderProfile != null ? mCamcorderProfile.videoFrameWidth : mPictureWidth,
                            mCamcorderProfile != null ? mCamcorderProfile.videoFrameHeight : mPictureHeight);
                }
                createCameraPreviewSession(mTexture, mCallback);
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
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    private void createCameraPreviewSession(@NonNull SurfaceTexture texture, RequestCallback callback) {
        mCallback = callback;
        mTexture = texture;
        mRequestMgr.setRequestCallback(callback);
        this.mParameters = this.mCamera.getParameters();
        this.mParameters.setPictureFormat(ImageFormat.JPEG);
        mParameters.setRecordingHint(true);
        if (mParameters.isVideoStabilizationSupported())
            mParameters.setVideoStabilization(true);
        setPictureSize(this.mParameters);
        setPreviewSize(this.mParameters);
//        setVideoSize(this.mParameters);
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
        parameters.setPictureSize(mPictureWidth, mPictureHeight);

//        List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
//        if (sizes != null) {
//            int maxSize = 0;
//            int width = 0;
//            int height = 0;
//            for (int i = 0; i < sizes.size(); i++) {
//                Camera.Size size = (Camera.Size) sizes.get(i);
//                int pix = size.width * size.height;
//                if (pix < 2000 * 10000 && pix > maxSize) {
//                    maxSize = pix;
//                    width = size.width;
//                    height = size.height;
//                }
//            }
//            Logger.d(TAG, "设置图片的大小：" + width + " height:" + height);
//            parameters.setPictureSize(width, height);
//        }
    }

//    private void setVideoSize(Camera.Parameters parameters) {
//        Size size = cameraSettings.getVideoSize(mCameraId, parameters);
//        mVideoWidth = size.getWidth();
//        mVideoHeight = size.getHeight();
//        parameters.setPictureSize(mPictureWidth, mPictureHeight);
//    }

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
//        List<Camera.Size>  a = parameters.getSupportedVideoSizes();
//        for(Camera.Size s : a) {
//            Logger.d(TAG, "getSupportedVideoSizes:" + s.width + " height:" + s.height);
//        }

        Size[] sizes = toSize(parameters.getSupportedPreviewSizes());
        Size videoSize = cameraSettings.getVideoSize(mCameraId, parameters);
        double ratio = videoSize.getWidth() / (double) videoSize.getHeight();

        mPreviewSize = cameraSettings.getPreviewSizeByRatio(sizes, CameraSettings.KEY_VIDEO_PREVIEW_SIZE, ratio);
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
//        int pre_w = this.preViewWidth;
//        int pre_h = this.preViewHeight;
//        if (pre_w > pre_h) {
//            pre_w = this.preViewHeight;
//            pre_h = this.preViewWidth;
//        }
//        int h = (int) ((((float) pre_h) / ((float) pre_w)) * ((float) w));

        int previewWidth = mPreviewSize.getWidth();
        int previewHeight = mPreviewSize.getHeight();
        if (previewWidth > previewHeight) {
            previewWidth = mPreviewSize.getHeight();
            previewHeight = mPreviewSize.getWidth();
        }
        int h = (int) ((((float) previewHeight) / ((float) previewWidth)) * ((float) w));

//        if (this.width != w || this.height != h) {
//            MainActivity.getInstance().setSize((float) w, (float) h);
        mCallback.onViewChange((int) w, (int) h);

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

//    private void runCaptureStep() {
//        this.mCamera.takePicture(null, null, new Camera.PictureCallback() {
//
//            public void onPictureTaken(final byte[] data, Camera camera) {
//                new Thread() {
//                    public void run() {
//                        Camera.Size size = mParameters.getPictureSize();
//                        mCallback.onDataBack(data,
//                                size.width, size.height);
//                    }
//                }.start();
//            }
//        });
//    }


    private void setUpMediaRecorder(int deviceRotation) {

        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.stop();
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
            mCamera.unlock();
//        if (mMediaRecorder == null) {
//            mMediaRecorder = new MediaRecorder();
//        }
            mMediaRecorder = new MediaRecorder();

            //将相机设置给MediaRecorder
            mMediaRecorder.setCamera(mCamera);

            mCurrentRecordFile = MediaFunc.getOutputMediaFile(MediaFunc.MEDIA_TYPE_VIDEO, "VIDEO",
                    mProperties != null ? mProperties.getSavePath() : null);
            if (mCurrentRecordFile == null) {
                Logger.e(TAG, " get video file error");
                return;
            }
            // 设置录制视频源和音频源
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);//设置视频编码
//        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);//设置音频编码
//        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);

            /*3.CamcorderProfile.QUALITY_HIGH:质量等级对应于最高可用分辨率*/
            // Set the recording profile.
//        CamcorderProfile profile = null;
//
//        Size videoSize = cameraSettings.getVideoSize(mCameraId, mParameters);
//        Logger.d(TAG, "setUpMediaRecorder setVideoSize width:" + videoSize.getWidth()
//                + ", height:" + videoSize.getHeight());
//
//        int[] profiles = {CamcorderProfile.QUALITY_2160P, CamcorderProfile.QUALITY_1080P,
//                CamcorderProfile.QUALITY_720P, CamcorderProfile.QUALITY_480P, CamcorderProfile.QUALITY_HIGH,
//                CamcorderProfile.QUALITY_QCIF};
//        int[] profilesSize = {2160, 1080, 720, 480, 360, 144};
//        int size = Math.min(videoSize.getWidth(), videoSize.getHeight());
//        for (int i = 0; i < profiles.length; i += 1) {
//            if (size >= profilesSize[i]) {
//                profile = CamcorderProfile.get(profiles[i]);
//                break;
//            }
//        }

//        if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_1080P))
//            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
//        else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P))
//              profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
//        else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P))
//              profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
//        else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_HIGH))
//              profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
//        if (profile != null)
            mCamcorderProfile = cameraSettings.getVideoProfile(mCameraId, mParameters);
            mMediaRecorder.setProfile(mCamcorderProfile);

//        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));

            // 设置录制完成后视频的封装格式THREE_GPP为3gp.MPEG_4为mp4
//        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mMediaRecorder.setVideoEncodingBitRate(10000000);
//        mMediaRecorder.setVideoFrameRate(30);

            // 设置录制的视频编码和音频编码
//        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

//        Logger.d(TAG, "setUpMediaRecorder setVideoSize width:" + mParameters.getPictureSize().width
//        +  ", height:" + mParameters.getPictureSize().height);
            // 设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
//        mMediaRecorder.setVideoSize(mParameters.getPictureSize().width, mParameters.getPictureSize().height);
            // 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错
//        mMediaRecorder.setVideoFrameRate(30);
//        mMediaRecorder.setVideoEncodingBitRate(1024*1024*20);
//        int rotation = CameraUtil.getJpgRotation(characteristics, deviceRotation);
//        mMediaRecorder.setOrientationHint(rotation);

//        mMediaRecorder.setPreviewDisplay(new Surface(mTexture));

            /*4.设置输出文件*/
            mMediaRecorder.setOutputFile(mCurrentRecordFile.getPath());
            Logger.d(TAG, "setUpMediaRecorder getPath:" + mCurrentRecordFile.getPath());

            /*摄像头默认是横屏，这是拍摄的视频旋转90度*/
//            mMediaRecorder.setOrientationHint(90);

            // See android.hardware.Camera.Parameters.setRotation for
            // documentation.
            // Note that mOrientation here is the device orientation, which is the
            // opposite of
            // what activity.getWindowManager().getDefaultDisplay().getRotation()
            // would return,
            // which is the orientation the graphics need to rotate in order to
            // render correctly.
            int rotation = 0;
            if (deviceRotation != OrientationEventListener.ORIENTATION_UNKNOWN) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(Integer.parseInt(mCameraId), info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    rotation = (info.orientation - deviceRotation + 360) % 360;
                } else { // back-facing camera
                    rotation = (info.orientation + deviceRotation) % 360;
                    Log.d(TAG, "_testo_ getJpegRotation info.orientation:" + info.orientation);
                    Log.d(TAG, "_testo_ getJpegRotation orientation:" + deviceRotation);
                }
            }
            Log.d(TAG, "_testo_ getJpegRotation rotation:" + rotation);
            mMediaRecorder.setOrientationHint(rotation);

            /*5.设置预览输出*/
            mMediaRecorder.setPreviewDisplay(new Surface(mTexture));

            try {
                mMediaRecorder.prepare();
            } catch (IOException e) {
                Logger.e(TAG, "error prepare video record", e);
            }

            mMediaRecorder.start();
            mCallback.onRecordStarted(true);
        } catch (Exception e) {
            e.printStackTrace();
            mCallback.onRecordStarted(false);
        }
    }
}
