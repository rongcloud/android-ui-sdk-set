package io.rong.imkit.userinfo.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import io.rong.imkit.userinfo.db.model.Group;


@Dao
public interface GroupDao {
    @Query("select * from `group`")
    LiveData<List<Group>> getAllGroups();

    @Query("select * from `group` where id=:id")
    LiveData<Group> getLiveGroup(String id);

    @Query("select * from `group` where id=:id")
    Group getGroup(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGroup(Group group);

    @Query("delete from `group` where id=:id")
    void deleteGroup(String id);
}
