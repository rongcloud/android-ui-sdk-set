package io.rong.imkit.usermanage.group.create;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.usermanage.handler.GroupOperationsHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeEnhancedListener;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupJoinPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 功能描述: 创建群组ViewModel
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class GroupCreateViewModel extends BaseViewModel {

    private final String groupId;
    private final ArrayList<String> inviteeUserIds;
    private final Random rand = new Random();

    protected final GroupOperationsHandler groupOperationsHandler;

    public GroupCreateViewModel(@NonNull Bundle arguments) {
        super(arguments);
        groupId = generateOrRetrieveGroupId(arguments);
        inviteeUserIds = arguments.getStringArrayList(KitConstants.KEY_INVITEE_USER_IDS);
        groupOperationsHandler = new GroupOperationsHandler(ConversationIdentifier.obtainGroup(""));
    }

    /**
     * 获取群组ID
     *
     * @return 群组ID
     */
    public String getGroupId() {
        return groupId;
    }

    List<String> getInviteeUserIds() {
        return inviteeUserIds;
    }

    /**
     * 创建群组
     *
     * @param groupName 群组名称
     * @param listener 数据变化监听器
     */
    @Deprecated
    public void createGroup(
            String groupName, OnDataChangeListener<IRongCoreEnum.CoreErrorCode> listener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_CREATE_GROUP, listener);
        GroupInfo groupInfo = new GroupInfo(groupId, groupName);
        groupInfo.setJoinPermission(GroupJoinPermission.Free);
        groupOperationsHandler.createGroup(groupInfo, inviteeUserIds);
    }

    /**
     * 创建群组
     *
     * @param groupName 群组名称
     * @param listener 数据变化监听器
     */
    public void createGroup(
            String groupName, OnDataChangeEnhancedListener<IRongCoreEnum.CoreErrorCode> listener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_CREATE_GROUP_EXAMINE, listener);
        GroupInfo groupInfo = new GroupInfo(groupId, groupName);
        groupInfo.setJoinPermission(GroupJoinPermission.Free);
        groupOperationsHandler.createGroupExamine(groupInfo, inviteeUserIds);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupOperationsHandler.stop();
    }

    private String generateOrRetrieveGroupId(Bundle arguments) {
        String id = arguments.getString(KitConstants.KEY_GROUP_ID);
        if (TextUtils.isEmpty(id)) {
            return generateUniqueGroupId();
        } else {
            return id;
        }
    }

    private String generateUniqueGroupId() {
        String allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder uniqueId = new StringBuilder();

        // Add the current timestamp in milliseconds
        long timestamp = System.currentTimeMillis();
        uniqueId.append(Long.toString(timestamp));

        // Fill the remaining length with random characters
        for (int i = uniqueId.length(); i < 32; i++) {
            int index = rand.nextInt(allowedChars.length());
            uniqueId.append(allowedChars.charAt(index));
        }

        return uniqueId.toString();
    }
}
