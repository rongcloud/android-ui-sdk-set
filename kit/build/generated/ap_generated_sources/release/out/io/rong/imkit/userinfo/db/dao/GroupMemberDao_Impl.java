package io.rong.imkit.userinfo.db.dao;

import android.database.Cursor;
import androidx.lifecycle.LiveData;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import io.rong.imkit.userinfo.db.model.GroupMember;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

@SuppressWarnings({"unchecked", "deprecation"})
public final class GroupMemberDao_Impl implements GroupMemberDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GroupMember> __insertionAdapterOfGroupMember;

  private final SharedSQLiteStatement __preparedStmtOfRemoveGroupMember;

  private final SharedSQLiteStatement __preparedStmtOfRemoveGroupAllMember;

  public GroupMemberDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGroupMember = new EntityInsertionAdapter<GroupMember>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR REPLACE INTO `group_member` (`group_id`,`user_id`,`member_name`) VALUES (?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, GroupMember value) {
        if (value.groupId == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.groupId);
        }
        if (value.userId == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.userId);
        }
        if (value.memberName == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.memberName);
        }
      }
    };
    this.__preparedStmtOfRemoveGroupMember = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "delete from group_member where group_id=? and user_id=?";
        return _query;
      }
    };
    this.__preparedStmtOfRemoveGroupAllMember = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "delete from group_member where group_id=?";
        return _query;
      }
    };
  }

  @Override
  public void insertGroupMember(final GroupMember groupMember) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfGroupMember.insert(groupMember);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertGroupMembers(final List<GroupMember> groupMemberList) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfGroupMember.insert(groupMemberList);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void removeGroupMember(final String groupId, final String userId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfRemoveGroupMember.acquire();
    int _argIndex = 1;
    if (groupId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, groupId);
    }
    _argIndex = 2;
    if (userId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, userId);
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfRemoveGroupMember.release(_stmt);
    }
  }

  @Override
  public void removeGroupAllMember(final String groupId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfRemoveGroupAllMember.acquire();
    int _argIndex = 1;
    if (groupId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, groupId);
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfRemoveGroupAllMember.release(_stmt);
    }
  }

  @Override
  public LiveData<List<GroupMember>> getGroupAllMembers(final String groupId) {
    final String _sql = "select * from group_member where group_id=?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (groupId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, groupId);
    }
    return __db.getInvalidationTracker().createLiveData(new String[]{"group_member"}, false, new Callable<List<GroupMember>>() {
      @Override
      public List<GroupMember> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "group_id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "user_id");
          final int _cursorIndexOfMemberName = CursorUtil.getColumnIndexOrThrow(_cursor, "member_name");
          final List<GroupMember> _result = new ArrayList<GroupMember>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final GroupMember _item;
            final String _tmpGroupId;
            if (_cursor.isNull(_cursorIndexOfGroupId)) {
              _tmpGroupId = null;
            } else {
              _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
            }
            final String _tmpUserId;
            if (_cursor.isNull(_cursorIndexOfUserId)) {
              _tmpUserId = null;
            } else {
              _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            }
            final String _tmpMemberName;
            if (_cursor.isNull(_cursorIndexOfMemberName)) {
              _tmpMemberName = null;
            } else {
              _tmpMemberName = _cursor.getString(_cursorIndexOfMemberName);
            }
            _item = new GroupMember(_tmpGroupId,_tmpUserId,_tmpMemberName);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public LiveData<List<GroupMember>> getAllGroupMembers() {
    final String _sql = "select * from group_member";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[]{"group_member"}, false, new Callable<List<GroupMember>>() {
      @Override
      public List<GroupMember> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "group_id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "user_id");
          final int _cursorIndexOfMemberName = CursorUtil.getColumnIndexOrThrow(_cursor, "member_name");
          final List<GroupMember> _result = new ArrayList<GroupMember>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final GroupMember _item;
            final String _tmpGroupId;
            if (_cursor.isNull(_cursorIndexOfGroupId)) {
              _tmpGroupId = null;
            } else {
              _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
            }
            final String _tmpUserId;
            if (_cursor.isNull(_cursorIndexOfUserId)) {
              _tmpUserId = null;
            } else {
              _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            }
            final String _tmpMemberName;
            if (_cursor.isNull(_cursorIndexOfMemberName)) {
              _tmpMemberName = null;
            } else {
              _tmpMemberName = _cursor.getString(_cursorIndexOfMemberName);
            }
            _item = new GroupMember(_tmpGroupId,_tmpUserId,_tmpMemberName);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public GroupMember getGroupMember(final String groupId, final String userId) {
    final String _sql = "select * from group_member where group_id=? and user_id=?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    if (groupId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, groupId);
    }
    _argIndex = 2;
    if (userId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, userId);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "group_id");
      final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "user_id");
      final int _cursorIndexOfMemberName = CursorUtil.getColumnIndexOrThrow(_cursor, "member_name");
      final GroupMember _result;
      if(_cursor.moveToFirst()) {
        final String _tmpGroupId;
        if (_cursor.isNull(_cursorIndexOfGroupId)) {
          _tmpGroupId = null;
        } else {
          _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
        }
        final String _tmpUserId;
        if (_cursor.isNull(_cursorIndexOfUserId)) {
          _tmpUserId = null;
        } else {
          _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
        }
        final String _tmpMemberName;
        if (_cursor.isNull(_cursorIndexOfMemberName)) {
          _tmpMemberName = null;
        } else {
          _tmpMemberName = _cursor.getString(_cursorIndexOfMemberName);
        }
        _result = new GroupMember(_tmpGroupId,_tmpUserId,_tmpMemberName);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<GroupMember> getLimitGroupMembers(final int limit) {
    final String _sql = "select * from group_member limit ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "group_id");
      final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "user_id");
      final int _cursorIndexOfMemberName = CursorUtil.getColumnIndexOrThrow(_cursor, "member_name");
      final List<GroupMember> _result = new ArrayList<GroupMember>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final GroupMember _item;
        final String _tmpGroupId;
        if (_cursor.isNull(_cursorIndexOfGroupId)) {
          _tmpGroupId = null;
        } else {
          _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
        }
        final String _tmpUserId;
        if (_cursor.isNull(_cursorIndexOfUserId)) {
          _tmpUserId = null;
        } else {
          _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
        }
        final String _tmpMemberName;
        if (_cursor.isNull(_cursorIndexOfMemberName)) {
          _tmpMemberName = null;
        } else {
          _tmpMemberName = _cursor.getString(_cursorIndexOfMemberName);
        }
        _item = new GroupMember(_tmpGroupId,_tmpUserId,_tmpMemberName);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
