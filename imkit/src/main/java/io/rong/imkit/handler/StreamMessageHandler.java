package io.rong.imkit.handler;

import androidx.annotation.NonNull;
import io.rong.common.rlog.RLog;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imkit.model.UiMessage;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.IRongCoreListener;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.StreamMessageChunkInfo;
import io.rong.imlib.params.StreamMessageRequestParams;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * StreamMessageHandler
 *
 * <p>处理 StreamMessage 的数据请求
 *
 * @since 5.16.0
 */
public class StreamMessageHandler extends MultiDataHandler {

    private static final String TAG = "StreamMessageHandler";

    public interface State {
        String CONNECT_ERROR = "CONNECT_ERROR";
        String RETRY_PULL = "RETRY_PULL";
    }

    public static final DataKey<UiMessage> KEY_FETCH_STREAM_MESSAGE =
            DataKey.obtain("KEY_FETCH_STREAM_MESSAGE", UiMessage.class);

    private final IRongCoreListener.StreamMessageRequestEventListener
            streamMessageRequestEventListener =
                    new IRongCoreListener.StreamMessageRequestEventListener() {
                        @Override
                        public void onInit(String messageUid) {}

                        @Override
                        public void onData(Message message, StreamMessageChunkInfo chunkInfo) {
                            notifyDataChange(KEY_FETCH_STREAM_MESSAGE, new UiMessage(message));
                        }

                        @Override
                        public void onComplete(
                                String messageUid, IRongCoreEnum.CoreErrorCode code) {
                            runningMsgSet.remove(messageUid);
                            RongCoreClient.getInstance()
                                    .getMessageByUid(
                                            messageUid,
                                            new IRongCoreCallback.ResultCallback<Message>() {
                                                @Override
                                                public void onSuccess(Message message) {
                                                    if (message == null) {
                                                        RLog.e(
                                                                TAG,
                                                                "getMessageByUid onSuccess: message is null, messageUid = "
                                                                        + messageUid);
                                                        return;
                                                    }
                                                    UiMessage uiMessage = getUiMessage(message);
                                                    notifyDataChange(
                                                            KEY_FETCH_STREAM_MESSAGE, uiMessage);
                                                }

                                                @NonNull
                                                private UiMessage getUiMessage(Message message) {
                                                    UiMessage uiMessage = new UiMessage(message);
                                                    if (code
                                                            == IRongCoreEnum.CoreErrorCode
                                                                    .STREAM_MESSAGE_REQUEST_FAIL) {
                                                        uiMessage.setBusinessState(
                                                                State.CONNECT_ERROR);
                                                    }
                                                    return uiMessage;
                                                }

                                                @Override
                                                public void onError(
                                                        IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                                    notifyDataError(
                                                            KEY_FETCH_STREAM_MESSAGE,
                                                            coreErrorCode);
                                                }
                                            });
                        }
                    };

    public StreamMessageHandler() {
        super();
        RongCoreClient.getInstance()
                .addStreamMessageRequestEventListener(streamMessageRequestEventListener);
    }

    private final CopyOnWriteArraySet<String> runningMsgSet = new CopyOnWriteArraySet<>();
    private final CopyOnWriteArraySet<String> hasPullMsgSet = new CopyOnWriteArraySet<>();

    public void fetchStreamMessage(String msgUId, boolean isRetry) {
        boolean isNeedPull = isRetry || !hasPullMsgSet.contains(msgUId);
        if (!runningMsgSet.contains(msgUId) && isNeedPull) {
            runningMsgSet.add(msgUId);
            hasPullMsgSet.add(msgUId);
            RongCoreClient.getInstance()
                    .requestStreamMessageContent(
                            StreamMessageRequestParams.obtain(msgUId),
                            new IRongCoreCallback.OperationCallback() {
                                @Override
                                public void onSuccess() {}

                                @Override
                                public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                    notifyDataError(KEY_FETCH_STREAM_MESSAGE, coreErrorCode);
                                    if (coreErrorCode
                                            != IRongCoreEnum.CoreErrorCode
                                                    .STREAM_MESSAGE_REQUEST_IN_PROCESS) {
                                        runningMsgSet.remove(msgUId);
                                        RongCoreClient.getInstance()
                                                .getMessageByUid(
                                                        msgUId,
                                                        new IRongCoreCallback.ResultCallback<
                                                                Message>() {
                                                            @Override
                                                            public void onSuccess(Message message) {
                                                                UiMessage uiMessage =
                                                                        new UiMessage(message);
                                                                uiMessage.setBusinessState(
                                                                        State.CONNECT_ERROR);
                                                                notifyDataChange(
                                                                        KEY_FETCH_STREAM_MESSAGE,
                                                                        uiMessage);
                                                            }

                                                            @Override
                                                            public void onError(
                                                                    IRongCoreEnum.CoreErrorCode e) {
                                                                notifyDataError(
                                                                        KEY_FETCH_STREAM_MESSAGE,
                                                                        e);
                                                            }
                                                        });
                                    }
                                }
                            });
        }
    }

    @Override
    public void stop() {
        super.stop();
        RongCoreClient.getInstance()
                .removeStreamMessageRequestEventListener(streamMessageRequestEventListener);
    }
}
