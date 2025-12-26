package io.rong.imkit.usermanage.handler;

import androidx.annotation.NonNull;
import io.rong.imkit.IMCenter;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.QuitGroupConfig;
import java.util.List;

/**
 * 群组操作
 *
 * <p>注意：使用完毕后需要调用 {@link #stop()} 方法释放资源
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class GroupOperationsHandler extends MultiDataHandler {

    /** 用于标识创建群组的操作 */
    public static final DataKey<IRongCoreEnum.CoreErrorCode> KEY_CREATE_GROUP =
            DataKey.obtain("KEY_CREATE_GROUP", IRongCoreEnum.CoreErrorCode.class);

    /** 用于标识创建群组的操作 [审核] */
    public static final DataKey<IRongCoreEnum.CoreErrorCode> KEY_CREATE_GROUP_EXAMINE =
            DataKey.obtain("KEY_CREATE_GROUP_EXAMINE", IRongCoreEnum.CoreErrorCode.class);

    /** 用于标识邀请用户加入群组的操作 */
    public static final DataKey<IRongCoreEnum.CoreErrorCode> KEY_INVITE_USERS_TO_GROUP =
            DataKey.obtain("KEY_INVITE_USERS_TO_GROUP", IRongCoreEnum.CoreErrorCode.class);

    /** 用于标识踢出群成员的操作 */
    public static final DataKey<Boolean> KEY_KICK_GROUP_MEMBERS =
            DataKey.obtain("KEY_KICK_GROUP_MEMBERS", Boolean.class);

    /** 用于标识更新群组信息的操作 */
    public static final DataKey<Boolean> KEY_UPDATE_GROUP_INFO =
            DataKey.obtain("KEY_UPDATE_GROUP_INFO", Boolean.class);

    /** 用于标识更新群组信息的操作[审核] */
    public static final DataKey<Boolean> KEY_UPDATE_GROUP_INFO_EXAMINE =
            DataKey.obtain("KEY_UPDATE_GROUP_INFO_EXAMINE", Boolean.class);

    /** 用于标识更新群成员资料的操作 */
    public static final DataKey<Boolean> KEY_SET_GROUP_MEMBER_INFO =
            DataKey.obtain("KEY_SET_GROUP_MEMBER_INFO", Boolean.class);

    /** 用于标识更新群成员资料的操作 */
    public static final DataKey<Boolean> KEY_SET_GROUP_MEMBER_INFO_EXAMINE =
            DataKey.obtain("KEY_SET_GROUP_MEMBER_INFO_EXAMINE", Boolean.class);

    /** 用于标识退出群组的操作 */
    public static final DataKey<Boolean> KEY_QUIT_GROUP =
            DataKey.obtain("KEY_QUIT_GROUP", Boolean.class);

    /** 用于标识解散群组的操作 */
    public static final DataKey<Boolean> KEY_DISMISS_GROUP =
            DataKey.obtain("KEY_DISMISS_GROUP", Boolean.class);

    /** 用于标识设置群备注的操作 */
    public static final DataKey<Boolean> KEY_SET_GROUP_REMARK =
            DataKey.obtain("KEY_SET_GROUP_REMARK", Boolean.class);

    /** 用于添加特别关注群成员 */
    public static final DataKey<Boolean> KEY_ADD_GROUP_FOLLOWS =
            DataKey.obtain("KEY_ADD_GROUP_FOLLOWS", Boolean.class);

    /** 用于移除特别关注群成员 */
    public static final DataKey<Boolean> KEY_REMOVE_GROUP_FOLLOWS =
            DataKey.obtain("KEY_REMOVE_GROUP_FOLLOWS", Boolean.class);

    /** 用于群转让的操作 */
    public static final DataKey<Boolean> KEY_TRANSFER_GROUP_OWNER =
            DataKey.obtain("KEY_TRANSFER_GROUP_OWNER", Boolean.class);

    /** 用于添加群管理员的操作 */
    public static final DataKey<Boolean> KEY_ADD_GROUP_MANAGERS =
            DataKey.obtain("KEY_ADD_GROUP_MANAGERS", Boolean.class);

    /** 用于移除群管理员的操作 */
    public static final DataKey<Boolean> KEY_REMOVE_GROUP_MANAGERS =
            DataKey.obtain("KEY_REMOVE_GROUP_MANAGERS", Boolean.class);

    /** 用于加入群组的操作 */
    public static final DataKey<IRongCoreEnum.CoreErrorCode> KEY_JOIN_GROUP =
            DataKey.obtain("KEY_JOIN_GROUP", IRongCoreEnum.CoreErrorCode.class);

    private final String groupId;

    /**
     * 构造方法初始化群组ID
     *
     * @param conversationIdentifier 会话标识符
     */
    public GroupOperationsHandler(@NonNull ConversationIdentifier conversationIdentifier) {
        this.groupId = conversationIdentifier.getTargetId();
    }

    @Deprecated
    public void createGroup(GroupInfo groupInfo, List<String> inviteeUserIds) {
        RongCoreClient.getInstance()
                .createGroup(
                        groupInfo,
                        inviteeUserIds,
                        new IRongCoreCallback.CreateGroupCallback() {
                            @Override
                            public void onSuccess(IRongCoreEnum.CoreErrorCode processCode) {
                                notifyDataChange(KEY_CREATE_GROUP, processCode);
                            }

                            @Override
                            public void onError(
                                    IRongCoreEnum.CoreErrorCode errorCode, String errorData) {
                                notifyDataChange(KEY_CREATE_GROUP, errorCode);
                                notifyDataError(KEY_CREATE_GROUP, errorCode, errorData);
                            }
                        });
    }

    public void createGroupExamine(GroupInfo groupInfo, List<String> inviteeUserIds) {
        RongCoreClient.getInstance()
                .createGroup(
                        groupInfo,
                        inviteeUserIds,
                        new IRongCoreCallback.ExamineCreateGroupCallback() {
                            @Override
                            public void onSuccess(IRongCoreEnum.CoreErrorCode processCode) {
                                notifyDataChange(KEY_CREATE_GROUP_EXAMINE, processCode);
                            }

                            @Override
                            public void onError(
                                    IRongCoreEnum.CoreErrorCode errorCode, List<String> errorData) {
                                notifyDataChange(KEY_CREATE_GROUP_EXAMINE, errorCode);
                                notifyDataError(KEY_CREATE_GROUP_EXAMINE, errorCode, errorData);
                            }
                        });
    }

    /**
     * 邀请用户加入群组
     *
     * @param userIds 用户ID列表
     */
    public void inviteUsersToGroup(@NonNull List<String> userIds) {
        RongCoreClient.getInstance()
                .inviteUsersToGroup(
                        groupId,
                        userIds,
                        new IRongCoreCallback.ResultCallback<IRongCoreEnum.CoreErrorCode>() {
                            @Override
                            public void onSuccess(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_INVITE_USERS_TO_GROUP, coreErrorCode);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                notifyDataError(KEY_INVITE_USERS_TO_GROUP, e);
                            }
                        });
    }

    /**
     * 踢出群成员
     *
     * @param userIds 用户ID列表
     * @param config 退出群组配置
     */
    public void kickGroupMembers(List<String> userIds, QuitGroupConfig config) {
        RongCoreClient.getInstance()
                .kickGroupMembers(
                        groupId,
                        userIds,
                        config,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_KICK_GROUP_MEMBERS, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_KICK_GROUP_MEMBERS, false);
                                notifyDataError(KEY_KICK_GROUP_MEMBERS, coreErrorCode);
                            }
                        });
    }

    /**
     * 更新群组信息
     *
     * @param groupInfo 群组信息
     */
    @Deprecated
    public void updateGroupInfo(@NonNull GroupInfo groupInfo) {
        IMCenter.getInstance()
                .updateGroupInfo(
                        groupInfo,
                        new IRongCoreCallback.OperationCallbackEx<String>() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_UPDATE_GROUP_INFO, true);
                            }

                            @Override
                            public void onError(
                                    IRongCoreEnum.CoreErrorCode errorCode, String errorData) {
                                notifyDataChange(KEY_UPDATE_GROUP_INFO, false);
                                notifyDataError(KEY_UPDATE_GROUP_INFO, errorCode, errorData);
                            }
                        });
    }

    /**
     * 更新群组信息
     *
     * @param groupInfo 群组信息
     */
    public void updateGroupInfoExamine(@NonNull GroupInfo groupInfo) {
        IMCenter.getInstance()
                .updateGroupInfo(
                        groupInfo,
                        new IRongCoreCallback.ExamineOperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_UPDATE_GROUP_INFO_EXAMINE, true);
                            }

                            @Override
                            public void onError(
                                    IRongCoreEnum.CoreErrorCode errorCode, List<String> errorData) {
                                notifyDataError(
                                        KEY_UPDATE_GROUP_INFO_EXAMINE, errorCode, errorData);
                            }
                        });
    }

    /**
     * 设置群成员信息
     *
     * @param userId 用户ID
     * @param nickname 昵称
     * @param extra 扩展信息
     */
    @Deprecated
    public void setGroupMemberInfo(String userId, String nickname, String extra) {
        IMCenter.getInstance()
                .setGroupMemberInfo(
                        groupId,
                        userId,
                        nickname,
                        extra,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_SET_GROUP_MEMBER_INFO, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_SET_GROUP_MEMBER_INFO, false);
                                notifyDataError(KEY_SET_GROUP_MEMBER_INFO, coreErrorCode);
                            }
                        });
    }

    /**
     * 设置群成员信息
     *
     * @param userId 用户ID
     * @param nickname 昵称
     * @param extra 扩展信息
     */
    public void setGroupMemberInfoExamine(String userId, String nickname, String extra) {
        IMCenter.getInstance()
                .setGroupMemberInfo(
                        groupId,
                        userId,
                        nickname,
                        extra,
                        new IRongCoreCallback.ExamineOperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_SET_GROUP_MEMBER_INFO_EXAMINE, true);
                            }

                            @Override
                            public void onError(
                                    IRongCoreEnum.CoreErrorCode coreErrorCode,
                                    List<String> errorKeys) {
                                notifyDataError(
                                        KEY_SET_GROUP_MEMBER_INFO_EXAMINE,
                                        coreErrorCode,
                                        errorKeys);
                            }
                        });
    }

    /**
     * 设置群备注
     *
     * @param remark 群备注
     */
    public void setGroupRemark(final String remark) {
        IMCenter.getInstance()
                .setGroupRemark(
                        groupId,
                        remark,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_SET_GROUP_REMARK, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_SET_GROUP_REMARK, false);
                                notifyDataError(KEY_SET_GROUP_REMARK, coreErrorCode);
                            }
                        });
    }

    /**
     * 退出群组
     *
     * @param config 退出群组配置
     */
    public void quitGroup(QuitGroupConfig config) {
        RongCoreClient.getInstance()
                .quitGroup(
                        groupId,
                        config,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_QUIT_GROUP, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_QUIT_GROUP, false);
                                notifyDataError(KEY_QUIT_GROUP, coreErrorCode);
                            }
                        });
    }

    /** 解散群组 */
    public void dismissGroup() {
        RongCoreClient.getInstance()
                .dismissGroup(
                        groupId,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_DISMISS_GROUP, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_DISMISS_GROUP, false);
                                notifyDataError(KEY_DISMISS_GROUP, coreErrorCode);
                            }
                        });
    }

    /**
     * 添加群关注人员
     *
     * @param userIds 用户ID列表
     * @since 5.12.2
     */
    public void addGroupFollows(List<String> userIds) {
        RongCoreClient.getInstance()
                .addGroupFollows(
                        groupId,
                        userIds,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_ADD_GROUP_FOLLOWS, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_ADD_GROUP_FOLLOWS, false);
                                notifyDataError(KEY_ADD_GROUP_FOLLOWS, coreErrorCode);
                            }
                        });
    }

    /**
     * 移除群关注人员
     *
     * @param userIds 用户ID列表
     * @since 5.12.2
     */
    public void removeGroupFollows(List<String> userIds) {
        RongCoreClient.getInstance()
                .removeGroupFollows(
                        groupId,
                        userIds,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_REMOVE_GROUP_FOLLOWS, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_REMOVE_GROUP_FOLLOWS, false);
                                notifyDataError(KEY_REMOVE_GROUP_FOLLOWS, coreErrorCode);
                            }
                        });
    }

    /**
     * 转让群
     *
     * @param newOwnerId 新群主ID
     * @param quitGroup 是否退出群组
     * @param config 退出群组配置
     * @since 5.12.2
     */
    public void transferGroupOwner(
            final String newOwnerId, final boolean quitGroup, final QuitGroupConfig config) {
        RongCoreClient.getInstance()
                .transferGroupOwner(
                        groupId,
                        newOwnerId,
                        quitGroup,
                        config,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_TRANSFER_GROUP_OWNER, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_TRANSFER_GROUP_OWNER, false);
                                notifyDataError(KEY_TRANSFER_GROUP_OWNER, coreErrorCode);
                            }
                        });
    }

    /**
     * 添加群管理员
     *
     * @param userIds 用户ID列表
     * @since 5.12.2
     */
    public void addGroupManagers(List<String> userIds) {
        RongCoreClient.getInstance()
                .addGroupManagers(
                        groupId,
                        userIds,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_ADD_GROUP_MANAGERS, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_ADD_GROUP_MANAGERS, false);
                                notifyDataError(KEY_ADD_GROUP_MANAGERS, coreErrorCode);
                            }
                        });
    }

    /**
     * 移除群管理员
     *
     * @param userIds 用户ID列表
     * @since 5.12.2
     */
    public void removeGroupManagers(List<String> userIds) {
        RongCoreClient.getInstance()
                .removeGroupManagers(
                        groupId,
                        userIds,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_REMOVE_GROUP_MANAGERS, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_REMOVE_GROUP_MANAGERS, false);
                                notifyDataError(KEY_REMOVE_GROUP_MANAGERS, coreErrorCode);
                            }
                        });
    }

    /**
     * 加入群组
     *
     * <p>加入群组权限决定是否可以直接加入群组。当群组需要审批时，会返回 RC_GROUP_JOIN_GROUP_NEED_MANAGER_ACCEPT
     * 状态码，表示需要等待群主或管理员审批。
     *
     * @since 5.34.0
     */
    public void joinGroup() {
        RongCoreClient.getInstance()
                .joinGroup(
                        groupId,
                        new IRongCoreCallback.ResultCallback<IRongCoreEnum.CoreErrorCode>() {
                            @Override
                            public void onSuccess(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_JOIN_GROUP, coreErrorCode);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_JOIN_GROUP, coreErrorCode);
                                notifyDataError(KEY_JOIN_GROUP, coreErrorCode);
                            }
                        });
    }
}
