package io.rong.imkit.usermanage.group.application;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.usermanage.handler.GroupApplicationOperationsHandler;
import io.rong.imkit.usermanage.handler.GroupApplicationsPagedHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.usermanage.interfaces.OnPagedDataLoader;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.model.GroupApplicationDirection;
import io.rong.imlib.model.GroupApplicationInfo;
import io.rong.imlib.model.GroupApplicationStatus;
import java.util.ArrayList;
import java.util.List;

/**
 * 群组申请 ViewModel
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupApplicationsViewModel extends BaseViewModel {

    private final MutableLiveData<List<GroupApplicationInfo>> groupApplicationInfoListLiveData =
            new MutableLiveData<>(new ArrayList<>());

    protected final GroupApplicationsPagedHandler groupApplicationsPagedHandler;
    protected final GroupApplicationOperationsHandler groupApplicationOperationsHandler;

    public GroupApplicationsViewModel(@NonNull Bundle arguments) {
        super(arguments);
        int maxCount = arguments.getInt(KitConstants.KEY_MAX_COUNT_PAGED, 50);
        int validatedMaxCountPaged = Math.max(1, Math.min(100, maxCount));
        groupApplicationsPagedHandler = new GroupApplicationsPagedHandler(validatedMaxCountPaged);
        groupApplicationsPagedHandler.addDataChangeListener(
                GroupApplicationsPagedHandler.KEY_GET_GROUP_APPLICATIONS,
                new SafeDataHandler<List<GroupApplicationInfo>>() {
                    @Override
                    public void onDataChange(List<GroupApplicationInfo> groupApplicationInfos) {
                        groupApplicationInfoListLiveData.postValue(groupApplicationInfos);
                    }
                });

        groupApplicationOperationsHandler = new GroupApplicationOperationsHandler();

        GroupApplicationDirection[] directions =
                new GroupApplicationDirection[] {
                    GroupApplicationDirection.ApplicationSent,
                    GroupApplicationDirection.InvitationSent,
                    GroupApplicationDirection.ApplicationReceived,
                    GroupApplicationDirection.InvitationReceived
                };
        GroupApplicationStatus[] status =
                new GroupApplicationStatus[] {
                    GroupApplicationStatus.ManagerUnHandled,
                    GroupApplicationStatus.ManagerRefused,
                    GroupApplicationStatus.Joined,
                    GroupApplicationStatus.Expired,
                    GroupApplicationStatus.InviteeRefused,
                    GroupApplicationStatus.InviteeUnHandled
                };
        getGroupApplications(directions, status);
    }

    public MutableLiveData<List<GroupApplicationInfo>> getGroupApplicationInfoListLiveData() {
        return groupApplicationInfoListLiveData;
    }

    /**
     * 获取群组申请
     *
     * @param directions 申请方向
     * @param status 申请状态
     */
    public void getGroupApplications(
            GroupApplicationDirection[] directions, GroupApplicationStatus[] status) {
        groupApplicationsPagedHandler.getGroupApplications(directions, status);
    }

    /**
     * 接受群组邀请
     *
     * @param groupId 群组 ID
     * @param inviterId 邀请者 ID
     * @param onDataChangeListener 数据变化监听
     */
    public void acceptGroupInvite(
            String groupId, String inviterId, OnDataChangeListener<Boolean> onDataChangeListener) {
        groupApplicationOperationsHandler.replaceDataChangeListener(
                GroupApplicationOperationsHandler.KEY_ACCEPT_GROUP_INVITE, onDataChangeListener);
        groupApplicationOperationsHandler.acceptGroupInvite(groupId, inviterId);
    }

    /**
     * 拒绝群组邀请
     *
     * @param groupId 群组 ID
     * @param inviterId 邀请者 ID
     * @param reason 拒绝原因
     * @param onDataChangeListener 数据变化监听
     */
    public void refuseGroupInvite(
            String groupId,
            String inviterId,
            String reason,
            OnDataChangeListener<Boolean> onDataChangeListener) {
        groupApplicationOperationsHandler.replaceDataChangeListener(
                GroupApplicationOperationsHandler.KEY_REFUSE_GROUP_INVITE, onDataChangeListener);
        groupApplicationOperationsHandler.refuseGroupInvite(groupId, inviterId, reason);
    }

    /**
     * 接受群组申请
     *
     * @param groupId 群组 ID
     * @param inviterId 邀请者 ID
     * @param applicantId 申请者 ID
     * @param onDataChangeListener 数据变化监听
     */
    public void acceptGroupApplication(
            String groupId,
            String inviterId,
            String applicantId,
            OnDataChangeListener<IRongCoreEnum.CoreErrorCode> onDataChangeListener) {
        groupApplicationOperationsHandler.replaceDataChangeListener(
                GroupApplicationOperationsHandler.KEY_ACCEPT_GROUP_APPLICATION,
                onDataChangeListener);
        groupApplicationOperationsHandler.acceptGroupApplication(groupId, inviterId, applicantId);
    }

    /**
     * 拒绝群组申请
     *
     * @param groupId 群组 ID
     * @param inviterId 邀请者 ID
     * @param applicantId 申请者 ID
     * @param reason 拒绝原因
     * @param onDataChangeListener 数据变化监听
     */
    public void refuseGroupApplication(
            String groupId,
            String inviterId,
            String applicantId,
            String reason,
            OnDataChangeListener<Boolean> onDataChangeListener) {
        groupApplicationOperationsHandler.replaceDataChangeListener(
                GroupApplicationOperationsHandler.KEY_REFUSE_GROUP_APPLICATION,
                onDataChangeListener);
        groupApplicationOperationsHandler.refuseGroupApplication(
                groupId, inviterId, applicantId, reason);
    }

    OnPagedDataLoader getOnPageDataLoader() {
        return groupApplicationsPagedHandler;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupApplicationsPagedHandler.stop();
        groupApplicationOperationsHandler.stop();
    }
}
