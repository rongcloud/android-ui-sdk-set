package io.rong.imkit.conversation.messgelist.processor;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.feature.mention.RongMentionManager;
import io.rong.imkit.manager.hqvoicemessage.AutoDownloadEntry;
import io.rong.imkit.manager.hqvoicemessage.HQVoiceMsgDownloadManager;
import io.rong.imkit.model.State;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.db.model.GroupMember;
import io.rong.imkit.userinfo.db.model.User;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.ReadReceiptInfo;
import io.rong.imlib.model.UserInfo;
import io.rong.message.HQVoiceMessage;

public class GroupBusinessProcessor extends BaseBusinessProcessor {
    private static final String TAG = "GroupBusinessProcessor";

    @Override
    public void init(final MessageViewModel messageViewModel, Bundle bundle) {
        messageViewModel.getPageEventLiveData().removeSource(RongUserInfoManager.getInstance().getAllUsersLiveData());
        messageViewModel.getPageEventLiveData().addSource(RongUserInfoManager.getInstance().getAllUsersLiveData(), new Observer<List<User>>() {
            @Override
            public void onChanged(List<User> users) {
                if (users != null && users.size() > 0) {
                    for (UiMessage item : messageViewModel.getUiMessages()) {
                        item.onUserInfoUpdate(users);
                    }
                    messageViewModel.updateUiMessages();
                }
            }
        });
        messageViewModel.getPageEventLiveData().removeSource(RongUserInfoManager.getInstance().getAllGroupMembersLiveData());
        messageViewModel.getPageEventLiveData().addSource(RongUserInfoManager.getInstance().getAllGroupMembersLiveData(), new Observer<List<GroupMember>>() {
            @Override
            public void onChanged(List<GroupMember> groupMembers) {
                if (groupMembers != null && groupMembers.size() > 0) {
                    boolean isExist = false;
                    for (GroupMember member : groupMembers) {
                        if (member.groupId == messageViewModel.getCurTargetId()) {
                            isExist = true;
                            for (UiMessage item : messageViewModel.getUiMessages()) {
                                item.onGroupMemberInfoUpdate(member);
                            }
                        }
                    }
                    if (isExist) {
                        messageViewModel.updateUiMessages();
                    }
                }
            }
        });
    }

    @Override
    public boolean onReceived(MessageViewModel viewModel, UiMessage message, int left, boolean hasPackage, boolean offline) {
        if (left == 0 && !hasPackage) {
            if (RongConfigCenter.conversationConfig().isEnableMultiDeviceSync(viewModel.getCurConversationType()) && !RongConfigCenter.conversationConfig().isShowReadReceipt(viewModel.getCurConversationType())) {
                IMCenter.getInstance().syncConversationReadStatus(message.getConversationType(), message.getTargetId(), message.getSentTime(), null);
            }
        }
        return super.onReceived(viewModel, message, left, hasPackage, offline);
    }

    @Override
    public void onExistUnreadMessage(MessageViewModel viewModel, Conversation conversation, int unreadMessageCount) {
        boolean syncReadStatus = RongConfigCenter.conversationConfig().isEnableMultiDeviceSync(viewModel.getCurConversationType());
        if (syncReadStatus) {
            IMCenter.getInstance().syncConversationReadStatus(viewModel.getCurConversationType(), viewModel.getCurTargetId(), conversation.getSentTime(), null);
        }
    }


    @Override
    public boolean onUserPortraitLongClick(Context context, Conversation.ConversationType conversationType, UserInfo userInfo, String targetId) {
        if (userInfo != null && !userInfo.getUserId().equals(RongIMClient.getInstance().getCurrentUserId())) {
            RongMentionManager.getInstance().mentionMember(conversationType, targetId, userInfo.getUserId());
            return true;
        }
        return false;
    }


    @Override
    public void onMessageReceiptRequest(final MessageViewModel viewModel, Conversation.ConversationType conversationType, String targetId, String messageUId) {
        if (!RongConfigCenter.conversationConfig().isShowReadReceiptRequest(viewModel.getCurConversationType())) {
            return;
        }
        for (final UiMessage item : viewModel.getUiMessages()) {
            if (item.getMessage().getUId().equals(messageUId)) {
                ReadReceiptInfo readReceiptInfo = item.getMessage().getReadReceiptInfo();
                if (readReceiptInfo == null) {
                    readReceiptInfo = new ReadReceiptInfo();
                    item.setReadReceiptInfo(readReceiptInfo);
                }
                if (readReceiptInfo.isReadReceiptMessage() && readReceiptInfo.hasRespond()) {
                    return;
                }
                readReceiptInfo.setIsReadReceiptMessage(true);
                readReceiptInfo.setHasRespond(false);
                List<Message> messageList = new ArrayList<>();
                messageList.add(item.getMessage());
                RongIMClient.getInstance().sendReadReceiptResponse(viewModel.getCurConversationType(), viewModel.getCurTargetId(), messageList, new RongIMClient.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        item.getMessage().getReadReceiptInfo().setHasRespond(true);
                        viewModel.refreshSingleMessage(item);
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode errorCode) {
                        RLog.e(TAG, "sendReadReceiptResponse failed, errorCode = " + errorCode);
                    }
                });
                break;
            }
        }
    }

    /**
     * 当加载完消息，群组发送已读回执
     */
    @Override
    public void onLoadMessage(MessageViewModel viewModel, List<Message> messages) {
        if (!RongConfigCenter.conversationConfig().isShowReadReceiptRequest(viewModel.getCurConversationType())) {
            return;
        }
        List<io.rong.imlib.model.Message> responseMessageList = new ArrayList<>();
        for (io.rong.imlib.model.Message message : messages) {
            ReadReceiptInfo readReceiptInfo = message.getReadReceiptInfo();
            if (readReceiptInfo == null) {
                continue;
            }
            if (readReceiptInfo.isReadReceiptMessage() && !readReceiptInfo.hasRespond()) {
                responseMessageList.add(message);
            }
        }
        if (responseMessageList.size() > 0) {
            RongIMClient.getInstance().sendReadReceiptResponse(viewModel.getCurConversationType(), viewModel.getCurTargetId(), responseMessageList, null);
        }
    }
}
