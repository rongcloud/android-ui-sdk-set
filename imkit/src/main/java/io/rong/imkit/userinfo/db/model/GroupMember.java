package io.rong.imkit.userinfo.db.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(
        tableName = "group_member",
        primaryKeys = {"group_id", "user_id"})
public class GroupMember {
    @Ignore
    public GroupMember(@NonNull String groupId, @NonNull String userId, String memberName) {
        this(groupId, userId, memberName, "");
    }

    public GroupMember(
            @NonNull String groupId, @NonNull String userId, String memberName, String extra) {
        this.groupId = groupId;
        this.userId = userId;
        this.memberName = memberName;
        this.extra = extra;
    }

    @NonNull
    @ColumnInfo(name = "group_id")
    public String groupId;

    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "member_name")
    public String memberName;

    @ColumnInfo(name = "extra")
    public String extra;
}
