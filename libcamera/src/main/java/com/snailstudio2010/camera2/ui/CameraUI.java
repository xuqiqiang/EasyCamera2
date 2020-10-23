package com.snailstudio2010.camera2.ui;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;

import com.snailstudio2010.camera2.callback.CameraUiEvent;
import com.snailstudio2010.camera2.utils.Logger;
import com.snailstudio2010.libcamera.R;

/**
 * Created by xuqiqiang on 16-3-18.
 */
public class CameraUI extends CameraBaseUI implements TextureView.SurfaceTextureListener {

    private final String TAG = this.getClass().getSimpleName();

    private View mRootView;
    private TextureView mPreviewTexture;

    public CameraUI(Context context, CameraUiEvent event) {
        super(event);
        mRootView = LayoutInflater.from(context)
                .inflate(R.layout.module_camera_layout, null);
        mPreviewTexture = mRootView.findViewById(R.id.texture_preview);
        mPreviewTexture.setSurfaceTextureListener(this);
    }

    public TextureView getTextureView() {
        return mPreviewTexture;
    }

    @Override
    public View getRootView() {
        return mRootView;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        uiEvent.onPreviewUiReady(surface, null);
        Logger.d(TAG, "onSurfaceTextureAvailable");
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Logger.d(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Logger.d(TAG, "onSurfaceTextureDestroyed");
        uiEvent.onPreviewUiDestroy();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // preview frame is ready when receive second frame
        Logger.d(TAG, "onSurfaceTextureUpdated:" + frameCount);
        if (frameCount == 2) {
            return;
        }
        frameCount++;
        if (frameCount == 2) {
            uiEvent.onAction(CameraUiEvent.ACTION_PREVIEW_READY, surface);
        }
    }
}
