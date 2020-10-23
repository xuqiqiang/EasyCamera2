package com.snailstudio2010.camera2.ui.gl;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.AttributeSet;

import com.snailstudio2010.camera2.utils.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class CameraRecordGLSurfaceView extends CameraGLSurfaceViewWithTexture {

    private final Object mRecordStateLock = new Object();
    private boolean mShouldRecord = false;
    private AudioRecordRunnable mAudioRecordRunnable;
    private Thread mAudioThread;

    public CameraRecordGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public synchronized boolean isRecording() {
        return mShouldRecord;
    }

    public void startRecording(final String filename) {
        startRecording(filename, null);
    }

    public void startRecording(final String filename, final StartRecordingCallback recordingCallback) {

        queueEvent(new Runnable() {
            @Override
            public void run() {

                if (mFrameRecorder == null) {
                    Logger.e(LOG_TAG, "Error: startRecording after release!!");
                    if (recordingCallback != null) {
                        recordingCallback.startRecordingOver(false);
                    }
                    return;
                }

                if (!mFrameRecorder.startRecording(30, filename)) {
                    Logger.e(LOG_TAG, "start recording error!");
                    if (recordingCallback != null)
                        recordingCallback.startRecordingOver(false);
                    return;
                }
                Logger.d(LOG_TAG, "glSurfaceView recording, file: " + filename);
                synchronized (mRecordStateLock) {
                    mShouldRecord = true;
                    mAudioRecordRunnable = new AudioRecordRunnable(recordingCallback);
                    if (mAudioRecordRunnable.audioRecord != null) {
                        mAudioThread = new Thread(mAudioRecordRunnable);
                        mAudioThread.start();
                    }
                }
            }
        });
    }

    public void endRecording() {
        endRecording(null, true);
    }

    public void endRecording(final EndRecordingCallback callback) {
        endRecording(callback, true);
    }

    // The video may be invalid if "shouldSave" is false;
    public void endRecording(final EndRecordingCallback callback, final boolean shouldSave) {
        Logger.d(LOG_TAG, "notify quit...");
        synchronized (mRecordStateLock) {
            mShouldRecord = false;
        }

        if (mFrameRecorder == null) {
            Logger.e(LOG_TAG, "Error: endRecording after release!!");
            return;
        }

        joinAudioRecording();

        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mFrameRecorder != null)
                    mFrameRecorder.endRecording(shouldSave);
                if (callback != null) {
                    callback.endRecordingOK();
                }
            }
        });
    }

    @Override
    protected void onRelease() {
        synchronized (mRecordStateLock) {
            mShouldRecord = false;
        }
        joinAudioRecording();
        super.onRelease();
    }

//    @Override
//    public void stopPreview() {
//        synchronized (mRecordStateLock) {
//            if (mShouldRecord) {
//                Logger.e(LOG_TAG, "The camera is recording! cannot stop!");
//                return;
//            }
//        }
//
//        super.stopPreview();
//    }

    public void joinAudioRecording() {

        if (mAudioThread != null) {
            try {
                mAudioThread.join();
                mAudioThread = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public interface StartRecordingCallback {
        void startRecordingOver(boolean success);
    }

    public interface EndRecordingCallback {
        void endRecordingOK();
    }

    class AudioRecordRunnable implements Runnable {

        private static final int sampleRate = 44100;
        public AudioRecord audioRecord;
        public volatile boolean isInitialized;
        int bufferSize;
        //        short[] audioData;
        int bufferReadResult;
        ByteBuffer audioBufferRef;
        ShortBuffer audioBuffer;
        StartRecordingCallback recordingCallback;

        private AudioRecordRunnable(StartRecordingCallback callback) {
            recordingCallback = callback;
            try {
                bufferSize = AudioRecord.getMinBufferSize(sampleRate,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                Logger.d(LOG_TAG, "audio min buffer size: " + bufferSize);
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
//                audioData = new short[bufferSize];
                audioBufferRef = ByteBuffer.allocateDirect(bufferSize * 2).order(ByteOrder.nativeOrder());
                audioBuffer = audioBufferRef.asShortBuffer();
            } catch (Exception e) {
                if (audioRecord != null) {
                    audioRecord.release();
                    audioRecord = null;
                }
            }

            if (audioRecord == null && recordingCallback != null) {
                recordingCallback.startRecordingOver(false);
                recordingCallback = null;
            }
        }

        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            this.isInitialized = false;

            if (this.audioRecord == null) {
                recordingCallback.startRecordingOver(false);
                recordingCallback = null;
                return;
            }

            //判断音频录制是否被初始化
            while (this.audioRecord.getState() == 0) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException localInterruptedException) {
                    localInterruptedException.printStackTrace();
                }
            }
            this.isInitialized = true;

            try {
                this.audioRecord.startRecording();
            } catch (Exception e) {
                if (recordingCallback != null) {
                    recordingCallback.startRecordingOver(false);
                    recordingCallback = null;
                }
                return;
            }

            if (this.audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                if (recordingCallback != null) {
                    recordingCallback.startRecordingOver(false);
                    recordingCallback = null;
                }
                return;
            }

            if (recordingCallback != null) {
                recordingCallback.startRecordingOver(true);
                recordingCallback = null;
            }


            while (true) {
                synchronized (mRecordStateLock) {
                    if (!mShouldRecord) //&& mFrameRecorder.getVideoStreamtime() <= mFrameRecorder.getAudioStreamtime()
                        break;
                }

                audioBufferRef.position(0);
                bufferReadResult = this.audioRecord.read(audioBufferRef, bufferSize * 2);
                if (mShouldRecord && bufferReadResult > 0 && mFrameRecorder != null &&
                        mFrameRecorder.getTimestamp() > mFrameRecorder.getAudioStreamtime()) {
//                    Logger.e(LOG_TAG, "buffer Result: " + bufferReadResult);
                    audioBuffer.position(0);
//                    audioBuffer.put(audioData).position(0);
                    mFrameRecorder.recordAudioFrame(audioBuffer, bufferReadResult / 2);
                }
            }
            this.audioRecord.stop();
            this.audioRecord.release();
            Logger.d(LOG_TAG, "Audio thread end!");
        }
    }
}
