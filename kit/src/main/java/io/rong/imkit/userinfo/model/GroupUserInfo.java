package io.rong.imkit.userinfo.model;

/**
 * 群成员信息实体类，用来存储群成员信息。
 */
public class GroupUserInfo {
    private String mNickname;
    private String mUserId;
    private String mGroupId;

    /**
     * 群成员对象
     * @param groupId 群 Id
     * @param userId 用户 Id
     * @param nickname 该用户在群里的昵称
     */
    public GroupUserInfo(String groupId, String userId, String nickname) {
        this.mGroupId = groupId;
        this.mNickname = nickname;
        this.mUserId = userId;
    }

    public String getGroupId() {
        return mGroupId;
    }

    public void setGroupId(String mGroupId) {
        this.mGroupId = mGroupId;
    }

    public String getNickname() {
        return mNickname;
    }

    public String getUserId() {
        return mUserId;
    }
}

