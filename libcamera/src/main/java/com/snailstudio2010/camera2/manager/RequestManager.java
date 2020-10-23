package com.snailstudio2010.camera2.manager;

import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;
import android.util.Range;

import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.Properties;
import com.snailstudio2010.camera2.callback.RequestCallback;
import com.snailstudio2010.camera2.utils.Logger;

public class RequestManager {

    private static final int MAX_ZOOM = 40;
    private final String TAG = Config.getTag(RequestManager.class);
    private CameraCharacteristics mCharacteristics;
    private MeteringRectangle[] mFocusArea;
    private MeteringRectangle[] mMeteringArea;
    // for reset AE/AF metering area
    private MeteringRectangle[] mResetRect = new MeteringRectangle[]{
            new MeteringRectangle(0, 0, 0, 0, 0)
    };
    private RequestCallback mRequestCallback;
    private Properties mProperties;

    public void setCharacteristics(CameraCharacteristics characteristics) {
        mCharacteristics = characteristics;
    }

    public void setRequestCallback(RequestCallback callback) {
        this.mRequestCallback = callback;
    }

    public void setProperties(Properties properties) {
        this.mProperties = properties;
    }

    public CaptureRequest getPreviewRequest(CaptureRequest.Builder builder) {
        int afMode = getValidAFMode(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE); // 自动对焦
        int antiBMode = getValidAntiBandingMode(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);
        builder.set(CaptureRequest.CONTROL_AF_MODE, afMode);
        builder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, antiBMode);
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
        fixCaptureRequest(builder);
        return builder.build();
    }

    public CaptureRequest.Builder fixCaptureRequest(CaptureRequest.Builder builder) {
        // 设置自动曝光帧率范围。部分手机在弱光环境下不管什么分辨率，预览和拍出来的照片都非常的暗
        Range<Integer> exposureRange = getRange();
        Logger.d(TAG, "getPreviewRequest exposureRange:" + exposureRange);
        if (exposureRange != null)
            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, exposureRange);
        chooseStabilizationMode(builder);
        return builder;
    }

    // Prefers optical stabilization over software stabilization if available. Only enables one of
    // the stabilization modes at a time because having both enabled can cause strange results.
    private void chooseStabilizationMode(CaptureRequest.Builder captureRequestBuilder) {
        final int[] availableOpticalStabilization = mCharacteristics.get(
                CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);
        if (availableOpticalStabilization != null) {
            for (int mode : availableOpticalStabilization) {
                if (mode == CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON) {
                    captureRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
                    captureRequestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
                    Logger.d(TAG, "Using optical stabilization.");
                    return;
                }
            }
        }
        if (mProperties != null && mProperties.isUseVideoStabilization()) {
            // If no optical mode is available, try software.
            final int[] availableVideoStabilization = mCharacteristics.get(
                    CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);
            for (int mode : availableVideoStabilization) {
                if (mode == CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON) {
                    captureRequestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
                    captureRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF);
                    Logger.d(TAG, "Using video stabilization.");
                    return;
                }
            }
        }
        Logger.d(TAG, "Stabilization not available.");
    }

    private Range<Integer> getRange() {
        Range<Integer>[] ranges = mCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        Range<Integer> result = null;

        for (Range<Integer> range : ranges) {
            Logger.d(TAG, "getRange:" + range);
            //帧率不能太低，大于10
//            if (range.getLower() < 10)
//                continue;
            if (result == null)
                result = range;
                //FPS下限小于15，弱光时能保证足够曝光时间，提高亮度。range范围跨度越大越好，光源足够时FPS较高，预览更流畅，光源不够时FPS较低，亮度更好。
            else if (range.getLower() <= 15 && (range.getUpper() - range.getLower()) > (result.getUpper() - result.getLower()))
                result = range;
        }
        return result;
    }

    public CaptureRequest getTouch2FocusRequest(CaptureRequest.Builder builder,
                                                MeteringRectangle focus, MeteringRectangle metering) {
        int afMode = getValidAFMode(CaptureRequest.CONTROL_AF_MODE_AUTO);
        builder.set(CaptureRequest.CONTROL_AF_MODE, afMode);
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        if (mFocusArea == null) {
            mFocusArea = new MeteringRectangle[]{focus};
        } else {
            mFocusArea[0] = focus;
        }
        if (mMeteringArea == null) {
            mMeteringArea = new MeteringRectangle[]{metering};
        } else {
            mMeteringArea[0] = metering;
        }
        if (isMeteringSupport(true)) {
            builder.set(CaptureRequest.CONTROL_AF_REGIONS, mFocusArea);
        }
        if (isMeteringSupport(false)) {
            builder.set(CaptureRequest.CONTROL_AE_REGIONS, mMeteringArea);
        }
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
        return builder.build();
    }

    public CaptureRequest getFocusModeRequest(CaptureRequest.Builder builder, int focusMode) {
        int afMode = getValidAFMode(focusMode);
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        builder.set(CaptureRequest.CONTROL_AF_MODE, afMode);
        builder.set(CaptureRequest.CONTROL_AF_REGIONS, mResetRect);
        builder.set(CaptureRequest.CONTROL_AE_REGIONS, mResetRect);
        // cancel af trigger
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
        return builder.build();
    }

    public CaptureRequest getStillPictureRequest(CaptureRequest.Builder builder, int rotation) {
        builder.set(CaptureRequest.JPEG_ORIENTATION, rotation);
        return builder.build();
    }

    public CaptureRequest getFocusDistanceRequest(CaptureRequest.Builder builder, float distance) {
        int afMode = getValidAFMode(CaptureRequest.CONTROL_AF_MODE_OFF);
        // preview
        builder.set(CaptureRequest.CONTROL_AF_MODE, afMode);
        float miniDistance = getMinimumDistance();
        if (miniDistance > 0) {
            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, miniDistance * distance);
        }
        return builder.build();
    }

    public CaptureRequest getCameraZoomRequest(CaptureRequest.Builder builder, float value) {

//        android.scaler.cropRegion a;
//        List<CaptureResult.Key<?>> test =  mCharacteristics.getAvailableCaptureResultKeys();
//        Logger.d(TAG, "sendCameraZoomRequest test:" + test.toString());

//        CaptureResult.get(test.get(0));


//        int i = (int) (value * 100f);
//
//        Rect rect2 = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
//        int radio2 = mCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM).intValue() / 3;
//        int realRadio2 = mCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM).intValue();
//        int centerX2 = rect2.centerX();
//        int centerY2 = rect2.centerY();
//        int minMidth2 = (rect2.right - ((i * centerX2) / 100 / radio2) - 1) - 20;
//        int minHeight2 = (rect2.bottom - ((i * centerY2) / 100 / radio2) - 1) - 20;
//        if (minMidth2 < rect2.right / realRadio2 || minHeight2 < rect2.bottom / realRadio2) {
//            Logger.d("sb_zoom", "sb_zoomsb_zoomsb_zoom");
//            return builder.build();
//        }
//        Rect newRect2 = new Rect(20, 20, rect2.right - ((i * centerX2) / 100 / radio2) - 1, rect2.bottom - ((i * centerY2) / 100 / radio2) - 1);
//        Logger.d("sb_zoom", "left--->" + "20" + ",,,top--->" + "20" + ",,,right--->" + (rect2.right - ((i * centerX2) / 100 / radio2) - 1) + ",,,bottom--->" + (rect2.bottom - ((i * centerY2) / 100 / radio2) - 1));
//        builder.set(CaptureRequest.SCALER_CROP_REGION, newRect2);
////        mSeekBarTextView.setText("放大：" + i + "%");
//
//
//        mRequestCallback.onZoomChanged(1, radio2);
//        return builder.build();


        Float maxZoom = mCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        Logger.d(TAG, "sendCameraZoomRequest maxZoom:" + maxZoom + ",value:" + value);
        if (maxZoom == null) return builder.build();

        Rect m = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (m == null) return builder.build();
//        float maxZ = maxZoom * 10;
//        float nowZ = 1 + (MAX_ZOOM - 1) * value;
//
//
//
//        int minW = (int) (m.width() / maxZ);
//        int minH = (int) (m.height() / maxZ);
//        int difW = m.width() - minW;
//        int difH = m.height() - minH;
//        int cropW = (int) (difW / 100 * nowZ);
//        int cropH = (int) (difH / 100 * nowZ);
//        cropW -= cropW & 3;
//        cropH -= cropH & 3;
//
//        Logger.d(TAG, "sendCameraZoomRequest zoom m:" + m.toString());
//        Logger.d(TAG, "sendCameraZoomRequest zoom maxZ:" + maxZ + ",nowZ:" + nowZ
//                + ",minW:" + minW + ",difW:" + difW + ",cropW:" + cropW);

        int cropW = (int) (m.width() * 0.45f * value);
        int cropH = (int) (m.height() * 0.45f * value);

        Rect zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
        Logger.d(TAG, "sendCameraZoomRequest zoom:" + zoom.toString());
        builder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
        mRequestCallback.onZoomChanged(1 + (maxZoom - 1) * value, maxZoom);
        return builder.build();
//        Float maxZoom = mCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
//        Logger.d(TAG, "sendCameraZoomRequest maxZoom:" + maxZoom);
//        if (maxZoom == null) return builder.build();
//
//        Rect m = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
//        if (m == null) return builder.build();
////        Logger.d(TAG, "sendCameraZoomRequest zoom last:" + builder.get(CaptureRequest.SCALER_CROP_REGION));
//        float maxZ = maxZoom * 10;
//        float nowZ = 1 + (MAX_ZOOM - 1) * value;
//
//
//
//        int minW = (int) (m.width() / maxZ);
//        int minH = (int) (m.height() / maxZ);
//        int difW = m.width() - minW;
//        int difH = m.height() - minH;
//        int cropW = (int) (difW / 100 * nowZ);
//        int cropH = (int) (difH / 100 * nowZ);
//        cropW -= cropW & 3;
//        cropH -= cropH & 3;
//
//        Logger.d(TAG, "sendCameraZoomRequest zoom m:" + m.toString());
//        Logger.d(TAG, "sendCameraZoomRequest zoom maxZ:" + maxZ + ",nowZ:" + nowZ
//                + ",minW:" + minW + ",difW:" + difW + ",cropW:" + cropW);
//
//        Rect zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
//        Logger.d(TAG, "sendCameraZoomRequest zoom:" + zoom.toString());
//        builder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
//        mRequestCallback.onZoomChanged(1 + (maxZoom - 1) * value, maxZoom);
//        return builder.build();
    }

    public CaptureRequest getFlashRequest(CaptureRequest.Builder builder, String value) {
        if (!isFlashSupport()) {
            Logger.e(TAG, " not support flash");
            return builder.build();
        }
        switch (value) {
            case CameraSettings.FLASH_VALUE_ON:
                builder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
                break;
            case CameraSettings.FLASH_VALUE_OFF:
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                break;
            case CameraSettings.FLASH_VALUE_AUTO:
                builder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
                break;
            case CameraSettings.FLASH_VALUE_TORCH:
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                break;
            default:
                Logger.e(TAG, "error value for flash mode");
                break;
        }
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
        return builder.build();
    }

    public void applyFlashRequest(CaptureRequest.Builder builder, String value) {
        if (!isFlashSupport()) {
            Logger.e(TAG, " not support flash");
            return;
        }
        switch (value) {
            case CameraSettings.FLASH_VALUE_ON:
                builder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
                break;
            case CameraSettings.FLASH_VALUE_OFF:
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                break;
            case CameraSettings.FLASH_VALUE_AUTO:
                builder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
                break;
            case CameraSettings.FLASH_VALUE_TORCH:
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                break;
            default:
                Logger.e(TAG, "error value for flash mode");
                break;
        }
    }

    /* ------------------------- private function------------------------- */
    private int getValidAFMode(int targetMode) {
        int[] allAFMode = mCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        for (int mode : allAFMode) {
            if (mode == targetMode) {
                return targetMode;
            }
        }
        Logger.d(TAG, "not support af mode:" + targetMode + " use mode:" + allAFMode[0]);
        return allAFMode[0];
    }

    private int getValidAntiBandingMode(int targetMode) {
        int[] allABMode = mCharacteristics.get(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES);
        for (int mode : allABMode) {
            if (mode == targetMode) {
                return targetMode;
            }
        }
        Logger.d(TAG, "not support anti banding mode:" + targetMode
                + " use mode:" + allABMode[0]);
        return allABMode[0];
    }

    private boolean isMeteringSupport(boolean focusArea) {
        int regionNum;
        if (focusArea) {
            regionNum = mCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        } else {
            regionNum = mCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        }
        return regionNum > 0;
    }

    private float getMinimumDistance() {
        Float distance = mCharacteristics.get(
                CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        if (distance == null) {
            return 0;
        }
        return distance;
    }

    private boolean isFlashSupport() {
        Boolean support = mCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        return support != null && support;
    }

    boolean canTriggerAf() {
        int[] allAFMode = mCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        return allAFMode != null && allAFMode.length > 1;
    }

}
