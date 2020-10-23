package com.snailstudio2010.camera2.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Size;
import android.view.WindowManager;

import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.Properties;
import com.snailstudio2010.camera2.utils.CameraUtil;
import com.snailstudio2010.camera2.utils.Logger;
import com.snailstudio2010.libcamera.R;

import java.util.ArrayList;

import static com.snailstudio2010.camera2.Properties.toSize;

/**
 * Created by xuqiqiang on 12/16/16.
 */
public class CameraSettings {
    public static final String KEY_PICTURE_SIZE = "pref_picture_size";
    public static final String KEY_PREVIEW_SIZE = "pref_preview_size";
    public static final String KEY_CAMERA_ID = "pref_camera_id";
    public static final String KEY_MAIN_CAMERA_ID = "pref_main_camera_id";
    public static final String KEY_AUX_CAMERA_ID = "pref_aux_camera_id";
    public static final String KEY_PICTURE_FORMAT = "pref_picture_format";
    public static final String KEY_RESTART_PREVIEW = "pref_restart_preview";
    public static final String KEY_SWITCH_CAMERA = "pref_switch_camera";
    public static final String KEY_CAMERA_ZOOM = "pref_camera_zoom";
    public static final String KEY_FOCUS_LENS = "pref_focus_lens";
    public static final String KEY_BRIGHTNESS = "pref_brightness";
    public static final String KEY_FILTER = "pref_filter";
    public static final String KEY_FLASH_MODE = "pref_flash_mode";
    public static final String KEY_ENABLE_DUAL_CAMERA = "pref_enable_dual_camera";
    public static final String KEY_SUPPORT_INFO = "pref_support_info";
    public static final String KEY_VIDEO_ID = "pref_video_camera_id";
    public static final String KEY_VIDEO_SIZE = "pref_video_size";
    public static final String KEY_VIDEO_QUALITY = "pref_video_quality";
    public static final String KEY_VIDEO_PREVIEW_SIZE = "pref_video_preview_size";
    //for flash mode
    public static final String FLASH_VALUE_ON = "on";
    public static final String FLASH_VALUE_OFF = "off";
    public static final String FLASH_VALUE_AUTO = "auto";
    public static final String FLASH_VALUE_TORCH = "torch";
    //for video size
    public static final int VIDEO_QUALITY_480P = 480;
    public static final int VIDEO_QUALITY_720P = 720;
    public static final int VIDEO_QUALITY_1080P = 1080;
    private static final ArrayList<String> SPEC_KEY = new ArrayList<>(3);

    static {
        SPEC_KEY.add(KEY_PICTURE_SIZE);
        SPEC_KEY.add(KEY_PREVIEW_SIZE);
        SPEC_KEY.add(KEY_PICTURE_FORMAT);
        SPEC_KEY.add(KEY_VIDEO_SIZE);
    }

    private final String TAG = Config.getTag(CameraSettings.class);
    private SharedPreferences mSharedPreference;
    private Context mContext;
    private Point mRealDisplaySize = new Point();
    private Properties mProperties;

    public CameraSettings(Context context) {
//        PreferenceManager.setDefaultValues(context, R.xml.camera_setting, false);
        mSharedPreference = PreferenceManager.getDefaultSharedPreferences(context);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context
                .WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealSize(mRealDisplaySize);
        mContext = context.getApplicationContext();
    }

    public CameraSettings(Context context, Properties properties) {
        this(context);
        this.mProperties = properties;
    }

    /**
     * get related shared preference by camera id
     *
     * @param cameraId valid camera id
     * @return related SharedPreference from the camera id
     */
    public SharedPreferences getSharedPrefById(String cameraId) {
        return mContext.getSharedPreferences(getSharedPrefName(cameraId), Context.MODE_PRIVATE);
    }

    public String getValueFromPref(String cameraId, String key, String defaultValue) {
        SharedPreferences preferences;
        if (!SPEC_KEY.contains(key)) {
            preferences = mSharedPreference;
        } else {
            preferences = getSharedPrefById(cameraId);
        }
        return preferences.getString(key, defaultValue);
    }

