package io.rong.imkit.handler;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import io.rong.common.rlog.RLog;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imkit.feature.expose.SubmitReadReceiptV5Manager;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.IRongCoreListener;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageIdentifier;
import io.rong.imlib.model.ReadReceiptInfoV5;
import io.rong.imlib.model.ReadReceiptResponseV5;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ReadReceiptV5Handler
 *
 * <p>处理已读回执V5相关的数据请求
 *
 * @since 5.30.0
 */
public class ReadReceiptV5Handler extends MultiDataHandler {

    private static final String TAG = "ReadReceiptV5Handler";

    /** 加载消息时，优先查询消息已读V5后再执行 notifyDataChange 如果没有开通V5或者不需要查询，则直接执行 notifyDataChange */
    public static final DataKey<HashMap<String, ReadReceiptInfoV5>>
            KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5 =
                    DataKey.obtain(
                            "KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5",
                            (Class<HashMap<String, ReadReceiptInfoV5>>) (Class<?>) HashMap.class);

    /** 收到已读V5回执监听后，刷新UI */
    public static final DataKey<HashMap<String, ReadReceiptInfoV5>>
            KEY_MESSAGE_READ_RECEIPT_V5_LISTENER =
                    DataKey.obtain(
                            "KEY_MESSAGE_READ_RECEIPT_V5_LISTENER",
                            (Class<HashMap<String, ReadReceiptInfoV5>>) (Class<?>) HashMap.class);

    /** 用来查询会话列表ConversationListViewModel单聊lastMsg的已读V5信息 */
    public static final DataKey<HashMap<String, ReadReceiptInfoV5>>
            KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5_BY_IDENTIFIER =
                    DataKey.obtain(
                            "KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5_BY_IDENTIFIER",
                            (Class<HashMap<String, ReadReceiptInfoV5>>) (Class<?>) HashMap.class);

    // 最大批处理大小
    private static final int MAX_BATCH_SIZE = 100;

    /** 批量提交管理器，用来批量发送已读V5响应 */
    private final SubmitReadReceiptV5Manager mSubmitReadReceiptV5Manager =
            new SubmitReadReceiptV5Manager();

    /** 调用sendReadReceiptResponseV5过的集合 */
    private final Set<String> mPendingSendReadReceiptResponseV5Cache = new HashSet<>();

    // 缓存Map，Key是ConversationIdentifier，Value是ReadReceiptInfoV5
    // 每个会话只缓存最后一个消息的已读回执信息
    private final ConcurrentHashMap<String, ReadReceiptInfoV5> readReceiptInfoCache =
            new ConcurrentHashMap<>();

    // 消息V5回执监听器
    private final IRongCoreListener.MessageReadReceiptV5Listener messageReadReceiptV5Listener =
            new IRongCoreListener.MessageReadReceiptV5Listener() {
                @Override
                public void onMessageReceiptResponse(List<ReadReceiptResponseV5> responses) {
                    // 更新UI
                    HashMap<String, ReadReceiptInfoV5> map = new HashMap<>();
                    for (ReadReceiptResponseV5 responseV5 : responses) {
                        if (responseV5.getIdentifier() == null
                                || TextUtils.isEmpty(responseV5.getMessageUId())) {
                            continue;
                        }
                        ConversationIdentifier id = responseV5.getIdentifier();
                        ReadReceiptInfoV5 infoV5 = new ReadReceiptInfoV5();
                        infoV5.setIdentifier(responseV5.getIdentifier());
                        infoV5.setMessageUId(responseV5.getMessageUId());
                        infoV5.setReadCount(responseV5.getReadCount());
                        infoV5.setUnreadCount(responseV5.getUnreadCount());
                        infoV5.setTotalCount(responseV5.getTotalCount());
                        map.put(responseV5.getMessageUId(), infoV5);
                        // 缓存通过Listener回来的 readReceiptInfoV5
                        readReceiptInfoCache.put(generateCacheKey(id), infoV5);
                    }
                    if (!map.isEmpty()) {
                        notifyDataChange(KEY_MESSAGE_READ_RECEIPT_V5_LISTENER, map);
                    }
                }
            };

