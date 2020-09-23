package com.snailstudio2010.camera2.callback;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;

public interface PictureListener extends Camera.ShutterCallback {
    void onComplete(Uri uri, String path, Bitmap thumbnail);

    void onError(String msg);

    void onVideoStart();

    void onVideoStop();
}