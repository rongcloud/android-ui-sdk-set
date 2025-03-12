package io.rong.imkit.usermanage.interfaces;

import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.UserProfile;
import java.util.Map;

/**
 * 群组和用户信息事件监听
 *
 * @since 5.12.2
 */
public interface OnGroupAndUserEventListener {

    /**
     * 更新群组信息
     *
     * @param groupInfo 群组信息
     * @param errorCode 错误码
     */
    void updateGroupInfo(GroupInfo groupInfo, IRongCoreEnum.CoreErrorCode errorCode);

    /**
     * 更新群组成员信息
     *
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param nickname 昵称
     * @param extra 扩展信息
     * @param errorCode 错误码
     */
    void setGroupMemberInfo(
            String groupId,
            String userId,
            String nickname,
            String extra,
            IRongCoreEnum.CoreErrorCode errorCode);

    /**
     * 设置群组备注
     *
     * @param groupId 群组ID
     * @param remark 备注
     * @param errorCode 错误码
     */
    void setGroupRemark(String groupId, String remark, IRongCoreEnum.CoreErrorCode errorCode);

    /**
     * 更新用户信息
     *
     * @param userProfile 用户信息
     * @param errorCode 错误码
     */
    void updateMyUserProfile(UserProfile userProfile, IRongCoreEnum.CoreErrorCode errorCode);

    /**
     * 设置好友信息
     *
     * @param userId 用户ID
     * @param remark 备注
     * @param extProfile 扩展信息
     * @param errorCode 错误码
     */
    void setFriendInfo(
            String userId,
            String remark,
            Map<String, String> extProfile,
            IRongCoreEnum.CoreErrorCode errorCode);
}