    public boolean setPrefValueById(String cameraId, String key, String value) {
        SharedPreferences preferences;
        if (!SPEC_KEY.contains(key)) {
            preferences = mSharedPreference;
        } else {
            preferences = getSharedPrefById(cameraId);
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    public boolean setGlobalPref(String key, String value) {
        SharedPreferences.Editor editor = mSharedPreference.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    public String getGlobalPref(String key, String defaultValue) {
        if (KEY_CAMERA_ID.equalsIgnoreCase(key)
                || KEY_VIDEO_ID.equalsIgnoreCase(key)) {
            String id = getCameraIdFromProperties();
            if (!TextUtils.isEmpty(id)) return id;
        }
        return mSharedPreference.getString(key, defaultValue);
    }

    public String getGlobalPref(String key) {
        if (mProperties != null) {
            Object o = mProperties.get(CameraSettings.KEY_FLASH_MODE);
            if (o instanceof String) return (String) o;
        }
        String defaultValue;
        switch (key) {
            case KEY_FLASH_MODE:
                defaultValue = CameraSettings.FLASH_VALUE_OFF;//mContext.getResources().getString(R.string.flash_off);
                break;
            case KEY_CAMERA_ID:
                String id = getCameraIdFromProperties();
                if (!TextUtils.isEmpty(id)) return id;
                defaultValue = mContext.getResources().getString(R.string.default_camera_id);
                break;
            default:
                defaultValue = "no value";
                break;
        }
        return mSharedPreference.getString(key, defaultValue);
    }

    private String getSharedPrefName(String cameraId) {
        return mContext.getPackageName() + "_camera_" + cameraId;
    }

    public int getPicFormat(String id, String key) {
        return Integer.parseInt(getValueFromPref(id, key, Config.IMAGE_FORMAT));
    }

    public String getPicFormatStr(String id, String key) {
        return getValueFromPref(id, key, Config.IMAGE_FORMAT);
    }

    public boolean needStartPreview() {
        return mSharedPreference.getBoolean(KEY_RESTART_PREVIEW, true);
    }

    public boolean isDualCameraEnable() {
        return mSharedPreference.getBoolean(KEY_ENABLE_DUAL_CAMERA, true);
    }

    public String getCameraIdFromProperties() {
        if (mProperties != null) {
            Object o = mProperties.get(CameraSettings.KEY_CAMERA_ID);
            if (o instanceof Boolean) {
                DeviceManager deviceManager = new DeviceManager(mContext, mProperties);
                String[] idList = deviceManager.getCameraIdList();
                for (String id : idList) {
                    Integer f = deviceManager.getCharacteristics(id)
                            .get(CameraCharacteristics.LENS_FACING);
                    int lens = Boolean.TRUE.equals(o) ? CameraCharacteristics.LENS_FACING_FRONT :
                            CameraCharacteristics.LENS_FACING_BACK;
                    if (f != null && lens == f) {
                        return id;
                    }
                }
            }
        }
        return null;
    }

    public Size getPictureSize(String id, StreamConfigurationMap map, int format) {
        if (mProperties != null) {
            Object o = mProperties.get(KEY_PICTURE_SIZE);
            if (o instanceof Size) return (Size) o;
            else if (o instanceof Properties.SizeSelector)
                return ((Properties.SizeSelector) o).select(map.getOutputSizes(format));
        }
        String picStr = getValueFromPref(id, KEY_PICTURE_SIZE, Config.NULL_VALUE);
        if (Config.NULL_VALUE.equals(picStr)) {
            // preference not set, use default value
            return CameraUtil.getDefaultPictureSize(map.getOutputSizes(format));
        } else {
            String[] size = picStr.split(CameraUtil.SPLIT_TAG);
            return new Size(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
        }
    }

    public String getPictureSizeStr(String id, StreamConfigurationMap map, int format) {
        String picStr = getValueFromPref(id, KEY_PICTURE_SIZE, Config.NULL_VALUE);
        if (Config.NULL_VALUE.equals(picStr)) {
            // preference not set, use default value
            Size size = CameraUtil.getDefaultPictureSize(map.getOutputSizes(format));
            return size.getWidth() + CameraUtil.SPLIT_TAG + size.getHeight();
        } else {
            return picStr;
        }
    }

    public Size getPictureSize(String id, Camera.Parameters parameters) {
        Size[] arr = toSize(parameters.getSupportedPictureSizes());
        if (mProperties != null) {
            Object o = mProperties.get(KEY_PICTURE_SIZE);
            if (o instanceof Size) return (Size) o;
            else if (o instanceof Properties.SizeSelector)
                return ((Properties.SizeSelector) o).select(arr);
        }
        String picStr = getValueFromPref(id, KEY_PICTURE_SIZE, Config.NULL_VALUE);
        if (Config.NULL_VALUE.equals(picStr)) {
            // preference not set, use default value
            return CameraUtil.getDefaultPictureSize(arr);
        } else {
            String[] size = picStr.split(CameraUtil.SPLIT_TAG);
            return new Size(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
        }
    }

    public Size getPreviewSize(String id, String key, StreamConfigurationMap map) {
        if (mProperties != null) {
            Object o = mProperties.get(key);
            if (o instanceof Size) return (Size) o;
            else if (o instanceof Properties.SizeSelector)
                return ((Properties.SizeSelector) o).select(map.getOutputSizes(SurfaceTexture.class));
        }
        String preStr = getValueFromPref(id, key, Config.NULL_VALUE);
        if (Config.NULL_VALUE.equals(preStr)) {
            // preference not set, use default value
            return CameraUtil.getDefaultPreviewSize(map.getOutputSizes(SurfaceTexture.class), mRealDisplaySize);
        } else {
            String[] size = preStr.split(CameraUtil.SPLIT_TAG);
            return new Size(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
        }
    }

    public Size getPreviewSize(String id, String key, Camera.Parameters parameters) {
        Size[] arr = toSize(parameters.getSupportedPreviewSizes());
        if (mProperties != null) {
            Object o = mProperties.get(key);
            if (o instanceof Size) return (Size) o;
            else if (o instanceof Properties.SizeSelector)
                return ((Properties.SizeSelector) o).select(arr);
        }
//        Size videoSize = getVideoSize(id, parameters);
//        return CameraUtil.getPreviewSizeByRatio(arr, mRealDisplaySize,
//                videoSize.getWidth() / (double) (videoSize.getHeight()));
        String preStr = getValueFromPref(id, key, Config.NULL_VALUE);
        if (Config.NULL_VALUE.equals(preStr)) {
            // preference not set, use default value
            return CameraUtil.getDefaultPreviewSize(arr, mRealDisplaySize);
        } else {
            String[] size = preStr.split(CameraUtil.SPLIT_TAG);
            return new Size(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
        }
    }

//    public Size getPreviewSizeByRatio(StreamConfigurationMap map, double ratio) {
//        if (mProperties != null) {
//            Object o = mProperties.get(CameraSettings.KEY_VIDEO_PREVIEW_SIZE);
//            if (o instanceof Size) return (Size) o;
//            else if (o instanceof Properties.SizeSelector)
//                return ((Properties.SizeSelector) o).select(map.getOutputSizes(SurfaceTexture.class));
//        }
//        return CameraUtil.getPreviewSizeByRatio(map.getOutputSizes(SurfaceTexture.class), mRealDisplaySize, ratio);
//    }

    public Size getPreviewSizeByRatio(Size[] sizes, String key, double ratio) {
        if (mProperties != null) {
            Object o = mProperties.get(key);
            if (o instanceof Size) return (Size) o;
            else if (o instanceof Properties.SizeSelector)
                return ((Properties.SizeSelector) o).select(sizes);
        }
        return CameraUtil.getPreviewSizeByRatio(sizes, mRealDisplaySize, ratio);
    }

    public String getPreviewSizeStr(String id, String key, StreamConfigurationMap map) {
        String preStr = getValueFromPref(id, key, Config.NULL_VALUE);
        if (Config.NULL_VALUE.equals(preStr)) {
            // preference not set, use default value
            Size size = CameraUtil.getDefaultPreviewSize(map.getOutputSizes(SurfaceTexture.class), mRealDisplaySize);
            return size.getWidth() + CameraUtil.SPLIT_TAG + size.getHeight();
        } else {
            return preStr;
        }
    }

    public Size getVideoSize(String id, StreamConfigurationMap map) {
        if (mProperties != null) {
            Object o = mProperties.get(KEY_VIDEO_SIZE);
            if (o instanceof Size) return (Size) o;
            else if (o instanceof Properties.SizeSelector)
                return ((Properties.SizeSelector) o).select(map.getOutputSizes(MediaRecorder.class));
        }
        String videoStr = getValueFromPref(id, KEY_VIDEO_SIZE, Config.NULL_VALUE);
        if (Config.NULL_VALUE.equals(videoStr)) {
            int quality = VIDEO_QUALITY_720P;
            if (mProperties != null) {
                Object o = mProperties.get(KEY_VIDEO_QUALITY);
                if (o instanceof Integer) {
                    quality = (int) o;
                }
            }
            // preference not set, use default value
            return CameraUtil.getDefaultVideoSize(map.getOutputSizes(MediaRecorder.class), mRealDisplaySize, quality);
        } else {
            String[] size = videoStr.split(CameraUtil.SPLIT_TAG);
            return new Size(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
        }
    }

    public Size getVideoSize(String id, Camera.Parameters parameters) {
        Size[] arr = toSize(parameters.getSupportedVideoSizes());
        if (mProperties != null) {
            Object o = mProperties.get(KEY_VIDEO_SIZE);
            if (o instanceof Size) return (Size) o;
            else if (o instanceof Properties.SizeSelector)
                return ((Properties.SizeSelector) o).select(arr);
        }
        String videoStr = getValueFromPref(id, KEY_VIDEO_SIZE, Config.NULL_VALUE);
        if (Config.NULL_VALUE.equals(videoStr)) {
            int quality = VIDEO_QUALITY_720P;
            if (mProperties != null) {
                Object o = mProperties.get(KEY_VIDEO_QUALITY);
                if (o instanceof Integer) {
                    quality = (int) o;
                }
            }
            // preference not set, use default value
            return CameraUtil.getDefaultVideoSize(arr, mRealDisplaySize, quality);
        } else {
            String[] size = videoStr.split(CameraUtil.SPLIT_TAG);
            return new Size(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
        }
    }

    public CamcorderProfile getVideoProfile(String id, Camera.Parameters parameters) {
        CamcorderProfile profile = null;

        Size videoSize = getVideoSize(id, parameters);
        Logger.d(TAG, "setUpMediaRecorder setVideoSize width:" + videoSize.getWidth()
                + ", height:" + videoSize.getHeight());

        int[] profiles = {CamcorderProfile.QUALITY_2160P, CamcorderProfile.QUALITY_1080P,
                CamcorderProfile.QUALITY_720P, CamcorderProfile.QUALITY_480P, CamcorderProfile.QUALITY_HIGH,
                CamcorderProfile.QUALITY_QCIF};
        int[] profilesSize = {2160, 1080, 720, 480, 360, 144};
        int size = Math.min(videoSize.getWidth(), videoSize.getHeight());
        for (int i = 0; i < profiles.length; i += 1) {
            if (size >= profilesSize[i]) {
                profile = CamcorderProfile.get(profiles[i]);
                break;
            }
        }
        if (profile == null) profile = CamcorderProfile.get(CamcorderProfile.QUALITY_QCIF);
        return profile;
    }

    public String getVideoSizeStr(String id, String key, StreamConfigurationMap map) {
        String videoStr = getValueFromPref(id, key, Config.NULL_VALUE);
        if (Config.NULL_VALUE.equals(videoStr)) {
            int quality = VIDEO_QUALITY_720P;
            if (mProperties != null) {
                Object o = mProperties.get(KEY_VIDEO_QUALITY);
                if (o instanceof Integer) {
                    quality = (int) o;
                }
            }
            // preference not set, use default value
            Size size = CameraUtil.getDefaultVideoSize(map.getOutputSizes(MediaRecorder.class), mRealDisplaySize, quality);
            return size.getWidth() + CameraUtil.SPLIT_TAG + size.getHeight();
        } else {
            return videoStr;
        }
    }

    public String getSupportInfo(Context context) {
        StringBuilder builder = new StringBuilder();
        DeviceManager deviceManager = new DeviceManager(context, mProperties);
        String[] idList = deviceManager.getCameraIdList();
        String splitLine = "- - - - - - - - - -";
        builder.append(splitLine).append("\n");
        for (String cameraId : idList) {
            builder.append("Camera ID: ").append(cameraId).append("\n");
            // hardware support level
            CameraCharacteristics c = deviceManager.getCharacteristics(cameraId);
            Integer level = c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            builder.append("Hardware Support Level:").append("\n");
            builder.append(CameraUtil.hardwareLevel2Sting(level)).append("\n");
            builder.append("(LEGACY < LIMITED < FULL < LEVEL_3)").append("\n");
            // Capabilities
            builder.append("Camera Capabilities:").append("\n");
            int[] caps = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
            for (int cap : caps) {
                builder.append(CameraUtil.capabilities2String(cap)).append(" ");
            }
            builder.append("\n");
            builder.append(splitLine).append("\n");
        }
        return builder.toString();
    }

}
