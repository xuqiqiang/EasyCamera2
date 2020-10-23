package com.snailstudio2010.camera2.widget;

import android.graphics.Color;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;

public class CaptureAnimation extends Animation implements AnimationListener {

    private View mView;

    public CaptureAnimation(View view) {
        mView = view;
        mView.setAnimation(this);
        setDuration(400);
        setAnimationListener(this);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        float alpha;
        if (interpolatedTime < 0.125) {
            alpha = interpolatedTime / 0.125f;
        } else if (interpolatedTime >= 0.125 && interpolatedTime < 0.5) {
            alpha = 1.0f;
        } else {
            alpha = 0.8f * (1 - interpolatedTime) / 0.5f;
        }
        mView.setBackgroundColor(Color.argb((int) (alpha * 255 * 0.8), 27, 30, 30));
    }

    @Override
    public void onAnimationStart(Animation animation) {
        mView.setBackgroundColor(Color.argb(0, 27, 30, 30));
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        mView.setVisibility(View.INVISIBLE);
    }

    public void start() {
        mView.setVisibility(View.VISIBLE);
        mView.startAnimation(this);
    }
}
