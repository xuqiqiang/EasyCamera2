package com.snailstudio2010.camera2.ui.gl;

/**
 * Created by wangyang on 15/7/27.
 */


import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.TextureView;

import com.snailstudio2010.camera2.utils.Logger;

import org.wysaid.common.Common;
import org.wysaid.nativePort.CGEFrameRecorder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by wangyang on 15/7/17.
 */
public class CameraGLSurfaceViewWithTexture extends CameraGLSurfaceView implements SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "SurfaceViewTexture";
    protected SurfaceTexture mSurfaceTexture;
    protected int mTextureID;
    protected boolean mIsTransformMatrixSet = false;
    protected CGEFrameRecorder mFrameRecorder;
    protected float[] mTransformMatrix = new float[16];
    private boolean isRunning;
//    private Camera2Instance mCamera2;
//    private FocusView mFocusView;
//    private FocusOverlayManager mFocusManager;


//    private RequestCallback mRequestCallback = new RequestCallback() {
////        @Override
////        public void onDataBack(byte[] data, int width, int height) {
////            super.onDataBack(data, width, height);
////            saveFile(data, width, height, mDeviceMgr.getCameraId(),
////                    CameraSettings.KEY_PICTURE_FORMAT, "CAMERA");
////            mSession.applyRequest(Session.RQ_RESTART_PREVIEW);
////        }
//
//        @Override
//        public void onViewChange(final int width, final int height) {
//            super.onViewChange(width, height);
////            getBaseUI().updateUiSize(width, height);
//            new Handler(Looper.getMainLooper()).post(new Runnable() {
//                @Override
//                public void run() {
//                    mFocusView.initFocusArea(width, height);
//                    mFocusManager.onPreviewChanged(width, height, mCamera2.getCharacteristics());
//                }
//            });
//        }
//
//        @Override
//        public void onAFStateChanged(int state) {
//            super.onAFStateChanged(state);
//            mFocusManager.updateAFState(state);
////            updateAFState(state, mFocusManager);
//        }
//    };


//    private CameraUiEvent mCameraUiEvent = new CameraUiEvent() {
//
//        //        @Override
////        public void onPreviewUiReady(SurfaceTexture mainSurface, SurfaceTexture auxSurface) {
////            Logger.d(TAG, "onSurfaceTextureAvailable");
////            mSurfaceTexture = mainSurface;
////            enableState(Controller.CAMERA_STATE_UI_READY);
////            if (stateEnabled(Controller.CAMERA_STATE_OPENED)) {
////                mSession.applyRequest(Session.RQ_START_PREVIEW, mSurfaceTexture, mRequestCallback);
////            }
////        }
////
////        @Override
////        public void onPreviewUiDestroy() {
////            disableState(Controller.CAMERA_STATE_UI_READY);
////            Logger.d(TAG, "onSurfaceTextureDestroyed");
////        }
////
//        @Override
//        public void onTouchToFocus(float x, float y) {
//            // close all menu when touch to focus
////            mCameraMenu.close();
//            mFocusManager.startFocus(x, y);
//            MeteringRectangle focusRect = mFocusManager.getFocusArea(x, y, true);
//            MeteringRectangle meterRect = mFocusManager.getFocusArea(x, y, false);
////            mSession.applyRequest(Session.RQ_AF_AE_REGIONS, focusRect, meterRect);
//            mCamera2.sendControlAfAeRequest(focusRect, meterRect);
//        }
//
//        @Override
//        public void resetTouchToFocus() {
//            // todo
//            mCamera2.sendControlFocusModeRequest(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
////            if (stateEnabled(Controller.CAMERA_MODULE_RUNNING)) {
////                mSession.applyRequest(Session.RQ_FOCUS_MODE,
////                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
////            }
//        }
////
////        @Override
////        public <T> void onSettingChange(CaptureRequest.Key<T> key, T value) {
////            if (key == CaptureRequest.LENS_FOCUS_DISTANCE) {
////                mSession.applyRequest(Session.RQ_FOCUS_DISTANCE, value);
////            }
////        }
////
////        @Override
////        public <T> void onAction(String type, T value) {
////            // close all menu when ui click
////            mCameraMenu.close();
////            switch (type) {
////                case CameraUiEvent.ACTION_CLICK:
////                    handleClick((View) value);
////                    break;
////                case CameraUiEvent.ACTION_CHANGE_MODULE:
////                    setNewModule((Integer) value);
////                    break;
////                case CameraUiEvent.ACTION_SWITCH_CAMERA:
////                    break;
////                case CameraUiEvent.ACTION_PREVIEW_READY:
////                    getCoverView().hideCoverWithAnimation();
////                    break;
////                default:
////                    break;
////            }
////        }
//    };

    private TextureView.SurfaceTextureListener mTextureListener;

    public CameraGLSurfaceViewWithTexture(Context context, AttributeSet attrs) {
        super(context, attrs);
//        mFocusView = new FocusView(getContext());
//        mFocusView.setVisibility(View.GONE);
//        ((ViewGroup)getParent()).addView(mFocusView);
//
//        mFocusManager = new FocusOverlayManager(mFocusView, Looper.getMainLooper());
//        mFocusManager.setListener(mCameraUiEvent);
    }

    public void setSurfaceTextureListener(TextureView.SurfaceTextureListener listener) {
        mTextureListener = listener;
    }

