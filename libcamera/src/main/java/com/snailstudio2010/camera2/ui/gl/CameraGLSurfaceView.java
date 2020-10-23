package com.snailstudio2010.camera2.ui.gl;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.utils.Logger;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer {

    public static final String LOG_TAG = Config.getTag(CameraGLSurfaceView.class);
    public static int mMaxTextureSize = 0;
    protected int mViewWidth;
    protected int mViewHeight;
    protected OnCreateCallback mOnCreateCallback;

    protected int mRecordWidth = 720;
    protected int mRecordHeight = 1280;

    protected int mMaxPreviewWidth = 1280;
    protected int mMaxPreviewHeight = 1280;
    //
    protected Viewport mDrawViewport = new Viewport();
    protected boolean mFitFullView;

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        getHolder().setFormat(PixelFormat.RGBA_8888);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
//        setZOrderOnTop(true);
//        setZOrderMediaOverlay(true);
    }

    public Viewport getDrawViewport() {
        return mDrawViewport;
    }

    //The max preview size. Change it to 1920+ if you want to preview with 1080P
    public void setMaxPreviewSize(int w, int h) {
        mMaxPreviewWidth = w;
        mMaxPreviewHeight = h;
    }

    public void setFitFullView(boolean fit) {
        mFitFullView = fit;
        calcViewport();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
    }

    //定制一些初始化操作
    public void setOnCreateCallback(final OnCreateCallback callback) {
        mOnCreateCallback = callback;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Logger.d(LOG_TAG, "onSurfaceCreated...");

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_STENCIL_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int[] texSize = new int[1];

        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, texSize, 0);
        mMaxTextureSize = texSize[0] / 2;

        if (mOnCreateCallback != null) {
            mOnCreateCallback.createOver();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Logger.d(LOG_TAG, String.format("onSurfaceChanged: %d x %d", width, height));

        GLES20.glClearColor(0, 0, 0, 0);

        mViewWidth = width;
        mViewHeight = height;

        calcViewport();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //Nothing . See `CameraGLSurfaceViewWithTexture` or `CameraGLSurfaceViewWithBuffer`
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.d(LOG_TAG, "glsurfaceview onResume...");
    }

    @Override
    public void onPause() {
        Logger.d(LOG_TAG, "glsurfaceview onPause in...");
        super.onPause();
        Logger.d(LOG_TAG, "glsurfaceview onPause out...");
    }

    protected void onRelease() {
    }

    public final void release(final ReleaseOKCallback callback) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                onRelease();
                Logger.d(LOG_TAG, "GLSurfaceview release...");
                if (callback != null)
                    callback.releaseOK();
            }
        });
    }

    //注意， 录制的尺寸将影响preview的尺寸
    //这里的width和height表示竖屏尺寸
    //在onSurfaceCreated之前设置有效
    public void presetRecordingSize(int width, int height) {
        if (width > mMaxPreviewWidth || height > mMaxPreviewHeight) {
            float scaling = Math.min(mMaxPreviewWidth / (float) width, mMaxPreviewHeight / (float) height);
            width = (int) (width * scaling);
            height = (int) (height * scaling);
        }

        mRecordWidth = width;
        mRecordHeight = height;
    }

    protected void calcViewport() {
        Logger.d(LOG_TAG, "calcViewport mRecordWidth:" + mRecordWidth + ",mRecordHeight:" + mRecordHeight);
        Logger.d(LOG_TAG, "calcViewport mViewWidth:" + mViewWidth + ",mViewHeight:" + mViewHeight);
        if (mViewWidth == 0 || mViewHeight == 0) return;

        if (true) {
            mDrawViewport.width = mViewWidth;
            mDrawViewport.height = mViewHeight;
            mDrawViewport.x = 0;
            mDrawViewport.y = 0;
            return;
        }

        float scaling = mRecordWidth / (float) mRecordHeight;
        float viewRatio = mViewWidth / (float) mViewHeight;
        float s = scaling / viewRatio;

        int w, h;

        if (mFitFullView) {
            //撑满全部view(内容大于view)
            if (s > 1.0) {
                w = (int) (mViewHeight * scaling);
                h = mViewHeight;
            } else {
                w = mViewWidth;
                h = (int) (mViewWidth / scaling);
            }
        } else {
            //显示全部内容(内容小于view)
            if (s > 1.0) {
                w = mViewWidth;
                h = (int) (mViewWidth / scaling);
            } else {
                h = mViewHeight;
                w = (int) (mViewHeight * scaling);
            }
        }

        mDrawViewport.width = w;
        mDrawViewport.height = h;
        mDrawViewport.x = (mViewWidth - mDrawViewport.width) / 2;
        mDrawViewport.y = (mViewHeight - mDrawViewport.height) / 2;
        Logger.d(LOG_TAG, String.format("View port: %d, %d, %d, %d", mDrawViewport.x, mDrawViewport.y, mDrawViewport.width, mDrawViewport.height));
    }

    public int getRecordWidth() {
        return mRecordWidth;
    }

    public int getRecordHeight() {
        return mRecordHeight;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return false;
    }

    public interface OnCreateCallback {
        void createOver();
    }

    public interface ReleaseOKCallback {
        void releaseOK();
    }

    public static class Viewport {
        public int x, y, width, height;
    }
}
