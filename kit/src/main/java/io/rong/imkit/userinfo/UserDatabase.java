package io.rong.imkit.userinfo;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import androidx.lifecycle.MutableLiveData;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import io.rong.imkit.userinfo.db.dao.GroupDao;
import io.rong.imkit.userinfo.db.dao.GroupMemberDao;
import io.rong.imkit.userinfo.db.dao.UserDao;
import io.rong.imkit.userinfo.db.model.Group;
import io.rong.imkit.userinfo.db.model.GroupMember;
import io.rong.imkit.userinfo.db.model.User;
import io.rong.common.rlog.RLog;

@Database(entities = {User.class, Group.class, GroupMember.class}, version = 1, exportSchema = false)
public abstract class UserDatabase extends RoomDatabase {
    private static final String TAG = UserDatabase.class.getCanonicalName();
    private static final String DB_NAME_FORMAT = "kit_user_%s";
    private static UserDatabase sInstance;
    private static MutableLiveData<Boolean> mDatabaseCreated = new MutableLiveData<>();
    protected static volatile Boolean isDatabaseCreated;
    private String mUserId;

    public abstract GroupDao getGroupDao();

    public abstract UserDao getUserDao();

    public abstract GroupMemberDao getGroupMemberDao();

    /**
     * 打开用户对应的数据库。
     *
     * @param context 应用上下文，用于初始数据库使用。
     * @param userId  用户 id，用于打开不同用户数据库时区分使用。
     * @return
     */
    static UserDatabase openDb(Context context, String userId) {
        synchronized (UserDatabase.class) {
            if (TextUtils.isEmpty(userId) || context == null) {
                RLog.e(TAG, "openDb - context or userId can't be empty.");
                return null;
            }
            if (sInstance != null && !TextUtils.isEmpty(sInstance.mUserId)
                    && !sInstance.mUserId.equals(userId)) {
                sInstance.close();
                RLog.d(TAG, "openDb - userId " + userId + " db closed.");
                sInstance = null;
            }
            if (sInstance == null) {
                sInstance = buildDatabase(context, userId);
                sInstance.mUserId = userId;
                mDatabaseCreated.postValue(true);
            }
            return sInstance;
        }
    }

    static MutableLiveData<Boolean> getDatabaseCreated() {
        return mDatabaseCreated;
    }

    public static void closeDb() {
        if (sInstance != null) {
            sInstance.close();
            RLog.d(TAG, "closeDb - userId " + sInstance.mUserId + " db closed.");
            sInstance = null;
        }
    }

    private static UserDatabase buildDatabase(Context context, String userId) {
        return Room.databaseBuilder(context, UserDatabase.class, getDbName(userId))
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();
    }

    private static String getDbName(String userId) {
        String idBase64 = Base64.encodeToString(userId.getBytes(), Base64.DEFAULT).replaceAll("//", "_");
        return String.format(DB_NAME_FORMAT, idBase64);
    }

}
