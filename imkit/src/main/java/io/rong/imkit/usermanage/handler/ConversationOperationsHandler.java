package io.rong.imkit.usermanage.handler;

import androidx.annotation.NonNull;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;

/**
 * 会话操作类
 *
 * <p>注意：使用完毕后需要调用 {@link #stop()} 方法释放资源
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class ConversationOperationsHandler extends MultiDataHandler {

    public static final DataKey<Conversation.ConversationNotificationStatus>
            KEY_SET_CONVERSATION_NOTIFICATION_STATUS =
                    DataKey.obtain(
                            "KEY_SET_CONVERSATION_NOTIFICATION_STATUS",
                            Conversation.ConversationNotificationStatus.class);

    public static final DataKey<Boolean> KEY_SET_CONVERSATION_TO_TOP =
            DataKey.obtain("KEY_SET_CONVERSATION_TO_TOP", Boolean.class);

    @NonNull private final ConversationIdentifier conversationIdentifier;

    public ConversationOperationsHandler(@NonNull ConversationIdentifier conversationIdentifier) {
        this.conversationIdentifier = conversationIdentifier;
    }

    /**
     * 设置会话置顶状态
     *
     * @param conversationNotificationStatus 会话置顶状态
     */
    public void setConversationNotificationStatus(
            Conversation.ConversationNotificationStatus conversationNotificationStatus) {
        RongCoreClient.getInstance()
                .setConversationNotificationStatus(
                        conversationIdentifier.getType(),
                        conversationIdentifier.getTargetId(),
                        conversationNotificationStatus,
                        new IRongCoreCallback.ResultCallback<
                                Conversation.ConversationNotificationStatus>() {
                            @Override
                            public void onSuccess(
                                    Conversation.ConversationNotificationStatus
                                            conversationNotificationStatus) {
                                notifyDataChange(
                                        KEY_SET_CONVERSATION_NOTIFICATION_STATUS,
                                        conversationNotificationStatus);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                notifyDataError(KEY_SET_CONVERSATION_NOTIFICATION_STATUS, e);
                            }
                        });
    }

    /**
     * 设置会话置顶状态
     *
     * @param isTop 是否置顶
     */
    public void setConversationToTop(boolean isTop) {
        RongCoreClient.getInstance()
                .setConversationToTop(
                        conversationIdentifier.getType(),
                        conversationIdentifier.getTargetId(),
                        isTop,
                        new IRongCoreCallback.ResultCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean aBoolean) {
                                notifyDataChange(KEY_SET_CONVERSATION_TO_TOP, aBoolean);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                notifyDataError(KEY_SET_CONVERSATION_TO_TOP, e);
                            }
                        });
    }
}
