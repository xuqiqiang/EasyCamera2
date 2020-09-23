package com.snailstudio2010.camera2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Log;
import android.util.Size;
import android.view.WindowManager;

import com.snailstudio2010.camera2.manager.CameraSettings;
import com.snailstudio2010.camera2.manager.DeviceManager;
import com.snailstudio2010.camera2.module.SingleCameraModule;
import com.snailstudio2010.camera2.utils.CameraUtil;
import com.snailstudio2010.camera2.utils.Logger;

import org.wysaid.myUtils.LoadAssetsImageCallback;
import org.wysaid.nativePort.CGENativeLibrary;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.snailstudio2010.camera2.utils.CameraUtil.sortCamera2Size;

/**
 * Created by xuqiqiang on 3/17/17.
 */
public class Properties {

    private Map<String, Object> mData = new HashMap<>();
    private boolean useCameraV1;
    private boolean useGPUImage;
    private String savePath;

    public Properties() {
        Context context = getContext();
        if (context == null) return;
        DeviceManager deviceManager = new DeviceManager(context, null);
        CameraCharacteristics c = deviceManager.getCharacteristics(deviceManager.getDefaultCameraId());
        Integer level = c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (level == null || CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY == level) {
            useCameraV1 = true;
            Log.d("camera2", "useCameraV1");
        }
    }

