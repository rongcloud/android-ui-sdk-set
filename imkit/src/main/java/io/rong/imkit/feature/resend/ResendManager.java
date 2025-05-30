package io.rong.imkit.feature.resend;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import io.rong.common.rlog.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.location.message.LocationMessage;
import io.rong.imlib.model.Message;
import io.rong.message.ImageMessage;
import io.rong.message.MediaMessageContent;
import io.rong.message.ReadReceiptMessage;
import io.rong.message.ReferenceMessage;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentLinkedQueue;

/** 用于管理重新发送 */
public class ResendManager {

    private final String TAG = "ResendManager";
    // 发送消息间隔
    private static final int TIME_DELAY = 300;
    private volatile boolean mIsProcessing = false;
    private Hashtable<Integer, Message> mMessageMap;
    private ConcurrentLinkedQueue<Integer> mMessageQueue;
    private Handler mResendHandler;
    private RongIMClient.ConnectionStatusListener connectionStatusListener =
            new RongIMClient.ConnectionStatusListener() {
                @Override
                public void onChanged(
                        RongIMClient.ConnectionStatusListener.ConnectionStatus connectionStatus) {
                    if (connectionStatus.equals(
                            RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED)) {
                        // 开始发送缓存队列因发送失败需要重发的消息
                        ResendManager.getInstance().beginResend();
                    } else if (connectionStatus.equals(
                            RongIMClient.ConnectionStatusListener.ConnectionStatus.SIGN_OUT)) {
                        ResendManager.getInstance().removeAllResendMessage();
                    }
                }
            };

    private ResendManager() {
        mMessageMap = new Hashtable<>();
        mMessageQueue = new ConcurrentLinkedQueue<>();
        HandlerThread resendThread = new HandlerThread("RESEND_WORK");
        resendThread.start();
        mResendHandler = new Handler(resendThread.getLooper());
        IMCenter.getInstance().addConnectionStatusListener(connectionStatusListener);
    }

    private static class ResendManagerHolder {
        private static ResendManager instance = new ResendManager();
    }

    public static ResendManager getInstance() {
        return ResendManagerHolder.instance;
    }

