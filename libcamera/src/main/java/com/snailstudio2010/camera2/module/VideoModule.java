package com.snailstudio2010.camera2.module;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;

import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.Properties;
import com.snailstudio2010.camera2.callback.CameraUiEvent;
import com.snailstudio2010.camera2.callback.PictureListener;
import com.snailstudio2010.camera2.callback.RequestCallback;
import com.snailstudio2010.camera2.manager.Camera2VideoSession;
import com.snailstudio2010.camera2.manager.CameraSettings;
import com.snailstudio2010.camera2.manager.CameraVideoSession;
import com.snailstudio2010.camera2.manager.Controller;
import com.snailstudio2010.camera2.manager.Session;
import com.snailstudio2010.camera2.ui.CameraBaseUI;
import com.snailstudio2010.camera2.ui.GLVideoUI;
import com.snailstudio2010.camera2.ui.VideoUI;
import com.snailstudio2010.camera2.ui.gl.CameraRecordGLSurfaceView;
import com.snailstudio2010.camera2.utils.JobExecutor;
import com.snailstudio2010.camera2.utils.MediaFunc;

/**
 * Created by xuqiqiang on 16-3-8.
 */
public class VideoModule extends SingleCameraModule {

    private static final String TAG = Config.getTag(VideoModule.class);

    private RequestCallback mRequestCallback = new RequestCallback() {
        @Override
        public void onViewChange(final int width, final int height) {
            super.onViewChange(width, height);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getBaseUI().updateUiSize(width, height);
                    mFocusManager.onPreviewChanged(width, height, mDeviceMgr.getCharacteristics());
                }
            });
        }

        @Override
        public void onAFStateChanged(final int state) {
            super.onAFStateChanged(state);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateAFState(state, mFocusManager);
                }
            });
        }

        @Override
        public void onRecordStarted(final boolean success) {
            super.onRecordStarted(success);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handleRecordStarted(success);
                }
            });
        }

        @Override
        public void onRecordStopped(final String filePath, final int width, final int height) {
            getExecutor().execute(new JobExecutor.Task<Bitmap>() {
                @Override
                public Bitmap run() {
                    return getVideoThumbnail(filePath);
                }

                @Override
                public void onJobThread(Bitmap result) {
                    fileSaver.saveVideoFile(width, height, getToolKit().getOrientation(),
                            filePath, MediaFunc.MEDIA_TYPE_VIDEO, result);
                }

//                @Override
//                public void onMainThread(Bitmap result) {
//                    getBaseUI().setThumbnail(result);
//                }
            });
        }

        @Override
        public void onZoomChanged(float currentZoom, float maxZoom) {
            getBaseUI().onZoomChanged(currentZoom, maxZoom);
        }
    };

    public VideoModule() {
        super();
    }

    public VideoModule(Properties properties) {
        super(properties);
    }

    @Override
    protected CameraBaseUI getUI(CameraUiEvent mCameraUiEvent) {
        return mProperties != null && mProperties.isUseGPUImage() ?
                new GLVideoUI(appContext, mainHandler, mCameraUiEvent, mProperties) :
                new VideoUI(appContext, mainHandler, mCameraUiEvent);
//        return new VideoUI(appContext, mainHandler, mCameraUiEvent);
    }

    @Override
    protected Session getSession() {
        return mProperties != null && mProperties.isUseCameraV1() ?
                new CameraVideoSession(appContext, mainHandler, getSettings(), mProperties) :
                new Camera2VideoSession(appContext, mainHandler, getToolKit().getBackgroundHandler(),
                        getSettings(), mProperties);
//        return new Camera2VideoSession(appContext, mainHandler, getSettings());
    }

    @Override
    protected String getCameraKey() {
        return CameraSettings.KEY_VIDEO_ID;
    }

    @Override
    protected RequestCallback getRequestCallback() {
        return mRequestCallback;
    }


    private Bitmap getVideoThumbnail(String path) {
        return ThumbnailUtils.createVideoThumbnail(
                path, MediaStore.Video.Thumbnails.MICRO_KIND);
    }

    private void handleRecordStarted(boolean success) {
        getBaseUI().setUIClickable(true);
        if (success) {
            if (mPictureListener != null) mPictureListener.onVideoStart();
            if (mUI instanceof VideoUI)
                ((VideoUI) mUI).startVideoTimer();
            else if (mUI instanceof GLVideoUI)
                ((GLVideoUI) mUI).startVideoTimer();
        } else {
            disableState(Controller.CAMERA_STATE_START_RECORD);
            if (mPictureListener != null) mPictureListener.onVideoStop();
        }
    }

    private void startVideoRecording() {
        enableState(Controller.CAMERA_STATE_START_RECORD);
        getBaseUI().setUIClickable(false);
        if (stateEnabled(Controller.CAMERA_STATE_UI_READY)) {
            if (mUI instanceof GLVideoUI) {
                ((GLVideoUI) mUI).startRecording(new CameraRecordGLSurfaceView.StartRecordingCallback() {

                    @Override
                    public void startRecordingOver(boolean success) {
                        mRequestCallback.onRecordStarted(success);
                    }
                });
                return;
            }
            getExecutor().execute(new JobExecutor.Task<Void>() {
                @Override
                public Void run() {
                    mSession.applyRequest(Session.RQ_START_RECORD,
                            getToolKit().getOrientation());
                    return super.run();
                }
            });
        }
    }

    private void stopVideoRecording() {
        disableState(Controller.CAMERA_STATE_START_RECORD);
        if (mPictureListener != null) mPictureListener.onVideoStop();
        getBaseUI().setUIClickable(false);

        if (mUI instanceof GLVideoUI) {
            ((GLVideoUI) mUI).endRecording(new GLVideoUI.RecordEndListener() {
                @Override
                public void onRecordEnd(String filePath, int width, int height) {
                    mRequestCallback.onRecordStopped(filePath, width, height);
                }
            });
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((GLVideoUI) mUI).stopVideoTimer();
                    getBaseUI().setUIClickable(true);
                }
            });
            return;
        }
        getExecutor().execute(new JobExecutor.Task<Void>() {
            @Override
            public Void run() {
                mSession.applyRequest(Session.RQ_STOP_RECORD);
                mSession.applyRequest(Session.RQ_START_PREVIEW,
                        mSurfaceTexture, mRequestCallback);
                return super.run();
            }

            @Override
            public void onMainThread(Void result) {
                super.onMainThread(result);
                ((VideoUI) mUI).stopVideoTimer();
                getBaseUI().setUIClickable(true);
            }
        });
    }

    public void handleVideoRecording(PictureListener listener) {
        if (stateEnabled(Controller.CAMERA_STATE_START_RECORD)) {
            stopVideoRecording();
        } else {
//            setPictureListener(listener);
            mPictureListener = listener;
            startVideoRecording();
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (stateEnabled(Controller.CAMERA_STATE_START_RECORD)) {
            stopVideoRecording();
        }
    }
}