    @SuppressLint("PrivateApi")
    private static Context getContext() {
        if (CameraView.mAppContext != null) return CameraView.mAppContext;
        try {
            return (Context) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Size[] toSize(List<Camera.Size> list) {
        Size[] res = new Size[list.size()];
        for (int i = 0; i < list.size(); i++) {
            res[i] = new Size(list.get(i).width, list.get(i).height);
        }
        return res;
    }

    public Properties debug(boolean debug) {
        Logger.enabled = debug;
        return this;
    }

    /**
     * 拍照预览大小。如果不设置，则previewSize为满足4:3比例且最接近屏幕尺寸的大小。
     */
    public Properties previewSize(Size size) {
        mData.put(CameraSettings.KEY_PREVIEW_SIZE, size);
        return this;
    }

    /**
     * @see #previewSize(Size)
     * 拍照预览大小。
     */
    public Properties previewSize(SizeSelector selector) {
        mData.put(CameraSettings.KEY_PREVIEW_SIZE, selector);
        return this;
    }

    /**
     * 拍照输出大小。如果不设置，则pictureSize为满足4:3比例且最接近2000万的大小。
     */
    public Properties pictureSize(Size size) {
        mData.put(CameraSettings.KEY_PICTURE_SIZE, size);
        return this;
    }

    /**
     * @see #pictureSize(Size)
     * 拍照输出大小。
     */
    public Properties pictureSize(SizeSelector selector) {
        mData.put(CameraSettings.KEY_PICTURE_SIZE, selector);
        return this;
    }

    /**
     * 视频输出大小。如果不设置，则videoSize为满足16:9比例且最接近屏幕尺寸的大小。
     */
    public Properties videoSize(Size size) {
        mData.put(CameraSettings.KEY_VIDEO_SIZE, size);
        return this;
    }

    /**
     * @see #videoSize(Size)
     * 视频输出大小。
     */
    public Properties videoSize(SizeSelector selector) {
        mData.put(CameraSettings.KEY_VIDEO_SIZE, selector);
        return this;
    }

    /**
     * 视频预览大小。如果不设置，则videoPreviewSize为满足videoSize比例且最接近屏幕尺寸的大小。
     */
    public Properties videoPreviewSize(Size size) {
        mData.put(CameraSettings.KEY_VIDEO_PREVIEW_SIZE, size);
        return this;
    }

    /**
     * @see #videoPreviewSize(Size)
     * 视频预览大小。
     */
    public Properties videoPreviewSize(SizeSelector selector) {
        mData.put(CameraSettings.KEY_VIDEO_PREVIEW_SIZE, selector);
        return this;
    }

    /**
     * 摄像头方向。如果不设置，则默认为后置摄像头，切换后会记录到缓存。
     *
     * @see SingleCameraModule#switchCamera()
     */
    public Properties cameraDevice(boolean isFront) {
        mData.put(CameraSettings.KEY_CAMERA_ID, isFront);
        return this;
    }

    /**
     * 默认闪光灯模式。如果不设置，则默认为off。
     *
     * @see SingleCameraModule#setFlashMode(String)
     */
    public Properties flashMode(String mode) {
        if (CameraSettings.FLASH_VALUE_ON.equalsIgnoreCase(mode)
                || CameraSettings.FLASH_VALUE_OFF.equalsIgnoreCase(mode)
                || CameraSettings.FLASH_VALUE_AUTO.equalsIgnoreCase(mode)
                || CameraSettings.FLASH_VALUE_TORCH.equalsIgnoreCase(mode)) {
            mData.put(CameraSettings.KEY_FLASH_MODE, mode);
        } else {
            throw new IllegalArgumentException("flashMode must be on/off/auto/torch.");
        }
        return this;
    }

    /**
     * 是否使用camera api 1版本。如果不设置，则根据设备Camera2的硬件兼容情况，
     * 兼容性为LEGACY以下的默认为camera api 1版本；其他默认为camera api 2版本。
     * <p>
     * 备注：Camera2是Google在Android 5.0中全新设计的框架，相机模块是和硬件紧密相关的，
     * Camera2中引入很多的特性，厂商的支持情况各有差异，所以Google定义了硬件兼容级别，方便开发者参考。
     * 硬件兼容性：LEGACY < LIMITED < FULL < LEVEL_3。
     *
     * @see CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL
     */
    public Properties useCameraV1(boolean useCameraV1) {
        this.useCameraV1 = useCameraV1;
        return this;
    }

    public boolean isUseCameraV1() {
        return useCameraV1;
    }

    /**
     * 是否使用GPUImage，启用需要引入相应library，可实现实时滤镜。默认不启用。
     */
    public Properties useGPUImage(boolean useGPUImage) {
        this.useGPUImage = useGPUImage;
        if (useGPUImage) {
            boolean isPresent = false;
            try {
                isPresent = null != Class.forName("org.wysaid.nativePort.CGENativeLibrary");
//                isPresent = CGENativeLibrary.check();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!isPresent) throw new IllegalArgumentException("Must import libgpuimage.");
        }
        return this;
    }

    public Properties setGPUImageAssetsDir(Context context, String dirName) {
        CGENativeLibrary.setLoadImageCallback(new LoadAssetsImageCallback(context, dirName), null);
        return this;
    }

    public boolean isUseGPUImage() {
        return useGPUImage;
    }

    /**
     * 输出文件目录。默认为/sdcard/DCIM/Camera2。
     */
    public Properties savePath(String savePath) {
        this.savePath = savePath;
        return this;
    }

    public String getSavePath() {
        return savePath;
    }

    public Object get(String key) {
        return mData.get(key);
    }

    public interface SizeSelector {
        Size select(Size[] sizes);
    }

    public static class MaxSizeSelector implements SizeSelector {
        private long limit = Long.MAX_VALUE;

        public MaxSizeSelector() {
        }

        public MaxSizeSelector(long limit) {
            this.limit = limit;
        }

        @Override
        public Size select(Size[] sizes) {
            return CameraUtil.findSize(sizes, new Comparator<Size>() {
                @Override
                public int compare(Size o1, Size o2) {
                    long s1 = o1.getWidth() * o1.getHeight();
                    long s2 = o2.getWidth() * o2.getHeight();
                    if (s1 > limit) return -1;
                    if (s2 > limit) return 1;
                    return s1 - s2 > 0 ? 1 : -1;
                }
            });
        }
    }

    public static class ScreenSizeSelector implements Properties.SizeSelector {
        private Point mRealDisplaySize = new Point();

        public ScreenSizeSelector(Context context) {
            WindowManager windowManager = (WindowManager) context.getSystemService(Context
                    .WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getRealSize(mRealDisplaySize);
        }

        @Override
        public Size select(Size[] sizes) {
            sortCamera2Size(sizes);
            for (Size size : sizes) {
                if (size.getHeight() <= mRealDisplaySize.x && size.getWidth() <= mRealDisplaySize.y) {
                    return size;
                }
            }
            return sizes[0];
        }
    }
}
