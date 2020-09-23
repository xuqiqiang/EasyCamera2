package com.snailstudio2010.camera2.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.Log;

import com.snailstudio2010.camera2.Config;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by xuqiqiang on 9/6/17.
 */

public class Storage {
    private static final String TAG = Config.getTag(Storage.class);
    public static final String DCIM =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();

    public static String DIRECTORY = DCIM + "/Camera";

    public static final long UNAVAILABLE = -1L;
    public static final long PREPARING = -2L;
    public static final long UNKNOWN_SIZE = -3L;
    public static final long LOW_STORAGE_THRESHOLD = 50000000; // 50M


    public static void writeFile(String path, byte[] data) throws IOException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            out.write(data);
        } catch (Exception e) {
            Logger.e(TAG, "error to write data", e);
            throw e;
        } finally {
            close(out);
        }
    }

    public static void writeBitmap(String path, Bitmap bitmap, int quality) throws IOException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            out.flush();
        } catch (Exception e) {
            Logger.e(TAG, "error to write data", e);
            throw e;
        } finally {
            close(out);
        }
    }

    public static void writeFile(File file, byte[] data) throws IOException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(data);
        } catch (Exception e) {
            Logger.e(TAG, "error to write data", e);
            throw e;
        } finally {
            close(out);
        }
    }

    // Add the image to media store.
    public static Uri addImageToDB(ContentResolver resolver, String title, long date,
                                   Location location, int orientation, long jpegLength,
                                   String path, int width, int height, String mimeType) {
        // Insert into MediaStore.
        ContentValues values = new ContentValues(11);
        values.put(MediaStore.Images.ImageColumns.TITLE, title);
        values.put(MediaStore.MediaColumns.WIDTH, width);
        values.put(MediaStore.MediaColumns.HEIGHT, height);
        if (mimeType.equalsIgnoreCase("jpeg")
                || mimeType.equalsIgnoreCase("image/jpeg")) {
            values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, title + ".jpg");
        } else {
            values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, title + ".raw");
        }
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date);
        values.put(MediaStore.Images.ImageColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.Images.ImageColumns.ORIENTATION, orientation);
        values.put(MediaStore.Images.ImageColumns.DATA, path);
        values.put(MediaStore.Images.ImageColumns.SIZE, jpegLength);
        if (location != null) {
            values.put(MediaStore.Images.ImageColumns.LATITUDE, location.getLatitude());
            values.put(MediaStore.Images.ImageColumns.LONGITUDE, location.getLongitude());
        }
        return insert(resolver, values, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    }

    // Add the video to media store.
    public static Uri addVideoToDB(ContentResolver resolver, String title, long date,
                                   Location location, long length, String path,
                                   int width, int height, String mimeType) {
        // Insert into MediaStore.
        ContentValues values = new ContentValues(10);
        values.put(MediaStore.Video.VideoColumns.TITLE, title);
        values.put(MediaStore.MediaColumns.WIDTH, width);
        values.put(MediaStore.MediaColumns.HEIGHT, height);
        values.put(MediaStore.Video.VideoColumns.DISPLAY_NAME, title + ".mp4");
        values.put(MediaStore.Video.VideoColumns.DATE_TAKEN, date);
        values.put(MediaStore.Video.VideoColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.Video.VideoColumns.DATA, path);
        values.put(MediaStore.Video.VideoColumns.SIZE, length);
        if (location != null) {
            values.put(MediaStore.Video.VideoColumns.LATITUDE, location.getLatitude());
            values.put(MediaStore.Video.VideoColumns.LONGITUDE, location.getLongitude());
        }
        return insert(resolver, values, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
    }

    private static Uri insert(ContentResolver resolver, ContentValues values, Uri targetUri) {
        Uri uri = null;
        try {
            uri = resolver.insert(targetUri, values);
        } catch (Throwable th) {
            Logger.e(TAG, "error to write MediaStore:" + th);
        }
        return uri;
    }

    private static void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Throwable e) {
                Logger.e(TAG, "error to close file after write", e);
            }
        }
    }

    public static long getAvailableSpace() {
        String state = Environment.getExternalStorageState();
        Log.d(TAG, "External storage state=" + state);
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return PREPARING;
        }
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return UNAVAILABLE;
        }

        File dir = new File(DIRECTORY);
        dir.mkdirs();
        if (!dir.isDirectory() || !dir.canWrite()) {
            return UNAVAILABLE;
        }

        try {
            StatFs stat = new StatFs(DIRECTORY);
            return stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {
            Log.i(TAG, "Error to access external storage", e);
        }
        return UNKNOWN_SIZE;
    }

}
