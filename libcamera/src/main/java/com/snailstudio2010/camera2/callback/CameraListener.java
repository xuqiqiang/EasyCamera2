package com.snailstudio2010.camera2.callback;

public interface CameraListener {
    void setUIClickable(boolean clickable);

//    void changeModule(int right);

    void closeMenu();

    boolean updateUiSize(int width, int height);

    void onZoomChanged(float currentZoom, float maxZoom);
}