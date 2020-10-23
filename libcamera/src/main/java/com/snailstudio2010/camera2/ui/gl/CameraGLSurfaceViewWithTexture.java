package com.snailstudio2010.camera2.ui.gl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.TextureView;

import com.snailstudio2010.camera2.callback.TakeShotListener;
import com.snailstudio2010.camera2.utils.Logger;

import org.wysaid.common.Common;
import org.wysaid.common.FrameBufferObject;
import org.wysaid.nativePort.CGEFrameRecorder;

import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraGLSurfaceViewWithTexture extends CameraGLSurfaceView implements SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "SurfaceViewTexture";
    protected SurfaceTexture mSurfaceTexture;
    protected int mTextureID;
    protected boolean mIsTransformMatrixSet = false;
    protected CGEFrameRecorder mFrameRecorder;
    protected float[] mTransformMatrix = new float[16];
    private boolean isRunning;

    private TextureView.SurfaceTextureListener mTextureListener;

    public CameraGLSurfaceViewWithTexture(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSurfaceTextureListener(TextureView.SurfaceTextureListener listener) {
        mTextureListener = listener;
    }

    public synchronized void setFilterWithConfig(final String config) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mFrameRecorder != null) {
                    mFrameRecorder.setFilterWidthConfig(config);
                } else {
                    Logger.e(LOG_TAG, "setFilterWithConfig after release!!");
                }
            }
        });
    }

    public void setFilterIntensity(final float intensity) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mFrameRecorder != null) {
                    mFrameRecorder.setFilterIntensity(intensity);
                } else {
                    Logger.e(LOG_TAG, "setFilterIntensity after release!!");
                }
            }
        });
    }

    //定制一些初始化操作
    public void setOnCreateCallback(final OnCreateCallback callback) {
        if (mFrameRecorder == null || callback == null) {
            mOnCreateCallback = callback;
        } else {
            // Already created, just run.
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    callback.createOver();
                }
            });
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Logger.d(TAG, "onSurfaceCreated");
        mFrameRecorder = new CGEFrameRecorder();
        mIsTransformMatrixSet = false;
        if (!mFrameRecorder.init(mRecordWidth, mRecordHeight, mRecordWidth, mRecordHeight)) {
            Logger.e(LOG_TAG, "Frame Recorder init error!");
        }

        mFrameRecorder.setSrcRotation((float) (Math.PI / 2.0));
        mFrameRecorder.setSrcFlipScale(1.0f, -1.0f);
        mFrameRecorder.setRenderFlipScale(1.0f, -1.0f);

        mTextureID = Common.genSurfaceTextureID();
        mSurfaceTexture = new SurfaceTexture(mTextureID);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        super.onSurfaceCreated(gl, config);
    }

    protected void onRelease() {
        super.onRelease();
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }

        if (mTextureID != 0) {
            Common.deleteTextureID(mTextureID);
            mTextureID = 0;
        }

        if (mFrameRecorder != null) {
            mFrameRecorder.release();
            mFrameRecorder = null;
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);
        Logger.d(TAG, "onSurfaceChanged");
        if (!isRunning) {
            isRunning = true;
            if (mTextureListener != null)
                mTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, width, height);
        }
        if (mTextureListener != null)
            mTextureListener.onSurfaceTextureSizeChanged(mSurfaceTexture, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        Logger.d(TAG, "surfaceDestroyed");
//        cameraInstance().stopCamera();
        isRunning = false;
        if (mTextureListener != null)
            mTextureListener.onSurfaceTextureDestroyed(mSurfaceTexture);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Logger.d(TAG, "onDrawFrame");
        if (mSurfaceTexture == null) return;
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mTransformMatrix);
//        if (mCamera2.getSensorOrientation() == 90) {
        android.opengl.Matrix.rotateM(mTransformMatrix, 0, 270, 0, 0, 1);
        android.opengl.Matrix.translateM(mTransformMatrix, 0, -1, 0, 0);
//        } else if (mCamera2.getSensorOrientation() == 180) {
//            android.opengl.Matrix.rotateM(mTransformMatrix, 0, 180, 0, 0, 1);
//            android.opengl.Matrix.translateM(mTransformMatrix, 0, -1, -1, 0);
//        } else if (mCamera2.getSensorOrientation() == 270) {
//            android.opengl.Matrix.rotateM(mTransformMatrix, 0, 90, 0, 0, 1);
//            android.opengl.Matrix.translateM(mTransformMatrix, 0, 0, -1, 0);
//        }
        mFrameRecorder.update(mTextureID, mTransformMatrix);

//        mSurfaceTexture.getTransformMatrix(mTransformMatrix);
//        if (mCamera2.getSensorOrientation() == 90) {
//            android.opengl.Matrix.rotateM(mTransformMatrix, 0, 270, 0, 0, 1);
//            android.opengl.Matrix.translateM(mTransformMatrix, 0, -1, 0, 0);
//        } else if (mCamera2.getSensorOrientation() == 180) {
//            android.opengl.Matrix.rotateM(mTransformMatrix, 0, 180, 0, 0, 1);
//            android.opengl.Matrix.translateM(mTransformMatrix, 0, -1, -1, 0);
//        } else if (mCamera2.getSensorOrientation() == 270) {
//            android.opengl.Matrix.rotateM(mTransformMatrix, 0, 90, 0, 0, 1);
//            android.opengl.Matrix.translateM(mTransformMatrix, 0, 0, -1, 0);
//        }
//        mFrameRecorder.update(mTextureID, mTransformMatrix);

        mFrameRecorder.runProc();

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        mFrameRecorder.render(mDrawViewport.x, mDrawViewport.y, mDrawViewport.width, mDrawViewport.height);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Logger.d(TAG, "onFrameAvailable");
        requestRender();
        if (mTextureListener != null)
            mTextureListener.onSurfaceTextureUpdated(mSurfaceTexture);
    }

    public void onSwitchCamera() {
        if (mFrameRecorder != null) {
            mFrameRecorder.setSrcRotation((float) (Math.PI / 2.0));
            mFrameRecorder.setRenderFlipScale(1.0f, -1.0f);
        }
    }

    public void takeShot(int width, int height, final TakeShotListener callback) {
        assert callback != null : "callback must not be null!";
        if (mFrameRecorder == null) {
            Logger.e(LOG_TAG, "Recorder not initialized!");
            callback.onTakeShot(null);
            return;
        }

        final int recordWidth = width <= 0 ? mRecordWidth : width;
        final int recordHeight = height <= 0 ? mRecordHeight : height;

        queueEvent(new Runnable() {
            @Override
            public void run() {

                FrameBufferObject frameBufferObject = new FrameBufferObject();
                int bufferTexID;
                IntBuffer buffer;
                bufferTexID = Common.genBlankTextureID(recordWidth, recordHeight);
                frameBufferObject.bindTexture(bufferTexID);
                GLES20.glViewport(0, 0, recordWidth, recordHeight);
                mFrameRecorder.drawCache();
                buffer = IntBuffer.allocate(recordWidth * recordHeight);
                GLES20.glReadPixels(0, 0, recordWidth, recordHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
                final Bitmap bmp = Bitmap.createBitmap(recordWidth, recordHeight, Bitmap.Config.ARGB_8888);
                bmp.copyPixelsFromBuffer(buffer);
                Logger.d(LOG_TAG, String.format("w: %d, h: %d", recordWidth, recordHeight));

                frameBufferObject.release();
                GLES20.glDeleteTextures(1, new int[]{bufferTexID}, 0);

                callback.onTakeShot(bmp);
            }
        });

    }
}
