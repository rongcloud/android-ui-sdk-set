package io.rong.imkit.utils;

/**
 * 常量工具类
 *
 * @author rongcloud
 */
public final class KitConstants {

    private KitConstants() {}

    /** 群组id */
    public static final String KEY_GROUP_ID = "groupId";
    /** 用户id */
    public static final String KEY_USER_ID = "userId";
    /** 邀请用户id */
    public static final String KEY_INVITEE_USER_IDS = "inviteeUserIds";

    /** 会话标识 */
    public static final String KEY_CONVERSATION_IDENTIFIER = "conversationIdentifier";

    /** 用户信息 */
    public static final String KEY_USER_PROFILER = "user_profiler";
    /** 好友信息 */
    public static final String KEY_FRIEND_INFO = "friendInfo";
    /** 群组信息 */
    public static final String KEY_GROUP_INFO = "groupInfo";

    /** 群组成员角色 */
    public static final String KEY_GROUP_MEMBER_ROLE = "groupMemberRole";
    /** 群组成员数量 */
    public static final String KEY_MAX_MEMBER_COUNT_DISPLAY = "displayMaxMemberCount";
    /** 选择好友最大数量 */
    public static final String KEY_MAX_FRIEND_SELECT_COUNT = "maxFriendSelectCount";
    /** 添加群成员最大数量 */
    public static final String KEY_MAX_MEMBER_COUNT_ADD = "maxFriendAddCount";
    /** 分页好友最大数量 */
    public static final String KEY_MAX_MEMBER_COUNT_PAGED = "maxFriendPagedCount";
}
