package io.rong.imkit.usermanage.handler;

import androidx.annotation.NonNull;
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
 * @author rongcloud
 */
public class GroupOperationsHandler extends MultiDataHandler {

    /** 用于标识创建群组的操作 */
    public static final DataKey<IRongCoreEnum.CoreErrorCode> KEY_CREATE_GROUP =
            DataKey.obtain("KEY_CREATE_GROUP", IRongCoreEnum.CoreErrorCode.class);

    /** 用于标识邀请用户加入群组的操作 */
    public static final DataKey<Boolean> KEY_INVITE_USERS_TO_GROUP =
            DataKey.obtain("KEY_INVITE_USERS_TO_GROUP", Boolean.class);

    /** 用于标识踢出群成员的操作 */
    public static final DataKey<Boolean> KEY_KICK_GROUP_MEMBERS =
            DataKey.obtain("KEY_KICK_GROUP_MEMBERS", Boolean.class);

    /** 用于标识更新群组信息的操作 */
    public static final DataKey<Boolean> KEY_UPDATE_GROUP_INFO =
            DataKey.obtain("KEY_UPDATE_GROUP_INFO", Boolean.class);

    /** 用于标识更新群成员资料的操作 */
    public static final DataKey<Boolean> KEY_SET_GROUP_MEMBER_INFO =
            DataKey.obtain("KEY_SET_GROUP_MEMBER_INFO", Boolean.class);
    /** 用于标识退出群组的操作 */
    public static final DataKey<Boolean> KEY_QUIT_GROUP =
            DataKey.obtain("KEY_QUIT_GROUP", Boolean.class);

    /** 用于标识解散群组的操作 */
    public static final DataKey<Boolean> KEY_DISMISS_GROUP =
            DataKey.obtain("KEY_DISMISS_GROUP", Boolean.class);

    /** 用于标识设置群备注的操作 */
    public static final DataKey<Boolean> KEY_SET_GROUP_REMARK =
            DataKey.obtain("KEY_SET_GROUP_REMARK", Boolean.class);

    private final String groupId;

    /**
     * 构造方法初始化群组ID
     *
     * @param conversationIdentifier 会话标识符
     */
    public GroupOperationsHandler(@NonNull ConversationIdentifier conversationIdentifier) {
        this.groupId = conversationIdentifier.getTargetId();
    }

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
                                notifyDataChange(KEY_INVITE_USERS_TO_GROUP, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                notifyDataChange(KEY_INVITE_USERS_TO_GROUP, false);
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
    public void updateGroupInfo(@NonNull GroupInfo groupInfo) {
        RongCoreClient.getInstance()
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
     * 设置群成员信息
     *
     * @param userId 用户ID
     * @param nickname 昵称
     * @param extra 扩展信息
     */
    public void setGroupMemberInfo(String userId, String nickname, String extra) {
        RongCoreClient.getInstance()
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
     * 设置群备注
     *
     * @param remark 群备注
     */
    public void setGroupRemark(final String remark) {
        RongCoreClient.getInstance()
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
}
