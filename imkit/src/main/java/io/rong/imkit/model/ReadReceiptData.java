package io.rong.imkit.model;

import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.ReadReceiptUser;

public class ReadReceiptData {
    private ReadReceiptUser user;
    private GroupMemberInfo info;

    public ReadReceiptData(ReadReceiptUser user, GroupMemberInfo info) {
        this.user = user;
        this.info = info;
    }

    public GroupMemberInfo getInfo() {
        return info;
    }

    public void setInfo(GroupMemberInfo info) {
        this.info = info;
    }

    public ReadReceiptUser getUser() {
        return user;
    }

    public void setUser(ReadReceiptUser user) {
        this.user = user;
    }
}
