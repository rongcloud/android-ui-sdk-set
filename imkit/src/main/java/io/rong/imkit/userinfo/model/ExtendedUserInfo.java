package io.rong.imkit.userinfo.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import io.rong.imlib.model.FriendInfo;
import io.rong.imlib.model.UserInfo;
import io.rong.imlib.model.UserProfile;

/**
 * 功能描述: 扩展用户信息实体类，用来存储用户信息。
 *
 * @author rongcloud
 * @since 5.10.5
 */
public class ExtendedUserInfo extends UserInfo implements Parcelable {

    private final UserProfile userProfile;
    private final FriendInfo friendInfo;

    /**
     * 创建 ExtendedUserInfo 对象。
     *
     * @param userInfo 用户信息。
     * @return ExtendedUserInfo 对象。
     */
    public static ExtendedUserInfo obtain(@NonNull UserInfo userInfo) {
        return new ExtendedUserInfo(userInfo, null, null);
    }

    /**
     * 创建 ExtendedUserInfo 对象。
     *
     * @param userProfile 用户信息。
     * @return ExtendedUserInfo 对象。
     */
    public static ExtendedUserInfo obtain(@NonNull UserProfile userProfile) {
        return new ExtendedUserInfo(null, userProfile, null);
    }

    /**
     * 创建 ExtendedUserInfo 对象。
     *
     * @param friendInfo 好友信息。
     * @return ExtendedUserInfo 对象。
     */
    public static ExtendedUserInfo obtain(@NonNull FriendInfo friendInfo) {
        return new ExtendedUserInfo(null, null, friendInfo);
    }

    private ExtendedUserInfo(UserInfo userInfo, UserProfile userProfile, FriendInfo friendInfo) {
        super("", "", null);
        if (userInfo != null) {
            this.setUserId(userInfo.getUserId());
            this.setName(userInfo.getName());
            this.setPortraitUri(userInfo.getPortraitUri());
            this.setAlias(userInfo.getAlias());
            this.setExtra(userInfo.getExtra());
        } else if (userProfile != null) {
            this.setUserId(userProfile.getUserId());
            this.setName(userProfile.getName());
            if (userProfile.getPortraitUri() != null) {
                this.setPortraitUri(Uri.parse(userProfile.getPortraitUri()));
            }
        } else if (friendInfo != null) {
            this.setUserId(friendInfo.getUserId());
            this.setName(friendInfo.getName());
            if (friendInfo.getPortraitUri() != null) {
                this.setPortraitUri(Uri.parse(friendInfo.getPortraitUri()));
            }
            if (friendInfo.getRemark() != null) {
                this.setAlias(friendInfo.getRemark());
            }
        }

        this.userProfile = userProfile != null ? userProfile : new UserProfile();
        this.userProfile.setUserId(getUserId());
        this.friendInfo = friendInfo != null ? friendInfo : new FriendInfo();
        this.friendInfo.setUserId(getUserId());
    }

    /**
     * 获取 UserProfile 对象。
     *
     * @return UserProfile 对象。
     */
    public UserProfile getUserProfile() {
        return userProfile;
    }

    /**
     * 获取 FriendInfo 对象。
     *
     * @return FriendInfo 对象。
     */
    public FriendInfo getFriendInfo() {
        return friendInfo;
    }

    /**
     * 将 ExtendedUserInfo 转换为 UserProfile 对象。
     *
     * @return 转换后的 UserProfile 对象。
     */
    public UserProfile toUserProfile() {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(getUserId());
        userProfile.setName(getName());
        userProfile.setPortraitUri(getPortraitUri() != null ? getPortraitUri().toString() : null);
        userProfile.setUniqueId(this.userProfile.getUniqueId());
        userProfile.setEmail(this.userProfile.getEmail());
        userProfile.setBirthday(this.userProfile.getBirthday());
        userProfile.setGender(this.userProfile.getGender());
        userProfile.setLocation(this.userProfile.getLocation());
        userProfile.setRole(this.userProfile.getRole());
        userProfile.setLevel(this.userProfile.getLevel());
        userProfile.setUserExtProfile(this.userProfile.getUserExtProfile());
        return userProfile;
    }

    /**
     * 将 ExtendedUserInfo 转换为 FriendInfo 对象。
     *
     * @return 转换后的 FriendInfo 对象。
     */
    public FriendInfo toFriendInfo() {
        FriendInfo friendInfo = new FriendInfo();
        friendInfo.setUserId(getUserId());
        friendInfo.setName(getName());
        friendInfo.setPortraitUri(getPortraitUri() != null ? getPortraitUri().toString() : null);
        friendInfo.setAddTime(this.friendInfo.getAddTime());
        friendInfo.setDirectionType(this.friendInfo.getDirectionType());
        friendInfo.setExtProfile(this.friendInfo.getExtProfile());
        friendInfo.setRemark(this.friendInfo.getRemark());
        return friendInfo;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(userProfile, flags);
        dest.writeParcelable(friendInfo, flags);
    }

    protected ExtendedUserInfo(Parcel in) {
        super(in);
        userProfile = in.readParcelable(UserProfile.class.getClassLoader());
        friendInfo = in.readParcelable(FriendInfo.class.getClassLoader());
    }

    public static final Creator<ExtendedUserInfo> CREATOR =
            new Creator<ExtendedUserInfo>() {
                @Override
                public ExtendedUserInfo createFromParcel(Parcel in) {
                    return new ExtendedUserInfo(in);
                }

                @Override
                public ExtendedUserInfo[] newArray(int size) {
                    return new ExtendedUserInfo[size];
                }
            };

    @Override
    public String toString() {
        return "ExtendedUserInfo{"
                + "userProfile="
                + userProfile
                + "friendInfo="
                + friendInfo
                + ", id='"
                + getUserId()
                + '\''
                + ", name='"
                + getName()
                + '\''
                + ", alias='"
                + getAlias()
                + '\''
                + ", portraitUri="
                + getPortraitUri()
                + ", extra='"
                + getExtra()
                + '\''
                + '}';
    }
}
