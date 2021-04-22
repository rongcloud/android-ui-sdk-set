package io.rong.imkit.userinfo.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import io.rong.imkit.userinfo.db.model.GroupMember;
import io.rong.imkit.userinfo.db.model.GroupUserInfo;


@Dao
public interface GroupMemberDao {

    @Query("select * from group_member where group_id=:groupId")
    LiveData<List<GroupMember>> getGroupAllMembers(String groupId);

    @Query("select * from group_member")
    LiveData<List<GroupMember>> getAllGroupMembers();

    @Query("select * from group_member where group_id=:groupId and user_id=:userId")
    GroupMember getGroupMember(String groupId, String userId);

    @Query("delete from group_member where group_id=:groupId and user_id=:userId")
    void removeGroupMember(String groupId, String userId);

    @Query("delete from group_member where group_id=:groupId")
    void removeGroupAllMember(String groupId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGroupMember(GroupMember groupMember);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGroupMembers(List<GroupMember> groupMemberList);
}
