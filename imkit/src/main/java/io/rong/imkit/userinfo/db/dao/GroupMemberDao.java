package io.rong.imkit.userinfo.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RoomWarnings;
import io.rong.imkit.userinfo.db.model.GroupMember;
import java.util.List;

@Dao
public interface GroupMemberDao {

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("select * from group_member where group_id=:groupId")
    LiveData<List<GroupMember>> getGroupAllMembers(String groupId);

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("select * from group_member")
    LiveData<List<GroupMember>> getAllGroupMembers();

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("select * from group_member where group_id=:groupId and user_id=:userId")
    GroupMember getGroupMember(String groupId, String userId);

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("delete from group_member where group_id=:groupId and user_id=:userId")
    void removeGroupMember(String groupId, String userId);

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("delete from group_member where group_id=:groupId")
    void removeGroupAllMember(String groupId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGroupMember(GroupMember groupMember);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGroupMembers(List<GroupMember> groupMemberList);

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("select * from group_member limit :limit")
    List<GroupMember> getLimitGroupMembers(int limit);
}
