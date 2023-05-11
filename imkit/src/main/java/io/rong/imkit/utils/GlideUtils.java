package io.rong.imkit.utils;

import android.net.Uri;
import android.text.TextUtils;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import io.rong.common.FileUtils;

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
    public static Object buildAuthUrl(Uri originalUri, String privateToken) {
        if (FileUtils.uriStartWithFile(originalUri)) {
            return originalUri;
        }
        if (TextUtils.isEmpty(privateToken)) {
            return new GlideUrl(originalUri.toString());
        }
        return new GlideUrl(
                originalUri.toString(),
                new LazyHeaders.Builder().addHeader("authorization", privateToken).build());
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
