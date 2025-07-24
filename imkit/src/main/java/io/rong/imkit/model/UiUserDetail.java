package io.rong.imkit.model;

public class UiUserDetail {
    private String userId;
    private String name;
    private String nickName;
    private String portrait;

    private boolean isFriend;

    public UiUserDetail(
            String userId, String name, String nickName, String portrait, boolean isFriend) {
        this.userId = userId;
        this.name = name;
        this.nickName = nickName;
        this.portrait = portrait;
        this.isFriend = isFriend;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getNickName() {
        return nickName;
    }

    public String getPortrait() {
        return portrait;
    }

    public boolean isFriend() {
        return isFriend;
    }
}
