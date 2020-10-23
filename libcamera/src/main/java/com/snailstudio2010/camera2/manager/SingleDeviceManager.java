package com.snailstudio2010.camera2.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;

import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.Properties;
import com.snailstudio2010.camera2.utils.JobExecutor;
import com.snailstudio2010.camera2.utils.Logger;
import com.snailstudio2010.camera2.utils.NonNull;

public class SingleDeviceManager extends DeviceManager {
    private final String TAG = Config.getTag(SingleDeviceManager.class);

    private Camera mCamera;
    private CameraDevice mDevice;
    private JobExecutor mJobExecutor;
    private String mCameraId = Config.MAIN_ID;
    private CameraEvent mCameraEvent;
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Logger.d(TAG, "device opened :" + camera.getId());
            mDevice = camera;
            mCameraEvent.onDeviceOpened(camera);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Logger.e(TAG, "onDisconnected");
            camera.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Logger.e(TAG, "error occur when open camera :" + camera.getId() + " error code:" + error);
            camera.close();
        }
    };

    public SingleDeviceManager(Context context, Properties properties, JobExecutor executor, CameraEvent event) {
        super(context, properties);
        mJobExecutor = executor;
        mCameraEvent = event;
    }

    public String getCameraId() {
        return mCameraId;
    }

    public void setCameraId(@NonNull String id) {
        mCameraId = id;
    }

    public CameraDevice getCameraDevice() {
        return mDevice;
    }

    public CameraCharacteristics getCharacteristics() {
        try {
            return cameraManager.getCameraCharacteristics(mCameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public StreamConfigurationMap getConfigMap() {
        try {
            CameraCharacteristics c = cameraManager.getCameraCharacteristics(mCameraId);
            return c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Camera.Parameters getParameters() {
        if (mCamera == null) return null;
        return mCamera.getParameters();
    }

    public void openCamera(final Handler mainHandler) {
//        if (mProperties != null && mProperties.isUseCameraV1()) {
//            Logger.d(TAG, "switchCamera openCamera:" + mCameraId);
//            this.mCamera = Camera.open(Integer.parseInt(mCameraId));
//            mCamera.setDisplayOrientation(90);
//            mCameraEvent.onDeviceOpened(mCamera);
//        } else {
//            mJobExecutor.execute(new JobExecutor.Task<Void>() {
//                @Override
//                public Void run() {
//                    openDevice(mainHandler);
//                    return super.run();
//                }
//            });
//        }
        mJobExecutor.execute(new JobExecutor.Task<Void>() {
            @Override
            public Void run() {
                if (mProperties != null && mProperties.isUseCameraV1()) {
                    Logger.d(TAG, "switchCamera openCamera:" + mCameraId);
                    mCamera = Camera.open(Integer.parseInt(mCameraId));
                    mCamera.setDisplayOrientation(90);
                    mCameraEvent.onDeviceOpened(mCamera);
                } else {
                    openDevice(mainHandler);
                }

//                openDevice(mainHandler);
                return super.run();
            }
        });
    }

    public boolean isFrontCamera() {
        if (mProperties != null && mProperties.isUseCameraV1()) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(Integer.parseInt(mCameraId), cameraInfo);
            return cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            CameraCharacteristics c = getCharacteristics(mCameraId);
            if (c == null) return false;
            Integer face = c.get(CameraCharacteristics.LENS_FACING);
            return face != null && CameraCharacteristics.LENS_FACING_FRONT == face;
        }
    }

    public void releaseCamera() {
        mJobExecutor.executeMust(new JobExecutor.Task<Void>() {
            @Override
            public Void run() {
                closeDevice();
                return super.run();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private synchronized void openDevice(Handler handler) {
        // no need to check permission, because we check permission in onStart() every time
        try {
            cameraManager.openCamera(mCameraId, stateCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private synchronized void closeDevice() {
        if (mProperties != null && mProperties.isUseCameraV1()) {
            if (mCamera != null) {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();// 停掉原来摄像头的预览
                mCamera.release();
                mCamera = null;
            }
        } else {
            if (mDevice != null) {
                mDevice.close();
                mDevice = null;
            }
        }
        mCameraEvent.onDeviceClosed();
    }
}
