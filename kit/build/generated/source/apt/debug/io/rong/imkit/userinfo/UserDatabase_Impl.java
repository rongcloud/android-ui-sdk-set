package io.rong.imkit.userinfo;

import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomOpenHelper;
import androidx.room.RoomOpenHelper.Delegate;
import androidx.room.RoomOpenHelper.ValidationResult;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.room.util.TableInfo.Column;
import androidx.room.util.TableInfo.ForeignKey;
import androidx.room.util.TableInfo.Index;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Callback;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration;
import io.rong.imkit.userinfo.db.dao.GroupDao;
import io.rong.imkit.userinfo.db.dao.GroupDao_Impl;
import io.rong.imkit.userinfo.db.dao.GroupMemberDao;
import io.rong.imkit.userinfo.db.dao.GroupMemberDao_Impl;
import io.rong.imkit.userinfo.db.dao.UserDao;
import io.rong.imkit.userinfo.db.dao.UserDao_Impl;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings({"unchecked", "deprecation"})
public final class UserDatabase_Impl extends UserDatabase {
  private volatile GroupDao _groupDao;

  private volatile UserDao _userDao;

  private volatile GroupMemberDao _groupMemberDao;

  @Override
  protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration configuration) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(configuration, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("CREATE TABLE IF NOT EXISTS `user` (`id` TEXT NOT NULL, `name` TEXT, `portraitUri` TEXT, `extra` TEXT, PRIMARY KEY(`id`))");
        _db.execSQL("CREATE TABLE IF NOT EXISTS `group` (`id` TEXT NOT NULL, `name` TEXT, `portraitUri` TEXT, PRIMARY KEY(`id`))");
        _db.execSQL("CREATE TABLE IF NOT EXISTS `group_member` (`group_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `member_name` TEXT, PRIMARY KEY(`group_id`, `user_id`))");
        _db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        _db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'a8f46f150166605ac162b5db4f089ca6')");
      }

      @Override
      public void dropAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("DROP TABLE IF EXISTS `user`");
        _db.execSQL("DROP TABLE IF EXISTS `group`");
        _db.execSQL("DROP TABLE IF EXISTS `group_member`");
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onDestructiveMigration(_db);
          }
        }
      }

      @Override
      protected void onCreate(SupportSQLiteDatabase _db) {
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onCreate(_db);
          }
        }
      }

      @Override
      public void onOpen(SupportSQLiteDatabase _db) {
        mDatabase = _db;
        internalInitInvalidationTracker(_db);
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onOpen(_db);
          }
        }
      }

      @Override
      public void onPreMigrate(SupportSQLiteDatabase _db) {
        DBUtil.dropFtsSyncTriggers(_db);
      }

      @Override
      public void onPostMigrate(SupportSQLiteDatabase _db) {
      }

      @Override
      protected RoomOpenHelper.ValidationResult onValidateSchema(SupportSQLiteDatabase _db) {
        final HashMap<String, TableInfo.Column> _columnsUser = new HashMap<String, TableInfo.Column>(4);
        _columnsUser.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUser.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUser.put("portraitUri", new TableInfo.Column("portraitUri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUser.put("extra", new TableInfo.Column("extra", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUser = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUser = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUser = new TableInfo("user", _columnsUser, _foreignKeysUser, _indicesUser);
        final TableInfo _existingUser = TableInfo.read(_db, "user");
        if (! _infoUser.equals(_existingUser)) {
          return new RoomOpenHelper.ValidationResult(false, "user(io.rong.imkit.userinfo.db.model.User).\n"
                  + " Expected:\n" + _infoUser + "\n"
                  + " Found:\n" + _existingUser);
        }
        final HashMap<String, TableInfo.Column> _columnsGroup = new HashMap<String, TableInfo.Column>(3);
        _columnsGroup.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroup.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroup.put("portraitUri", new TableInfo.Column("portraitUri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGroup = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGroup = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGroup = new TableInfo("group", _columnsGroup, _foreignKeysGroup, _indicesGroup);
        final TableInfo _existingGroup = TableInfo.read(_db, "group");
        if (! _infoGroup.equals(_existingGroup)) {
          return new RoomOpenHelper.ValidationResult(false, "group(io.rong.imkit.userinfo.db.model.Group).\n"
                  + " Expected:\n" + _infoGroup + "\n"
                  + " Found:\n" + _existingGroup);
        }
        final HashMap<String, TableInfo.Column> _columnsGroupMember = new HashMap<String, TableInfo.Column>(3);
        _columnsGroupMember.put("group_id", new TableInfo.Column("group_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroupMember.put("user_id", new TableInfo.Column("user_id", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroupMember.put("member_name", new TableInfo.Column("member_name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGroupMember = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGroupMember = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGroupMember = new TableInfo("group_member", _columnsGroupMember, _foreignKeysGroupMember, _indicesGroupMember);
        final TableInfo _existingGroupMember = TableInfo.read(_db, "group_member");
        if (! _infoGroupMember.equals(_existingGroupMember)) {
          return new RoomOpenHelper.ValidationResult(false, "group_member(io.rong.imkit.userinfo.db.model.GroupMember).\n"
                  + " Expected:\n" + _infoGroupMember + "\n"
                  + " Found:\n" + _existingGroupMember);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "a8f46f150166605ac162b5db4f089ca6", "f2cd6dc9db28b50863076a5058051852");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
        .name(configuration.name)
        .callback(_openCallback)
        .build();
    final SupportSQLiteOpenHelper _helper = configuration.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "user","group","group_member");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `user`");
      _db.execSQL("DELETE FROM `group`");
      _db.execSQL("DELETE FROM `group_member`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  public GroupDao getGroupDao() {
    if (_groupDao != null) {
      return _groupDao;
    } else {
      synchronized(this) {
        if(_groupDao == null) {
          _groupDao = new GroupDao_Impl(this);
        }
        return _groupDao;
      }
    }
  }

  @Override
  public UserDao getUserDao() {
    if (_userDao != null) {
      return _userDao;
    } else {
      synchronized(this) {
        if(_userDao == null) {
          _userDao = new UserDao_Impl(this);
        }
        return _userDao;
      }
    }
  }

  @Override
  public GroupMemberDao getGroupMemberDao() {
    if (_groupMemberDao != null) {
      return _groupMemberDao;
    } else {
      synchronized(this) {
        if(_groupMemberDao == null) {
          _groupMemberDao = new GroupMemberDao_Impl(this);
        }
        return _groupMemberDao;
      }
    }
  }
}
