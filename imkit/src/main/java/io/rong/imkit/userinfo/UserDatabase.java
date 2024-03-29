package io.rong.imkit.userinfo;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import io.rong.common.rlog.RLog;
import io.rong.imkit.userinfo.db.dao.GroupDao;
import io.rong.imkit.userinfo.db.dao.GroupMemberDao;
import io.rong.imkit.userinfo.db.dao.UserDao;
import io.rong.imkit.userinfo.db.model.Group;
import io.rong.imkit.userinfo.db.model.GroupMember;
import io.rong.imkit.userinfo.db.model.User;
import io.rong.imkit.utils.ExecutorHelper;

@Database(
        entities = {User.class, Group.class, GroupMember.class},
        version = 3,
        exportSchema = false)
public abstract class UserDatabase extends RoomDatabase {
    private static final String TAG = UserDatabase.class.getCanonicalName();
    private static final String DB_NAME_FORMAT = "kit_user_%s";
    private static UserDatabase sInstance;
    private String mUserId;

    public abstract GroupDao getGroupDao();

    public abstract UserDao getUserDao();

    public abstract GroupMemberDao getGroupMemberDao();

    /**
     * 打开用户对应的数据库。
     *
     * @param context 应用上下文，用于初始数据库使用。
     * @param userId 用户 id，用于打开不同用户数据库时区分使用。
     * @return
     */
    static synchronized UserDatabase openDb(Context context, String userId, Callback callback) {
        if (TextUtils.isEmpty(userId) || context == null) {
            RLog.e(TAG, "openDb - context or userId can't be empty.");
            return null;
        }
        if (sInstance != null
                && !TextUtils.isEmpty(sInstance.mUserId)
                && !sInstance.mUserId.equals(userId)) {
            final UserDatabase oldDatabase = UserDatabase.sInstance;
            ExecutorHelper.getInstance()
                    .diskIO()
                    .execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    oldDatabase.close();
                                    RLog.d(
                                            TAG,
                                            "openDb - userId = "
                                                    + userId
                                                    + ", oldUserId = "
                                                    + oldDatabase.mUserId
                                                    + " db closed.");
                                }
                            });
            UserDatabase.sInstance = null;
        }
        if (sInstance == null) {
            sInstance = buildDatabase(context, userId, callback);
            sInstance.mUserId = userId;
        }
        return sInstance;
    }

    public static synchronized void closeDb() {
        if (sInstance != null) {
            sInstance.close();
            RLog.d(TAG, "closeDb - userId " + sInstance.mUserId + " db closed.");
            sInstance = null;
        }
    }

    private static UserDatabase buildDatabase(Context context, String userId, Callback callback) {
        return Room.databaseBuilder(context, UserDatabase.class, getDbName(userId))
                .fallbackToDestructiveMigration()
                .addCallback(callback)
                .addMigrations(
                        new Migration(2, 3) {
                            @Override
                            public void migrate(@NonNull SupportSQLiteDatabase database) {
                                database.execSQL("ALTER TABLE `group` ADD COLUMN `extra` TEXT");
                                database.execSQL(
                                        "ALTER TABLE `group_member` ADD COLUMN `extra` TEXT");
                            }
                        })
                .build();
    }

    private static String getDbName(String userId) {
        String idBase64 =
                Base64.encodeToString(userId.getBytes(), Base64.DEFAULT).replaceAll("//", "_");
        return String.format(DB_NAME_FORMAT, idBase64);
    }
}
