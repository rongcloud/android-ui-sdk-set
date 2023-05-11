package io.rong.sticker.businesslogic;

import androidx.annotation.Nullable;
import io.rong.sticker.model.Sticker;
import io.rong.sticker.model.StickerPackageDownloadUrlInfo;
import io.rong.sticker.model.StickerPackagesConfigInfo;
import io.rong.sticker.util.HttpUtil;
import io.rong.sticker.util.SHA1Util;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Created by luoyanlong on 2018/08/15. */
public class StickerPackageApiTask {

    private static final String HOST = "https://stickerservice.ronghub.com/";

    /** 获取全局配置文件 */
    private static final String ALL_CONFIG = "emoticonservice/emopkgs";

    private static final String PACKAGE_DOWNLOAD_URL = "emoticonservice/emopkgs/%s";
    private static final String GET_STICKER_URL = "emoticonservice/emopkgs/%s/stickers/%s";
    private static String sAppKey;
    private static ExecutorService service = Executors.newCachedThreadPool();
    private static Random random = new Random();

    public static void init(String appKey) {
        sAppKey = appKey;
    }

    public static void getAllConfig(HttpUtil.Callback<StickerPackagesConfigInfo> callback) {
        final String url = getUrl(ALL_CONFIG);
        service.submit(new Worker<>(url, callback));
    }

    public static void getStickerPackageDownloadUrl(
            String packageId, HttpUtil.Callback<StickerPackageDownloadUrlInfo> callback) {
        String s = String.format(PACKAGE_DOWNLOAD_URL, packageId);
        String url = getUrl(s);
        service.submit(new Worker<>(url, callback));
    }

    /** 没有网络返回null */
    @Nullable
    public static Sticker getStickerSync(String packageId, String stickerId) {
        String s = String.format(GET_STICKER_URL, packageId, stickerId);
        String url = getUrl(s);
        try {
            return HttpUtil.get(url, createHeader(), Sticker.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getUrl(String api) {
        return HOST + api;
    }

    private static class Worker<T> implements Runnable {

        private String url;
        private HttpUtil.Callback<T> callback;

        Worker(String url, HttpUtil.Callback<T> callback) {
            this.url = url;
            this.callback = callback;
        }

        @Override
        public void run() {
            HttpUtil.get(url, createHeader(), callback);
        }
    }

    private static Map<String, String> createHeader() {
        Map<String, String> map = new HashMap<>();
        String nonce = Integer.toString(random.nextInt(10000));
        String timestamp = Long.toString(System.currentTimeMillis());
        String signature = SHA1Util.SHA1(SHA1Util.SHA1(sAppKey) + nonce + timestamp);
        map.put("AppKey", sAppKey);
        map.put("Nonce", nonce);
        map.put("Timestamp", timestamp);
        map.put("Signature", signature);
        return map;
    }
}
