package io.rong.imkit.feature.reference;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;

/** 消息列表中引用展示区的自定义内容 Provider。 */
public abstract class ReferenceContentMessageItemProvider<T extends MessageContent> {

    /**
     * 创建消息列表中消息 item 内的自定义引用展示 View。
     *
     * @return 返回 null 时使用默认引用摘要展示
     */
    public View onCreateView(Context context, ViewGroup parent) {
        return null;
    }

    /** 绑定消息列表中消息 item 内的引用展示 View。 */
    public void onBindView(View view, Message quotedMessage, T quotedContent) {}

    /** 当前 Provider 是否支持该消息内容。 */
    public abstract boolean isReferenceContentType(MessageContent content);
}
