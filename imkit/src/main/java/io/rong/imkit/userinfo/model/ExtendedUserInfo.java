package io.rong.imkit.userinfo.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
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

    /**
     * 创建 ExtendedUserInfo 对象。
     *
     * @param userInfo 用户信息。
     * @return ExtendedUserInfo 对象。
     */
    public static ExtendedUserInfo obtain(@NonNull UserInfo userInfo) {
        return new ExtendedUserInfo(userInfo, null);
    }

    /**
     * 创建 ExtendedUserInfo 对象。
     *
     * @param userProfile 用户信息。
     * @return ExtendedUserInfo 对象。
     */
    public static ExtendedUserInfo obtain(@NonNull UserProfile userProfile) {
        return new ExtendedUserInfo(null, userProfile);
    }

    private ExtendedUserInfo(UserInfo userInfo, UserProfile userProfile) {
        super(
                userInfo != null
                        ? userInfo.getUserId()
                        : (userProfile != null ? userProfile.getUserId() : ""),
                userInfo != null
                        ? userInfo.getName()
                        : (userProfile != null ? userProfile.getName() : ""),
                userInfo != null
                        ? userInfo.getPortraitUri()
                        : (userProfile != null && userProfile.getPortraitUri() != null
                                ? Uri.parse(userProfile.getPortraitUri())
                                : null));

        if (userInfo != null) {
            this.setAlias(userInfo.getAlias());
            this.setExtra(userInfo.getExtra());
        }

        this.userProfile = userProfile != null ? userProfile : new UserProfile();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(userProfile, flags);
    }

    protected ExtendedUserInfo(Parcel in) {
        super(in);
        userProfile = in.readParcelable(UserProfile.class.getClassLoader());
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
