package com.snailstudio2010.camera2.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;

import com.snailstudio2010.camera2.Config;
import com.snailstudio2010.camera2.Properties;
import com.snailstudio2010.camera2.exif.ExifInterface;
import com.snailstudio2010.camera2.exif.ExifTag;
import com.snailstudio2010.libcamera.R;

import org.wysaid.nativePort.CGENativeLibrary;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.snailstudio2010.camera2.ui.gl.CameraGLSurfaceView.mMaxTextureSize;

/**
 * Created by xuqiqiang on 9/6/17.
 */

public class FileSaver {

    private static final String TAG = Config.getTag(FileSaver.class);

    private final String JPEG = "image/jpeg";
    private final String VIDEO = "video/mpeg";
    private final String YUV = "image/yuv";

    private ContentResolver mResolver;
    private Context mContext;
    private FileListener mListener;
    private Handler mHandler;
    private String mFilterConfig;
    private float mFilterIntensity = 1.0f;
    private Properties mProperties;

    public FileSaver(Context context, Handler handler) {
        mHandler = handler;
        mContext = context;
        mResolver = context.getContentResolver();
    }

    public void setFileListener(FileListener listener) {
        mListener = listener;
    }

    public void setProperties(Properties properties) {
        this.mProperties = properties;
    }

