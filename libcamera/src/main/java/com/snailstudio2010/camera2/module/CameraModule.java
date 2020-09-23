package com.snailstudio2010.camera2.module;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CaptureResult;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;

import com.snailstudio2010.camera2.CameraView;
import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.Properties;
import com.snailstudio2010.camera2.manager.CameraSettings;
import com.snailstudio2010.camera2.manager.CameraToolKit;
import com.snailstudio2010.camera2.manager.Controller;
import com.snailstudio2010.camera2.manager.DeviceManager;
import com.snailstudio2010.camera2.manager.FocusOverlayManager;
import com.snailstudio2010.camera2.ui.ICoverView;
import com.snailstudio2010.camera2.ui.IFocusView;
import com.snailstudio2010.camera2.utils.FileSaver;
import com.snailstudio2010.camera2.utils.JobExecutor;
import com.snailstudio2010.camera2.utils.Logger;
import com.snailstudio2010.camera2.utils.MediaFunc;

/**
 * Created by xuqiqiang on 16-3-9.
 */
public abstract class CameraModule {

    private static final String TAG = Config.getTag(CameraModule.class);
    protected Properties mProperties;
    Handler mainHandler;
    FileSaver fileSaver;
    ViewGroup rootView;
    Context appContext;
    private int mCameraState = Controller.CAMERA_MODULE_STOP;
    private Controller mController;
    private CameraSettings mCameraSettings;

    public CameraModule() {
    }

    public CameraModule(Properties properties) {
        this.mProperties = properties;
    }

    public void init(Context context, Controller controller) {
        // just need init once
        if (mController != null) {
            return;
        }
        appContext = context;
        mController = controller;
        mainHandler = getToolKit().getMainHandler();
        fileSaver = getToolKit().getFileSaver();
        rootView = controller.getBaseUI().getRootView();
        mCameraSettings = new CameraSettings(context, mProperties);
        // call subclass init()
        init();
    }

    boolean isAndTrue(int param1, int param2) {
        return (param1 & param2) != 0;
    }

    void enableState(int state) {
        mCameraState = mCameraState | state;
    }

    void disableState(int state) {
        mCameraState = mCameraState & (~state);
    }

    boolean stateEnabled(int state) {
        return isAndTrue(mCameraState, state);
    }

    public void startModule() {
        if (isAndTrue(mCameraState, Controller.CAMERA_MODULE_STOP)) {
            disableState(Controller.CAMERA_MODULE_STOP);
            enableState(Controller.CAMERA_MODULE_RUNNING);
            getToolKit().startBackgroundThread();
            start();
        }
    }

    public void stopModule() {
        if (isAndTrue(mCameraState, Controller.CAMERA_MODULE_RUNNING)) {
            disableState(Controller.CAMERA_MODULE_RUNNING);
            enableState(Controller.CAMERA_MODULE_STOP);
            stop();
//            getToolKit().stopBackgroundThread();
        }
    }

    protected abstract void init();

    protected abstract void start();

    protected abstract void stop();

    void addModuleView(View view) {
        View child = rootView.getChildAt(0);
        if (child != view) {
//            if (rootView.getChildCount() > 1) {
//                rootView.removeViewAt(0);
//            }
            if (child != null && !(child instanceof IFocusView)) {
                rootView.removeViewAt(0);
            }
//            rootView.removeAllViews();
            rootView.addView(view, 0);
        }
    }

    void saveFile(final byte[] data, final int width, final int height, final String cameraId,
                  final String formatKey, final String tag) {
        getExecutor().execute(new JobExecutor.Task<Void>() {
            @Override
            public Void run() {
                int format = getSettings().getPicFormat(cameraId, formatKey);
                int saveType = MediaFunc.MEDIA_TYPE_IMAGE;
                if (format != ImageFormat.JPEG) {
                    saveType = MediaFunc.MEDIA_TYPE_YUV;
                }
                fileSaver.saveFile(width, height, getToolKit().getOrientation(), data, tag, saveType,
                        mProperties != null ? mProperties.getSavePath() : null);
                return super.run();
            }
        });
    }

//    void setNewModule(int index) {
//        mController.changeModule(index);
//        getBaseUI().getIndicatorView().select(index);
//    }

    CameraToolKit getToolKit() {
        return mController.getToolKit();
    }

    ICoverView getCoverView() {
        return mController.getBaseUI().getCoverView();
    }

    public abstract DeviceManager getDeviceManager();

    public CameraSettings getSettings() {
        return mCameraSettings;
//        return CameraSettings.getInstance(appContext);//mController.getCameraSettings(appContext);
    }

    JobExecutor getExecutor() {
        return getToolKit().getExecutor();
    }

    CameraView getBaseUI() {
        return mController.getBaseUI();
    }

    protected void runOnUiThread(Runnable runnable) {
        getToolKit().getMainHandler().post(runnable);
    }

    protected void runOnUiThreadDelay(Runnable runnable, long delay) {
        getToolKit().getMainHandler().postDelayed(runnable, delay);
    }

//    void showSetting() {
//        mController.showSetting();
//    }

    void updateAFState(int state, FocusOverlayManager overlayManager) {
        Logger.d("IFocusView", "updateAFState:" + state);
        switch (state) {
            case CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN:
                overlayManager.startFocus();
                break;
            case CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED:
                overlayManager.focusSuccess();
                break;
            case CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED:
                overlayManager.focusError();
                break;
            case CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED:
                overlayManager.focusSuccess();
                break;
            case CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN:
                overlayManager.autoFocus();
                break;
            case CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED:
                overlayManager.focusError();
                break;
            case CaptureResult.CONTROL_AF_STATE_INACTIVE:
                overlayManager.hideFocusUI();
                break;
        }
    }
}
