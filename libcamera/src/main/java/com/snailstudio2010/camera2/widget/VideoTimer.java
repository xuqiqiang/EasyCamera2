package com.snailstudio2010.camera2.widget;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;

import com.snailstudio2010.libcamera.R;

/**
 * Created by xuqiqiang on 2020/09/28.
 */
public class VideoTimer extends LinearLayout implements IVideoTimer {

    private Button mRecButton;
    private Chronometer mChronometer;
    private long mRecordingTime;

    public VideoTimer(Context context) {
        this(context, null, 0);
    }

    public VideoTimer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoTimer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.video_timer_layout, this);
        mChronometer = findViewById(R.id.record_time);
        mRecButton = findViewById(R.id.btn_record);
    }

    @Override
    public void start() {
        setVisibility(View.VISIBLE);
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mChronometer.start();
    }

    @Override
    public void pause() {
        mChronometer.stop();
        mRecordingTime = SystemClock.elapsedRealtime() - mChronometer.getBase();
    }

    @Override
    public void resume() {
        mChronometer.setBase(SystemClock.elapsedRealtime() - mRecordingTime);
        mChronometer.start();
    }

    @Override
    public void stop() {
        mChronometer.stop();
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mRecordingTime = 0;
        setVisibility(View.INVISIBLE);
    }

    @Override
    public void refresh(boolean recording) {
        if (recording) {
            mRecButton.setBackgroundResource(R.drawable.ic_vector_recoding);
        } else {
            mRecButton.setBackgroundResource(R.drawable.ic_vector_record_pause);
        }
    }
}