    public ReadReceiptV5Handler() {
        super();
        RongCoreClient.getInstance().addMessageReadReceiptV5Listener(messageReadReceiptV5Listener);
    }

    @Override
    public void stop() {
        super.stop();
        RongCoreClient.getInstance()
                .removeMessageReadReceiptV5Listener(messageReadReceiptV5Listener);
        mSubmitReadReceiptV5Manager.release();
        readReceiptInfoCache.clear();
    }

    /**
     * 绑定会话
     *
     * @param id
     */
    public void bindConversation(ConversationIdentifier id) {
        mSubmitReadReceiptV5Manager.bindConversation(id);
    }

    /**
     * 批量获取消息已读信息（V5）
     *
     * @param id 会话标识
     * @param messages 消息列表
     */
    public void getMessageReadReceiptInfoV5(ConversationIdentifier id, List<Message> messages) {
        if (!AppSettingsHandler.getInstance().isReadReceiptV5Enabled(id.getType())
                || messages == null
                || messages.isEmpty()) {
            notifyDataChange(KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5, new HashMap<>());
            return;
        }
        List<String> uIds = new ArrayList<>();
        // 查询 messages 是否合法，如果都不合法，则不用查询直接返回
        for (Message message : messages) {
            // 需要满足：合法的消息、发送方向的消息、非本地消息（有uid）
            if (message != null
                    && Message.MessageDirection.SEND == message.getMessageDirection()
                    && !TextUtils.isEmpty(message.getUId())
                    && message.isNeedReceipt()) {
                uIds.add(message.getUId());
            }
        }
        if (uIds.isEmpty()) {
            notifyDataChange(KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5, new HashMap<>());
            return;
        }
        RongCoreClient.getInstance()
                .getMessageReadReceiptInfoV5(
                        id,
                        uIds,
                        new IRongCoreCallback.ResultCallback<List<ReadReceiptInfoV5>>() {
                            @Override
                            public void onSuccess(List<ReadReceiptInfoV5> readReceiptInfoV5s) {
                                HashMap<String, ReadReceiptInfoV5> map = new HashMap<>();
                                if (readReceiptInfoV5s != null && !readReceiptInfoV5s.isEmpty()) {
                                    for (ReadReceiptInfoV5 infoV5 : readReceiptInfoV5s) {
                                        map.put(infoV5.getMessageUId(), infoV5);
                                    }
                                }
                                notifyDataChange(KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5, map);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                notifyDataChange(
                                        KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5, new HashMap<>());
                            }
                        });
    }

    /**
     * 发送已读回执V5响应
     *
     * @param message Message
     */
    public void sendReadReceiptResponseV5(Message message) {
        if (message == null || TextUtils.isEmpty(message.getUId())) {
            return;
        }
        // 已执行过任务，不再执行
        if (mPendingSendReadReceiptResponseV5Cache.contains(message.getUId())) {
            return;
        }
        mPendingSendReadReceiptResponseV5Cache.add(message.getUId());
        // 不需要发送V5回执的场景
        if (!AppSettingsHandler.getInstance().isReadReceiptV5Enabled(message.getConversationType())
                || TextUtils.isEmpty(message.getUId())
                || !message.isNeedReceipt()
                || message.isSentReceipt()
                || Message.MessageDirection.SEND == message.getMessageDirection()) {
            return;
        }
        mSubmitReadReceiptV5Manager.addSubmitTask(message.getUId());
    }

    /**
     * 生成缓存键
     *
     * @param identifier 会话标识
     * @return 缓存键
     */
    private String generateCacheKey(ConversationIdentifier identifier) {
        return identifier.getType().getValue()
                + "_"
                + identifier.getTargetId()
                + "_"
                + identifier.getChannelId();
    }

    /** 获取消息已读信息V5 ByIdentifier入口 */
    public void getMessageReadReceiptInfoV5ByIdentifiers(
            @NonNull List<MessageIdentifier> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return;
        }

        List<MessageIdentifier> unCachedIdentifiers = new ArrayList<>();
        HashMap<String, ReadReceiptInfoV5> cacheMap = new HashMap<>();

