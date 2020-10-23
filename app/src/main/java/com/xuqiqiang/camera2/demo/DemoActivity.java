package com.xuqiqiang.camera2.demo;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.snailstudio2010.camera2.CameraView;
import com.snailstudio2010.camera2.Properties;
import com.snailstudio2010.camera2.callback.PictureListener;
import com.snailstudio2010.camera2.module.CameraModule;
import com.snailstudio2010.camera2.module.PhotoModule;
import com.snailstudio2010.camera2.module.SingleCameraModule;
import com.snailstudio2010.camera2.module.VideoModule;
import com.snailstudio2010.camera2.widget.CoverBlurView;
import com.xuqiqiang.camera2.demo.ui.ShutterButton;
import com.xuqiqiang.camera2.demo.utils.Permission;

/**
 * Created by xuqiqiang on 2020/07/12.
 */
public class DemoActivity extends BaseActivity {

    private CameraView mCameraView;
    private ShutterButton mShutter;
    private PhotoModule mPhotoModule;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        mCameraView = findViewById(R.id.camera_view);
        mCameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mPhotoModule.onTouchToFocus(event.getX(), event.getY());
                }
                return true;
            }
        });

        mShutter = findViewById(R.id.btn_shutter);
        mShutter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PictureListener listener = new PictureListener() {
                    @Override
                    public void onShutter() {
                    }

                    @Override
                    public void onComplete(Uri uri, String path, Bitmap thumbnail) {
                        Toast.makeText(DemoActivity.this, path, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String msg) {
                        Toast.makeText(DemoActivity.this, msg, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onVideoStart() {
                        mShutter.setMode(ShutterButton.VIDEO_RECORDING_MODE);
                    }

                    @Override
                    public void onVideoStop() {
                        mShutter.setMode(ShutterButton.VIDEO_MODE);
                    }
                };
                mPhotoModule.takePicture(listener);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Permission.checkPermission(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Permission.isPermissionGranted(this) && mPhotoModule == null) {
            mPhotoModule = new PhotoModule();
            mCameraView.setCameraModule(mPhotoModule);
        }
        if (mCameraView != null)
            mCameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraView != null)
            mCameraView.onPause();
    }
}
