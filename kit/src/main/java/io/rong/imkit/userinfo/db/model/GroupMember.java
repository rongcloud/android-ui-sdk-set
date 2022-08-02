package io.rong.imkit.userinfo.db.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "group_member", primaryKeys = {"group_id", "user_id"})
public class GroupMember {
    public GroupMember(@NonNull String groupId, @NonNull String userId, String memberName) {
        this.groupId = groupId;
        this.userId = userId;
        this.memberName = memberName;
    }

    @NonNull
    @ColumnInfo(name = "group_id")
    public String groupId;

    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "member_name")
    public String memberName;
}
