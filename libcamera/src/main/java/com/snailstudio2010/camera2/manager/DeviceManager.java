package com.snailstudio2010.camera2.manager;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;

import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.Properties;

/**
 * Use for get basic camera info, not for open camera
 */
public class DeviceManager {
    private final String TAG = Config.getTag(DeviceManager.class);
    CameraManager cameraManager;
    Properties mProperties;

    public DeviceManager(Context context, Properties properties) {
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mProperties = properties;
    }

    public CameraCharacteristics getCharacteristics(String cameraId) {
        try {
            return cameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getDefaultCameraId() {
        if (mProperties != null && mProperties.isUseCameraV1()) {
            int numberOfCameras = Camera.getNumberOfCameras();
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    return i + "";
                }
            }
            return "0";
        }
        return getCameraIdList()[0];
    }

    public String[] getCameraIdList() {
        try {
            return cameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public StreamConfigurationMap getConfigMap(String cameraId) {
        try {
            CameraCharacteristics c = cameraManager.getCameraCharacteristics(cameraId);
            return c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * For camera open/close event
     */
    public static abstract class CameraEvent {
        public void onDeviceOpened(Object device) {
            // default empty implementation
        }

//        public void onDeviceOpened(CameraDevice device) {
//            // default empty implementation
//        }

        public void onAuxDeviceOpened(CameraDevice device) {
            // default empty implementation
        }

        public void onDeviceClosed() {
            // default empty implementation
        }

//        public void onDeviceOpened(Camera device) {
//            // default empty implementation
//        }
    }
}
