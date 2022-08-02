package io.rong.imkit.manager;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.feature.destruct.DestructManager;
import io.rong.imkit.picture.config.PictureMimeType;
import io.rong.imkit.picture.entity.LocalMedia;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.GIFMessage;
import io.rong.message.ImageMessage;


public class SendImageManager {
    private final static String TAG = "SendImageManager";

    private ExecutorService executorService;
    private UploadController uploadController;

    static class SingletonHolder {
        static SendImageManager sInstance = new SendImageManager();
    }

    public static SendImageManager getInstance() {
        return SingletonHolder.sInstance;
    }

    private SendImageManager() {
        executorService = getExecutorService();
        uploadController = new UploadController();
    }

    public void sendImage(Conversation.ConversationType conversationType, String targetId, LocalMedia image, boolean isFull) {
        if (image.getPath() == null) {
            return;
        }
        MessageContent content;
        String mimeType = image.getMimeType();
        String path = image.getPath();
        if (!path.startsWith("content://") && !path.startsWith("file://")) {
            path = "file://" + path;
        }
        Uri uri = Uri.parse(path);
        if (PictureMimeType.isGif(mimeType)) {
            content = GIFMessage.obtain(uri);
        } else {
            content = ImageMessage.obtain(uri, uri, isFull);
        }
        if (DestructManager.isActive()) {
            if (content != null) {
                content.setDestruct(true);
                content.setDestructTime(DestructManager.IMAGE_DESTRUCT_TIME);
            }
        }
        IMCenter.getInstance().insertOutgoingMessage(conversationType, targetId, Message.SentStatus.SENDING, content, new RongIMClient.ResultCallback<Message>() {
            @Override
            public void onSuccess(Message message) {
                //RongIM.OnSendMessageListener listener = RongContext.getInstance().getOnSendMessageListener();
                uploadController.execute(message);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {

            }
        });
        //todo 旧版逻辑分 if else 要判断回调

    }

    public void cancelSendingImages(Conversation.ConversationType conversationType, String targetId) {
        RLog.d(TAG, "cancelSendingImages");
        if (conversationType != null && targetId != null && uploadController != null)
            uploadController.cancel(conversationType, targetId);
    }

    public void cancelSendingImage(Conversation.ConversationType conversationType, String targetId, int messageId) {
        RLog.d(TAG, "cancelSendingImages");
        if (conversationType != null && targetId != null && uploadController != null && messageId > 0)
            uploadController.cancel(conversationType, targetId, messageId);
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


        public void cancel(Conversation.ConversationType conversationType, String targetId) {
            synchronized (pendingMessages) {

                Iterator<Message> it = pendingMessages.iterator();
                while (it.hasNext()) {
                    Message msg = it.next();
                    if (msg.getConversationType().equals(conversationType) && msg.getTargetId().equals(targetId)) {
                        it.remove();
                    }
                }

                if (pendingMessages.size() == 0)
                    executingMessage = null;
            }
        }

        public void cancel(Conversation.ConversationType conversationType, String targetId, int messageId) {
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
                if (pendingMessages.size() == 0)
                    executingMessage = null;
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
            if (executingMessage.getContent() != null)
                isDestruct = executingMessage.getContent().isDestruct();
            IMCenter.getInstance().sendMediaMessage(executingMessage, isDestruct ? IMCenter.getInstance().getContext().getString(R.string.rc_conversation_summary_content_burn) : null, null, new IRongCallback.ISendMediaMessageCallback() {
                @Override
                public void onAttached(Message message) {

                }

                @Override
                public void onError(Message message, RongIMClient.ErrorCode code) {
                    polling();
                }

                @Override
                public void onSuccess(Message message) {
                    polling();
                }

                @Override
                public void onProgress(Message message, int progress) {

                }

                @Override
                public void onCanceled(Message message) {

                }
            });
        }
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(1,
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
            public Thread newThread(Runnable runnable) {
                Thread result = new Thread(runnable, "Rong SendMediaManager");
                result.setDaemon(false);
                return result;
            }
        };
    }
}
