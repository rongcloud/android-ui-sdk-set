package io.rong.imkit.utils.videocompressor;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import io.rong.common.RLog;
import io.rong.imkit.utils.videocompressor.videoslimmer.VideoSlimEncoder;
import io.rong.imkit.utils.videocompressor.videoslimmer.listner.SlimProgressListener;

@SuppressLint("NewApi")
public class VideoController {
    private final static String TAG = VideoController.class.getSimpleName();

    private VideoController() {

    }

    private static class SingletonHolder {
        static VideoController sInstance = new VideoController();
    }

    public static VideoController getInstance() {
        return SingletonHolder.sInstance;
    }


    public static void copyFile(File src, File dst) {
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            fileInputStream = new FileInputStream(src);
            fileOutputStream = new FileOutputStream(dst);
            inChannel = fileInputStream.getChannel();
            outChannel = fileOutputStream.getChannel();
            inChannel.transferTo(1, inChannel.size(), outChannel);
        } catch (Exception e) {
            RLog.e(TAG, "copyFile", e);
        } finally {
            try {
                if (inChannel != null) {
                    inChannel.close();
                }
            } catch (IOException e) {
                RLog.e(TAG, "copyFile inChannel close", e);
            }
            try {
                if (outChannel != null) {
                    outChannel.close();
                }
            } catch (IOException e) {
                RLog.e(TAG, "copyFile outChannel close", e);
            }

            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                RLog.e(TAG, "copyFile fileInputStream close", e);
            }

            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                RLog.e(TAG, "copyFile fileOutputStream close", e);
            }
        }
    }


    /**
     * 不支持content和file，请转换后再使用
     *
     * @param sourcePath      the source uri for the file as per
     * @param destinationPath the destination directory where compressed video is eventually saved
     * @return
     */
    @TargetApi(16)
    public boolean convertVideo(final String sourcePath, String destinationPath, SlimProgressListener listener) {
        if (!new File(sourcePath).exists()) {
            return false;
        }
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(sourcePath);
        } catch (Exception e) {
            RLog.e(TAG, e.toString());
            return false;
        }
        String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        if (TextUtils.isEmpty(width) || TextUtils.isEmpty(height) || TextUtils.isEmpty(rotation)) {
            return false;
        }
        long startTime = -1;

        int rotationValue = Integer.valueOf(rotation);
        int originalWidth = Integer.valueOf(width);
        int originalHeight = Integer.valueOf(height);
        RLog.d(TAG, "Resolution of origin width is " + originalWidth);
        RLog.d(TAG, "Resolution of origin height is " + originalHeight);
        RLog.d(TAG, "Origin rotation value is " + rotationValue);
        // 视频的分辨率需均为 16 的倍数，防止 YUV 格式的视频解码后出现花屏
        if (originalWidth % 16 != 0) {
            int quotient = originalWidth / 16;
            originalWidth = 16 * quotient;
        }
        if (originalHeight % 16 != 0) {
            int quotient = originalHeight / 16;
            originalHeight = 16 * quotient;
        }

        int resultWidth;
        int resultHeight;
        // 加入横竖屏判断，竖屏压成960x544,横屏压成544x960
        if (originalHeight >= originalWidth) {
            if (originalHeight <= 960 && originalWidth <= 544) {
                if (originalHeight == 960 && originalWidth == 544) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        resultWidth = originalWidth - 16;
                        resultHeight = originalHeight - 16;
                    } else {
                        resultWidth = originalWidth;
                        resultHeight = originalHeight;
                    }

                } else {
                    resultWidth = originalWidth;
                    resultHeight = originalHeight;
                }
            } else if (originalHeight <= 960 && originalWidth > 544) {
                resultWidth = 544;
                double ratio = (double) originalHeight / originalWidth;
                int quotient = (int) (ratio * resultWidth) / 16;
                resultHeight = 16 * quotient;
            } else {
                resultHeight = 960;
                double ratio = (double) originalWidth / originalHeight;
                int quotient = (int) (ratio * resultHeight) / 16;
                resultWidth = 16 * quotient;
            }
        } else {
            // 将压缩视频的分辨率最大值设为 544x960
            if (originalWidth <= 960 && originalHeight <= 544) {
                resultWidth = originalWidth;
                resultHeight = originalHeight;
            } else if (originalWidth <= 960 && originalHeight > 544) {
                resultHeight = 544;
                double ratio = (double) originalWidth / originalHeight;
                int quotient = (int) (ratio * resultHeight) / 16;
                resultWidth = 16 * quotient;
            } else {
                resultWidth = 960;
                double ratio = (double) originalHeight / originalWidth;
                int quotient = (int) (ratio * resultWidth) / 16;
                resultHeight = 16 * quotient;
            }
        }


        int bitrate = (resultWidth / 2) * (resultHeight / 2) * 10;
        int rotateRender = 0;

        if (Build.VERSION.SDK_INT < 18 && resultHeight > resultWidth && resultWidth != originalWidth && resultHeight != originalHeight) {
            int temp = resultHeight;
            resultHeight = resultWidth;
            resultWidth = temp;
            rotationValue = 90;
            rotateRender = 270;
        } else if (Build.VERSION.SDK_INT > 20) {
            if (rotationValue == 90) {
                int temp = resultHeight;
                resultHeight = resultWidth;
                resultWidth = temp;
                rotationValue = 0;
                rotateRender = 270;
            } else if (rotationValue == 180) {
                rotateRender = 180;
                rotationValue = 0;
            } else if (rotationValue == 270) {
                int temp = resultHeight;
                resultHeight = resultWidth;
                resultWidth = temp;
                rotationValue = 0;
                rotateRender = 90;
            }
        }
        RLog.d(TAG, "Resolution of result width is " + resultWidth);
        RLog.d(TAG, "Resolution of result height is " + resultHeight);
        RLog.d(TAG, "Result rotation value is " + rotationValue);
        RLog.d(TAG, "Result render value is " + rotateRender);

        //大于18选择最新的编解码
        if (resultWidth != 0 && resultHeight != 0 && Build.VERSION.SDK_INT >= 18) {
            try {
                boolean result;
                //进行第一次压缩，如果失败，尝试宽高+16进行，第二次压缩，+16是为了防止花屏，第二次压缩失败，则不再继续，返回失败结果
                result = new VideoSlimEncoder().convertVideo(sourcePath, destinationPath, resultWidth, resultHeight, bitrate, listener);
                if (!result) {
                    File file = new File(destinationPath);
                    if (file != null && file.exists()) {
                        boolean delete = file.delete();
                        RLog.d(TAG, "delete:" + delete);
                    }
                    resultWidth += 16;
                    resultHeight += 16;
                    result = new VideoSlimEncoder().convertVideo(sourcePath, destinationPath, resultWidth, resultHeight, (resultWidth / 2) * (resultHeight / 2) * 10, listener);
                }
                return result;
            } catch (Exception e) {
                RLog.e(TAG, "compress fail", e);
                return false;
            }
        }
        return true;
    }
}