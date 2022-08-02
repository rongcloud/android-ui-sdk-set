package io.rong.sight.record;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.rong.common.RLog;

/**
 * 445263848@qq.com.
 */
public class FileUtil {
    private static final String TAG = "FileUtil";
    private static final File parentPath = Environment.getExternalStorageDirectory();
    private static String storagePath = "";
    private static final String DST_FOLDER_NAME = "PlayCamera";

    private static String initPath() {
        if (storagePath.equals("")) {
            storagePath = parentPath.getAbsolutePath() + "/" + DST_FOLDER_NAME;
            File f = new File(storagePath);
            if (!f.exists()) {
                boolean mkdirSuccess = f.mkdir();
                if (!mkdirSuccess) {
                    Log.e(TAG, "initPath mkdir failed");
                }
            }
        }
        return storagePath;
    }

    public static void saveBitmap(Bitmap b) {
        String path = initPath();
        long dataTake = System.currentTimeMillis();
        String jpegName = path + "/" + dataTake + ".jpg";
        Log.i(TAG, "saveBitmap:jpegName = " + jpegName);
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(jpegName);
            BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream);
            b.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
            Log.i(TAG, "saveBitmap success");
        } catch (IOException e) {
            Log.i(TAG, "saveBitmap:fail");
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    RLog.e(TAG, "saveBitmap: fileOutputStream close!", e);
                }
            }
        }

    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
