package com.snailstudio2010.camera2.manager;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.OrientationEventListener;

import com.snailstudio2010.camera2.utils.FileSaver;
import com.snailstudio2010.camera2.utils.JobExecutor;

/**
 * Created by xuqiqiang on 9/12/17.
 */

public class CameraToolKit {

    private Context mContext;
//    private Handler mMainHandler;
    private MyOrientationListener mOrientationListener;
    private FileSaver mFileSaver;
    private int mRotation = 0;
    private JobExecutor mJobExecutor;

    private HandlerThread mBackgroundThread;
    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    public CameraToolKit(Context context) {
        mContext = context;
//        mMainHandler = new Handler(Looper.getMainLooper());
        mJobExecutor = new JobExecutor();
        mFileSaver = new FileSaver(mContext, mJobExecutor.getMainHandler());
        setOrientationListener();
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    public void startBackgroundThread() {
        if (mBackgroundThread == null) {
            mBackgroundThread = new HandlerThread("CameraBackground");
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    public void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void destroy() {
        if (mFileSaver != null) {
            mFileSaver.release();
            mFileSaver = null;
        }
        if (mOrientationListener != null) {
            mOrientationListener.disable();
            mOrientationListener = null;
        }
        if (mJobExecutor != null) {
            mJobExecutor.destroy();
//            mJobExecutor = null;
        }
        stopBackgroundThread();
    }

    public FileSaver getFileSaver() {
        return mFileSaver;
    }

    public int getOrientation() {
        return mRotation;
    }

    public Handler getMainHandler() {
        return mJobExecutor.getMainHandler();
    }

    public JobExecutor getExecutor() {
        return mJobExecutor;
    }

    public Handler getBackgroundHandler() {
        return mBackgroundHandler;
    }

    private void setOrientationListener() {
        mOrientationListener = new MyOrientationListener(mContext, SensorManager.SENSOR_DELAY_UI);
        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        } else {
            mOrientationListener.disable();
        }
    }

    private class MyOrientationListener extends OrientationEventListener {

        MyOrientationListener(Context context, int rate) {
            super(context, rate);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            mRotation = (orientation + 45) / 90 * 90;
        }
    }
}
