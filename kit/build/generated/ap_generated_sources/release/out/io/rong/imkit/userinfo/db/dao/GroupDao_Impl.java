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
import io.rong.imkit.userinfo.db.model.Group;
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
public final class GroupDao_Impl implements GroupDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Group> __insertionAdapterOfGroup;

  private final SharedSQLiteStatement __preparedStmtOfDeleteGroup;

  public GroupDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGroup = new EntityInsertionAdapter<Group>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR REPLACE INTO `group` (`id`,`name`,`portraitUri`) VALUES (?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Group value) {
        if (value.id == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.id);
        }
        if (value.name == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.name);
        }
        if (value.portraitUrl == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.portraitUrl);
        }
      }
    };
    this.__preparedStmtOfDeleteGroup = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "delete from `group` where id=?";
        return _query;
      }
    };
  }

  @Override
  public void insertGroup(final Group group) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfGroup.insert(group);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteGroup(final String id) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteGroup.acquire();
    int _argIndex = 1;
    if (id == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, id);
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteGroup.release(_stmt);
    }
  }

  @Override
  public LiveData<List<Group>> getAllGroups() {
    final String _sql = "select * from `group`";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[]{"group"}, false, new Callable<List<Group>>() {
      @Override
      public List<Group> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPortraitUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "portraitUri");
          final List<Group> _result = new ArrayList<Group>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final Group _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpPortraitUrl;
            if (_cursor.isNull(_cursorIndexOfPortraitUrl)) {
              _tmpPortraitUrl = null;
            } else {
              _tmpPortraitUrl = _cursor.getString(_cursorIndexOfPortraitUrl);
            }
            _item = new Group(_tmpId,_tmpName,_tmpPortraitUrl);
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
  public LiveData<Group> getLiveGroup(final String id) {
    final String _sql = "select * from `group` where id=?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (id == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, id);
    }
    return __db.getInvalidationTracker().createLiveData(new String[]{"group"}, false, new Callable<Group>() {
      @Override
      public Group call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPortraitUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "portraitUri");
          final Group _result;
          if(_cursor.moveToFirst()) {
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpPortraitUrl;
            if (_cursor.isNull(_cursorIndexOfPortraitUrl)) {
              _tmpPortraitUrl = null;
            } else {
              _tmpPortraitUrl = _cursor.getString(_cursorIndexOfPortraitUrl);
            }
            _result = new Group(_tmpId,_tmpName,_tmpPortraitUrl);
          } else {
            _result = null;
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
  public Group getGroup(final String id) {
    final String _sql = "select * from `group` where id=?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (id == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, id);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfPortraitUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "portraitUri");
      final Group _result;
      if(_cursor.moveToFirst()) {
        final String _tmpId;
        if (_cursor.isNull(_cursorIndexOfId)) {
          _tmpId = null;
        } else {
          _tmpId = _cursor.getString(_cursorIndexOfId);
        }
        final String _tmpName;
        if (_cursor.isNull(_cursorIndexOfName)) {
          _tmpName = null;
        } else {
          _tmpName = _cursor.getString(_cursorIndexOfName);
        }
        final String _tmpPortraitUrl;
        if (_cursor.isNull(_cursorIndexOfPortraitUrl)) {
          _tmpPortraitUrl = null;
        } else {
          _tmpPortraitUrl = _cursor.getString(_cursorIndexOfPortraitUrl);
        }
        _result = new Group(_tmpId,_tmpName,_tmpPortraitUrl);
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
  public List<Group> getLimitGroups(final int limit) {
    final String _sql = "select * from `group` limit ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfPortraitUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "portraitUri");
      final List<Group> _result = new ArrayList<Group>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final Group _item;
        final String _tmpId;
        if (_cursor.isNull(_cursorIndexOfId)) {
          _tmpId = null;
        } else {
          _tmpId = _cursor.getString(_cursorIndexOfId);
        }
        final String _tmpName;
        if (_cursor.isNull(_cursorIndexOfName)) {
          _tmpName = null;
        } else {
          _tmpName = _cursor.getString(_cursorIndexOfName);
        }
        final String _tmpPortraitUrl;
        if (_cursor.isNull(_cursorIndexOfPortraitUrl)) {
          _tmpPortraitUrl = null;
        } else {
          _tmpPortraitUrl = _cursor.getString(_cursorIndexOfPortraitUrl);
        }
        _item = new Group(_tmpId,_tmpName,_tmpPortraitUrl);
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
