package io.rong.imkit.utils;

import android.net.Uri;
import android.text.TextUtils;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import io.rong.imlib.filetransfer.upload.MediaUploadAuthorInfo;
import java.util.HashMap;
import java.util.Map;

/**
 * Glide工具类
 *
 * @author chenjialong
 */
public class GlideUtils {

    /**
     * 如果是私有云，则需要获取私有云Token放到图片请求header中
     *
     * @param originalUri
     * @return
     */
    public static Object buildAuthUrl(Uri originalUri, MediaUploadAuthorInfo auth) {
        // 如果是非http、https协议头，返回originalUri
        if (!TextUtils.equals(originalUri.getScheme(), "http")
                && !TextUtils.equals(originalUri.getScheme(), "https")) {
            return originalUri;
        }

        String downloadUrl = originalUri.toString();
        Map<String, String> map = new HashMap<>();
        if (auth != null) {
            String token = auth.getToken();
            if (!TextUtils.isEmpty(token)) {
                map.put("authorization", token);
            } else if (auth.getDownloadAuthInfo() != null) {
                switch (auth.getDownloadAuthInfo().getType()) {
                    case 1:
                        downloadUrl = auth.getDownloadAuthInfo().getUrl();
                        break;
                        //                    case 2:
                        //
                        // map.putAll(auth.getDownloadAuthInfo().getAuthParams());
                        //                        break;
                }
            }
        }
        return buildGlideUrl(downloadUrl, map);
    }

    /**
     * Glide加载图片和头像前，先处理拦截器
     *
     * @param url 图片加载地址
     * @param headers 图片加载header
     */
    public static Object buildGlideUrl(String url, Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return new GlideUrl(url);
        } else {
            LazyHeaders.Builder builder = new LazyHeaders.Builder();
            for (Map.Entry<String, String> item : headers.entrySet()) {
                builder.addHeader(item.getKey(), item.getValue());
            }
            return new GlideUrl(url, builder.build());
        }
    }

    /**
     * 获取图片url的文件名
     *
     * @param url 图片地址
     * @return 图片url的文件名
     */
    public static String getUrlName(String url) {
        if (TextUtils.isEmpty(url)) {
            return "temp";
        }
        String name = "";
        int start = url.lastIndexOf("/");
        if (start != -1) {
            name = url.substring(start + 1);
        }
        int indexOfSuffix = name.lastIndexOf(".");
        if (indexOfSuffix != -1) {
            return name.substring(0, indexOfSuffix);
        }
        return name;
    }
}
