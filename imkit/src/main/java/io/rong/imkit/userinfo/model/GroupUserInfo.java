package io.rong.imkit.userinfo.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 功能描述: 群成员信息实体类，用来存储群成员信息。
 *
 * @author rongcloud
 * @since 5.10.5
 */
public class GroupUserInfo implements Parcelable {

    private String mNickname;
    private String mUserId;
    private String mGroupId;
    private String extra;

    /**
     * 群成员对象
     *
     * @param groupId 群 Id
     * @param userId 用户 Id
     * @param nickname 该用户在群里的昵称
     */
    public GroupUserInfo(String groupId, String userId, String nickname) {
        this(groupId, userId, nickname, "");
    }

    public GroupUserInfo(String groupId, String userId, String nickname, String extra) {
        this.mGroupId = groupId;
        this.mNickname = nickname;
        this.mUserId = userId;
        this.extra = extra;
    }

    protected GroupUserInfo(Parcel in) {
        mNickname = in.readString();
        mUserId = in.readString();
        mGroupId = in.readString();
        extra = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mNickname);
        dest.writeString(mUserId);
        dest.writeString(mGroupId);
        dest.writeString(extra);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<GroupUserInfo> CREATOR =
            new Creator<GroupUserInfo>() {
                @Override
                public GroupUserInfo createFromParcel(Parcel in) {
                    return new GroupUserInfo(in);
                }

                @Override
                public GroupUserInfo[] newArray(int size) {
                    return new GroupUserInfo[size];
                }
            };

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

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}
