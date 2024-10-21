package io.rong.imkit.usermanage.handler;

import io.rong.imkit.base.MultiDataHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.DirectionType;
import io.rong.imlib.model.FriendInfo;
import io.rong.imlib.model.FriendRelationInfo;
import io.rong.imlib.model.QueryFriendsDirectionType;
import io.rong.imlib.model.UserProfile;
import java.util.ArrayList;
import java.util.List;

/**
 * @author rongcloud
 * @since 5.10.4
 */
public class FriendInfoHandler extends MultiDataHandler {
    public static final MultiDataHandler.DataKey<FriendRelationInfo> KEY_CHECK_FRIEND =
            MultiDataHandler.DataKey.obtain("KEY_CHECK_FRIEND", FriendRelationInfo.class);
    public static final MultiDataHandler.DataKey<List<FriendInfo>> KEY_GET_FRIENDS =
            MultiDataHandler.DataKey.obtain(
                    "KEY_GET_FRIENDS", (Class<List<FriendInfo>>) (Class<?>) List.class);

    public static final MultiDataHandler.DataKey<UserProfile> KEY_SEARCH_USER =
            MultiDataHandler.DataKey.obtain("KEY_SEARCH_USER", UserProfile.class);
    public static final MultiDataHandler.DataKey<FriendInfo> KEY_GET_FRIEND =
            MultiDataHandler.DataKey.obtain("KEY_GET_FRIEND", FriendInfo.class);
    public static final MultiDataHandler.DataKey<Boolean> KEY_DELETE_FRIEND =
            MultiDataHandler.DataKey.obtain("KEY_DELETE_FRIEND", Boolean.class);
    public static final MultiDataHandler.DataKey<IRongCoreEnum.CoreErrorCode> KEY_APPLY_FRIEND =
            MultiDataHandler.DataKey.obtain("KEY_APPLY_FRIEND", IRongCoreEnum.CoreErrorCode.class);
    public static final MultiDataHandler.DataKey<List<FriendInfo>> KEY_SEARCH_FRIENDS =
            MultiDataHandler.DataKey.obtain(
                    "KEY_SEARCH_FRIENDS", (Class<List<FriendInfo>>) (Class<?>) List.class);

    public FriendInfoHandler() {}

    public void getFriends(QueryFriendsDirectionType directionType) {
        RongCoreClient.getInstance()
                .getFriends(
                        directionType,
                        new IRongCoreCallback.ResultCallback<List<FriendInfo>>() {
                            @Override
                            public void onSuccess(List<FriendInfo> friendInfos) {
                                notifyDataChange(KEY_GET_FRIENDS, friendInfos);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                notifyDataError(KEY_GET_FRIENDS, e);
                            }
                        });
    }

    public void checkFriend(String userId) {
        List<String> userIds = new ArrayList<>(1);
        userIds.add(userId);
        RongCoreClient.getInstance()
                .checkFriends(
                        userIds,
                        DirectionType.Both,
                        new IRongCoreCallback.ResultCallback<List<FriendRelationInfo>>() {
                            @Override
                            public void onSuccess(List<FriendRelationInfo> result) {
                                notifyDataChange(KEY_CHECK_FRIEND, result.get(0));
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {}
                        });
    }

    public void getFriendInfo(String userId) {
        List<String> userIds = new ArrayList<>(1);
        userIds.add(userId);
        RongCoreClient.getInstance()
                .getFriendsInfo(
                        userIds,
                        new IRongCoreCallback.ResultCallback<List<FriendInfo>>() {
                            @Override
                            public void onSuccess(List<FriendInfo> info) {
                                if (info != null && !info.isEmpty()) {
                                    notifyDataChange(KEY_GET_FRIEND, info.get(0));
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {}
                        });
    }

    public void deleteFriend(String userId, OnDataChangeListener<Boolean> listener) {
        replaceDataChangeListener(KEY_DELETE_FRIEND, listener);
        List<String> userIds = new ArrayList<>(1);
        userIds.add(userId);
        RongCoreClient.getInstance()
                .deleteFriends(
                        userIds,
                        DirectionType.Both,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_DELETE_FRIEND, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_DELETE_FRIEND, false);
                            }
                        });
    }

    public void applyFriend(
            String userId,
            String remark,
            OnDataChangeListener<IRongCoreEnum.CoreErrorCode> listener) {
        replaceDataChangeListener(KEY_APPLY_FRIEND, listener);
        RongCoreClient.getInstance()
                .addFriend(
                        userId,
                        DirectionType.Both,
                        remark,
                        new IRongCoreCallback.ResultCallback<IRongCoreEnum.CoreErrorCode>() {
                            @Override
                            public void onSuccess(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_APPLY_FRIEND, coreErrorCode);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                notifyDataChange(KEY_APPLY_FRIEND, e);
                            }
                        });
    }

    public void findUser(String uniqueId) {
        RongCoreClient.getInstance()
                .searchUserProfileByUniqueId(
                        uniqueId,
                        new IRongCoreCallback.ResultCallback<UserProfile>() {
                            @Override
                            public void onSuccess(UserProfile userProfile) {
                                notifyDataChange(KEY_SEARCH_USER, userProfile);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                notifyDataChange(KEY_SEARCH_USER, null);
                            }
                        });
    }

    public void searchFriendsInfo(String query) {
        RongCoreClient.getInstance()
                .searchFriendsInfo(
                        query,
                        new IRongCoreCallback.ResultCallback<List<FriendInfo>>() {
                            @Override
                            public void onSuccess(List<FriendInfo> friendInfos) {
                                notifyDataChange(KEY_SEARCH_FRIENDS, friendInfos);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                notifyDataChange(KEY_SEARCH_FRIENDS, new ArrayList<>());
                            }
                        });
    }
}
