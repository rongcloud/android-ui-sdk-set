package io.rong.imkit.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.StringRes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

import io.rong.common.LibStorageUtils;
import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imlib.common.SavePathUtils;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

/**
 * Created by Android Studio.
 * User: lvhongzhen
 * Date: 2019-11-28
 * Time: 11:43
 */
public class KitStorageUtils {
    private static final String TAG = "LibStorageUtils";

    public static class MediaType {
        public static final String IMAGE = "image";
        public static final String VIDEO = "video";
    }


    public static boolean isScopedStorageMode(Context context) {
        return LibStorageUtils.isScopedStorageMode(context);
    }

    public static boolean isBuildAndTargetForQ(Context context) {
        return LibStorageUtils.isBuildAndTargetForQ(context);
    }

    public static String getImageSavePath(Context context) {
        return getSavePath(context, LibStorageUtils.IMAGE, R.string.rc_image_default_saved_path);
    }


    public static String getVideoSavePath(Context context) {
        return getSavePath(context, LibStorageUtils.VIDEO, R.string.rc_video_default_saved_path);
    }

    public static String getFileSavePath(Context context) {
        return getSavePath(context, LibStorageUtils.FILE, R.string.rc_file_default_saved_path);
    }

    public static String getSavePath(Context context, String type, @StringRes int res) {
        if (!SavePathUtils.isSavePathEmpty()) {
            String savePath = SavePathUtils.getSavePath();
            File imageDir = new File(savePath, type);
            if (!imageDir.exists()) {
                if (!imageDir.mkdirs()) {
                    RLog.e(TAG, "getSavePath mkdirs error path is  " + imageDir.getAbsolutePath());
                }
            }
            return imageDir.getAbsolutePath();
        }
        boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        String result = context.getCacheDir().getPath();
        if (!sdCardExist) {
            RLog.d(TAG, "getSavePath error, sdcard does not exist.");
            return result;
        }
        if (isScopedStorageMode(context)) {
            File path = context.getExternalFilesDir(LibStorageUtils.DIR);
            File file = new File(path, type);
            if (!file.exists() && !file.mkdirs()) {
                result = path.getPath();
            } else {
                result = file.getPath();
            }
        } else {
            String path = Environment.getExternalStorageDirectory().getPath();
            String defaultPath = context.getString(res);
            StringBuilder builder = new StringBuilder(defaultPath);
            String appName = LibStorageUtils.getAppName(context);
            if (!TextUtils.isEmpty(appName)) {
                builder.append(appName).append(File.separator);
            }
            String appPath = builder.toString();
            path = path + appPath;
            final File dir = new File(path);
            if (!dir.exists() && !dir.mkdirs()) {
                RLog.e(TAG, "mkdirs error path is  " + path);
                result = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getPath();
            } else {
                result = path;
            }
        }
        return result;
    }

