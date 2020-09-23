package com.snailstudio2010.camera2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.snailstudio2010.camera2.callback.CameraListener;
import com.snailstudio2010.camera2.manager.CameraToolKit;
import com.snailstudio2010.camera2.manager.Controller;
import com.snailstudio2010.camera2.module.CameraModule;
import com.snailstudio2010.camera2.ui.ICoverView;
import com.snailstudio2010.camera2.ui.IFocusView;

import java.util.HashSet;
import java.util.Set;

public class CameraView extends FrameLayout implements CameraListener {
    private static final String TAG = Config.getTag(CameraView.class);

    static Context mAppContext;

    private ICoverView mCoverView;
    private IFocusView mFocusView;

    private CameraToolKit mToolKit;
    private CameraModule mCameraModule;

    private Set<CameraListener> mListeners = new HashSet<>();
    private Controller mController = new Controller() {

        @Override
        public CameraToolKit getToolKit() {
            return mToolKit;
        }

        @Override
        public CameraView getBaseUI() {
            return CameraView.this;
        }
    };

    public CameraView(Context context) {
        this(context, null, 0);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mAppContext = context.getApplicationContext();
        mToolKit = new CameraToolKit(mAppContext);
        mToolKit.startBackgroundThread();
    }

    public ViewGroup getRootView() {
        return this;
    }

    public ICoverView getCoverView() {
        return mCoverView;
    }

    public void setCoverView(ICoverView coverView) {
        this.mCoverView = coverView;
    }

    public IFocusView getFocusView() {
        return mFocusView;
    }

    public void setFocusView(IFocusView focusView) {
        if (!(focusView instanceof View))
            throw new IllegalArgumentException("FocusView must be view.");
        if (this.mFocusView != null) {
            this.removeView((View) this.mFocusView);
        }
        this.mFocusView = focusView;
        ((View) mFocusView).setVisibility(View.GONE);
        this.addView((View) mFocusView);
    }

    public void setCameraModule(CameraModule cameraModule) {
        if (mCameraModule != null) {
            mCameraModule.stopModule();
        }

        mCameraModule = cameraModule;
        mCameraModule.init(mAppContext, mController);
        mCameraModule.startModule();
    }

    public void onResume() {
        if (mCameraModule != null) {
            mCameraModule.startModule();
        }
    }

    public void onPause() {
        if (mCameraModule != null) {
            mCameraModule.stopModule();
        }
    }

    /**
     * Adjust layout when based on preview width
     *
     * @param width  preview screen width
     * @param height preview screen height
     */
    @Override
    public boolean updateUiSize(int width, int height) {
        if (mFocusView != null)
            mFocusView.initFocusArea(width, height);

        for (CameraListener listener : mListeners) {
            if (listener.updateUiSize(width, height)) return true;
        }
        setLayoutParams(new FrameLayout.LayoutParams(width, height));
        return true;
    }

    @Override
    public void onZoomChanged(float currentZoom, float maxZoom) {
        for (CameraListener listener : mListeners) {
            listener.onZoomChanged(currentZoom, maxZoom);
        }
    }

    public void addListener(CameraListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(CameraListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public void setUIClickable(boolean clickable) {
        for (CameraListener listener : mListeners) {
            listener.setUIClickable(clickable);
        }
    }

    @Override
    public void closeMenu() {
        for (CameraListener listener : mListeners) {
            listener.closeMenu();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        destroy();
    }

    public void destroy() {
        mToolKit.destroy();
    }
}
