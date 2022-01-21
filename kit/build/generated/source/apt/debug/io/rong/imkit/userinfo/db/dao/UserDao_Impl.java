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
import io.rong.imkit.userinfo.db.model.User;
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
public final class UserDao_Impl implements UserDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<User> __insertionAdapterOfUser;

  private final SharedSQLiteStatement __preparedStmtOfDeleteUser;

  public UserDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUser = new EntityInsertionAdapter<User>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR REPLACE INTO `user` (`id`,`name`,`alias`,`portraitUri`,`extra`) VALUES (?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, User value) {
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
        if (value.alias == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.alias);
        }
        if (value.portraitUrl == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.portraitUrl);
        }
        if (value.extra == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.extra);
        }
      }
    };
    this.__preparedStmtOfDeleteUser = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "delete from user where id=?";
        return _query;
      }
    };
  }

  @Override
  public void insertUser(final User user) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfUser.insert(user);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteUser(final String id) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteUser.acquire();
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
      __preparedStmtOfDeleteUser.release(_stmt);
    }
  }

  @Override
  public LiveData<User> getLiveUser(final String id) {
    final String _sql = "select * from user where id=?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (id == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, id);
    }
    return __db.getInvalidationTracker().createLiveData(new String[]{"user"}, false, new Callable<User>() {
      @Override
      public User call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAlias = CursorUtil.getColumnIndexOrThrow(_cursor, "alias");
          final int _cursorIndexOfPortraitUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "portraitUri");
          final int _cursorIndexOfExtra = CursorUtil.getColumnIndexOrThrow(_cursor, "extra");
          final User _result;
          if(_cursor.moveToFirst()) {
            _result = new User();
            if (_cursor.isNull(_cursorIndexOfId)) {
              _result.id = null;
            } else {
              _result.id = _cursor.getString(_cursorIndexOfId);
            }
            if (_cursor.isNull(_cursorIndexOfName)) {
              _result.name = null;
            } else {
              _result.name = _cursor.getString(_cursorIndexOfName);
            }
            if (_cursor.isNull(_cursorIndexOfAlias)) {
              _result.alias = null;
            } else {
              _result.alias = _cursor.getString(_cursorIndexOfAlias);
            }
            if (_cursor.isNull(_cursorIndexOfPortraitUrl)) {
              _result.portraitUrl = null;
            } else {
              _result.portraitUrl = _cursor.getString(_cursorIndexOfPortraitUrl);
            }
            if (_cursor.isNull(_cursorIndexOfExtra)) {
              _result.extra = null;
            } else {
              _result.extra = _cursor.getString(_cursorIndexOfExtra);
            }
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
  public User getUser(final String id) {
    final String _sql = "select * from user where id=?";
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
      final int _cursorIndexOfAlias = CursorUtil.getColumnIndexOrThrow(_cursor, "alias");
      final int _cursorIndexOfPortraitUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "portraitUri");
      final int _cursorIndexOfExtra = CursorUtil.getColumnIndexOrThrow(_cursor, "extra");
      final User _result;
      if(_cursor.moveToFirst()) {
        _result = new User();
        if (_cursor.isNull(_cursorIndexOfId)) {
          _result.id = null;
        } else {
          _result.id = _cursor.getString(_cursorIndexOfId);
        }
        if (_cursor.isNull(_cursorIndexOfName)) {
          _result.name = null;
        } else {
          _result.name = _cursor.getString(_cursorIndexOfName);
        }
        if (_cursor.isNull(_cursorIndexOfAlias)) {
          _result.alias = null;
        } else {
          _result.alias = _cursor.getString(_cursorIndexOfAlias);
        }
        if (_cursor.isNull(_cursorIndexOfPortraitUrl)) {
          _result.portraitUrl = null;
        } else {
          _result.portraitUrl = _cursor.getString(_cursorIndexOfPortraitUrl);
        }
        if (_cursor.isNull(_cursorIndexOfExtra)) {
          _result.extra = null;
        } else {
          _result.extra = _cursor.getString(_cursorIndexOfExtra);
        }
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
  public LiveData<User> getUserLiveData(final String id) {
    final String _sql = "select * from user where id=?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (id == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, id);
    }
    return __db.getInvalidationTracker().createLiveData(new String[]{"user"}, false, new Callable<User>() {
      @Override
      public User call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAlias = CursorUtil.getColumnIndexOrThrow(_cursor, "alias");
          final int _cursorIndexOfPortraitUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "portraitUri");
          final int _cursorIndexOfExtra = CursorUtil.getColumnIndexOrThrow(_cursor, "extra");
          final User _result;
          if(_cursor.moveToFirst()) {
            _result = new User();
            if (_cursor.isNull(_cursorIndexOfId)) {
              _result.id = null;
            } else {
              _result.id = _cursor.getString(_cursorIndexOfId);
            }
            if (_cursor.isNull(_cursorIndexOfName)) {
              _result.name = null;
            } else {
              _result.name = _cursor.getString(_cursorIndexOfName);
            }
            if (_cursor.isNull(_cursorIndexOfAlias)) {
              _result.alias = null;
            } else {
              _result.alias = _cursor.getString(_cursorIndexOfAlias);
            }
            if (_cursor.isNull(_cursorIndexOfPortraitUrl)) {
              _result.portraitUrl = null;
            } else {
              _result.portraitUrl = _cursor.getString(_cursorIndexOfPortraitUrl);
            }
            if (_cursor.isNull(_cursorIndexOfExtra)) {
              _result.extra = null;
            } else {
              _result.extra = _cursor.getString(_cursorIndexOfExtra);
            }
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
  public LiveData<List<User>> getAllUsers() {
    final String _sql = "select * from user";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[]{"user"}, false, new Callable<List<User>>() {
      @Override
      public List<User> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAlias = CursorUtil.getColumnIndexOrThrow(_cursor, "alias");
          final int _cursorIndexOfPortraitUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "portraitUri");
          final int _cursorIndexOfExtra = CursorUtil.getColumnIndexOrThrow(_cursor, "extra");
          final List<User> _result = new ArrayList<User>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final User _item;
            _item = new User();
            if (_cursor.isNull(_cursorIndexOfId)) {
              _item.id = null;
            } else {
              _item.id = _cursor.getString(_cursorIndexOfId);
            }
            if (_cursor.isNull(_cursorIndexOfName)) {
              _item.name = null;
            } else {
              _item.name = _cursor.getString(_cursorIndexOfName);
            }
            if (_cursor.isNull(_cursorIndexOfAlias)) {
              _item.alias = null;
            } else {
              _item.alias = _cursor.getString(_cursorIndexOfAlias);
            }
            if (_cursor.isNull(_cursorIndexOfPortraitUrl)) {
              _item.portraitUrl = null;
            } else {
              _item.portraitUrl = _cursor.getString(_cursorIndexOfPortraitUrl);
            }
            if (_cursor.isNull(_cursorIndexOfExtra)) {
              _item.extra = null;
            } else {
              _item.extra = _cursor.getString(_cursorIndexOfExtra);
            }
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
  public List<User> getLimitUsers(final int limit) {
    final String _sql = "select * from user limit ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfAlias = CursorUtil.getColumnIndexOrThrow(_cursor, "alias");
      final int _cursorIndexOfPortraitUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "portraitUri");
      final int _cursorIndexOfExtra = CursorUtil.getColumnIndexOrThrow(_cursor, "extra");
      final List<User> _result = new ArrayList<User>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final User _item;
        _item = new User();
        if (_cursor.isNull(_cursorIndexOfId)) {
          _item.id = null;
        } else {
          _item.id = _cursor.getString(_cursorIndexOfId);
        }
        if (_cursor.isNull(_cursorIndexOfName)) {
          _item.name = null;
        } else {
          _item.name = _cursor.getString(_cursorIndexOfName);
        }
        if (_cursor.isNull(_cursorIndexOfAlias)) {
          _item.alias = null;
        } else {
          _item.alias = _cursor.getString(_cursorIndexOfAlias);
        }
        if (_cursor.isNull(_cursorIndexOfPortraitUrl)) {
          _item.portraitUrl = null;
        } else {
          _item.portraitUrl = _cursor.getString(_cursorIndexOfPortraitUrl);
        }
        if (_cursor.isNull(_cursorIndexOfExtra)) {
          _item.extra = null;
        } else {
          _item.extra = _cursor.getString(_cursorIndexOfExtra);
        }
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
