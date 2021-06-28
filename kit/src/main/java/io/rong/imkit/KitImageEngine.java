package io.rong.imkit;

import android.content.Context;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import io.rong.imkit.picture.engine.ImageEngine;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;

public interface KitImageEngine extends ImageEngine {
    /**
     * 加载会话列表页，单个会话的会话头像
     *
     * @param context      上下文
     * @param url          单个会话头像地址
     * @param imageView    加载头像的 view 控件
     * @param conversation 当前会话，可根据会话类型配置不同默认图
     */
    void loadConversationListPortrait(@NonNull Context context, @NonNull String url, @NonNull ImageView imageView, Conversation conversation);


    /**
     * 加载会话页，单个消息的用户头像
     *
     * @param context   上下文
     * @param url       单个消息用户头像地址
     * @param imageView 加载头像的 view 控件
     * @param message   当前消息，可根据会话类型配置不同默认图
     */
    void loadConversationPortrait(@NonNull Context context, @NonNull String url, @NonNull ImageView imageView, Message message);
}
