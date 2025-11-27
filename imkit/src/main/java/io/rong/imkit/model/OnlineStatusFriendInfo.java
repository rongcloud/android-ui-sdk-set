package io.rong.imkit.model;

import io.rong.imlib.model.FriendInfo;

/**
 * 组合了好友信息与该好友的在线状态信息
 *
 * @since 5.32.0
 */
public class OnlineStatusFriendInfo {
    // 好友信息
    private FriendInfo friendInfo;
    // 在线状态
    private boolean isOnline;

    public OnlineStatusFriendInfo(FriendInfo friendInfo, boolean isOnline) {
        this.friendInfo = friendInfo;
        this.isOnline = isOnline;
    }

    public FriendInfo getFriendInfo() {
        return friendInfo;
    }

    public void setFriendInfo(FriendInfo friendInfo) {
        this.friendInfo = friendInfo;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }
}
