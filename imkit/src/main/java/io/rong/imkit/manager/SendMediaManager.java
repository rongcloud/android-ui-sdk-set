package io.rong.imkit.manager;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.Nullable;
import io.rong.common.FileUtils;
import io.rong.common.rlog.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.feature.destruct.DestructManager;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.Message;
import io.rong.message.SightMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SendMediaManager {
    private static final String TAG = SendMediaManager.class.getSimpleName();

    private ExecutorService executorService;
    private UploadController uploadController;

    static class SingletonHolder {
        static SendMediaManager sInstance = new SendMediaManager();
    }

    public static SendMediaManager getInstance() {
        return SingletonHolder.sInstance;
    }

    private SendMediaManager() {
        executorService = getExecutorService();
        uploadController = new UploadController();
    }

    public void sendMedia(
            Context context,
            ConversationIdentifier conversationIdentifier,
            Uri mediaUri,
            long duration) {
        if (!TextUtils.isEmpty(mediaUri.toString())) {
            if (!FileUtils.isFileExistsWithUri(context, mediaUri)) {
                return;
            }
            SightMessage sightMessage =
                    SightMessage.obtain(context, mediaUri, (int) duration / 1000);
            if (DestructManager.isActive()) {
                sightMessage.setDestruct(true);
                sightMessage.setDestructTime(DestructManager.SIGHT_DESTRUCT_TIME);
            }
            IMCenter.getInstance()
                    .insertOutgoingMessage(
                            conversationIdentifier,
                            Message.SentStatus.SENDING,
                            sightMessage,
                            System.currentTimeMillis(),
                            new RongIMClient.ResultCallback<Message>() {
                                @Override
                                public void onSuccess(Message message) {
                                    uploadController.execute(message);
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode errorCode) {
                                    // do nothing
                                }
                            });
        }
    }

    public void cancelSendingMedia(
            Conversation.ConversationType conversationType, String targetId) {
        RLog.d(TAG, "cancel Sending media");
        if (conversationType != null && targetId != null && uploadController != null)
            uploadController.cancel(conversationType, targetId);
    }

    public void cancelSendingMedia(
            Conversation.ConversationType conversationType, String targetId, int messageId) {
        RLog.d(TAG, "cancel Sending media");
        if (conversationType != null
                && targetId != null
                && uploadController != null
                && messageId > 0) uploadController.cancel(conversationType, targetId, messageId);
    }

    public void reset() {
        uploadController.reset();
    }

    private class UploadController implements Runnable {
        final List<Message> pendingMessages;
        Message executingMessage;

        public UploadController() {
            this.pendingMessages = new ArrayList<>();
        }

        public void execute(Message message) {
            synchronized (pendingMessages) {
                pendingMessages.add(message);
                if (executingMessage == null) {
                    executingMessage = pendingMessages.remove(0);
                    executorService.submit(this);
                }
            }
        }

        public void reset() {
            RLog.w(TAG, "Reset Sending media.");
            synchronized (pendingMessages) {
                for (Message message : pendingMessages) {
                    message.setSentStatus(Message.SentStatus.FAILED);
                    IMCenter.getInstance().refreshMessage(message);
                }
                pendingMessages.clear();
            }
            if (executingMessage != null) {
                executingMessage.setSentStatus(Message.SentStatus.FAILED);
                IMCenter.getInstance().refreshMessage(executingMessage);
                executingMessage = null;
            }
        }

        public void cancel(Conversation.ConversationType conversationType, String targetId) {
            synchronized (pendingMessages) {
                int count = pendingMessages.size();
                for (int i = 0; i < count; i++) {
                    Message msg = pendingMessages.get(i);
                    if (msg.getConversationType().equals(conversationType)
                            && msg.getTargetId().equals(targetId)) {
                        pendingMessages.remove(msg);
                    }
                }
                if (pendingMessages.size() == 0) executingMessage = null;
            }
        }

        public void cancel(
                Conversation.ConversationType conversationType, String targetId, int messageId) {
            synchronized (pendingMessages) {
                int count = pendingMessages.size();
                for (int i = 0; i < count; i++) {
                    Message msg = pendingMessages.get(i);
                    if (msg.getConversationType().equals(conversationType)
                            && msg.getTargetId().equals(targetId)
                            && msg.getMessageId() == messageId) {
                        pendingMessages.remove(msg);
                        break;
                    }
                }
                if (pendingMessages.size() == 0) executingMessage = null;
            }
        }

        private void polling() {
            synchronized (pendingMessages) {
                RLog.d(TAG, "polling " + pendingMessages.size());
                if (pendingMessages.size() > 0) {
                    executingMessage = pendingMessages.remove(0);
                    executorService.submit(this);
                } else {
                    executingMessage = null;
                }
            }
        }

        @Override
        public void run() {
            boolean isDestruct = false;
            if (executingMessage.getContent() != null) {
                isDestruct = executingMessage.getContent().isDestruct();
            }
            IMCenter.getInstance()
                    .sendMediaMessage(
                            executingMessage,
                            isDestruct
                                    ? IMCenter.getInstance()
                                            .getContext()
                                            .getString(
                                                    R.string.rc_conversation_summary_content_burn)
                                    : null,
                            null,
                            new IRongCallback.ISendMediaMessageCallback() {
                                @Override
                                public void onAttached(Message message) {
                                    executingMessage.setSentStatus(Message.SentStatus.SENDING);
                                    RongIMClient.getInstance()
                                            .setMessageSentStatus(executingMessage, null);
                                    IMCenter.getInstance().refreshMessage(executingMessage);
                                    RLog.d(TAG, "Compressing video file starts.");
                                }

                                @Override
                                public void onSuccess(Message message) {
                                    polling();
                                }

                                @Override
                                public void onError(
                                        Message message, RongIMClient.ErrorCode errorCode) {
                                    // 压缩失败的错误码才弹提示
                                    if (errorCode.code
                                            == IRongCoreEnum.CoreErrorCode.RC_VIDEO_COMPRESS_FAILED
                                                    .code) {
                                        Context context = IMCenter.getInstance().getContext();
                                        String text =
                                                context.getString(
                                                        R.string.rc_picsel_video_corrupted);
                                        ToastUtils.show(context, text, Toast.LENGTH_SHORT);
                                        return;
                                    }
                                    polling();
                                }

                                @Override
                                public void onProgress(Message message, int progress) {
                                    // do nothing
                                }

                                @Override
                                public void onCanceled(Message message) {
                                    // do nothing
                                }
                            });
            //            final String originLocalPath = ((SightMessage)
            // executingMessage.getContent()).getLocalPath().toString();
            //            final String compressPath =
            // KitStorageUtils.getImageSavePath(IMCenter.getInstance().getContext()) +
            // File.separator
            //                    + "VID_" + new SimpleDateFormat("yyyyMMdd_HHmmss",
            // Locale.CHINA).format(new Date()) + ".mp4";
            //            VideoCompress.compressVideo(IMCenter.getInstance().getContext(),
            // originLocalPath, compressPath, new VideoCompress.CompressListener() {
            //                @Override
            //                public void onStart() {
            //
            //                }
            //
            //                @Override
            //                public void onSuccess() {
            //                    RLog.d(TAG, "Compressing video file successes.");
            //                    if (executingMessage == null) {
            //                        return;
            //                    }
            //                    ((SightMessage)
            // executingMessage.getContent()).setLocalPath(Uri.parse("file://" + compressPath));
            //
            //                    File file = new File(compressPath);
            //                    ((SightMessage)
            // executingMessage.getContent()).setSize(file.length());
            //
            //                }
            //
            //                @Override
            //                public void onFail() {
            //
            //                }
            //
            //                @Override
            //                public void onProgress(float percent) {
            //                    RLog.d(TAG, "The progress of compressing video file is " +
            // percent);
            //                }
            //            });
        }
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService =
                    new ThreadPoolExecutor(
                            1,
                            Integer.MAX_VALUE,
                            60,
                            TimeUnit.SECONDS,
                            new SynchronousQueue<Runnable>(),
                            threadFactory());
        }
        return executorService;
    }

    private ThreadFactory threadFactory() {
        return new ThreadFactory() {
            @Override
            public Thread newThread(@Nullable Runnable runnable) {
                Thread result = new Thread(runnable, "Rong SendMediaManager");
                result.setDaemon(false);
                return result;
            }
        };
    }
}
