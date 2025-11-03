package io.rong.imkit.handler;

import static io.rong.imlib.model.ReadReceiptUsersOption.ReadStatus.READ;
import static io.rong.imlib.model.ReadReceiptUsersOption.ReadStatus.UNREAD;

import android.os.Handler;
import android.os.Looper;
import io.rong.common.rlog.RLog;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.ReadReceiptInfoV5;
import io.rong.imlib.model.ReadReceiptUser;
import io.rong.imlib.model.ReadReceiptUsersOption;
import io.rong.imlib.model.ReadReceiptUsersResult;
import java.util.ArrayList;
import java.util.List;

/**
 * ReadReceiptDetailHandler
 *
 * <p>处理已读回执V5详情相关的数据请求
 *
 * @since 5.30.0
 */
public class ReadReceiptDetailHandler extends MultiDataHandler {

    private static final String TAG = "ReadReceiptV5Handler";

    /** 查询这条消息的已读V5信息 */
    public static final DataKey<ReadReceiptInfoV5> KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5 =
            DataKey.obtain("KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5", ReadReceiptInfoV5.class);

    /** 获取消息的已读列表 */
    public static final DataKey<List<ReadReceiptUser>> KEY_MESSAGE_READ_V5_USER_LIST =
            DataKey.obtain(
                    "KEY_MESSAGE_READ_V5_USER_LIST",
                    (Class<List<ReadReceiptUser>>) (Class<?>) List.class);

    /** 获取消息的未读列表 */
    public static final DataKey<List<ReadReceiptUser>> KEY_MESSAGE_UNREAD_V5_USER_LIST =
            DataKey.obtain(
                    "KEY_MESSAGE_UNREAD_V5_USER_LIST",
                    (Class<List<ReadReceiptUser>>) (Class<?>) List.class);

    private final int pageCount = 100;
    private String readPageToken = null;
    private String unreadPageToken = null;
    private final List<ReadReceiptUser> readUsers = new ArrayList<>();
    private final List<ReadReceiptUser> unreadUsers = new ArrayList<>();
    private boolean isRequestReadList = false;
    private boolean isRequestUnreadList = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final int maxRetryCount = 5;
    private int retryCountForReadInfoV5 = 0;

    public ReadReceiptDetailHandler() {
        super();
    }

    @Override
    public void stop() {
        super.stop();
    }

    /**
     * 批量获取消息已读信息（V5）
     *
     * @param message 消息
     */
    public void getMessageReadReceiptInfoV5(Message message) {
        if (!isAlive()) {
            return;
        }
        if (message == null) {
            return;
        }
        if (!AppSettingsHandler.getInstance()
                .isReadReceiptV5Enabled(message.getConversationType())) {
            return;
        }
        List<String> uIds = new ArrayList<>();
        uIds.add(message.getUId());
        RongCoreClient.getInstance()
                .getMessageReadReceiptInfoV5(
                        ConversationIdentifier.obtain(message),
                        uIds,
                        new IRongCoreCallback.ResultCallback<List<ReadReceiptInfoV5>>() {
                            @Override
                            public void onSuccess(List<ReadReceiptInfoV5> result) {
                                if (result != null && !result.isEmpty()) {
                                    notifyDataChange(
                                            KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5, result.get(0));
                                    return;
                                }
                                // 重试逻辑
                                if (retryCountForReadInfoV5++ < maxRetryCount) {
                                    runDelay(() -> getMessageReadReceiptInfoV5(message));
                                } else {
                                    notifyDataChange(KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5, null);
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode code) {
                                if (code == IRongCoreEnum.CoreErrorCode.RC_REQUEST_OVERFREQUENCY) {
                                    getMessageReadReceiptInfoV5(message);
                                } else {
                                    notifyDataChange(KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5, null);
                                }
                            }
                        });
    }

    public void getMessagesReadUsersByPage(Message message) {
        if (!isAlive()) {
            return;
        }
        String pageToken;
        if (readPageToken == null) {
            pageToken = "";
        } else if ("".equals(readPageToken)) {
            RLog.e(TAG, "getMessagesReadUsersByPage return ,readPageToken empty!");
            return;
        } else {
            pageToken = readPageToken;
        }
        if (isRequestReadList) {
            return;
        }
        isRequestReadList = true;
        ReadReceiptUsersOption option =
                new ReadReceiptUsersOption(
                        pageToken, pageCount, ReadReceiptUsersOption.Order.DESCEND, READ);
        RongCoreClient.getInstance()
                .getMessagesReadReceiptUsersByPageV5(
                        ConversationIdentifier.obtain(message),
                        message.getUId(),
                        option,
                        new IRongCoreCallback.ResultCallback<ReadReceiptUsersResult>() {
                            @Override
                            public void onSuccess(ReadReceiptUsersResult result) {
                                readPageToken = result.getPageToken();
                                if (result.getUsers() != null && !result.getUsers().isEmpty()) {
                                    readUsers.addAll(result.getUsers());
                                }
                                notifyDataChange(KEY_MESSAGE_READ_V5_USER_LIST, result.getUsers());
                                isRequestReadList = false;
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode code) {
                                if (code == IRongCoreEnum.CoreErrorCode.RC_REQUEST_OVERFREQUENCY) {
                                    isRequestReadList = false;
                                    getMessagesReadUsersByPage(message);
                                } else {
                                    notifyDataChange(
                                            KEY_MESSAGE_READ_V5_USER_LIST, new ArrayList<>());
                                    isRequestReadList = false;
                                }
                            }
                        });
    }

    public void getMessagesUnReadUsersByPage(Message message) {
        if (!isAlive()) {
            return;
        }
        String pageToken;
        if (unreadPageToken == null) {
            pageToken = "";
        } else if ("".equals(unreadPageToken)) {
            RLog.e(TAG, "getMessagesUnReadUsersByPage return ,unreadPageToken empty!");
            return;
        } else {
            pageToken = unreadPageToken;
        }
        if (isRequestUnreadList) {
            return;
        }
        isRequestUnreadList = true;
        ReadReceiptUsersOption option =
                new ReadReceiptUsersOption(
                        pageToken, pageCount, ReadReceiptUsersOption.Order.DESCEND, UNREAD);
        RongCoreClient.getInstance()
                .getMessagesReadReceiptUsersByPageV5(
                        ConversationIdentifier.obtain(message),
                        message.getUId(),
                        option,
                        new IRongCoreCallback.ResultCallback<ReadReceiptUsersResult>() {
                            @Override
                            public void onSuccess(ReadReceiptUsersResult result) {
                                unreadPageToken = result.getPageToken();
                                if (result.getUsers() != null && !result.getUsers().isEmpty()) {
                                    unreadUsers.addAll(result.getUsers());
                                }
                                notifyDataChange(
                                        KEY_MESSAGE_UNREAD_V5_USER_LIST, result.getUsers());
                                isRequestUnreadList = false;
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode code) {
                                if (code == IRongCoreEnum.CoreErrorCode.RC_REQUEST_OVERFREQUENCY) {
                                    isRequestUnreadList = false;
                                    getMessagesUnReadUsersByPage(message);
                                } else {
                                    notifyDataChange(
                                            KEY_MESSAGE_UNREAD_V5_USER_LIST, new ArrayList<>());
                                    isRequestUnreadList = false;
                                }
                            }
                        });
    }

    private void runDelay(Runnable runnable) {
        mainHandler.postDelayed(runnable, 1000);
    }
}
