package com.snailstudio2010.camera2.callback;

import android.graphics.SurfaceTexture;

import com.snailstudio2010.camera2.module.CameraModule;

public interface CameraUIListener {
    void onUIReady(CameraModule module, SurfaceTexture mainSurface, SurfaceTexture auxSurface);

    void onUIDestroy(CameraModule module);
}