    public void saveFile(int width, int height, int orientation, boolean isFront, byte[] data, String tag,
                         int saveType, String savePath) {
        File file = MediaFunc.getOutputMediaFile(saveType, tag, savePath);
        if (file == null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onFileSaveError("can not create file or directory");
                }
            });
            return;
        }
        ImageInfo info = new ImageInfo();
        info.imgWidth = width;
        info.imgHeight = height;
        info.imgData = data;
        info.imgOrientation = 0;
        info.isFront = isFront;
        info.imgDate = System.currentTimeMillis();
        info.imgPath = file.getPath();
        info.imgTitle = file.getName();
        info.imgMimeType = getMimeType(saveType);
        if (saveType == MediaFunc.MEDIA_TYPE_YUV) {
            saveYuvFile(info);
        } else {
            saveJpegFile(info);
        }
    }

    public void saveVideoFile(int width, int height, int orientation,
                              final String path, int type, final Bitmap thumbnail) {
        if (true) {
            final Uri uri = Uri.fromFile(new File(path));//Uri.parse("file://" + path);
            mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
            if (mListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onFileSaved(uri, path, thumbnail);
                    }
                });
            }
            return;
        }
        if (orientation % 180 == 0) {
            width = width + height;
            height = width - height;
            width = width - height;
        }
        File file = new File(path);
        final Uri uri = Storage.addVideoToDB(mResolver, file.getName(),
                System.currentTimeMillis(), null, file.length(), path,
                width, height, getMimeType(type));
        if (mListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onFileSaved(uri, path, thumbnail);
                }
            });
        }
    }

    public void setFilterConfig(String config) {
        this.mFilterConfig = config;
    }

    private void saveJpegFile(final ImageInfo info) {
        try {
            ExifInterface exif = new ExifInterface();
            exif.readExif(info.imgData);
            final Bitmap thumbnail = rotateAndWriteJpegData(exif, info);

            if (true) {
                final Uri uri = Uri.fromFile(new File(info.imgPath));
                mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                if (mListener != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onFileSaved(uri, info.imgPath, thumbnail);
                        }
                    });
                }
                return;
            }
            final Uri uri = Storage.addImageToDB(mResolver, info.imgTitle, info.imgDate,
                    info.imgLocation, info.imgOrientation, info.imgData.length, info.imgPath,
                    info.imgWidth, info.imgHeight, info.imgMimeType);
            if (mListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onFileSaved(uri, info.imgPath, thumbnail);
                    }
                });
            }
        } catch (final Exception e) {
            Logger.e(TAG, "error get exif msg", e);
            if (mListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onFileSaveError(e.getMessage());
                    }
                });
            }
        }
    }

    private void saveYuvFile(final ImageInfo info) {
        try {
            Storage.writeFile(info.imgPath, info.imgData);
            final Uri uri = Storage.addImageToDB(mResolver, info.imgTitle, info.imgDate,
                    info.imgLocation, info.imgOrientation, info.imgData.length, info.imgPath,
                    info.imgWidth, info.imgHeight, info.imgMimeType);
            final Bitmap thumbnail = (mProperties == null || !mProperties.isNeedThumbnail()) ? null :
                    BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.yuv_file);
            if (mListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onFileSaved(uri, info.imgPath, thumbnail);
                    }
                });
            }
        } catch (final Exception e) {
            Logger.e(TAG, "error get yuv msg", e);
            if (mListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onFileSaveError(e.getMessage());
                    }
                });
            }
        }
    }

    private String getMimeType(int type) {
        if (type == MediaFunc.MEDIA_TYPE_IMAGE) {
            return JPEG;
        } else if (type == MediaFunc.MEDIA_TYPE_VIDEO) {
            return VIDEO;
        } else {
            return YUV;
        }
    }

    private Bitmap handleGLBitmap(Bitmap bitmap, ImageInfo info) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (mMaxTextureSize > 0 && (width > mMaxTextureSize || height > mMaxTextureSize)) {
            float scaling = Math.max(width / (float) mMaxTextureSize, height / (float) mMaxTextureSize);
            Logger.d(TAG, String.format("目标尺寸(%d x %d)超过当前设备OpenGL 能够处理的最大范围(%d x %d)， 现在将图片压缩至合理大小!",
                    width, height, mMaxTextureSize, mMaxTextureSize));

            try {
                Bitmap origin = bitmap;
                bitmap = Bitmap.createScaledBitmap(origin, (int) (width / scaling), (int) (height / scaling), false);
                Logger.d(TAG, "handleGLBitmap createScaledBitmap:" + (origin != bitmap));
                if (origin != bitmap)
                    origin.recycle();
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                return bitmap;
            }
            Logger.d(TAG, "handleGLBitmap bitmap:" + bitmap.getWidth() + "," + bitmap.getHeight());
            Logger.d(TAG, "handleGLBitmap info:" + info.imgWidth + "," + info.imgHeight);
            width = info.imgWidth > info.imgHeight ?
                    Math.max(bitmap.getWidth(), bitmap.getHeight()) : Math.min(bitmap.getWidth(), bitmap.getHeight());
            height = info.imgWidth > info.imgHeight ?
                    Math.min(bitmap.getWidth(), bitmap.getHeight()) : Math.max(bitmap.getWidth(), bitmap.getHeight());
            info.imgWidth = width;
            info.imgHeight = height;
        }
        return bitmap;
    }

    private Bitmap rotateAndWriteJpegData(ExifInterface exif, ImageInfo info) throws IOException {
//        int orientation = info.isFront ? ExifInterface.Orientation.TOP_RIGHT :
//                ExifInterface.Orientation.TOP_LEFT;
        int orientation = 0;
        int oriW = info.imgWidth;
        int oriH = info.imgHeight;
        try {
            orientation = exif.getTagIntValue(ExifInterface.TAG_ORIENTATION);
            oriW = exif.getTagIntValue(ExifInterface.TAG_IMAGE_WIDTH);
            oriH = exif.getTagIntValue(ExifInterface.TAG_IMAGE_LENGTH);
        } catch (Exception e) {
            // getTagIntValue() may cause NullPointerException
            e.printStackTrace();
        }

        if (orientation == 0 || orientation == ExifInterface.Orientation.TOP_LEFT) {
            orientation = info.isFront ? ExifInterface.Orientation.TOP_RIGHT :
                    ExifInterface.Orientation.TOP_LEFT;
        }

        Logger.d(TAG, "rotateAndWriteJpegData:" + orientation);
        // no need rotate, just save and return
        if (orientation == ExifInterface.Orientation.TOP_LEFT) {
            // use exif width & height
            info.imgWidth = oriW;
            info.imgHeight = oriH;

            if (!TextUtils.isEmpty(mFilterConfig)) {
                Bitmap origin = BitmapFactory.decodeByteArray(info.imgData, 0, info.imgData.length);
                Bitmap bitmap = handleGLBitmap(origin, info);
                CGENativeLibrary.filterImage_MultipleEffectsWriteBack(bitmap, mFilterConfig, mFilterIntensity);
                Storage.writeBitmap(info.imgPath, bitmap, 100);
                return getThumbnail(bitmap);
            }
            Storage.writeFile(info.imgPath, info.imgData);
            return getThumbnail(info);
        }
        if (orientation <= 0) {
            Logger.e(TAG, "invalid orientation value:" + orientation);
        }
        Matrix matrix = new Matrix();
        switch (orientation) {
            //case ExifInterface.Orientation.TOP_LEFT:
            // do nothing, just save jpeg data
            //    break;
            case ExifInterface.Orientation.TOP_RIGHT:
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.Orientation.BOTTOM_LEFT:
                matrix.postRotate(180);
                break;
            case ExifInterface.Orientation.BOTTOM_RIGHT:
                matrix.postScale(1, -1);
                break;
            case ExifInterface.Orientation.LEFT_TOP:
                matrix.postScale(1, -1);
                matrix.postRotate(90);
                // swap width and height
                exif.setTagValue(ExifInterface.TAG_IMAGE_WIDTH, oriH);
                exif.setTagValue(ExifInterface.TAG_IMAGE_LENGTH, oriW);
                break;
            case ExifInterface.Orientation.RIGHT_TOP:
                matrix.postRotate(90);
                exif.setTagValue(ExifInterface.TAG_IMAGE_WIDTH, oriH);
                exif.setTagValue(ExifInterface.TAG_IMAGE_LENGTH, oriW);
                break;
            case ExifInterface.Orientation.LEFT_BOTTOM:
                matrix.postScale(-1, 1);
                matrix.postRotate(90);
                exif.setTagValue(ExifInterface.TAG_IMAGE_WIDTH, oriH);
                exif.setTagValue(ExifInterface.TAG_IMAGE_LENGTH, oriW);
                break;
            case ExifInterface.Orientation.RIGHT_BOTTOM:
                matrix.postRotate(270);
                exif.setTagValue(ExifInterface.TAG_IMAGE_WIDTH, oriH);
                exif.setTagValue(ExifInterface.TAG_IMAGE_LENGTH, oriW);
                break;
            default:
                Logger.e(TAG, "exif orientation error value:" + orientation);
                break;
        }
        // jpeg rotated, set orientation to normal
        exif.setTagValue(ExifInterface.TAG_ORIENTATION, ExifInterface.Orientation.TOP_LEFT);
        try {
            // use exif width & height
            info.imgWidth = exif.getTagIntValue(ExifInterface.TAG_IMAGE_WIDTH);
            info.imgHeight = exif.getTagIntValue(ExifInterface.TAG_IMAGE_LENGTH);
        } catch (Exception e) {
            // getTagIntValue() may cause NullPointerException
            e.printStackTrace();
        }
        Bitmap origin = BitmapFactory.decodeByteArray(info.imgData, 0, info.imgData.length);
        Bitmap rotatedMap = Bitmap.createBitmap(origin,
                0, 0, origin.getWidth(), origin.getHeight(), matrix, true);
        if (!TextUtils.isEmpty(mFilterConfig)) {
            rotatedMap = handleGLBitmap(rotatedMap, info);
            CGENativeLibrary.filterImage_MultipleEffectsWriteBack(rotatedMap, mFilterConfig, mFilterIntensity);
        }
        Bitmap thumb = getThumbnail(rotatedMap);
        List<ExifTag> tags = exif.getAllTags();
        exif.setExif(tags);
        try {
            exif.writeExif(rotatedMap, info.imgPath, 100);
        } catch (IOException e) {
            Logger.e(TAG, "write file error msg", e);
            throw e;
        } finally {
            origin.recycle();
            rotatedMap.recycle();
        }
        return thumb;
    }

    private Bitmap getThumbnail(ImageInfo info) {
        if (mProperties == null || !mProperties.isNeedThumbnail()) return null;
        if (JPEG.equals(info.imgMimeType)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = info.imgWidth / Config.THUMB_SIZE;
            return BitmapFactory.decodeByteArray(
                    info.imgData, 0, info.imgData.length, options);
        } else {
            return null;
        }
    }

    private Bitmap getThumbnail(Bitmap origin) {
        if (mProperties == null || !mProperties.isNeedThumbnail()) return null;
        int height = origin.getHeight() / (origin.getWidth() / Config.THUMB_SIZE);
        return Bitmap.createScaledBitmap(origin, Config.THUMB_SIZE, height, true);
    }

    public void release() {
//        mListener = null;
//        mContext = null;
    }

    public interface FileListener {
        void onFileSaved(Uri uri, String path, @Nullable Bitmap thumbnail);

        void onFileSaveError(String msg);
    }

    private class ImageInfo {
        byte[] imgData;
        int imgWidth;
        int imgHeight;
        int imgOrientation;
        boolean isFront;
        long imgDate;
        Location imgLocation;
        String imgTitle;
        String imgPath;
        String imgMimeType;
    }
}
