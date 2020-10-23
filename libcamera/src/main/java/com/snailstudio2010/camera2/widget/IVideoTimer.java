package com.snailstudio2010.camera2.widget;

public interface IVideoTimer {
    void start();

    void pause();

    void resume();

    void stop();

    void refresh(boolean recording);
}
