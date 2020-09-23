package com.snailstudio2010.camera2.manager;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.view.Surface;

import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.utils.Logger;

import java.nio.ByteBuffer;

public abstract class Camera2Session extends Session {

    private final String TAG = Config.getTag(Camera2Session.class);

    Handler mMainHandler;
    Handler mBackgroundHandler;
    RequestManager mRequestMgr;
    CameraCharacteristics characteristics;
    CameraDevice cameraDevice;
    CameraCaptureSession cameraSession;

    Camera2Session(Context context, Handler mainHandler, Handler backgroundThread, CameraSettings settings) {
        super(context, settings);
        mMainHandler = mainHandler;
        mBackgroundHandler = backgroundThread;
//        mRequestMgr = new RequestManager();
        mRequestMgr = new RequestManager();
    }

//    Camera2Session(Context context, CameraSettings settings) {
//        super(context, settings);
//    }

    void initCharacteristics() {
        CameraManager manager = (CameraManager) appContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            assert manager != null;
            characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
        } catch (CameraAccessException e) {
            Logger.e(TAG, "getCameraCharacteristics error", e);
        }
    }

    byte[] getByteFromReader(ImageReader reader) {
        Image image = reader.acquireLatestImage();
        int totalSize = 0;
        for (Image.Plane plane : image.getPlanes()) {
            totalSize += plane.getBuffer().remaining();
        }
        ByteBuffer totalBuffer = ByteBuffer.allocate(totalSize);
        for (Image.Plane plane : image.getPlanes()) {
            totalBuffer.put(plane.getBuffer());
        }
        image.close();
        return totalBuffer.array();
    }

    CaptureRequest.Builder createBuilder(int type, Surface surface) {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(type);
            builder.addTarget(surface);
            return builder;
        } catch (CameraAccessException | IllegalStateException e) {
            e.printStackTrace();
        }
        return null;
    }

    void sendRepeatingRequest(CaptureRequest request,
                              CameraCaptureSession.CaptureCallback callback, Handler handler) {
        try {
            if (cameraSession != null)
                cameraSession.setRepeatingRequest(request, callback, handler);
        } catch (CameraAccessException | IllegalStateException e) {
            Logger.e(TAG, "send repeating request error", e);
        }
    }

    void sendCaptureRequest(CaptureRequest request,
                            CameraCaptureSession.CaptureCallback callback, Handler handler) {
        try {
            if (cameraSession != null)
                cameraSession.capture(request, callback, handler);
        } catch (CameraAccessException | IllegalStateException e) {
            Logger.e(TAG, "send capture request error", e);
        }
    }

    void sendCaptureRequestWithStop(CaptureRequest request,
                                    CameraCaptureSession.CaptureCallback callback, Handler handler) {
        if (cameraSession != null) {
            try {
                cameraSession.stopRepeating();
                cameraSession.abortCaptures();
                cameraSession.capture(request, callback, handler);
            } catch (CameraAccessException | IllegalStateException e) {
                Logger.e(TAG, "send capture request error", e);
            }
        }
    }
}
