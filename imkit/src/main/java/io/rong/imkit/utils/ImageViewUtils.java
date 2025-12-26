package io.rong.imkit.utils;

import android.widget.ImageView;

/** 处理 ImageView */
public class ImageViewUtils {

    /**
     * 设置 ImageView 的 Drawable 自动翻转，适用于带有方向性的 Drawable
     *
     * @param imageView ImageView
     */
    public static void enableDrawableAutoMirror(ImageView imageView) {
        // 启用群成员箭头的 RTL 自动镜像
        if (imageView != null && imageView.getDrawable() != null) {
            imageView.getDrawable().setAutoMirrored(true);
        }
    }
}
