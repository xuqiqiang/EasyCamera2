package com.xuqiqiang.camera2.demo;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.snailstudio2010.camera2.CameraView;
import com.snailstudio2010.camera2.callback.PictureListener;
import com.snailstudio2010.camera2.module.PhotoModule;
import com.xuqiqiang.camera2.demo.utils.Permission;

/**
 * Created by xuqiqiang on 2020/07/12.
 */
public class DemoActivity extends BaseActivity {

    private CameraView mCameraView;
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

        View shutter = findViewById(R.id.btn_shutter);
        shutter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPhotoModule.takePicture(new PictureListener() {
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
                });
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
