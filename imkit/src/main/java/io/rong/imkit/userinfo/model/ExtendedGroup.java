package io.rong.imkit.userinfo.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.GroupInfo;

/**
 * 功能描述: 扩展群组信息实体类，用来存储群组信息。
 *
 * @author rongcloud
 * @since 5.10.5
 */
public class ExtendedGroup extends Group implements Parcelable {

    private final GroupInfo groupInfo;

    /**
     * 创建 ExtendedGroupInfo 对象。
     *
     * @param group Group 对象
     * @return ExtendedGroupInfo 对象
     */
    public static ExtendedGroup obtain(@NonNull Group group) {
        return new ExtendedGroup(group, null);
    }

    /**
     * 创建 ExtendedGroupInfo 对象。
     *
     * @param groupInfo GroupInfo 对象
     * @return ExtendedGroupInfo 对象
     */
    public static ExtendedGroup obtain(@NonNull GroupInfo groupInfo) {
        return new ExtendedGroup(null, groupInfo);
    }

    private ExtendedGroup(Group group, GroupInfo groupInfo) {
        super(
                group != null ? group.getId() : (groupInfo != null ? groupInfo.getGroupId() : ""),
                group != null
                        ? group.getName()
                        : (groupInfo != null ? groupInfo.getGroupName() : ""),
                group != null
                        ? group.getPortraitUri()
                        : (groupInfo != null && groupInfo.getPortraitUri() != null
                                ? Uri.parse(groupInfo.getPortraitUri())
                                : null),
                group != null ? group.getExtra() : "");

        // 初始化 groupInfo，处理 groupInfo 可能为 null 的情况
        this.groupInfo = groupInfo != null ? groupInfo : new GroupInfo();
    }

    /**
     * 获取 GroupInfo 对象。
     *
     * @return GroupInfo 对象。
     */
    public GroupInfo getGroupInfo() {
        return groupInfo;
    }

    /**
     * 将 ExtendedGroupInfo 转换为 GroupInfo 对象。
     *
     * @return 转换后的 GroupInfo 对象。
     */
    public GroupInfo toGroupInfo() {
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setGroupId(getId());
        groupInfo.setGroupName(getName());
        groupInfo.setPortraitUri(getPortraitUri() != null ? getPortraitUri().toString() : null);
        groupInfo.setIntroduction(this.groupInfo.getIntroduction());
        groupInfo.setNotice(this.groupInfo.getNotice());
        groupInfo.setJoinPermission(this.groupInfo.getJoinPermission());
        groupInfo.setRemoveMemberPermission(this.groupInfo.getRemoveMemberPermission());
        groupInfo.setInvitePermission(this.groupInfo.getInvitePermission());
        groupInfo.setInviteHandlePermission(this.groupInfo.getInviteHandlePermission());
        groupInfo.setGroupInfoEditPermission(this.groupInfo.getGroupInfoEditPermission());
        groupInfo.setMemberInfoEditPermission(this.groupInfo.getMemberInfoEditPermission());
        groupInfo.setExtProfile(this.groupInfo.getExtProfile());
        groupInfo.setCreatorId(this.groupInfo.getCreatorId());
        groupInfo.setOwnerId(this.groupInfo.getOwnerId());
        groupInfo.setCreateTime(this.groupInfo.getCreateTime());
        groupInfo.setMembersCount(this.groupInfo.getMembersCount());
        groupInfo.setRemark(this.groupInfo.getRemark());
        groupInfo.setJoinedTime(this.groupInfo.getJoinedTime());
        groupInfo.setRole(this.groupInfo.getRole());
        return groupInfo;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(groupInfo, flags);
    }

    protected ExtendedGroup(Parcel in) {
        super(in);
        groupInfo = in.readParcelable(GroupInfo.class.getClassLoader());
    }

    public static final Creator<ExtendedGroup> CREATOR =
            new Creator<ExtendedGroup>() {
                @Override
                public ExtendedGroup createFromParcel(Parcel in) {
                    return new ExtendedGroup(in);
                }

                @Override
                public ExtendedGroup[] newArray(int size) {
                    return new ExtendedGroup[size];
                }
            };

    @Override
    public String toString() {
        return "ExtendedGroupInfo{"
                + "groupInfo="
                + groupInfo
                + ", id='"
                + getId()
                + '\''
                + ", name='"
                + getName()
                + '\''
                + ", portraitUri="
                + getPortraitUri()
                + ", extra='"
                + getExtra()
                + '\''
                + '}';
    }
}