//    public void initFocusOverlay(FocusView focusView) {
//        mFocusView = focusView;
//        mFocusView.setVisibility(View.GONE);
//        ((ViewGroup) getParent()).addView(mFocusView);
//
//        mFocusManager = new FocusOverlayManager(mFocusView, Looper.getMainLooper());
//        mFocusManager.setListener(mCameraUiEvent);
//    }
//
//    public CGEFrameRecorder getRecorder() {
//        return mFrameRecorder;
//    }

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

//        mCamera2 = new Camera2Instance((Activity) getContext());
//        mCamera2.setCallback(mRequestCallback);

        super.onSurfaceCreated(gl, config);
//        if (mTextureListener != null)
//            mTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, mViewWidth, mViewHeight);
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
//        if (!cameraInstance().isPreviewing()) {
//            resumePreview();
//        }
//        if (!mCamera2.isPreviewing()) {
//            resumePreview();
//        }
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

//    public void resumePreview() {
//
//        if (mFrameRecorder == null) {
//            Logger.e(LOG_TAG, "resumePreview after release!!");
//            return;
//        }
//
//        if (!mCamera2.isCameraOpened()) {
//            int facing = mIsCameraBackForward ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
//
//            mCamera2.tryOpenCamera(1080, 1920, new CameraInstance.CameraOpenCallback() {
//                @Override
//                public void cameraReady() {
//                    Logger.d(LOG_TAG, "tryOpenCamera OK...");
//
//                    if (!mCamera2.isPreviewing()) {
//                        mCamera2.startPreview(mSurfaceTexture);
//                        mFrameRecorder.srcResize(mCamera2.previewHeight(), mCamera2.previewWidth());
//                    }
//                }
//            }, facing);
//        }
//
//        mFocusView.initFocusArea(1080, 1920);
//        requestRender();
//    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Logger.d(TAG, "onDrawFrame");
        if (mSurfaceTexture == null) {// || !mCamera2.isPreviewing()) {
            return;
        }
//        if (mSurfaceTexture == null || !cameraInstance().isPreviewing()) {
//            return;
//        }

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

