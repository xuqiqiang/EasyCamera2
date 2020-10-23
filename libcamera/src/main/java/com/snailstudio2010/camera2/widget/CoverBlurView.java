package com.snailstudio2010.camera2.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.snailstudio2010.camera2.CameraView;
import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.module.CameraModule;
import com.snailstudio2010.camera2.module.SingleCameraModule;
import com.snailstudio2010.camera2.utils.BlurryUtils;

/**
 * Created by xuqiqiang on 2020/09/27.
 */
public class CoverBlurView extends FrameLayout implements ICoverView {
    private static final String TAG = Config.getTag(CoverBlurView.class);
    private ImageView ivCapture;
    private ImageView ivCaptureBlur;

    public CoverBlurView(Context context) {
        this(context, null, 0);
    }

    public CoverBlurView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CoverBlurView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ivCapture = new ImageView(context);
        addView(ivCapture, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        ivCapture.setScaleType(ImageView.ScaleType.FIT_XY);

        ivCaptureBlur = new ImageView(context);
        addView(ivCaptureBlur, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        ivCaptureBlur.setScaleType(ImageView.ScaleType.FIT_XY);
        this.setVisibility(View.GONE);
    }

    @Override
    public void show(CameraModule module) {
        CameraView cameraView = module.getCameraView();
        if (!(module instanceof SingleCameraModule)) return;
        Bitmap shot = ((SingleCameraModule) module).takeShot(cameraView.getWidth() / 3,
                cameraView.getHeight() / 3);

        if (shot == null) return;
        setLayoutParams(cameraView.getLayoutParams());
        ivCapture.setImageBitmap(shot);
        setVisibility(View.VISIBLE);

        ivCaptureBlur.setImageBitmap(BlurryUtils.getBlurBitmap(shot, 50));

        AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
        alpha.setDuration(300);
        ivCaptureBlur.startAnimation(alpha);
    }

    @Override
    public void hide(CameraModule module) {
        setVisibility(View.GONE);
    }
}
