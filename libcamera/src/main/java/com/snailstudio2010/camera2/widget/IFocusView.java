package com.snailstudio2010.camera2.widget;

/**
 * Created by xuqiqiang on 16-3-23.
 */
public interface IFocusView {
    void initFocusArea(int width, int height);

    void resetToDefaultPosition();

    void moveToPosition(float x, float y);

    void startFocus();

    void focusSuccess();

    void focusError();

    void hide();
}
