package com.snailstudio2010.camera2.ui;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;

import com.snailstudio2010.camera2.callback.CameraUiEvent;
import com.snailstudio2010.camera2.ui.gl.CameraGLSurfaceViewWithTexture;
import com.snailstudio2010.camera2.utils.Logger;
import com.snailstudio2010.libcamera.R;

/**
 * Created by xuqiqiang on 16-3-18.
 */
public class GLPhotoUI extends GLCameraUI implements TextureView.SurfaceTextureListener {

    private final String TAG = this.getClass().getSimpleName();

    private View mRootView;
    private CameraGLSurfaceViewWithTexture mPreviewTexture;

    public GLPhotoUI(Context context, Handler handler, CameraUiEvent event) {
        super(event);
        mRootView = LayoutInflater.from(context)
                .inflate(R.layout.module_photo_layout_gl, null);
        mPreviewTexture = mRootView.findViewById(R.id.texture_preview);


        Point mRealDisplaySize = new Point();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context
                .WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealSize(mRealDisplaySize);

        Logger.d(TAG, "mRealDisplaySize:" + mRealDisplaySize);
//        DisplayMetrics dm = new DisplayMetrics();
//        context.getWindowManager().getDefaultDisplay().getMetrics(dm);
        mPreviewTexture.setMaxPreviewSize(mRealDisplaySize.x, mRealDisplaySize.y);
        mPreviewTexture.presetRecordingSize(mRealDisplaySize.x, mRealDisplaySize.y);
        /*拍照大小。*/
//        mPreviewTexture.setPictureSize(2048, 2048, true);
        /*充满view*/
        mPreviewTexture.setFitFullView(true);

        mPreviewTexture.setSurfaceTextureListener(this);
//        mPreviewTexture.setGestureListener(this);
    }

//    @Override
//    public void setUIClickable(boolean clickable) {
//        super.setUIClickable(clickable);
//        mPreviewTexture.setClickable(clickable);
//    }

    @Override
    public void setFilterConfig(String config) {
        Logger.d(TAG, "setFilterConfig:" + config);
        mPreviewTexture.setFilterWithConfig(config);
    }

    @Override
    public View getRootView() {
        return mRootView;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Logger.d(TAG, "onSurfaceTextureAvailable:" + (surface != null));
        uiEvent.onPreviewUiReady(surface, null);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Logger.d(TAG, "onSurfaceTextureSizeChanged");
//        mPreviewTexture.requestRender();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Logger.d(TAG, "onSurfaceTextureDestroyed");
        uiEvent.onPreviewUiDestroy();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Logger.d(TAG, "onSurfaceTextureUpdated:" + frameCount);
        // preview frame is ready when receive second frame
        if (frameCount == 2) {
            return;
        }
        frameCount++;
        if (frameCount == 2) {
            uiEvent.onAction(CameraUiEvent.ACTION_PREVIEW_READY, null);
        }
    }

}
