package io.rong.imkit.userinfo;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.room.RoomDatabase;
import io.rong.common.RLog;
import io.rong.common.utils.function.Action0;
import io.rong.common.utils.function.Action1;
import io.rong.common.utils.function.Func0;
import io.rong.common.utils.function.Func1;
import io.rong.common.utils.optional.Option;
import io.rong.imkit.userinfo.db.model.Group;
import io.rong.imkit.userinfo.db.model.GroupMember;
import io.rong.imkit.userinfo.db.model.User;
import io.rong.imkit.utils.ExecutorHelper;
import java.util.Collections;
import java.util.List;

public class DbDataSource {
    private static final String TAG = DbDataSource.class.getSimpleName();
    private UserDatabase database;

    DbDataSource(@NonNull Context context, @NonNull String userId, RoomDatabase.Callback callback) {
        database = UserDatabase.openDb(context, userId, callback);
    }

    public UserDatabase getDatabase() {
        return database;
    }

    void getUserInfo(final String userId, final Consumer<User> callback) {
        ExecutorHelper.getInstance()
                .diskIO()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                User user =
                                        Option.ofObj(database)
                                                .map(
                                                        new Func1<UserDatabase, User>() {
                                                            @Override
                                                            public User call(
                                                                    UserDatabase userDatabase) {
                                                                try {
                                                                    return userDatabase
                                                                            .getUserDao()
                                                                            .getUser(userId);
                                                                } catch (Exception e) {
                                                                    RLog.e(TAG, "getUser fail", e);
                                                                    return null;
                                                                }
                                                            }
                                                        })
                                                .orDefault(
                                                        new Func0<User>() {
                                                            @Override
                                                            public User call() {
                                                                RLog.e(TAG, "UserDatabase is null");
                                                                return null;
                                                            }
                                                        });
                                callback.accept(user);
                            }
                        });
    }

    void getGroupInfo(final String groupId, final Consumer<Group> callback) {
        ExecutorHelper.getInstance()
                .diskIO()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                Group group =
                                        Option.ofObj(database)
                                                .map(
                                                        new Func1<UserDatabase, Group>() {
                                                            @Override
                                                            public Group call(
                                                                    UserDatabase userDatabase) {
                                                                try {
                                                                    return userDatabase
                                                                            .getGroupDao()
                                                                            .getGroup(groupId);
                                                                } catch (Exception e) {
                                                                    RLog.e(TAG, "getGroup fail", e);
                                                                    return null;
                                                                }
                                                            }
                                                        })
                                                .orDefault(
                                                        new Func0<Group>() {
                                                            @Override
                                                            public Group call() {
                                                                RLog.e(TAG, "UserDatabase is null");
                                                                return null;
                                                            }
                                                        });
                                callback.accept(group);
                            }
                        });
    }

    void getGroupUserInfo(
            final String groupId, final String userId, final Consumer<GroupMember> callback) {
        ExecutorHelper.getInstance()
                .diskIO()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                GroupMember groupMember =
                                        Option.ofObj(database)
                                                .map(
                                                        new Func1<UserDatabase, GroupMember>() {
                                                            @Override
                                                            public GroupMember call(
                                                                    UserDatabase userDatabase) {
                                                                try {
                                                                    return userDatabase
                                                                            .getGroupMemberDao()
                                                                            .getGroupMember(
                                                                                    groupId,
                                                                                    userId);
                                                                } catch (Exception e) {
                                                                    RLog.e(
                                                                            TAG,
                                                                            "getGroupMember fail",
                                                                            e);
                                                                    return null;
                                                                }
                                                            }
                                                        })
                                                .orDefault(
                                                        new Func0<GroupMember>() {
                                                            @Override
                                                            public GroupMember call() {
                                                                RLog.e(TAG, "UserDatabase is null");
                                                                return null;
                                                            }
                                                        });
                                callback.accept(groupMember);
                            }
                        });
    }

    void refreshUserInfo(@NonNull final User user) {
        ExecutorHelper.getInstance()
                .diskIO()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                Option.ofObj(database)
                                        .ifSome(
                                                new Action1<UserDatabase>() {
                                                    @Override
                                                    public void call(UserDatabase userDatabase) {
                                                        try {
                                                            userDatabase
                                                                    .getUserDao()
                                                                    .insertUser(user);
                                                        } catch (Exception e) {
                                                            RLog.e(TAG, "insertUser fail", e);
                                                        }
                                                    }
                                                })
                                        .ifNone(
                                                new Action0() {
                                                    @Override
                                                    public void call() {
                                                        RLog.e(TAG, "UserDatabase is null");
                                                    }
                                                });
                            }
                        });
    }

    void refreshGroupInfo(@NonNull final Group group) {
        ExecutorHelper.getInstance()
                .diskIO()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                Option.ofObj(database)
                                        .ifSome(
                                                new Action1<UserDatabase>() {
                                                    @Override
                                                    public void call(UserDatabase userDatabase) {
                                                        try {
                                                            userDatabase
                                                                    .getGroupDao()
                                                                    .insertGroup(group);
                                                        } catch (Exception e) {
                                                            RLog.e(TAG, "insertGroup fail", e);
                                                        }
                                                    }
                                                })
                                        .ifNone(
                                                new Action0() {
                                                    @Override
                                                    public void call() {
                                                        RLog.e(TAG, "UserDatabase is null");
                                                    }
                                                });
                            }
                        });
    }

    void refreshGroupUserInfo(@NonNull final GroupMember groupMember) {
        ExecutorHelper.getInstance()
                .diskIO()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                Option.ofObj(database)
                                        .ifSome(
                                                new Action1<UserDatabase>() {
                                                    @Override
                                                    public void call(UserDatabase userDatabase) {
                                                        try {
                                                            userDatabase
                                                                    .getGroupMemberDao()
                                                                    .insertGroupMember(groupMember);
                                                        } catch (Exception e) {
                                                            RLog.e(
                                                                    TAG,
                                                                    "insertGroupMember fail",
                                                                    e);
                                                        }
                                                    }
                                                })
                                        .ifNone(
                                                new Action0() {
                                                    @Override
                                                    public void call() {
                                                        RLog.e(TAG, "UserDatabase is null");
                                                    }
                                                });
                            }
                        });
    }

    void getLimitUser(final int limit, final Consumer<List<User>> callback) {
        ExecutorHelper.getInstance()
                .diskIO()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                List<User> user =
                                        Option.ofObj(database)
                                                .map(
                                                        new Func1<UserDatabase, List<User>>() {
                                                            @Override
                                                            public List<User> call(
                                                                    UserDatabase userDatabase) {
                                                                try {
                                                                    return userDatabase
                                                                            .getUserDao()
                                                                            .getLimitUsers(limit);
                                                                } catch (Exception e) {
                                                                    RLog.e(TAG, "getUser fail", e);
                                                                    return Collections.emptyList();
                                                                }
                                                            }
                                                        })
                                                .orDefault(
                                                        new Func0<List<User>>() {
                                                            @Override
                                                            public List<User> call() {
                                                                RLog.e(TAG, "UserDatabase is null");
                                                                return Collections.emptyList();
                                                            }
                                                        });
                                callback.accept(user);
                            }
                        });
    }

    void getLimitGroup(final int limit, final Consumer<List<Group>> callback) {
        ExecutorHelper.getInstance()
                .diskIO()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                List<Group> group =
                                        Option.ofObj(database)
                                                .map(
                                                        new Func1<UserDatabase, List<Group>>() {
                                                            @Override
                                                            public List<Group> call(
                                                                    UserDatabase userDatabase) {
                                                                try {
                                                                    return userDatabase
                                                                            .getGroupDao()
                                                                            .getLimitGroups(limit);
                                                                } catch (Exception e) {
                                                                    RLog.e(TAG, "getUser fail", e);
                                                                    return Collections.emptyList();
                                                                }
                                                            }
                                                        })
                                                .orDefault(
                                                        new Func0<List<Group>>() {
                                                            @Override
                                                            public List<Group> call() {
                                                                RLog.e(TAG, "UserDatabase is null");
                                                                return Collections.emptyList();
                                                            }
                                                        });
                                callback.accept(group);
                            }
                        });
    }

    void getLimitGroupMember(final int limit, final Consumer<List<GroupMember>> callback) {
        ExecutorHelper.getInstance()
                .diskIO()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                List<GroupMember> groupMembers =
                                        Option.ofObj(database)
                                                .map(
                                                        new Func1<
                                                                UserDatabase, List<GroupMember>>() {
                                                            @Override
                                                            public List<GroupMember> call(
                                                                    UserDatabase userDatabase) {
                                                                try {
                                                                    return userDatabase
                                                                            .getGroupMemberDao()
                                                                            .getLimitGroupMembers(
                                                                                    limit);
                                                                } catch (Exception e) {
                                                                    RLog.e(TAG, "getUser fail", e);
                                                                    return Collections.emptyList();
                                                                }
                                                            }
                                                        })
                                                .orDefault(
                                                        new Func0<List<GroupMember>>() {
                                                            @Override
                                                            public List<GroupMember> call() {
                                                                RLog.e(TAG, "UserDatabase is null");
                                                                return Collections.emptyList();
                                                            }
                                                        });
                                callback.accept(groupMembers);
                            }
                        });
    }
}