    public void addResendMessage(
            final Message message,
            final RongIMClient.ErrorCode errorCode,
            final AddResendMessageCallBack callBack) {
        mResendHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (errorCode == null) {
                            return;
                        }
                        if (isResendErrorCode(errorCode)) {
                            if (!mMessageMap.containsKey(message.getMessageId())) {
                                RLog.d(TAG, "addResendMessage : id=" + message.getMessageId());
                                if (mMessageMap != null && mMessageQueue != null) {
                                    mMessageMap.put(message.getMessageId(), message);
                                    mMessageQueue.add(message.getMessageId());
                                    beginResend();
                                    message.setSentStatus(Message.SentStatus.SENDING);
                                }
                            }
                        }
                        callBack.onComplete(message, errorCode);
                    }
                });
    }

    public void removeResendMessage(final int messageId) {
        mResendHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mMessageMap != null) {
                            mMessageMap.remove(messageId);
                            mMessageQueue.remove(messageId);
                        }
                    }
                });
    }

    public void removeResendMessages(final int[] messageIds) {
        if (messageIds == null || messageIds.length == 0) {
            return;
        }
        mResendHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mMessageMap != null) {
                            for (int messageId : messageIds) {
                                mMessageMap.remove(messageId);
                                mMessageQueue.remove(messageId);
                            }
                        }
                    }
                });
    }

    public void removeAllResendMessage() {
        mResendHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mMessageMap != null) {
                            mMessageMap.clear();
                            mMessageQueue.clear();
                            mIsProcessing = false;
                        }
                    }
                });
    }

    public boolean needResend(int messageId) {
        if (mMessageMap == null) {
            return false;
        }
        return mMessageMap.containsKey(messageId);
    }

    public void beginResend() {
        mResendHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mMessageMap == null || mMessageMap.size() == 0) {
                            RLog.i(TAG, "beginResend onChanged no message need resend");
                            mIsProcessing = false;
                            return;
                        }
                        if (mIsProcessing) {
                            RLog.i(TAG, "beginResend ConnectionStatus is resending");
                            return;
                        }
                        mIsProcessing = true;
                        loopResendMessage();
                    }
                });
    }

    private void loopResendMessage() {
        mResendHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        final Integer idInteger = mMessageQueue.peek();
                        RLog.d(TAG, "beginResend: messageId = " + idInteger);
                        if (idInteger == null
                                || IMCenter.getInstance().getCurrentConnectionStatus()
                                        != RongIMClient.ConnectionStatusListener.ConnectionStatus
                                                .CONNECTED) {
                            mIsProcessing = false;
                            return;
                        }
                        resendMessage(
                                mMessageMap.get(idInteger),
                                new ReSendMessageCallback() {
                                    @Override
                                    public void onCancel(Message message) {
                                        removeResendMessage(idInteger);
                                        loopResendMessage();
                                    }

                                    @Override
                                    public void onAttached(Message message) {
                                        // do nothing
                                    }

                                    @Override
                                    public void onSuccess(Message message) {
                                        RLog.i(
                                                TAG,
                                                "resendMessage success messageId = "
                                                        + message.getMessageId());
                                        removeResendMessage(idInteger);
                                        loopResendMessage();
                                    }

                                    @Override
                                    public void onError(
                                            Message message, RongIMClient.ErrorCode coreErrorCode) {
                                        RLog.i(
                                                TAG,
                                                "resendMessage success messageId = "
                                                        + (message != null
                                                                ? message.getMessageId()
                                                                : null));
                                        if (!isResendErrorCode(coreErrorCode)) {
                                            removeResendMessage(idInteger);
                                        }
                                        loopResendMessage();
                                    }
                                });
                    }
                },
                TIME_DELAY);
    }

    /**
     * 是否为需要重发处理的错误码
     *
     * @param errorCode 发送消息的失败错误码
     * @return 是否重发，true 会重发， false 不需要重发处理。
     */
    public boolean isResendErrorCode(RongIMClient.ErrorCode errorCode) {
        int code = errorCode.getValue();
        return (code == IRongCoreEnum.CoreErrorCode.RC_NET_CHANNEL_INVALID.getValue()
                || code == IRongCoreEnum.CoreErrorCode.RC_NET_UNAVAILABLE.getValue()
                || code == IRongCoreEnum.CoreErrorCode.RC_MSG_RESP_TIMEOUT.getValue()
                || code == IRongCoreEnum.CoreErrorCode.RC_FILE_UPLOAD_FAILED.getValue());
    }

    /**
     * 重发洗消息
     *
     * @param message 消息
     */
    private void resendMessage(Message message, final ReSendMessageCallback callback) {
        if (message == null) {
            RLog.i(TAG, "resendMessage: Message is Null");
            return;
        }
        if (TextUtils.isEmpty(message.getTargetId()) || message.getContent() == null) {
            RLog.e(TAG, "targetId or messageContent is Null");
            removeResendMessage(message.getMessageId());
            return;
        }
        if (message.getContent() instanceof ImageMessage) {
            ImageMessage imageMessage = (ImageMessage) message.getContent();
            if (imageMessage.getRemoteUri() != null
                    && !imageMessage.getRemoteUri().toString().startsWith("file")) {
                IMCenter.getInstance().sendMessage(message, null, null, callback);
            } else {
                IMCenter.getInstance()
                        .sendMediaMessage(
                                message,
                                null,
                                null,
                                new IRongCallback.ISendMediaMessageCallback() {

                                    @Override
                                    public void onAttached(Message message) {
                                        // do nothing
                                    }

                                    @Override
                                    public void onSuccess(Message message) {
                                        callback.onSuccess(message);
                                    }

                                    @Override
                                    public void onError(
                                            Message message, RongIMClient.ErrorCode errorCode) {
                                        callback.onError(message, errorCode);
                                    }

                                    @Override
                                    public void onProgress(Message message, int i) {
                                        // do nothing
                                    }

                                    @Override
                                    public void onCanceled(Message message) {
                                        // do nothing
                                    }
                                });
            }
        } else if (message.getContent() instanceof LocationMessage) {
            IMCenter.getInstance().sendLocationMessage(message, null, null, callback);
        } else if (message.getContent() instanceof ReferenceMessage) {
            IMCenter.getInstance().sendMessage(message, null, null, callback);
        } else if (message.getContent() instanceof ReadReceiptMessage) {
            IMCenter.getInstance()
                    .sendReadReceiptMessage(
                            message.getConversationType(),
                            message.getTargetId(),
                            message.getSentTime(),
                            callback);
        } else if (message.getContent() instanceof MediaMessageContent) {
            MediaMessageContent mediaMessageContent = (MediaMessageContent) message.getContent();
            if (mediaMessageContent.getMediaUrl() != null) {
                IMCenter.getInstance().sendMessage(message, null, null, callback);
            } else {
                IMCenter.getInstance()
                        .sendMediaMessage(
                                message,
                                null,
                                null,
                                new IRongCallback.ISendMediaMessageCallback() {
                                    @Override
                                    public void onProgress(Message message, int progress) {
                                        // do nothing
                                    }

                                    @Override
                                    public void onCanceled(Message message) {
                                        callback.onCancel(message);
                                    }

                                    @Override
                                    public void onAttached(Message message) {
                                        // do nothing
                                    }

                                    @Override
                                    public void onSuccess(Message message) {
                                        callback.onSuccess(message);
                                    }

                                    @Override
                                    public void onError(
                                            Message message, RongIMClient.ErrorCode errorCode) {
                                        callback.onError(message, errorCode);
                                    }
                                });
            }
        } else {
            IMCenter.getInstance().sendMessage(message, null, null, callback);
        }
    }

    public interface AddResendMessageCallBack {
        void onComplete(Message message, RongIMClient.ErrorCode errorCode);
    }

    interface ReSendMessageCallback extends IRongCallback.ISendMessageCallback {
        /**
         * 消息被取消
         *
         * @param message 已存库的消息体。
         */
        void onCancel(Message message);
    }
}
