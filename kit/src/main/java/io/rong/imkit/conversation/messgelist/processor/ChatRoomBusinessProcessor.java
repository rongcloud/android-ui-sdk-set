package io.rong.imkit.conversation.messgelist.processor;

import android.content.Context;
import android.os.Bundle;

import androidx.lifecycle.Observer;

import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.config.ConversationConfig;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.event.uievent.ShowWarningDialogEvent;
import io.rong.imkit.feature.mention.RongMentionManager;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.db.model.User;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;

public class ChatRoomBusinessProcessor extends BaseBusinessProcessor {
    private static final String TAG = "ChatRoomBusinessProcess";
    private int rc_chatRoom_first_pull_message_count;

    @Override
    public int getHistoryMessageCount() {
        int count = rc_chatRoom_first_pull_message_count;
        if (count == 0) {
            //等于0取默认值
            return 10;
        } else if (count == -1) {
            return 0;
        } else {
            return count;
        }
    }

    @Override
    public void init(final MessageViewModel messageViewModel, Bundle bundle) {
        rc_chatRoom_first_pull_message_count = RongConfigCenter.conversationConfig().rc_chatroom_first_pull_message_count;
        boolean createIfNotExist = bundle.getBoolean(RouteUtils.CREATE_CHATROOM, true);
        if (createIfNotExist) {
            RongIMClient.getInstance().joinChatRoom(messageViewModel.getCurTargetId(), getHistoryMessageCount(), new RongIMClient.OperationCallback() {
                @Override
                public void onSuccess() {
                    RLog.i(TAG, "joinChatRoom onSuccess : " + messageViewModel.getCurTargetId());
                }

                @Override
                public void onError(RongIMClient.ErrorCode coreErrorCode) {
                    RLog.e(TAG, "joinChatRoom onError : " + coreErrorCode);
                    if (coreErrorCode == RongIMClient.ErrorCode.RC_NET_UNAVAILABLE || coreErrorCode == RongIMClient.ErrorCode.RC_NET_CHANNEL_INVALID) {
                        messageViewModel.executePageEvent(new ShowWarningDialogEvent(messageViewModel.getApplication().getString(R.string.rc_notice_network_unavailable)));
                    } else {
                        messageViewModel.executePageEvent(new ShowWarningDialogEvent(messageViewModel.getApplication().getString(R.string.rc_join_chatroom_failure)));
                    }
                }
            });
        } else {
            RongIMClient.getInstance().joinExistChatRoom(messageViewModel.getCurTargetId(), rc_chatRoom_first_pull_message_count, new RongIMClient.OperationCallback() {
                @Override
                public void onSuccess() {
                    RLog.i(TAG, "joinExistChatRoom onSuccess : " + messageViewModel.getCurTargetId());
                }

                @Override
                public void onError(RongIMClient.ErrorCode coreErrorCode) {
                    RLog.e(TAG, "joinExistChatRoom onError : " + coreErrorCode);
                    if (coreErrorCode == RongIMClient.ErrorCode.RC_NET_UNAVAILABLE || coreErrorCode == RongIMClient.ErrorCode.RC_NET_CHANNEL_INVALID) {
                        messageViewModel.executePageEvent(new ShowWarningDialogEvent(messageViewModel.getApplication().getString(R.string.rc_notice_network_unavailable)));
                    } else {
                        messageViewModel.executePageEvent(new ShowWarningDialogEvent(messageViewModel.getApplication().getString(R.string.rc_join_chatroom_failure)));
                    }
                }
            });
        }
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
    }

    @Override
    public boolean onUserPortraitLongClick(Context context, Conversation.ConversationType conversationType, UserInfo userInfo, String targetId) {
        if (userInfo != null && !userInfo.getUserId().equals(RongIMClient.getInstance().getCurrentUserId())) {
            RongMentionManager.getInstance().mentionMember(conversationType, targetId, userInfo.getUserId());
            return true;
        }
        return false;
    }

}