//    @Override
//    public void takeShot(final TakePictureCallback callback) {
//        assert callback != null : "callback must not be null!";
//
//        if (mFrameRecorder == null) {
//            Logger.e(LOG_TAG, "Recorder not initialized!");
//            callback.takePictureOK(null);
//            return;
//        }
//
//        queueEvent(new Runnable() {
//            @Override
//            public void run() {
//
//                FrameBufferObject frameBufferObject = new FrameBufferObject();
//                int bufferTexID;
//                IntBuffer buffer;
//                Bitmap bmp;
//
//                bufferTexID = Common.genBlankTextureID(mRecordWidth, mRecordHeight);
//                frameBufferObject.bindTexture(bufferTexID);
//                GLES20.glViewport(0, 0, mRecordWidth, mRecordHeight);
//                mFrameRecorder.drawCache();
//                buffer = IntBuffer.allocate(mRecordWidth * mRecordHeight);
//                GLES20.glReadPixels(0, 0, mRecordWidth, mRecordHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
//                bmp = Bitmap.createBitmap(mRecordWidth, mRecordHeight, Bitmap.Config.ARGB_8888);
//                bmp.copyPixelsFromBuffer(buffer);
//                Logger.d(LOG_TAG, String.format("w: %d, h: %d", mRecordWidth, mRecordHeight));
//
//                frameBufferObject.release();
//                GLES20.glDeleteTextures(1, new int[]{bufferTexID}, 0);
//
//                callback.takePictureOK(bmp);
//            }
//        });
//
//    }