        // 检查缓存并收集结果，不聚合内容
        for (MessageIdentifier identifier : identifiers) {
            String cacheKey = generateCacheKey(identifier.getIdentifier());
            ReadReceiptInfoV5 cachedInfo = readReceiptInfoCache.get(cacheKey);
            if (cachedInfo != null) {
                // 只有当缓存的消息UID与请求的消息UID匹配时才使用缓存
                if (cachedInfo.getMessageUId() != null
                        && cachedInfo.getMessageUId().equals(identifier.getMessageUId())) {
                    cacheMap.put(cachedInfo.getMessageUId(), cachedInfo);
                } else {
                    // 缓存的是其他消息，需要重新请求
                    unCachedIdentifiers.add(identifier);
                }
            } else {
                unCachedIdentifiers.add(identifier);
            }
        }

        // 如果有缓存结果，先通知
        if (!cacheMap.isEmpty()) {
            notifyDataChange(KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5_BY_IDENTIFIER, cacheMap);
        }

        // 如果没有未缓存的消息，直接返回
        if (unCachedIdentifiers.isEmpty()) {
            return;
        }

        // 分批处理未缓存的消息，参考 sendReadReceiptResponseV5 方法
        if (unCachedIdentifiers.size() <= MAX_BATCH_SIZE) {
            getMessageReadReceiptInfoV5ByIdentifierBatch(unCachedIdentifiers);
        } else {
            RLog.d(
                    TAG,
                    "getMessageReadReceiptInfoV5ByIdentifier: splitting "
                            + unCachedIdentifiers.size()
                            + " messages into batches");
            for (int i = 0; i < unCachedIdentifiers.size(); i += MAX_BATCH_SIZE) {
                int endIndex = Math.min(i + MAX_BATCH_SIZE, unCachedIdentifiers.size());
                List<MessageIdentifier> batch = unCachedIdentifiers.subList(i, endIndex);
                getMessageReadReceiptInfoV5ByIdentifierBatch(batch);
            }
        }
    }

    /**
     * 分批获取消息已读信息V5ByIdentifier - 单批处理
     *
     * @param identifiers MessageIdentifier列表（不超过100个）
     */
    private void getMessageReadReceiptInfoV5ByIdentifierBatch(
            @NonNull List<MessageIdentifier> identifiers) {
        if (identifiers.isEmpty()) {
            return;
        }
        RongCoreClient.getInstance()
                .getMessageReadReceiptInfoV5ByIdentifiers(
                        identifiers,
                        new IRongCoreCallback.ResultCallback<List<ReadReceiptInfoV5>>() {
                            @Override
                            public void onSuccess(List<ReadReceiptInfoV5> readReceiptInfoV5s) {
                                RLog.d(
                                        TAG,
                                        "getMessageReadReceiptInfoV5ByIdentifierBatch onSuccess, batch size: "
                                                + identifiers.size());
                                if (readReceiptInfoV5s == null || readReceiptInfoV5s.isEmpty()) {
                                    return;
                                }
                                HashMap<String, ReadReceiptInfoV5> resultMap = new HashMap<>();
                                // 更新缓存，每个会话只缓存最后一个消息的信息
                                for (ReadReceiptInfoV5 info : readReceiptInfoV5s) {
                                    // 找到对应的 MessageIdentifier 并更新缓存
                                    String cacheKey = generateCacheKey(info.getIdentifier());
                                    // 直接覆盖缓存，每个会话只保留最后一个消息的信息
                                    readReceiptInfoCache.put(cacheKey, info);
                                    resultMap.put(info.getMessageUId(), info);
                                    RLog.d(
                                            TAG,
                                            "Updated cache for conversation: "
                                                    + cacheKey
                                                    + ", messageUId: "
                                                    + info.getMessageUId());
                                }
                                if (!resultMap.isEmpty()) {
                                    notifyDataChange(
                                            KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5_BY_IDENTIFIER,
                                            resultMap);
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                RLog.e(
                                        TAG,
                                        "getMessageReadReceiptInfoV5ByIdentifierBatch onError: "
                                                + coreErrorCode
                                                + ", batch size: "
                                                + identifiers.size());
                            }
                        });
    }
}
