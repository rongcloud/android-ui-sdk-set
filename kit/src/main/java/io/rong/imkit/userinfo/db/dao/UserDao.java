package io.rong.imkit.userinfo.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import io.rong.imkit.userinfo.db.model.User;


@Dao
public interface UserDao {
    @Query("select * from user where id=:id")
    LiveData<User> getLiveUser(String id);

    @Query("select * from user where id=:id")
    User getUser(String id);

    @Query("select * from user where id=:id")
    LiveData<User> getUserLiveData(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(User user);

    @Query("delete from user where id=:id")
    void deleteUser(String id);

    @Query("select * from user")
    LiveData<List<User>> getAllUsers();
}
