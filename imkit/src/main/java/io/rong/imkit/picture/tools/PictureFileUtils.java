package io.rong.imkit.picture.tools;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import io.rong.common.CursorUtils;
import io.rong.common.rlog.RLog;
import io.rong.imkit.picture.config.PictureConfig;
import io.rong.imkit.picture.config.PictureMimeType;
import io.rong.imkit.picture.permissions.PermissionChecker;
import io.rong.imkit.utils.AndroidConstant;
import io.rong.imkit.utils.PermissionCheckUtil;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

public class PictureFileUtils {

    public static final String POSTFIX = ".jpg";
    public static final String POST_VIDEO = ".mp4";
    public static final String POST_AUDIO = ".mp3";

    /**
     * @param context
     * @param type
     * @param format
     * @return
     */
    public static File createCameraFile(Context context, int type, String fileName, String format) {
        return createMediaFile(context, type, fileName, format);
    }

    /**
     * 创建文件
     *
     * @param context
     * @param chooseMode
     * @param fileName
     * @param format
     * @return
     */
    private static File createMediaFile(
            Context context, int chooseMode, String fileName, String format) {
        return createOutFile(context, chooseMode, fileName, format);
    }

    private static File createOutFile(
            Context context, int chooseMode, String fileName, String format) {
        File rootDir = getRootDirFile(context, chooseMode);
        if (rootDir != null && !rootDir.exists() && rootDir.mkdirs()) {
            // default implementation ignored
        }

        File folderDir = null;
        if (rootDir != null) {
            folderDir =
                    new File(
                            rootDir.getAbsolutePath() + File.separator + "Camera" + File.separator);
        }
        if (folderDir != null && !folderDir.exists() && folderDir.mkdirs()) {
            // default implementation ignored
        }
        boolean isOutFileNameEmpty = TextUtils.isEmpty(fileName);
        switch (chooseMode) {
            case PictureConfig.TYPE_VIDEO:
                String newFileVideoName =
                        isOutFileNameEmpty
                                ? DateUtils.getInstance().getCreateFileName("VID_") + POST_VIDEO
                                : fileName;
                return new File(folderDir, newFileVideoName);
            default:
                String suffix = TextUtils.isEmpty(format) ? POSTFIX : format;
                String newFileImageName =
                        isOutFileNameEmpty
                                ? DateUtils.getInstance().getCreateFileName("IMG_") + suffix
                                : fileName;
                return new File(folderDir, newFileImageName);
        }
    }

    /**
     * 文件根目录
     *
     * @param context
     * @param type
     * @return
     */
    private static File getRootDirFile(Context context, int type) {

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return context.getExternalFilesDir(Environment.DIRECTORY_DCIM);
        }

