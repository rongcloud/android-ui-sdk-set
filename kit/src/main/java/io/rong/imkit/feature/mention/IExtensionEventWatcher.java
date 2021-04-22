package io.rong.imkit.feature.mention;

import android.content.Context;
import android.widget.EditText;

import java.util.List;

import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MentionedInfo;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.TextMessage;


public interface IExtensionEventWatcher {
    /**
     * 输入框文本变化时的回调
     * @param context
     * @param type
     * @param targetId
     * @param cursorPos
     * @param count
     * @param text
     */
    void onTextChanged(Context context, Conversation.ConversationType type, String targetId, int cursorPos, int count, String text);

    /**
     * Extension 模块点击发送按钮时的预处理，其它模块可以通过此回调设置附加信息到 Extension
     * @param message extension 模块点击发送按钮时构建的初始消息。其它模块可以更改该消息里的配置。
     * @return  经各模块处理后的消息
     */
    void onSendToggleClick(Message message);

    void onDeleteClick(Conversation.ConversationType type, String targetId, EditText editText, int cursorPos);

    void onDestroy(Conversation.ConversationType type, String targetId);
}
