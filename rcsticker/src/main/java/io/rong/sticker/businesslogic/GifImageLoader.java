package io.rong.sticker.businesslogic;

import android.os.Handler;
import android.util.LruCache;
import io.rong.common.rlog.RLog;
import io.rong.sticker.message.StickerMessage;
import io.rong.sticker.model.Sticker;
import io.rong.sticker.util.FileUtil;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Created by luoyanlong on 2018/08/17. 工作线程解码Gif文件 */
public class GifImageLoader {

    private static final String SEPARATOR = " ";
    private static final int CACHE_SIZE = 5 * 1024 * 1024; // 最大缓存5MB
    private static final String TAG = GifImageLoader.class.getSimpleName();

    private ExecutorService executorService = Executors.newCachedThreadPool();

    private LruCache<String, byte[]> cache =
            new LruCache<String, byte[]>(CACHE_SIZE) {
                @Override
                protected int sizeOf(String key, byte[] bytes) {
                    return bytes.length;
                }

                @Override
                protected synchronized byte[] create(String key) {
                    String[] strings = key.split(SEPARATOR);
                    String packageId = strings[0];
                    String stickerId = strings[1];
                    try {
                        return FileUtil.toByteArray(
                                StickerPackageStorageTask.getStickerImageFilePath(
                                        packageId, stickerId));
                    } catch (IOException e) {
                        RLog.e(TAG, e.getMessage());
                        return null;
                    }
                }
            };

    private static GifImageLoader instance;

    public static synchronized GifImageLoader getInstance() {
        if (instance == null) {
            instance = new GifImageLoader();
        }
        return instance;
    }

    private GifImageLoader() {
        // default implementation ignored
    }

    public void obtain(Sticker sticker, SimpleCallback callback) {
        Worker worker = new Worker(sticker, callback);
        executorService.submit(worker);
    }

    public void obtain(StickerMessage stickerMessage, SimpleCallback callback) {
        Worker worker = new Worker(stickerMessage, callback);
        executorService.submit(worker);
    }

    /** 清空缓存 */
    public void clear() {
        cache.evictAll();
    }

    private class Worker implements Runnable {

        private String packageId;
        private String stickerId;
        private Callback callback;

        Worker(StickerMessage stickerMessage, Callback callback) {
            packageId = stickerMessage.getPackageId();
            stickerId = stickerMessage.getStickerId();
            this.callback = callback;
        }

        Worker(Sticker sticker, Callback callback) {
            packageId = sticker.getPackageId();
            stickerId = sticker.getStickerId();
            this.callback = callback;
        }

        @Override
        public void run() {
            if (!StickerPackageStorageTask.isStickerExist(packageId, stickerId)) {
                StickerDownloadTask task = new StickerDownloadTask(packageId, stickerId);
                task.downloadSticker();
            }
            byte[] bytes = cache.get(createKey());
            callback.onResult(bytes);
        }

        private String createKey() {
            return packageId + SEPARATOR + stickerId;
        }
    }

    public interface Callback {
        void onResult(byte[] bytes);
    }

    public abstract static class SimpleCallback implements Callback {

        @Override
        public void onResult(final byte[] bytes) {
            Handler handler = StickerPackagesUiHandler.getUiHandler();
            if (handler != null) {
                if (bytes != null) {
                    handler.post(
                            new Runnable() {
                                @Override
                                public void run() {
                                    onSuccess(bytes);
                                }
                            });
                } else {
                    handler.post(
                            new Runnable() {
                                @Override
                                public void run() {
                                    onFail();
                                }
                            });
                }
            }
        }

        public abstract void onSuccess(byte[] bytes);

        public abstract void onFail();
    }
}
