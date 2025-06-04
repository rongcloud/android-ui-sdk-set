package io.rong.imkit.handler;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import io.rong.common.rlog.RLog;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.IRongCoreListener;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.ReadReceiptInfoV5;
import io.rong.imlib.model.ReadReceiptResponseV5;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * ReadReceiptV5Handler
 *
 * <p>处理 V5 版本已读回执的数据请求
 *
 * @since 5.20.0
 */
public class ReadReceiptV5Handler extends MultiDataHandler {

    private static final String TAG = "ReadReceiptV5Handler";

    // 数据键定义
    public static final DataKey<List<ReadReceiptInfoV5>> KEY_READ_RECEIPT_INFO_V5 =
            DataKey.obtain(
                    "KEY_READ_RECEIPT_INFO_V5",
                    (Class<List<ReadReceiptInfoV5>>) (Class<?>) List.class);

    public static final DataKey<Boolean> KEY_SEND_READ_RECEIPT_RESPONSE_V5 =
            DataKey.obtain("KEY_SEND_READ_RECEIPT_RESPONSE_V5", Boolean.class);

    // V5已读回执事件数据键
    public static final DataKey<List<ReadReceiptResponseV5>> KEY_MESSAGE_READ_RECEIPT_V5_EVENT =
            DataKey.obtain(
                    "KEY_MESSAGE_READ_RECEIPT_V5_EVENT",
                    (Class<List<ReadReceiptResponseV5>>) (Class<?>) List.class);

    // 记录正在处理的sendReadReceiptResponseV5请求
    private final CopyOnWriteArraySet<String> processingSendRequests = new CopyOnWriteArraySet<>();

    // 记录正在处理的getMessageReadReceiptInfoV5请求
    private final CopyOnWriteArraySet<String> processingGetRequests = new CopyOnWriteArraySet<>();

    // 缓存Map，Key是identifier+messageUId，Value是ReadReceiptInfoV5
    private final ConcurrentHashMap<String, ReadReceiptInfoV5> readReceiptInfoCache =
            new ConcurrentHashMap<>();

    // 最大批处理大小
    private static final int MAX_BATCH_SIZE = 100;
    // 限频处理相关常量
    private static final int RATE_LIMIT_BATCH_SIZE = 10; // 每次处理10个请求
    private static final long RATE_LIMIT_INTERVAL = 500; // 每500秒处理一次

    // 限频处理队列和相关变量
    private final ConcurrentLinkedQueue<ReadReceiptRequest> pendingReadReceiptRequests =
            new ConcurrentLinkedQueue<>();
    private final Handler rateLimitHandler = new Handler(Looper.getMainLooper());
    private final Runnable rateLimitRunnable =
            new Runnable() {
                @Override
                public void run() {
                    processRateLimitedRequests();
                    // 如果还有待处理的请求，继续调度
                    if (!pendingReadReceiptRequests.isEmpty()) {
                        rateLimitHandler.postDelayed(this, RATE_LIMIT_INTERVAL);
                    }
                }
            };
    private volatile boolean isRateLimitProcessing = false;

    // 已读回执请求数据类
    private static class ReadReceiptRequest {
        final ConversationIdentifier identifier;
        final List<String> messageUIds;

        ReadReceiptRequest(ConversationIdentifier identifier, List<String> messageUIds) {
            this.identifier = identifier;
            this.messageUIds = messageUIds;
        }
    }

    // V5已读回执监听器
    private final IRongCoreListener.MessageReadReceiptV5Listener messageReadReceiptV5Listener =
            responses -> {
                // 更新缓存
                updateReadReceiptInfoCache(responses);
                // 通知数据变化
                notifyDataChange(KEY_MESSAGE_READ_RECEIPT_V5_EVENT, responses);
            };

    public ReadReceiptV5Handler() {
        super();
        RongCoreClient.getInstance().addMessageReadReceiptV5Listener(messageReadReceiptV5Listener);
    }

    /**
     * 发送已读回执响应V5
     *
     * @param identifier 会话标识
     * @param messageUIds 消息UID列表
     */
    public void sendReadReceiptResponseV5(
            @NonNull ConversationIdentifier identifier, @NonNull List<String> messageUIds) {
        if (messageUIds.isEmpty()) {
            RLog.w(TAG, "sendReadReceiptResponseV5: messageUIds is empty");
            return;
        }

        // 分批处理，每批最多100个
        if (messageUIds.size() <= MAX_BATCH_SIZE) {
            sendReadReceiptResponseV5Batch(identifier, messageUIds);
        } else {
            RLog.d(
                    TAG,
                    "sendReadReceiptResponseV5: splitting "
                            + messageUIds.size()
                            + " messages into batches");
            for (int i = 0; i < messageUIds.size(); i += MAX_BATCH_SIZE) {
                int endIndex = Math.min(i + MAX_BATCH_SIZE, messageUIds.size());
                List<String> batch = messageUIds.subList(i, endIndex);
                sendReadReceiptResponseV5Batch(identifier, batch);
            }
        }
    }

