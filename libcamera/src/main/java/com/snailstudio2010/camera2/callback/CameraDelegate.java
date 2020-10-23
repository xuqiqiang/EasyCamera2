package com.snailstudio2010.camera2.callback;

import com.snailstudio2010.camera2.module.SingleCameraModule;

public interface CameraDelegate {
    /**
     * 设置ui用户事件可用性，防止camera逻辑处理时发送错误
     */
    void setUIClickable(boolean clickable);

    /**
     * Adjust layout when based on preview width
     *
     * @param width  preview screen width
     * @param height preview screen height
     */
    boolean updateUiSize(int width, int height);

    /**
     * 当手动设置了相机缩放比例后，会得到相机当前的缩放值
     *
     * @param currentZoom 相机当前缩放值
     * @param maxZoom     相机支持的最大缩放值
     * @see SingleCameraModule#setCameraZoom(float)
     */
    void onZoomChanged(float currentZoom, float maxZoom);
}