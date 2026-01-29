package io.rong.imkit.conversation.readreceipt;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.handler.ReadReceiptDetailHandler;
import io.rong.imkit.model.ReadReceiptData;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.ExtendedGroupUserInfo;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imkit.usermanage.interfaces.OnDataChangeEnhancedListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.ReadReceiptInfoV5;
import io.rong.imlib.model.ReadReceiptUser;
import io.rong.imlib.model.UserInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * 功能描述: 群组消息阅读状态详情页面
 *
 * @author rongcloud
 * @since 5.30.0
 */
public class MessageReadDetailViewModel extends BaseViewModel
        implements RongUserInfoManager.UserDataObserver {
    private static final String TAG = "MessageReadDetailVM";

    private final Message message;
    private ReadReceiptInfoV5 readReceiptInfoV5;
    private final ReadReceiptDetailHandler readReceiptDetailHandler;
    private boolean isInfoProvider = false;
    // LiveData
    private final MutableLiveData<ReadReceiptInfoV5> readReceiptInfoV5LiveData =
            new MutableLiveData<>();
    private final MutableLiveData<List<ReadReceiptData>> readUsersLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<List<ReadReceiptData>> unreadUsersLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<GroupMemberInfo> memberInfoUpdateLiveData =
            new MutableLiveData<>();

    public MessageReadDetailViewModel(@NonNull Bundle arguments) {
        super(arguments);
        message = arguments.getParcelable(KitConstants.KEY_MESSAGE);
        readReceiptInfoV5 = arguments.getParcelable(KitConstants.KEY_READ_RECEIPT_INFO_V5);
        RongUserInfoManager.getInstance().addUserDataObserver(this);
        isInfoProvider =
                RongUserInfoManager.getInstance().getDataSourceType()
                        == RongUserInfoManager.DataSourceType.INFO_PROVIDER;
        readReceiptDetailHandler = new ReadReceiptDetailHandler();
        readReceiptDetailHandler.addDataChangeListener(
                ReadReceiptDetailHandler.KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5,
                new OnDataChangeEnhancedListener<ReadReceiptInfoV5>() {
                    @Override
                    public void onDataChange(ReadReceiptInfoV5 value) {
                        readReceiptInfoV5 = value;
                        readReceiptInfoV5LiveData.postValue(value);
                    }
                });
        readReceiptDetailHandler.addDataChangeListener(
                ReadReceiptDetailHandler.KEY_MESSAGE_READ_V5_USER_LIST,
                (OnDataChangeEnhancedListener<List<ReadReceiptUser>>)
                        result -> readUsersLiveData.postValue(convertGroupMemberInfos(result)));
        readReceiptDetailHandler.addDataChangeListener(
                ReadReceiptDetailHandler.KEY_MESSAGE_UNREAD_V5_USER_LIST,
                (OnDataChangeEnhancedListener<List<ReadReceiptUser>>)
                        result -> unreadUsersLiveData.postValue(convertGroupMemberInfos(result)));
        // 如果没有传进来已读V5信息，则手动查询
        if (readReceiptInfoV5 != null) {
            readReceiptInfoV5LiveData.setValue(readReceiptInfoV5);
        } else {
            readReceiptDetailHandler.getMessageReadReceiptInfoV5(message);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        RongUserInfoManager.getInstance().removeUserDataObserver(this);
        readReceiptDetailHandler.stop();
    }

    public MutableLiveData<ReadReceiptInfoV5> getReadReceiptInfoV5LiveData() {
        return readReceiptInfoV5LiveData;
    }

    public MutableLiveData<List<ReadReceiptData>> getReadUsersLiveData() {
        return readUsersLiveData;
    }

    public MutableLiveData<List<ReadReceiptData>> getUnreadUsersLiveData() {
        return unreadUsersLiveData;
    }

    public MutableLiveData<GroupMemberInfo> getMemberInfoUpdateLiveData() {
        return memberInfoUpdateLiveData;
    }

    public ReadReceiptInfoV5 getReadReceiptInfoV5() {
        return readReceiptInfoV5;
    }

    public Message getMessage() {
        return message;
    }

    public void getMessagesReadReceiptUsersByPage(boolean isRead) {
        if (isRead) readReceiptDetailHandler.getMessagesReadUsersByPage(message);
        else readReceiptDetailHandler.getMessagesUnReadUsersByPage(message);
    }

    @Override
    public void onUserUpdate(UserInfo info) {}

    @Override
    public void onGroupUpdate(Group group) {}

    @Override
    public void onGroupUserInfoUpdate(GroupUserInfo groupUserInfo) {
        memberInfoUpdateLiveData.setValue(convertGroupMemberInfo(groupUserInfo.getUserId()));
    }

    private List<ReadReceiptData> convertGroupMemberInfos(List<ReadReceiptUser> users) {
        List<ReadReceiptData> readReceiptDataList = new ArrayList<>();
        if (users.isEmpty()) {
            return readReceiptDataList;
        }
        for (int i = 0; i < users.size(); i++) {
            ReadReceiptUser user = users.get(i);
            GroupMemberInfo info = convertGroupMemberInfo(user.getUserId());
            readReceiptDataList.add(new ReadReceiptData(user, info));
        }
        return readReceiptDataList;
    }

    private GroupMemberInfo convertGroupMemberInfo(String userId) {
        GroupMemberInfo groupMemberInfo = new GroupMemberInfo();
        GroupUserInfo info =
                RongUserInfoManager.getInstance().getGroupUserInfo(message.getTargetId(), userId);
        if (info == null || isInfoProvider) {
            groupMemberInfo.setUserId(userId);
            setUserInfo(groupMemberInfo);
        } else {
            ExtendedGroupUserInfo extendedGroupUserInfo = (ExtendedGroupUserInfo) info;
            groupMemberInfo = extendedGroupUserInfo.getGroupMemberInfo();
            if (TextUtils.isEmpty(groupMemberInfo.getUserId())) {
                groupMemberInfo.setUserId(userId);
                setUserInfo(groupMemberInfo);
            }
        }
        return groupMemberInfo;
    }

    private void setUserInfo(GroupMemberInfo info) {
        UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(info.getUserId());
        if (userInfo != null) {
            info.setName(userInfo.getName());
            if (userInfo.getPortraitUri() != null) {
                info.setPortraitUri(userInfo.getPortraitUri().toString());
            }
        }
    }
}