    /**
     * 发送已读回执响应V5 - 单批处理
     *
     * @param identifier 会话标识
     * @param messageUIds 消息UID列表（不超过100个）
     */
    private void sendReadReceiptResponseV5Batch(
            @NonNull ConversationIdentifier identifier, @NonNull List<String> messageUIds) {
        if (messageUIds.isEmpty()) {
            return;
        }

        // 生成请求标识，用于去重
        String requestKey = generateRequestKey(messageUIds);

        // 检查是否正在处理相同请求
        if (processingSendRequests.contains(requestKey)) {
            RLog.d(TAG, "sendReadReceiptResponseV5Batch: request already in progress");
            return;
        }

        processingSendRequests.add(requestKey);

        RongCoreClient.getInstance()
                .sendReadReceiptResponseV5(
                        identifier,
                        messageUIds,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                RLog.d(
                                        TAG,
                                        "sendReadReceiptResponseV5Batch onSuccess, batch size: "
                                                + messageUIds.size());
                                processingSendRequests.remove(requestKey);

                                // 通知数据变化
                                notifyDataChange(KEY_SEND_READ_RECEIPT_RESPONSE_V5, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                RLog.e(
                                        TAG,
                                        "sendReadReceiptResponseV5Batch onError: "
                                                + coreErrorCode
                                                + ", batch size: "
                                                + messageUIds.size());
                                processingSendRequests.remove(requestKey);
                                notifyDataError(KEY_SEND_READ_RECEIPT_RESPONSE_V5, coreErrorCode);
                            }
                        });
    }

    /**
     * 获取消息已读信息V5 - 支持限频处理和缓存
     *
     * @param identifier 会话标识
     * @param messageUIds 消息UID列表
     */
    public void getMessageReadReceiptInfoV5(
            @NonNull ConversationIdentifier identifier, @NonNull List<String> messageUIds) {
        if (messageUIds.isEmpty()) {
            RLog.w(TAG, "getMessageReadReceiptInfoV5: messageUIds is empty");
            return;
        }

        // 如果只有一个消息UID，先检查缓存
        if (messageUIds.size() == 1) {
            String messageUId = messageUIds.get(0);
            String cacheKey = generateCacheKey(identifier, messageUId);
            ReadReceiptInfoV5 cachedInfo = readReceiptInfoCache.get(cacheKey);

            if (cachedInfo != null) {
                // 直接返回缓存结果
                List<ReadReceiptInfoV5> cachedList = new ArrayList<>();
                cachedList.add(cachedInfo);
                notifyDataChange(KEY_READ_RECEIPT_INFO_V5, cachedList);
                return;
            }
        }

        // 分批处理，每批最多100个
        if (messageUIds.size() <= MAX_BATCH_SIZE) {
            addToRateLimitQueue(identifier, messageUIds);
        } else {
            RLog.d(
                    TAG,
                    "getMessageReadReceiptInfoV5: splitting "
                            + messageUIds.size()
                            + " messages into batches");
            for (int i = 0; i < messageUIds.size(); i += MAX_BATCH_SIZE) {
                int endIndex = Math.min(i + MAX_BATCH_SIZE, messageUIds.size());
                List<String> batch = messageUIds.subList(i, endIndex);
                addToRateLimitQueue(identifier, batch);
            }
        }
    }

    /**
     * 将请求添加到限频队列
     *
     * @param identifier 会话标识
     * @param messageUIds 消息UID列表
     */
    private void addToRateLimitQueue(ConversationIdentifier identifier, List<String> messageUIds) {
        ReadReceiptRequest request = new ReadReceiptRequest(identifier, messageUIds);
        pendingReadReceiptRequests.offer(request);

        RLog.d(
                TAG,
                "Added request to rate limit queue, queue size: "
                        + pendingReadReceiptRequests.size());

        // 如果当前没有在处理限频队列，启动处理
        if (!isRateLimitProcessing) {
            isRateLimitProcessing = true;
            rateLimitHandler.post(rateLimitRunnable);
        }
    }

    /** 处理限频队列中的请求 */
    private void processRateLimitedRequests() {
        int processedCount = 0;

        // 每次最多处理10个请求
        while (processedCount < RATE_LIMIT_BATCH_SIZE && !pendingReadReceiptRequests.isEmpty()) {
            ReadReceiptRequest request = pendingReadReceiptRequests.poll();
            if (request != null) {
                getMessageReadReceiptInfoV5Batch(request.identifier, request.messageUIds);
                processedCount++;
            }
        }

        if (processedCount > 0) {
            RLog.d(
                    TAG,
                    "Processed "
                            + processedCount
                            + " requests from rate limit queue, remaining: "
                            + pendingReadReceiptRequests.size());
        }

        // 如果队列为空，停止处理
        if (pendingReadReceiptRequests.isEmpty()) {
            isRateLimitProcessing = false;
            RLog.d(TAG, "Rate limit processing completed, queue is empty");
        }
    }

    /**
     * 获取消息已读信息V5 - 单批处理
     *
     * @param identifier 会话标识
     * @param messageUIds 消息UID列表（不超过100个）
     */
    private void getMessageReadReceiptInfoV5Batch(
            @NonNull ConversationIdentifier identifier, @NonNull List<String> messageUIds) {
        if (messageUIds.isEmpty()) {
            return;
        }

        // 生成请求标识，用于去重
        String requestKey = generateRequestKey(messageUIds);

        // 检查是否正在处理相同请求
        if (processingGetRequests.contains(requestKey)) {
            RLog.d(TAG, "getMessageReadReceiptInfoV5Batch: request already in progress");
            return;
        }

        processingGetRequests.add(requestKey);

        RongCoreClient.getInstance()
                .getMessageReadReceiptInfoV5(
                        identifier,
                        messageUIds,
                        new IRongCoreCallback.ResultCallback<List<ReadReceiptInfoV5>>() {
                            @Override
                            public void onSuccess(List<ReadReceiptInfoV5> readReceiptInfoV5s) {
                                RLog.d(
                                        TAG,
                                        "getMessageReadReceiptInfoV5Batch onSuccess, batch size: "
                                                + messageUIds.size());
                                processingGetRequests.remove(requestKey);

                                // 如果只有一个消息UID且没有缓存，将结果存入缓存
                                if (messageUIds.size() == 1
                                        && readReceiptInfoV5s != null
                                        && !readReceiptInfoV5s.isEmpty()) {
                                    String messageUId = messageUIds.get(0);
                                    String cacheKey = generateCacheKey(identifier, messageUId);
                                    ReadReceiptInfoV5 readReceiptInfo = readReceiptInfoV5s.get(0);
                                    readReceiptInfoCache.put(cacheKey, readReceiptInfo);
                                }

                                // 通知数据变化
                                notifyDataChange(KEY_READ_RECEIPT_INFO_V5, readReceiptInfoV5s);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                RLog.e(
                                        TAG,
                                        "getMessageReadReceiptInfoV5Batch onError: "
                                                + coreErrorCode
                                                + ", batch size: "
                                                + messageUIds.size());
                                processingGetRequests.remove(requestKey);
                                notifyDataError(KEY_READ_RECEIPT_INFO_V5, coreErrorCode);
                            }
                        });
    }

    /**
     * 生成请求的唯一标识
     *
     * @param messageUIds 消息UID列表
     * @return 请求唯一标识
     */
    private String generateRequestKey(List<String> messageUIds) {
        StringBuilder sb = new StringBuilder();
        for (String uid : messageUIds) {
            sb.append(uid).append(",");
        }
        return sb.toString();
    }

    /**
     * 更新缓存
     *
     * @param responses 已读回执响应列表
     */
    private void updateReadReceiptInfoCache(List<ReadReceiptResponseV5> responses) {
        for (ReadReceiptResponseV5 response : responses) {
            String cacheKey = generateCacheKey(response.getIdentifier(), response.getMessageUId());

            // 查找缓存中的现有数据
            ReadReceiptInfoV5 cachedInfo = readReceiptInfoCache.get(cacheKey);
            if (cachedInfo != null) {
                // 更新缓存中的readCount
                cachedInfo.setReadCount(response.getReadCount());
                RLog.d(
                        TAG,
                        "Updated cache for key: "
                                + cacheKey
                                + ", new readCount: "
                                + response.getReadCount());

                // 通知数据变化
                List<ReadReceiptInfoV5> updatedList = new ArrayList<>();
                updatedList.add(cachedInfo);
                notifyDataChange(KEY_READ_RECEIPT_INFO_V5, updatedList);
            }
        }
    }

    /**
     * 生成缓存键
     *
     * @param identifier 会话标识
     * @param messageUId 消息UID
     * @return 缓存键
     */
    private String generateCacheKey(ConversationIdentifier identifier, String messageUId) {
        return identifier.getType().getValue() + "_" + identifier.getTargetId() + "_" + messageUId;
    }

    @Override
    public void stop() {
        super.stop();
        processingSendRequests.clear();
        processingGetRequests.clear();

        // 清理限频处理相关资源
        rateLimitHandler.removeCallbacks(rateLimitRunnable);
        pendingReadReceiptRequests.clear();
        isRateLimitProcessing = false;

        // 清理缓存
        readReceiptInfoCache.clear();

        RLog.d(TAG, "ReadReceiptV5Handler stopped and cleared all records");
        RongCoreClient.getInstance()
                .removeMessageReadReceiptV5Listener(messageReadReceiptV5Listener);
    }
}
