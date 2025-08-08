package io.rong.imkit.userinfo.model;

import android.os.Parcel;
import android.os.Parcelable;
import io.rong.imlib.model.GroupMemberInfo;

/**
 * 功能描述: 扩展群成员信息实体类，用来存储群成员信息。
 *
 * @author rongcloud
 * @since 5.10.5
 */
public class ExtendedGroupUserInfo extends GroupUserInfo implements Parcelable {

    private final GroupMemberInfo groupMemberInfo;

    /**
     * 创建 ExtendedGroupUserInfo 对象。
     *
     * @param groupUserInfo GroupUserInfo 对象
     * @return ExtendedGroupUserInfo 对象
     */
    public static ExtendedGroupUserInfo obtain(GroupUserInfo groupUserInfo) {
        return new ExtendedGroupUserInfo(groupUserInfo, null);
    }

    /**
     * 创建 ExtendedGroupUserInfo 对象。
     *
     * @param groupMemberInfo GroupMemberInfo 对象
     * @return ExtendedGroupUserInfo 对象
     */
    public static ExtendedGroupUserInfo obtain(GroupMemberInfo groupMemberInfo) {
        return new ExtendedGroupUserInfo(null, groupMemberInfo);
    }

    private ExtendedGroupUserInfo(GroupUserInfo groupUserInfo, GroupMemberInfo groupMemberInfo) {
        super(
                groupUserInfo != null ? groupUserInfo.getGroupId() : "",
                groupUserInfo != null
                        ? groupUserInfo.getUserId()
                        : (groupMemberInfo != null ? groupMemberInfo.getUserId() : ""),
                groupUserInfo != null
                        ? groupUserInfo.getNickname()
                        : (groupMemberInfo != null ? groupMemberInfo.getNickname() : ""),
                groupUserInfo != null
                        ? groupUserInfo.getExtra()
                        : (groupMemberInfo != null ? groupMemberInfo.getExtra() : ""));

        this.groupMemberInfo = groupMemberInfo != null ? groupMemberInfo : new GroupMemberInfo();
    }

    /**
     * 获取 GroupMemberInfo 对象。
     *
     * @return GroupMemberInfo 对象。
     */
    public GroupMemberInfo getGroupMemberInfo() {
        return groupMemberInfo;
    }

    /**
     * 将 ExtendedGroupUserInfo 转换为 GroupMemberInfo 对象。
     *
     * @return 转换后的 GroupMemberInfo 对象。
     */
    public GroupMemberInfo toGroupMemberInfo() {
        GroupMemberInfo groupMemberInfo = new GroupMemberInfo();
        groupMemberInfo.setUserId(getUserId());
        groupMemberInfo.setName(groupMemberInfo.getName());
        groupMemberInfo.setPortraitUri(groupMemberInfo.getPortraitUri());
        groupMemberInfo.setNickname(getNickname());
        groupMemberInfo.setExtra(getExtra());
        groupMemberInfo.setRole(this.groupMemberInfo.getRole());
        groupMemberInfo.setJoinedTime(this.groupMemberInfo.getJoinedTime());
        return groupMemberInfo;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(groupMemberInfo, flags);
    }

    protected ExtendedGroupUserInfo(Parcel in) {
        super(in.readString(), in.readString(), in.readString(), in.readString());
        groupMemberInfo = in.readParcelable(GroupMemberInfo.class.getClassLoader());
    }

    public static final Creator<ExtendedGroupUserInfo> CREATOR =
            new Creator<ExtendedGroupUserInfo>() {
                @Override
                public ExtendedGroupUserInfo createFromParcel(Parcel in) {
                    return new ExtendedGroupUserInfo(in);
                }

                @Override
                public ExtendedGroupUserInfo[] newArray(int size) {
                    return new ExtendedGroupUserInfo[size];
                }
            };

    @Override
    public String toString() {
        return "ExtendedGroupUserInfo{"
                + "groupMemberInfo="
                + groupMemberInfo
                + ", groupId='"
                + getGroupId()
                + '\''
                + ", userId='"
                + getUserId()
                + '\''
                + ", nickname='"
                + getNickname()
                + '\''
                + ", extra='"
                + getExtra()
                + '\''
                + '}';
    }
}
