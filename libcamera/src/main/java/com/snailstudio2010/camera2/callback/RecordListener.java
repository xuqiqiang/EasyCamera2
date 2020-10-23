package com.snailstudio2010.camera2.callback;

public interface RecordListener extends PictureListener {
    void onVideoStart();

    void onVideoStop();
}