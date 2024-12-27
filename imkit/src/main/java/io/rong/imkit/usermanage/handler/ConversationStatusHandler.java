package io.rong.imkit.usermanage.handler;

import androidx.annotation.NonNull;
import io.rong.imkit.IMCenter;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.ConversationStatus;

/**
 * 会话状态处理类
 *
 * <p>注意：使用完毕后需要调用 {@link #stop()} 方法释放资源
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class ConversationStatusHandler extends MultiDataHandler {

    public static final DataKey<Conversation.ConversationNotificationStatus>
            KEY_GET_CONVERSATION_NOTIFICATION_STATUS =
                    DataKey.obtain(
                            "KEY_GET_CONVERSATION_NOTIFICATION_STATUS",
                            Conversation.ConversationNotificationStatus.class);

    public static final DataKey<Boolean> KEY_GET_CONVERSATION_TOP_STATUS =
            DataKey.obtain("KEY_GET_CONVERSATION_TOP_STATUS", Boolean.class);
    @NonNull private final ConversationIdentifier conversationIdentifier;

    private final RongIMClient.ConversationStatusListener conversationStatusListener =
            new RongIMClient.ConversationStatusListener() {
                @Override
                public void onStatusChanged(ConversationStatus[] conversationStatus) {
                    if (conversationStatus != null && conversationStatus.length > 0) {
                        for (ConversationStatus status : conversationStatus) {
                            if (conversationIdentifier.getTargetId().equals(status.getTargetId())
                                    && conversationIdentifier
                                            .getType()
                                            .equals(status.getConversationType())) {
                                notifyDataChange(
                                        KEY_GET_CONVERSATION_NOTIFICATION_STATUS,
                                        status.getNotifyStatus());
                                notifyDataChange(KEY_GET_CONVERSATION_TOP_STATUS, status.isTop());
                            }
                        }
                    }
                }
            };

    public ConversationStatusHandler(@NonNull ConversationIdentifier conversationIdentifier) {
        this.conversationIdentifier = conversationIdentifier;
        IMCenter.getInstance().addConversationStatusListener(conversationStatusListener);
    }

    /** 设置会话置顶状态 */
    public void getConversationNotificationStatus() {
        RongCoreClient.getInstance()
                .getConversationNotificationStatus(
                        conversationIdentifier.getType(),
                        conversationIdentifier.getTargetId(),
                        new IRongCoreCallback.ResultCallback<
                                Conversation.ConversationNotificationStatus>() {
                            @Override
                            public void onSuccess(
                                    Conversation.ConversationNotificationStatus
                                            conversationNotificationStatus) {
                                notifyDataChange(
                                        KEY_GET_CONVERSATION_NOTIFICATION_STATUS,
                                        conversationNotificationStatus);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                notifyDataError(KEY_GET_CONVERSATION_NOTIFICATION_STATUS, e);
                            }
                        });
    }

    /** 设置会话置顶状态 */
    public void getConversationTopStatus() {
        RongCoreClient.getInstance()
                .getConversationTopStatus(
                        conversationIdentifier.getTargetId(),
                        conversationIdentifier.getType(),
                        new IRongCoreCallback.ResultCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean aBoolean) {
                                notifyDataChange(KEY_GET_CONVERSATION_TOP_STATUS, aBoolean);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                notifyDataError(KEY_GET_CONVERSATION_TOP_STATUS, e);
                            }
                        });
    }

    @Override
    public void stop() {
        super.stop();
        IMCenter.getInstance().removeConversationStatusListener(conversationStatusListener);
    }
}
