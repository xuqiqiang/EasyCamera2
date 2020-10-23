package com.snailstudio2010.camera2.manager;

import android.graphics.ImageFormat;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Size;
import android.view.Surface;

import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.module.SingleCameraModule;
import com.snailstudio2010.camera2.utils.ImageUtils;
import com.snailstudio2010.camera2.utils.Logger;

import java.util.concurrent.locks.ReentrantLock;

public class OnImageAvailableListenerImpl implements ImageReader.OnImageAvailableListener {

    private static final String TAG = Config.getTag(Camera2PhotoSession.class);
    // 处理的间隔帧
//    private static final int PROCESS_INTERVAL = 30;
    private SingleCameraModule.PreviewCallback mCameraPreviewCallback;
    private Size previewSize;
    private byte[] y;
    private byte[] u;
    private byte[] v;
    private ReentrantLock lock = new ReentrantLock();
    // 当前获取的帧数
//    private int currentIndex = 0;
    // 图像帧数据，全局变量避免反复创建，降低gc频率
    private byte[] nv21;

    private Handler mBackgroundHandler;
    //    private CaptureRequest.Builder mPreviewBuilder;
    private ImageReader mPreviewReader;
    private Surface mPreviewReaderSurface;

    public OnImageAvailableListenerImpl(Handler backgroundHandler) {
//        this.previewSize = previewSize;
//        this.mPreviewBuilder = previewBuilder;
        this.mBackgroundHandler = backgroundHandler;
    }

    public Surface handleImage(Size previewSize, CaptureRequest.Builder previewBuilder,
                               SingleCameraModule.PreviewCallback callback) {
        this.previewSize = previewSize;
//        this.mPreviewBuilder = previewBuilder;
        this.mCameraPreviewCallback = callback;
        if (mPreviewReaderSurface != null) {
            previewBuilder.removeTarget(mPreviewReaderSurface);
            mPreviewReaderSurface.release();
            mPreviewReaderSurface = null;
        }

        if (mPreviewReader != null) {
            mPreviewReader.close();
            mPreviewReader = null;
        }

        if (callback == null) return null;

        mPreviewReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(),
                ImageFormat.YUV_420_888, 2);
        mPreviewReader.setOnImageAvailableListener(
                this, mBackgroundHandler);

//            mPreviewReader = ImageReader.newInstance(pictureSize.getWidth(),
//                    pictureSize.getHeight(), format, 1);
//            mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
//                @Override
//                public void onImageAvailable(ImageReader reader) {
//                    mCallback.onDataBack(getByteFromReader(reader),
//                            reader.getWidth(), reader.getHeight());
//                }
//            }, mMainHandler);
        mPreviewReaderSurface = mPreviewReader.getSurface();
        Logger.d(TAG, "_test2_ 0");
//        list.add(mPreviewReaderSurface);
        previewBuilder.addTarget(mPreviewReaderSurface);
        return mPreviewReaderSurface;
    }

//    public OnImageAvailableListenerImpl(Size previewSize) {
//        this.previewSize = previewSize;
//    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        Logger.d(TAG, "_test2_ 1");
        try {
            handleImageReader(reader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleImageReader(ImageReader reader) {
        Image image = reader.acquireNextImage();
        // Y:U:V == 4:2:2
        Logger.d(TAG, "_test2_ 2:" + image.getFormat());
        if (image.getFormat() == ImageFormat.YUV_420_888) {
            Image.Plane[] planes = image.getPlanes();
            // 加锁确保y、u、v来源于同一个Image
            lock.lock();
            // 重复使用同一批byte数组，减少gc频率
            if (y == null) {
                y = new byte[planes[0].getBuffer().limit() - planes[0].getBuffer().position()];
                u = new byte[planes[1].getBuffer().limit() - planes[1].getBuffer().position()];
                v = new byte[planes[2].getBuffer().limit() - planes[2].getBuffer().position()];
            }
            if (image.getPlanes()[0].getBuffer().remaining() == y.length) {
                Logger.d(TAG, "_test2_ 3");
                planes[0].getBuffer().get(y);
                planes[1].getBuffer().get(u);
                planes[2].getBuffer().get(v);
//                    camera2Listener.onPreview(y, u, v, mPreviewSize, planes[0].getRowStride());
                handleYUVImage(planes[0].getRowStride());
            }
            lock.unlock();
        }
        image.close();
    }

    private void handleYUVImage(final int stride) {
        int size = stride * previewSize.getHeight() * 3 / 2;
        if (nv21 == null || nv21.length < size) {
            nv21 = new byte[size];
        }
//                    Logger.d(TAG, "test:" + y.length + "," + (stride * previewSize.getHeight() * 3 / 2));
        // 回传数据是YUV422
        if (y.length / u.length == 2) {
            ImageUtils.yuv422ToYuv420sp(y, u, v, nv21, stride, previewSize.getHeight());
        }
        // 回传数据是YUV420
        else if (y.length / u.length == 4) {
            ImageUtils.yuv420ToYuv420sp(y, u, v, nv21, stride, previewSize.getHeight());
        }
        if (mCameraPreviewCallback != null)
            mCameraPreviewCallback.onPreviewFrame(nv21, previewSize);
    }
}