//    //isBigger 为true 表示当宽高不满足时，取最近的较大值.
//    // 若为 false 则取较小的
//    public void setPictureSize(int width, int height, boolean isBigger) {
//        //默认会旋转90度.
//        cameraInstance().setPictureSize(height, width, isBigger);
//    }
//
//    public synchronized void takePicture(final TakePictureCallback photoCallback, Camera.ShutterCallback shutterCallback, final String config, final float intensity, final boolean isFrontMirror) {
//
//        if(true) {
//            mCamera2.takePicture(new Camera2Instance.PictureCallback() {
//                @Override
//                public void onPictureTaken(final byte[] data, int w, int h) {
//
////                    Camera.Parameters params = camera.getParameters();
////                    Camera.Size sz = params.getPictureSize();
//
//                    boolean shouldRotate;
//
//                    Bitmap bmp;
//                    int width, height;
//
//                    //当拍出相片不为正方形时， 可以判断图片是否旋转
//                    if (w != h) {
//                        //默认数据格式已经设置为 JPEG
//                        bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
//                        width = bmp.getWidth();
//                        height = bmp.getHeight();
//
//                        Logger.d(LOG_TAG, "_TEST2_ w:" + w + ", h:" + h);
//                        Logger.d(LOG_TAG, "_TEST2_ width:" + width + ", height:" + height);
//                        shouldRotate = (w > h && width > height) || (w < h && width < height);
//                        Logger.d(LOG_TAG, "_TEST2_ shouldRotate:" + shouldRotate);
//                    } else {
//                        Logger.d(LOG_TAG, "Cache image to get exif.");
//
//                        try {
//                            String tmpFilename = getContext().getExternalCacheDir() + "/picture_cache000.jpg";
//                            FileOutputStream fileout = new FileOutputStream(tmpFilename);
//                            BufferedOutputStream bufferOutStream = new BufferedOutputStream(fileout);
//                            bufferOutStream.write(data);
//                            bufferOutStream.flush();
//                            bufferOutStream.close();
//
//                            ExifInterface exifInterface = new ExifInterface(tmpFilename);
//                            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
//
//                            switch (orientation) {
//                                //被保存图片exif记录只有旋转90度， 和不旋转两种情况
//                                case ExifInterface.ORIENTATION_ROTATE_90:
//                                    shouldRotate = true;
//                                    break;
//                                default:
//                                    shouldRotate = false;
//                                    break;
//                            }
//
//                            bmp = BitmapFactory.decodeFile(tmpFilename);
//                            width = bmp.getWidth();
//                            height = bmp.getHeight();
//
//                        } catch (IOException e) {
//                            Logger.e(LOG_TAG, "Err when saving bitmap...");
//                            e.printStackTrace();
//                            return;
//                        }
//                    }
//
//
//                    if (width > mMaxTextureSize || height > mMaxTextureSize) {
//                        float scaling = Math.max(width / (float) mMaxTextureSize, height / (float) mMaxTextureSize);
//                        Logger.d(LOG_TAG, String.format("目标尺寸(%d x %d)超过当前设备OpenGL 能够处理的最大范围(%d x %d)， 现在将图片压缩至合理大小!", width, height, mMaxTextureSize, mMaxTextureSize));
//
//                        bmp = Bitmap.createScaledBitmap(bmp, (int) (width / scaling), (int) (height / scaling), false);
//
//                        width = bmp.getWidth();
//                        height = bmp.getHeight();
//                    }
//
//                    Bitmap bmp2 = bmp;
//
////                    if (shouldRotate) {
////                        bmp2 = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);
////
////                        Canvas canvas = new Canvas(bmp2);
////
////                        if (cameraInstance().getFacing() == Camera.CameraInfo.CAMERA_FACING_BACK) {
////                            Matrix mat = new Matrix();
////                            int halfLen = Math.min(width, height) / 2;
////                            mat.setRotate(90, halfLen, halfLen);
////                            canvas.drawBitmap(bmp, mat, null);
////                        } else {
////                            Matrix mat = new Matrix();
////
////                            if (isFrontMirror) {
////                                mat.postTranslate(-width / 2, -height / 2);
////                                mat.postScale(-1.0f, 1.0f);
////                                mat.postTranslate(width / 2, height / 2);
////                                int halfLen = Math.min(width, height) / 2;
////                                mat.postRotate(90, halfLen, halfLen);
////                            } else {
////                                int halfLen = Math.max(width, height) / 2;
////                                mat.postRotate(-90, halfLen, halfLen);
////                            }
////
////                            canvas.drawBitmap(bmp, mat, null);
////                        }
////
////                        bmp.recycle();
////                    } else {
////                        if (cameraInstance().getFacing() == Camera.CameraInfo.CAMERA_FACING_BACK) {
////                            bmp2 = bmp;
////                        } else {
////
////                            bmp2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
////                            Canvas canvas = new Canvas(bmp2);
////                            Matrix mat = new Matrix();
////                            if (isFrontMirror) {
////                                mat.postTranslate(-width / 2, -height / 2);
////                                mat.postScale(1.0f, -1.0f);
////                                mat.postTranslate(width / 2, height / 2);
////                            } else {
////                                mat.postTranslate(-width / 2, -height / 2);
////                                mat.postScale(-1.0f, -1.0f);
////                                mat.postTranslate(width / 2, height / 2);
////                            }
////
////                            canvas.drawBitmap(bmp, mat, null);
////                        }
////
////                    }
//
//                    if (config != null) {
//                        CGENativeLibrary.filterImage_MultipleEffectsWriteBack(bmp2, config, intensity);
//                    }
//
//                    photoCallback.takePictureOK(bmp2);
//
////                    cameraInstance().getCameraDevice().startPreview();
//                }
//            });
//            return;
//        }
//        Camera.Parameters params = cameraInstance().getParams();
//
//        if (photoCallback == null || params == null) {
//            Logger.e(LOG_TAG, "takePicture after release!");
//            if (photoCallback != null) {
//                photoCallback.takePictureOK(null);
//            }
//            return;
//        }
//
//        try {
//            params.setRotation(90);
//            cameraInstance().setParams(params);
//        } catch (Exception e) {
//            Logger.e(LOG_TAG, "Error when takePicture: " + e.toString());
//            if (photoCallback != null) {
//                photoCallback.takePictureOK(null);
//            }
//            return;
//        }
//
//        cameraInstance().getCameraDevice().takePicture(shutterCallback, null, new Camera.PictureCallback() {
//            @Override
//            public void onPictureTaken(final byte[] data, Camera camera) {
//
//                Camera.Parameters params = camera.getParameters();
//                Camera.Size sz = params.getPictureSize();
//
//                boolean shouldRotate;
//
//                Bitmap bmp;
//                int width, height;
//
//                //当拍出相片不为正方形时， 可以判断图片是否旋转
//                if (sz.width != sz.height) {
//                    //默认数据格式已经设置为 JPEG
//                    bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
//                    width = bmp.getWidth();
//                    height = bmp.getHeight();
//                    shouldRotate = (sz.width > sz.height && width > height) || (sz.width < sz.height && width < height);
//                } else {
//                    Logger.d(LOG_TAG, "Cache image to get exif.");
//
//                    try {
//                        String tmpFilename = getContext().getExternalCacheDir() + "/picture_cache000.jpg";
//                        FileOutputStream fileout = new FileOutputStream(tmpFilename);
//                        BufferedOutputStream bufferOutStream = new BufferedOutputStream(fileout);
//                        bufferOutStream.write(data);
//                        bufferOutStream.flush();
//                        bufferOutStream.close();
//
//                        ExifInterface exifInterface = new ExifInterface(tmpFilename);
//                        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
//
//                        switch (orientation) {
//                            //被保存图片exif记录只有旋转90度， 和不旋转两种情况
//                            case ExifInterface.ORIENTATION_ROTATE_90:
//                                shouldRotate = true;
//                                break;
//                            default:
//                                shouldRotate = false;
//                                break;
//                        }
//
//                        bmp = BitmapFactory.decodeFile(tmpFilename);
//                        width = bmp.getWidth();
//                        height = bmp.getHeight();
//
//                    } catch (IOException e) {
//                        Logger.e(LOG_TAG, "Err when saving bitmap...");
//                        e.printStackTrace();
//                        return;
//                    }
//                }
//
//
//                if (width > mMaxTextureSize || height > mMaxTextureSize) {
//                    float scaling = Math.max(width / (float) mMaxTextureSize, height / (float) mMaxTextureSize);
//                    Logger.d(LOG_TAG, String.format("目标尺寸(%d x %d)超过当前设备OpenGL 能够处理的最大范围(%d x %d)， 现在将图片压缩至合理大小!", width, height, mMaxTextureSize, mMaxTextureSize));
//
//                    bmp = Bitmap.createScaledBitmap(bmp, (int) (width / scaling), (int) (height / scaling), false);
//
//                    width = bmp.getWidth();
//                    height = bmp.getHeight();
//                }
//
//                Bitmap bmp2;
//
//                if (shouldRotate) {
//                    bmp2 = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);
//
//                    Canvas canvas = new Canvas(bmp2);
//
//                    if (cameraInstance().getFacing() == Camera.CameraInfo.CAMERA_FACING_BACK) {
//                        Matrix mat = new Matrix();
//                        int halfLen = Math.min(width, height) / 2;
//                        mat.setRotate(90, halfLen, halfLen);
//                        canvas.drawBitmap(bmp, mat, null);
//                    } else {
//                        Matrix mat = new Matrix();
//
//                        if (isFrontMirror) {
//                            mat.postTranslate(-width / 2, -height / 2);
//                            mat.postScale(-1.0f, 1.0f);
//                            mat.postTranslate(width / 2, height / 2);
//                            int halfLen = Math.min(width, height) / 2;
//                            mat.postRotate(90, halfLen, halfLen);
//                        } else {
//                            int halfLen = Math.max(width, height) / 2;
//                            mat.postRotate(-90, halfLen, halfLen);
//                        }
//
//                        canvas.drawBitmap(bmp, mat, null);
//                    }
//
//                    bmp.recycle();
//                } else {
//                    if (cameraInstance().getFacing() == Camera.CameraInfo.CAMERA_FACING_BACK) {
//                        bmp2 = bmp;
//                    } else {
//
//                        bmp2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//                        Canvas canvas = new Canvas(bmp2);
//                        Matrix mat = new Matrix();
//                        if (isFrontMirror) {
//                            mat.postTranslate(-width / 2, -height / 2);
//                            mat.postScale(1.0f, -1.0f);
//                            mat.postTranslate(width / 2, height / 2);
//                        } else {
//                            mat.postTranslate(-width / 2, -height / 2);
//                            mat.postScale(-1.0f, -1.0f);
//                            mat.postTranslate(width / 2, height / 2);
//                        }
//
//                        canvas.drawBitmap(bmp, mat, null);
//                    }
//
//                }
//
//                if (config != null) {
//                    CGENativeLibrary.filterImage_MultipleEffectsWriteBack(bmp2, config, intensity);
//                }
//
//                photoCallback.takePictureOK(bmp2);
//
//                cameraInstance().getCameraDevice().startPreview();
//            }
//        });
//    }
}