    /**
     * @param context        上下文
     * @param file           文件
     * @param outputFileName
     */
    private static boolean copyVideoToPublicDir(Context context, File file, String outputFileName) {
        if (file == null || !file.exists()) {
            RLog.e(TAG, "file is not exist");
            return false;
        }
        boolean result = true;
        if (!KitStorageUtils.isBuildAndTargetForQ(context)) {
            File dirFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
            if (dirFile != null && !dirFile.exists()) {
                boolean mkdirResult = dirFile.mkdirs();
                if (!mkdirResult) {
                    RLog.e(TAG, "mkdir fail,dir path is " + dirFile.getAbsolutePath());
                    return false;
                }
            }
            if (dirFile == null) {
                RLog.e(TAG, "dirFile is null");
                return false;
            }

            FileInputStream fis = null;
            FileOutputStream fos = null;
            try {
                String name;
                if (!TextUtils.isEmpty(outputFileName)) {
                    name = outputFileName;
                } else {
                    name = file.getName();
                }
                String filePath = dirFile.getPath() + "/" + name;
                fis = new FileInputStream(file);
                fos = new FileOutputStream(filePath);
                copy(fis, fos);
                File destFile = new File(filePath);
                updatePhotoMedia(destFile, context);
            } catch (FileNotFoundException e) {
                result = false;
                RLog.e(TAG, "copyVideoToPublicDir file not found", e);
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException e) {
                    RLog.e(TAG, "copyVideoToPublicDir: ", e);
                }
                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException e) {
                    RLog.e(TAG, "copyVideoToPublicDir: ", e);
                }
            }
        } else {
            result = copyVideoToPublicDirForQ(context, file, outputFileName);
        }
        return result;
    }

    // 通知图库进行数据刷新
    public static void updatePhotoMedia(File file, Context context) {
        if (file != null && file.exists()) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            context.sendBroadcast(intent);
        }
    }

    private static boolean copyVideoToPublicDirForQ(Context context, File file, String outputFileName) {
        boolean result = true;
        String filePath = "";
        if (file.exists() && file.isFile() && context != null) {
            String name;
            if (!TextUtils.isEmpty(outputFileName)) {
                name = outputFileName;
            } else {
                name = file.getName();
            }
            Uri uri = insertVideoIntoMediaStore(context, name);
            if (uri != null) {
                filePath = uri.getPath();
            }
            try {
                ParcelFileDescriptor w = context.getContentResolver().openFileDescriptor(uri, "w");
                writeToPublicDir(file, w);
            } catch (FileNotFoundException pE) {
                RLog.e(TAG, "copyVideoToPublicDir uri is not Found, uri is" + uri.toString());
                result = false;
            }
            File destFile = new File(filePath);
            updatePhotoMedia(destFile, context);
        } else {
            RLog.e(TAG, "file is not Found or context is null ");
            result = false;
        }
        return result;
    }

    private static boolean copyImageToPublicDir(Context pContext, File pFile, String outputFileName) {
        boolean result = true;
        File file = pFile;
        if (file.exists() && file.isFile() && pContext != null) {
            if (!KitStorageUtils.isBuildAndTargetForQ(pContext)) {
                File dirFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                if (dirFile != null && !dirFile.exists()) {
                    boolean mkdirResult = dirFile.mkdirs();
                    if (!mkdirResult) {
                        RLog.e(TAG, "mkdir fail,dir path is " + dirFile.getAbsolutePath());
                        return false;
                    }
                }
                if (dirFile == null) {
                    RLog.e(TAG, "dirFile is null");
                    return false;
                }

                FileInputStream fis = null;
                FileOutputStream fos = null;
                try {
                    String name;
                    if (!TextUtils.isEmpty(outputFileName)) {
                        name = outputFileName;
                    } else {
                        String imgMimeType = getImgMimeType(file);
                        int i = imgMimeType.lastIndexOf('/');
                        name = "Rong_Image_" + System.currentTimeMillis();
                        if (i != -1) {
                            name = name + "." + imgMimeType.substring(i + 1);
                        }
                    }

                    String filePath = dirFile.getPath() + "/" + name;
                    fis = new FileInputStream(file);
                    fos = new FileOutputStream(filePath);
                    copy(fis, fos);
                    File destFile = new File(filePath);
                    updatePhotoMedia(destFile, pContext);
                } catch (FileNotFoundException e) {
                    result = false;
                    RLog.e(TAG, "copyImageToPublicDir file not found", e);
                } finally {
                    try {
                        if (fis != null) {
                            fis.close();
                        }
                    } catch (IOException e) {
                        RLog.e(TAG, "copyImageToPublicDir: ", e);
                    }
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        RLog.e(TAG, "copyImageToPublicDir: ", e);
                    }
                }
            } else {
                String name;
                String imgMimeType = getImgMimeType(file);
                if (!TextUtils.isEmpty(outputFileName)) {
                    name = outputFileName;
                } else {
                    int i = imgMimeType.lastIndexOf('/');
                    name = "Rong_Image_" + System.currentTimeMillis();
                    if (i != -1) {
                        name = name + "." + imgMimeType.substring(i + 1);
                    }
                }
                Uri uri = insertImageIntoMediaStore(pContext, name, imgMimeType);
                try {
                    ParcelFileDescriptor w = pContext.getContentResolver().openFileDescriptor(uri, "w");
                    writeToPublicDir(file, w);
                } catch (FileNotFoundException pE) {
                    result = false;
                    RLog.e(TAG, "copyImageToPublicDir uri is not Found, uri is" + uri.toString());
                }
            }
        } else {
            result = false;
            RLog.e(TAG, "file is not Found or context is null ");
        }
        return result;
    }

    public static Uri insertImageIntoMediaStore(Context context, String fileName, String mimeType) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        return uri;
    }

    public static Uri insertVideoIntoMediaStore(Context context, String fileName) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
        contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");

        Uri uri = context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
        return uri;
    }

    public static void writeToPublicDir(File pFile, ParcelFileDescriptor pParcelFileDescriptor) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(pFile);
            fos = new FileOutputStream(pParcelFileDescriptor.getFileDescriptor());
            copy(fis, fos);
        } catch (FileNotFoundException pE) {
            RLog.e(TAG, "writeToPublicDir file is not found file path is " + pFile.getAbsolutePath());
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                RLog.e(TAG, "writeToPublicDir: ", e);
            }
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                RLog.e(TAG, "writeToPublicDir: ", e);
            }
        }
    }

    public static void read(ParcelFileDescriptor parcelFileDescriptor, File dst) throws IOException {
        FileInputStream istream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
        try {
            FileOutputStream ostream = new FileOutputStream(dst);
            try {
                copy(istream, ostream);
            } finally {
                ostream.close();
            }
        } finally {
            istream.close();
        }
    }

    public static void copy(FileInputStream ist, FileOutputStream ost) {
        if (ist == null || ost == null)
            return;
        FileChannel fileChannelInput = null;
        FileChannel fileChannelOutput = null;
        try {
            //得到fileInputStream的文件通道
            fileChannelInput = ist.getChannel();
            //得到fileOutputStream的文件通道
            fileChannelOutput = ost.getChannel();
            //将fileChannelInput通道的数据，写入到fileChannelOutput通道
            fileChannelInput.transferTo(0, fileChannelInput.size(), fileChannelOutput);
        } catch (IOException e) {
            RLog.e(TAG, "copy method error", e);
        } finally {
            try {
                ist.close();
                if (fileChannelInput != null)
                    fileChannelInput.close();
                ost.close();
                if (fileChannelOutput != null)
                    fileChannelOutput.close();
            } catch (IOException e) {
                RLog.e(TAG, "copy method error", e);
            }
        }
    }

    public static String getImgMimeType(File imgFile) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgFile.getPath(), options);
        return options.outMimeType;
    }

    /**
     * @param type MediaStore类型，0：Images，1：Video，2：Audio
     * @param id   通过扫描获取到的MediaStore."xxx".Media._ID
     * @return content uri
     */
    public Uri getContentUri(int type, String id) {
        Uri uri;
        switch (type) {
            case 0:
                uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
                break;
            case 1:
                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
                break;
            case 2:
                uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
                break;
            default:
                uri = null;
        }
        return uri;
    }

    public InputStream getFileInputStreamWithUri(Context pContext, Uri pUri) {
        InputStream inputStream = null;
        ContentResolver cr = pContext.getContentResolver();
        try {
            AssetFileDescriptor r = cr.openAssetFileDescriptor(pUri, "r");
            ParcelFileDescriptor parcelFileDescriptor = r.getParcelFileDescriptor();
            if (parcelFileDescriptor != null) {
                inputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
            }
        } catch (FileNotFoundException e) {
            RLog.e(TAG, "getFileInputStreamWithUri: ", e);
        }
        return inputStream;
    }

    /**
     * @param context 上下文
     * @param file    文件
     * @param type    KitStorageUtils.MediaType
     * @return 保存媒体数据到公有目录返回结果
     */
    public static boolean saveMediaToPublicDir(Context context, File file, String type) {
        return saveMediaToPublicDir(context, file, null, type);
    }

    /**
     * @param context        上下文
     * @param file           文件
     * @param outputFileName 输出的文件名，不包含路径
     * @param type           KitStorageUtils.MediaType
     * @return 保存媒体数据到公有目录返回结果
     */
    public static boolean saveMediaToPublicDir(Context context, File file, String outputFileName, String type) {
        if (MediaType.IMAGE.equals(type)) {
            return copyImageToPublicDir(context, file, outputFileName);
        } else if (MediaType.VIDEO.equals(type)) {
            return copyVideoToPublicDir(context, file, outputFileName);
        } else {
            RLog.i(TAG, "type is error");
            return false;
        }
    }
}
