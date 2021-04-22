package io.rong.imkit.utils.videocompressor;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.File;

import io.rong.common.FileUtils;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imkit.utils.KitStorageUtils;
import io.rong.imkit.utils.videocompressor.videoslimmer.listner.SlimProgressListener;

/**
 * Created by Vincent Woo
 * Date: 2017/8/16
 * Time: 15:15
 */

public class VideoCompress {
    private static final String TAG = VideoCompress.class.getSimpleName();
    private static final String CACHE = "/cache_";

    public static void compressVideo(final Context context, final String srcPath, final String destPath, final CompressListener listener) {
        ExecutorHelper.getInstance().compressExecutor().execute(new Runnable() {
            @Override
            public void run() {
                ExecutorHelper.getInstance().mainThread().execute(new Runnable() {
                    @Override
                    public void run() {
                        listener.onStart();
                    }
                });
                String path = KitStorageUtils.getVideoSavePath(context) + CACHE + System.currentTimeMillis() + ".mp4";
                Uri srcUri = Uri.parse(srcPath);
                boolean isContent = false;
                // content 类型先 copy 一份缓存
                if (FileUtils.uriStartWithContent(srcUri)) {
                    boolean result = FileUtils.copyFile(context, srcUri, path);
                    //拷贝失败直接返回失败
                    if (result) {
                        isContent = true;

                    } else {
                        ExecutorHelper.getInstance().mainThread().execute(new Runnable() {
                            @Override
                            public void run() {
                                listener.onFail();
                            }
                        });
                        return;
                    }
                } else if (FileUtils.uriStartWithFile(srcUri)) {
                    path = srcPath.substring(7);
                } else {
                    path = srcPath;
                }
                boolean result = VideoController.getInstance().convertVideo(path, destPath, new SlimProgressListener() {
                    @Override
                    public void onProgress(final float percent) {
                        ExecutorHelper.getInstance().mainThread().execute(new Runnable() {
                            @Override
                            public void run() {
                                listener.onProgress(percent);
                            }
                        });

                    }
                });
                if (result) {
                    ExecutorHelper.getInstance().mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onSuccess();
                        }
                    });

                } else {
                    ExecutorHelper.getInstance().mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onFail();
                        }
                    });
                }
                //删除临时文件
                if (isContent) {
                    new File(path).delete();
                }
            }
        });
    }

    public interface CompressListener {
        void onStart();

        void onSuccess();

        void onFail();

        void onProgress(float percent);
    }
}
