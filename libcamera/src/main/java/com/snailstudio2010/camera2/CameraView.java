package com.snailstudio2010.camera2;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.snailstudio2010.camera2.callback.CameraDelegate;
import com.snailstudio2010.camera2.callback.CameraUIListener;
import com.snailstudio2010.camera2.manager.CameraToolKit;
import com.snailstudio2010.camera2.manager.Controller;
import com.snailstudio2010.camera2.module.CameraModule;
import com.snailstudio2010.camera2.widget.ICoverView;
import com.snailstudio2010.camera2.widget.IFocusView;
import com.snailstudio2010.camera2.widget.IVideoTimer;

import java.util.HashSet;
import java.util.Set;

public class CameraView extends FrameLayout implements CameraDelegate, CameraUIListener {
    private static final String TAG = Config.getTag(CameraView.class);

    static Context mAppContext;

    private ICoverView mCoverView;
    private IFocusView mFocusView;
    private IVideoTimer mVideoTimer;

    private CameraToolKit mToolKit;
    private CameraModule mCameraModule;

    private Set<CameraDelegate> mDelegates = new HashSet<>();
    private Set<CameraUIListener> mUIListeners = new HashSet<>();
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

    public void setCoverView(ICoverView coverView, boolean attachToRoot) {
        this.mCoverView = coverView;
        if (attachToRoot) {
            ViewGroup parent = (ViewGroup) getParent();
            int index = parent.indexOfChild(this);
            parent.addView((View) mCoverView, index < 0 ? -1 : (index + 1));
        }
    }

    public void addCoverView(ICoverView coverView) {
        this.mCoverView = coverView;
        addView((View) coverView);
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

    public IVideoTimer getVideoTimer() {
        return mVideoTimer;
    }

    public void setVideoTimer(IVideoTimer videoTimer) {
        this.mVideoTimer = videoTimer;
    }

    public void setCameraModule(final CameraModule cameraModule) {
        if (mCameraModule != null && mCameraModule != cameraModule) {
            mCameraModule.destroy();
            post(new Runnable() {
                @Override
                public void run() {
                    mCameraModule = cameraModule;
                    mCameraModule.init(mAppContext, mController);
                    mCameraModule.startModule();
                }
            });
            return;
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

        for (CameraDelegate delegate : mDelegates) {
            if (delegate.updateUiSize(width, height)) return true;
        }
        setLayoutParams(new FrameLayout.LayoutParams(width, height));
        return true;
    }

    @Override
    public void onZoomChanged(float currentZoom, float maxZoom) {
        for (CameraDelegate delegate : mDelegates) {
            delegate.onZoomChanged(currentZoom, maxZoom);
        }
    }

    public void addDelegate(CameraDelegate listener) {
        mDelegates.add(listener);
    }

    public void removeDelegate(CameraDelegate listener) {
        mDelegates.remove(listener);
    }

    @Override
    public void onUIReady(CameraModule module, SurfaceTexture mainSurface, SurfaceTexture auxSurface) {
        for (CameraUIListener listener : mUIListeners) {
            listener.onUIReady(module, mainSurface, auxSurface);
        }
        if (mCoverView != null)
            mCoverView.hide(module);
    }

    @Override
    public void onUIDestroy(CameraModule module) {
        for (CameraUIListener listener : mUIListeners) {
            listener.onUIDestroy(module);
        }
        if (mCoverView != null)
            mCoverView.show(module);
    }

    public void addUIListener(CameraUIListener listener) {
        mUIListeners.add(listener);
    }

    public void removeUIListener(CameraUIListener listener) {
        mUIListeners.remove(listener);
    }

    @Override
    public void setUIClickable(boolean clickable) {
        for (CameraDelegate delegate : mDelegates) {
            delegate.setUIClickable(clickable);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        destroy();
    }

    public void destroy() {
        mToolKit.destroy();
        mDelegates.clear();
        mUIListeners.clear();
        if (mCameraModule != null) {
            mCameraModule.destroy();
        }
    }
}
