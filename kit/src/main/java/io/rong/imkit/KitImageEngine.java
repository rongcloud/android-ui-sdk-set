package io.rong.imkit;

import android.content.Context;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import io.rong.imkit.picture.engine.ImageEngine;

public interface KitImageEngine extends ImageEngine {
    /**
     * 加载会话列表头像
     *
     * @param context
     * @param url
     * @param imageView
     */
    void loadConversationListPortrait(@NonNull Context context, @NonNull String url, @NonNull ImageView imageView);


    /**
     * 加载会话头像
     *
     * @param context
     * @param url
     * @param imageView
     */
    void loadConversationPortrait(@NonNull Context context, @NonNull String url, @NonNull ImageView imageView);
}