        switch (type) {
            case PictureConfig.TYPE_VIDEO:
                return context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            default:
                return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        }
    }

    /** TAG for log messages. */
    static final String TAG = "PictureFileUtils";

    private PictureFileUtils() {
        // default implementation ignored
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     * @author paulburke
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     * @author paulburke
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     * @author paulburke
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * Get the value of the data column for this Uri. This is useful for MediaStore Uris, and other
     * file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     * @author paulburke
     */
    public static String getDataColumn(
            Context context, Uri uri, String selection, String[] selectionArgs) {
        if (!PermissionCheckUtil.checkMediaStoragePermissions(context)) {
            if (Build.VERSION.SDK_INT >= AndroidConstant.ANDROID_UPSIDE_DOWN_CAKE) {
                String[] subPermissions =
                        new String[] {Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED};
                if (!PermissionChecker.checkSelfPermission(context, subPermissions)) {
                    return "";
                }
            } else {
                return "";
            }
        }
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = CursorUtils.query(context, uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (Exception ex) {
            RLog.i(
                    TAG,
                    String.format(
                            Locale.getDefault(), "getDataColumn: _data - [%s]", ex.getMessage()));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access Framework
     * Documents, as well as the _data field for the MediaStore and other file-based
     * ContentProviders.<br>
     * <br>
     * Callers should check whether the path is local before assuming it represents a local file.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    if (SdkVersionUtils.checkedAndroid_Q()) {
                        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                                + "/"
                                + split[1];
                    } else {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri =
                        ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"),
                                Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static long getMediaSize(Context context, String path) {
        if (path.startsWith("content://")) {
            DocumentFile df = DocumentFile.fromSingleUri(context, Uri.parse(path));
            if (df != null) {
                return df.length();
            } else {
                return 0;
            }
        } else if (path.startsWith("file://")) {
            return new File(path.substring(7)).length();
        } else {
            return new File(path).length();
        }
    }

    /**
     * 读取图片属性：旋转的角度
     *
     * @param path 图片绝对路径
     * @return degree旋转的角度
     */
    public static int readPictureDegree(Context context, String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface;
            if (SdkVersionUtils.checkedAndroid_Q()) {
                exifInterface =
                        new ExifInterface(
                                context.getContentResolver().openInputStream(Uri.parse(path)));
            } else {
                exifInterface = new ExifInterface(path);
            }
            int orientation =
                    exifInterface.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
                default:
                    degree = 90;
                    break;
            }
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
        }
        return degree;
    }

    /**
     * 旋转Bitmap
     *
     * @param angle
     * @param bitmap
     * @return
     */
    public static Bitmap rotatingImageView(int angle, Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        // 旋转图片 动作
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        // 创建新的图片
        Bitmap resizedBitmap =
                Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizedBitmap;
    }

    public static void saveBitmapFile(Bitmap bitmap, File file) {
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
        }
    }

    /**
     * 创建文件夹
     *
     * @param filename
     * @return
     */
    public static String createDir(Context context, String filename) {
        File rootDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!rootDir.exists())
        // 若不存在，创建目录，可以在应用启动的时候创建
        {
            rootDir.mkdirs();
        }
        return rootDir + "/" + filename;
    }

    public static String getDCIMCameraPath(Context ctx, String mimeType) {
        String absolutePath;
        try {
            if (SdkVersionUtils.checkedAndroid_Q()) {
                if (PictureMimeType.eqVideo(mimeType)) {
                    absolutePath = "%" + ctx.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
                } else {
                    absolutePath = "%" + ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                }
            } else {
                absolutePath =
                        "%"
                                + Environment.getExternalStoragePublicDirectory(
                                                Environment.DIRECTORY_DCIM)
                                        .getAbsolutePath()
                                + "/Camera";
            }
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
            return "";
        }
        return absolutePath;
    }

    /**
     * set empty PictureSelector Cache
     *
     * @param mContext
     * @param type image or video ...
     */
    public static void deleteCacheDirFile(Context mContext, int type) {
        File cutDir =
                mContext.getExternalFilesDir(
                        type == PictureMimeType.ofImage()
                                ? Environment.DIRECTORY_PICTURES
                                : Environment.DIRECTORY_MOVIES);
        if (cutDir != null) {
            File[] files = cutDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        file.delete();
                    }
                }
            }
        }
    }

    /**
     * set empty PictureSelector Cache
     *
     * @param context
     */
    public static void deleteAllCacheDirFile(Context context) {

        File dirPictures = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (dirPictures != null) {
            File[] files = dirPictures.listFiles();
            for (File file : files) {
                if (file.isFile()) {
                    file.delete();
                }
            }
        }

        File dirMovies = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (dirPictures != null) {
            File[] files = dirMovies.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        file.delete();
                    }
                }
            }
        }

        File dirMusic = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (dirPictures != null) {
            File[] files = dirMusic.listFiles();
            for (File file : files) {
                if (file.isFile()) {
                    file.delete();
                }
            }
        }
    }

    /**
     * @param ctx
     * @return
     */
    public static String getDiskCacheDir(Context ctx) {
        return ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath();
    }

    /**
     * 生成uri
     *
     * @param context
     * @param cameraFile
     * @return
     */
    public static Uri parUri(Context context, File cameraFile) {
        Uri imageUri;
        String authority = context.getPackageName() + ".provider";
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            // 通过FileProvider创建一个content类型的Uri
            imageUri = FileProvider.getUriForFile(context, authority, cameraFile);
        } else {
            imageUri = Uri.fromFile(cameraFile);
        }
        return imageUri;
    }

    /**
     * 获取图片后缀
     *
     * @param input
     * @return
     */
    public static String extSuffix(InputStream input) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            return options.outMimeType.replace("image/", ".");
        } catch (Exception e) {
            return PictureMimeType.JPEG;
        }
    }

    /**
     * 判断拍照 图片是否旋转
     *
     * @param degree
     * @param path
     */
    public static void rotateImage(int degree, String path) {
        if (degree > 0) {
            try {
                // 针对相片有旋转问题的处理方式
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 2;
                File file = new File(path);
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
                Bitmap bmp = PictureFileUtils.rotatingImageView(degree, bitmap);
                if (bmp != null) {
                    PictureFileUtils.saveBitmapFile(bmp, file);
                }
            } catch (Exception e) {
                RLog.e(TAG, e.getMessage());
                ;
            }
        }
    }

    /**
     * 判断拍照 图片是否旋转
     *
     * @param degree
     * @param path
     */
    public static String rotateImageToAndroidQ(
            Context context, int degree, String path, String newFileName) {
        if (degree > 0) {
            try {
                // 针对相片有旋转问题的处理方式
                if (SdkVersionUtils.checkedAndroid_Q()) {
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inSampleSize = 2;
                    ParcelFileDescriptor parcelFileDescriptor =
                            context.getContentResolver().openFileDescriptor(Uri.parse(path), "r");
                    FileInputStream inputStream =
                            new FileInputStream(parcelFileDescriptor.getFileDescriptor());
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, opts);
                    String suffix = PictureFileUtils.extSuffix(inputStream);
                    Bitmap bmp = PictureFileUtils.rotatingImageView(degree, bitmap);
                    if (bmp != null) {
                        String dir =
                                createDir(
                                        context,
                                        TextUtils.isEmpty(newFileName)
                                                ? DateUtils.getInstance().getCreateFileName("IMG_")
                                                        + suffix
                                                : newFileName);
                        PictureFileUtils.saveBitmapFile(bmp, new File(dir));
                        return dir;
                    }
                }
            } catch (Exception e) {
                RLog.e(TAG, e.getMessage());
            }
        }
        return "";
    }
}
