package com.snailstudio2010.camera2.callback;

import android.graphics.Bitmap;

public interface TakeShotListener {
    //You can recycle the bitmap.
    void onTakeShot(Bitmap bmp);
}