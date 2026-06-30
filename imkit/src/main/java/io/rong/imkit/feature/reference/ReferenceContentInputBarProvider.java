package io.rong.imkit.feature.reference;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;

/** 输入框上方引用展示区的自定义内容 Provider。 */
public abstract class ReferenceContentInputBarProvider<T extends MessageContent> {

    /**
     * 创建聊天输入框上方的自定义引用展示 View。
     *
     * @return 返回 null 时使用默认引用条展示
     */
    public View onCreateView(Context context, ViewGroup parent, ReferenceInputBarAction action) {
        return null;
    }

    /** 绑定聊天输入框上方的引用展示 View。 */
    public void onBindView(View view, Message quotedMessage, T quotedContent) {}

    /** 当前 Provider 是否支持该消息内容。 */
    public abstract boolean isReferenceContentType(MessageContent content);
}
