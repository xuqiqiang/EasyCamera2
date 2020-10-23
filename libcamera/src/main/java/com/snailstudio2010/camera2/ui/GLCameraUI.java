package com.snailstudio2010.camera2.ui;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.os.Vibrator;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;

import com.snailstudio2010.camera2.Properties;
import com.snailstudio2010.camera2.callback.CameraUiEvent;
import com.snailstudio2010.camera2.ui.gl.CameraRecordGLSurfaceView;
import com.snailstudio2010.camera2.utils.Logger;
import com.snailstudio2010.camera2.utils.MediaFunc;
import com.snailstudio2010.libcamera.R;

import java.io.File;

/**
 * Created by xuqiqiang on 16-3-18.
 */
public class GLCameraUI extends CameraBaseUI implements TextureView.SurfaceTextureListener {

    private final String TAG = this.getClass().getSimpleName();

    private View mRootView;
    private CameraRecordGLSurfaceView mPreviewTexture;
    private File mCurrentRecordFile;
    private Properties mProperties;

    public GLCameraUI(Context context, CameraUiEvent event, Size recordSize, Properties properties) {
        super(event);
        mRootView = LayoutInflater.from(context)
                .inflate(R.layout.module_gl_camera_layout, null);
        mPreviewTexture = mRootView.findViewById(R.id.texture_preview);
        Point mRealDisplaySize = new Point();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context
                .WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealSize(mRealDisplaySize);

        Logger.d(TAG, "mRealDisplaySize:" + mRealDisplaySize);
        mPreviewTexture.setMaxPreviewSize(mRealDisplaySize.x, mRealDisplaySize.y);
        if (recordSize != null)
            mPreviewTexture.presetRecordingSize(recordSize.getHeight(), recordSize.getWidth());
//        mPreviewTexture.presetRecordingSize(mRealDisplaySize.x, mRealDisplaySize.y);
        /*拍照大小。*/
//        mPreviewTexture.setPictureSize(2048, 2048, true);
        /*充满view*/
        mPreviewTexture.setFitFullView(true);

        mPreviewTexture.setSurfaceTextureListener(this);
        mProperties = properties;
    }

    public void presetRecordingSize(int width, int height) {
        mPreviewTexture.presetRecordingSize(width, height);
    }

    public void setFilterConfig(String config) {
        mPreviewTexture.setFilterWithConfig(config);
    }

    @Override
    public void switchCamera() {
        mPreviewTexture.onSwitchCamera();
    }

    @Override
    public View getRootView() {
        return mRootView;
    }

    public CameraRecordGLSurfaceView getGLSurfaceView() {
        return mPreviewTexture;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        uiEvent.onPreviewUiReady(surface, null);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        uiEvent.onPreviewUiDestroy();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // preview frame is ready when receive second frame
        if (frameCount == 2) {
            return;
        }
        frameCount++;
        if (frameCount == 2) {
            uiEvent.onAction(CameraUiEvent.ACTION_PREVIEW_READY, surface);
        }
    }

    public void startRecording(final CameraRecordGLSurfaceView.StartRecordingCallback recordingCallback) {
        mCurrentRecordFile = MediaFunc.getOutputMediaFile(MediaFunc.MEDIA_TYPE_VIDEO, "VIDEO",
                mProperties != null ? mProperties.getSavePath() : null);
        Vibrator vibrator = (Vibrator) mRootView.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(50);
        mPreviewTexture.startRecording(mCurrentRecordFile.getPath(), recordingCallback);
    }

    public void endRecording(final RecordEndListener listener) {
        mPreviewTexture.endRecording(listener == null ? null : new CameraRecordGLSurfaceView.EndRecordingCallback() {
            @Override
            public void endRecordingOK() {
                listener.onRecordEnd(mCurrentRecordFile.getPath(),
                        mPreviewTexture.getRecordWidth(), mPreviewTexture.getRecordHeight());
                Vibrator vibrator = (Vibrator) mRootView.getContext().getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(50);
            }
        });
    }

    public interface RecordEndListener {
        void onRecordEnd(String filePath, int width, int height);
    }
}
