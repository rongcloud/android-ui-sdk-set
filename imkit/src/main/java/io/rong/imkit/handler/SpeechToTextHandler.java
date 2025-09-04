package io.rong.imkit.handler;

import io.rong.imkit.RongIM;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imkit.model.UiMessage;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.IRongCoreListener;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.SpeechToTextInfo;

/**
 * 语音转文字处理器
 *
 * <p>负责处理语音消息转文字的数据请求和回调，包括：
 *
 * <ul>
 *   <li>发起语音转文字请求
 *   <li>设置转文字结果的可见性
 *   <li>监听转文字结果回调
 *   <li>管理转文字操作的错误状态
 * </ul>
 *
 * @since 5.22.0
 */
public class SpeechToTextHandler extends MultiDataHandler {

    private static final String TAG = "SpeechToTextHandler";

    public static final DataKey<UiMessage> KEY_SPEECH_TO_TEXT_LISTENER =
            DataKey.obtain("KEY_SPEECH_TO_TEXT_LISTENER", UiMessage.class);

    public static final DataKey<UiMessage> KEY_REQUEST_SPEECH_TO_TEXT =
            DataKey.obtain("KEY_REQUEST_SPEECH_TO_TEXT", UiMessage.class);

    public static final DataKey<UiMessage> KEY_SET_SPEECH_TO_TEXT_VISIBLE =
            DataKey.obtain("KEY_SET_SPEECH_TO_TEXT_VISIBLE", UiMessage.class);

    /** 语音转文字隐藏状态标识符 */
    public static final String SPEECH_TO_TEXT_HIDDEN_STATE = "SpeechToText_Hidden";

    /** 语音转文字加载中状态标识符 */
    public static final String SPEECH_TO_TEXT_LOADING_STATE = "SpeechToText_Loading";

    private final IRongCoreListener.SpeechToTextListener speechToTextListener =
            new IRongCoreListener.SpeechToTextListener() {

                @Override
                public void onSpeechToTextComplete(
                        SpeechToTextInfo info,
                        String messageUId,
                        IRongCoreEnum.CoreErrorCode coreErrorCode) {
                    if (info != null) {
                        RongCoreClient.getInstance()
                                .getMessageByUid(
                                        messageUId,
                                        new IRongCoreCallback.ResultCallback<Message>() {
                                            @Override
                                            public void onSuccess(Message message) {
                                                UiMessage uiMessage = new UiMessage(message);
                                                // 如果是转换成功,且消息没有被已读
                                                if (info.getStatus()
                                                                == SpeechToTextInfo
                                                                        .SpeechToTextStatus.SUCCESS
                                                        && !message.getReceivedStatus()
                                                                .isListened()) {
                                                    message.getReceivedStatus().setListened();
                                                    RongIM.getInstance()
                                                            .setMessageReceivedStatus(
                                                                    message.getMessageId(),
                                                                    message.getConversationType(),
                                                                    message.getTargetId(),
                                                                    message.getReceivedStatus(),
                                                                    null);
                                                }
                                                notifyDataChange(
                                                        KEY_SPEECH_TO_TEXT_LISTENER, uiMessage);
                                            }

                                            @Override
                                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                                notifyDataError(KEY_SPEECH_TO_TEXT_LISTENER, e);
                                            }
                                        });
                    } else {
                        notifyDataError(KEY_SPEECH_TO_TEXT_LISTENER, coreErrorCode);
                    }
                }
            };

    public SpeechToTextHandler() {
        super();
        RongCoreClient.getInstance().addSpeechToTextListener(speechToTextListener);
    }

    public void requestSpeechToTextForMessage(String messageUId) {
        RongCoreClient.getInstance()
                .requestSpeechToTextForMessage(
                        messageUId,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                RongCoreClient.getInstance()
                                        .getMessageByUid(
                                                messageUId,
                                                new IRongCoreCallback.ResultCallback<Message>() {
                                                    @Override
                                                    public void onSuccess(Message message) {
                                                        UiMessage uiMessage =
                                                                new UiMessage(message);
                                                        uiMessage.setBusinessState(
                                                                SPEECH_TO_TEXT_LOADING_STATE);
                                                        notifyDataChange(
                                                                KEY_REQUEST_SPEECH_TO_TEXT,
                                                                uiMessage);
                                                    }

                                                    @Override
                                                    public void onError(
                                                            IRongCoreEnum.CoreErrorCode e) {
                                                        notifyDataError(
                                                                KEY_REQUEST_SPEECH_TO_TEXT, e);
                                                    }
                                                });
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataError(KEY_REQUEST_SPEECH_TO_TEXT, coreErrorCode);
                            }
                        });
    }

    public void setMessageSpeechToTextVisible(final int messageId, final boolean isVisible) {
        RongCoreClient.getInstance()
                .setMessageSpeechToTextVisible(
                        messageId,
                        isVisible,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                RongCoreClient.getInstance()
                                        .getMessage(
                                                messageId,
                                                new IRongCoreCallback.ResultCallback<Message>() {
                                                    @Override
                                                    public void onSuccess(Message message) {
                                                        notifyDataChange(
                                                                KEY_SET_SPEECH_TO_TEXT_VISIBLE,
                                                                new UiMessage(message));
                                                    }

                                                    @Override
                                                    public void onError(
                                                            IRongCoreEnum.CoreErrorCode e) {
                                                        notifyDataError(
                                                                KEY_SET_SPEECH_TO_TEXT_VISIBLE, e);
                                                    }
                                                });
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataError(KEY_SET_SPEECH_TO_TEXT_VISIBLE, coreErrorCode);
                            }
                        });
    }

    @Override
    public void stop() {
        super.stop();
        RongCoreClient.getInstance().removeSpeechToTextListener(speechToTextListener);
    }
}
