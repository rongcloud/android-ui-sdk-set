package io.rong.imkit.model;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import io.rong.imlib.model.FriendApplicationInfo;

public class UiFriendApplicationInfo {
    private FriendApplicationInfo info;
    private @StringRes int showTime;

    public UiFriendApplicationInfo(@NonNull FriendApplicationInfo info, @StringRes int showTime) {
        this.info = info;
        this.showTime = showTime;
    }

    public FriendApplicationInfo getInfo() {
        return info;
    }

    public @StringRes int getShowTime() {
        return showTime;
    }
}
