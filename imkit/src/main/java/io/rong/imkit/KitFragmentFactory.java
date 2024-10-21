package io.rong.imkit;

import android.os.Bundle;
import androidx.annotation.NonNull;
import io.rong.imkit.usermanage.friend.add.AddFriendListFragment;
import io.rong.imkit.usermanage.friend.apply.ApplyFriendListFragment;
import io.rong.imkit.usermanage.friend.friendlist.FriendListFragment;
import io.rong.imkit.usermanage.friend.my.gender.UpdateGenderFragment;
import io.rong.imkit.usermanage.friend.my.nikename.UpdateNickNameFragment;
import io.rong.imkit.usermanage.friend.my.profile.MyProfileFragment;
import io.rong.imkit.usermanage.friend.search.FriendSearchFragment;
import io.rong.imkit.usermanage.friend.select.FriendSelectFragment;
import io.rong.imkit.usermanage.friend.user.profile.UserProfileFragment;
import io.rong.imkit.usermanage.group.add.AddGroupMembersFragment;
import io.rong.imkit.usermanage.group.create.GroupCreateFragment;
import io.rong.imkit.usermanage.group.memberlist.GroupMemberListFragment;
import io.rong.imkit.usermanage.group.name.GroupNameFragment;
import io.rong.imkit.usermanage.group.nickname.GroupNicknameFragment;
import io.rong.imkit.usermanage.group.notice.GroupNoticeFragment;
import io.rong.imkit.usermanage.group.profile.GroupProfileFragment;
import io.rong.imkit.usermanage.group.remove.RemoveGroupMembersFragment;

/**
 * 创建一个新Fragment。
 *
 * <p>UIKit提供的每个屏幕都会通过这个工厂创建一个Fragment。
 *
 * <p>要使用自定义片段，而不是默认片段，必须继承此 Factory。
 *
 * <p>扩展工厂必须通过 {@link IMCenter#setKitFragmentFactory(KitFragmentFactory)}方法在SDK中注册。
 *
 * @author rongcloud
 * @since 5.10.5
 */
public class KitFragmentFactory {

    /**
     * 返回 MyProfileFragment.
     *
     * @param args 创建 MyProfileFragment 提供的参数
     * @return {@link MyProfileFragment} since 5.10.5
     */
    @NonNull
    public MyProfileFragment newMyProfileFragment(@NonNull Bundle args) {
        return new MyProfileFragment();
    }

    /**
     * 返回 UserProfileFragment.
     *
     * @param args 创建 UserProfileFragment 提供的参数
     * @return {@link UserProfileFragment} since 5.10.5
     */
    @NonNull
    public UserProfileFragment newUserProfileFragment(@NonNull Bundle args) {
        UserProfileFragment fragment = new UserProfileFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * 返回 GroupNoticeFragment.
     *
     * @param args 创建 GroupNoticeFragment 提供的参数
     * @return {@link GroupNoticeFragment} since 5.10.5
     */
    @NonNull
    public ApplyFriendListFragment newApplyFriendListFragment(@NonNull Bundle args) {
        ApplyFriendListFragment fragment = new ApplyFriendListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * 返回 GroupCreateFragment.
     *
     * @param args 创建 GroupCreateFragment 提供的参数
     * @return {@link GroupCreateFragment} since 1.0
     */
    @NonNull
    public GroupCreateFragment newGroupCreateFragment(@NonNull Bundle args) {
        GroupCreateFragment groupCreateFragment = new GroupCreateFragment();
        groupCreateFragment.setArguments(args);
        return groupCreateFragment;
    }

    /**
     * 返回 UpdateGenderFragment.
     *
     * @param args 创建 UpdateGenderFragment 提供的参数
     * @return {@link UpdateGenderFragment} since 1.0
     */
    @NonNull
    public UpdateGenderFragment newUpdateGenderFragment(@NonNull Bundle args) {
        UpdateGenderFragment updateGenderFragment = new UpdateGenderFragment();
        updateGenderFragment.setArguments(args);
        return updateGenderFragment;
    }

    /**
     * 返回 UpdateNikeNameFragment.
     *
     * @param args 创建 UpdateNikeNameFragment 提供的参数
     * @return {@link UpdateNickNameFragment} since 1.0
     */
    @NonNull
    public UpdateNickNameFragment newUpdateNikeNameFragment(@NonNull Bundle args) {
        UpdateNickNameFragment updateGenderFragment = new UpdateNickNameFragment();
        updateGenderFragment.setArguments(args);
        return updateGenderFragment;
    }

    /**
     * 返回 GroupProfileFragment.
     *
     * @param args 创建 GroupProfileFragment 提供的参数
     * @return {@link GroupProfileFragment} since 1.0
     */
    @NonNull
    public GroupProfileFragment newGroupProfileFragment(@NonNull Bundle args) {
        GroupProfileFragment groupProfileFragment = new GroupProfileFragment();
        groupProfileFragment.setArguments(args);
        return groupProfileFragment;
    }

    /**
     * 返回 FriendSelectFragment.
     *
     * @param args 创建 FriendSelectFragment 提供的参数
     * @return {@link FriendSelectFragment} since 1.0
     */
    @NonNull
    public FriendSelectFragment newFriendSelectFragment(@NonNull Bundle args) {
        FriendSelectFragment friendSelectFragment = new FriendSelectFragment();
        friendSelectFragment.setArguments(args);
        return friendSelectFragment;
    }

    /**
     * 返回 AddFriendListFragment.
     *
     * @param args 创建 AddFriendListFragment 提供的参数
     * @return {@link AddFriendListFragment} since 1.0
     */
    @NonNull
    public AddFriendListFragment newAddFriendListFragment(@NonNull Bundle args) {
        AddFriendListFragment addFriendListFragment = new AddFriendListFragment();
        addFriendListFragment.setArguments(args);
        return addFriendListFragment;
    }
    /**
     * 返回 FriendListFragment.
     *
     * @param args 创建 FriendListFragment 提供的参数
     * @return {@link FriendListFragment} since 1.0
     */
    @NonNull
    public FriendListFragment newFriendListFragment(@NonNull Bundle args) {
        FriendListFragment friendListFragment = new FriendListFragment();
        friendListFragment.setArguments(args);
        return friendListFragment;
    }

    /**
     * 返回 FriendSearchFragment.
     *
     * @param args 创建 FriendSearchFragment 提供的参数
     * @return {@link FriendSearchFragment} since 1.0
     */
    @NonNull
    public FriendSearchFragment newFriendSearchFragment(@NonNull Bundle args) {
        FriendSearchFragment friendSearchFragment = new FriendSearchFragment();
        friendSearchFragment.setArguments(args);
        return friendSearchFragment;
    }

    /**
     * 返回 AddGroupMembersFragment.
     *
     * @param args 创建 AddGroupMembersFragment 提供的参数
     * @return {@link AddGroupMembersFragment} since 1.0
     */
    @NonNull
    public AddGroupMembersFragment newAddGroupMembersFragment(@NonNull Bundle args) {
        AddGroupMembersFragment addGroupMembersFragment = new AddGroupMembersFragment();
        addGroupMembersFragment.setArguments(args);
        return addGroupMembersFragment;
    }

    /**
     * 返回 RemoveGroupMembersFragment.
     *
     * @param args 创建 RemoveGroupMembersFragment 提供的参数
     * @return {@link RemoveGroupMembersFragment} since 1.0
     */
    @NonNull
    public RemoveGroupMembersFragment newRemoveGroupMembersFragment(@NonNull Bundle args) {
        RemoveGroupMembersFragment removeGroupMembersFragment = new RemoveGroupMembersFragment();
        removeGroupMembersFragment.setArguments(args);
        return removeGroupMembersFragment;
    }

    /**
     * 返回 GroupNicknameFragment.
     *
     * @param args 创建 GroupNicknameFragment 提供的参数
     * @return {@link GroupNicknameFragment} since 1.0
     */
    @NonNull
    public GroupNicknameFragment newGroupNicknameFragment(@NonNull Bundle args) {
        GroupNicknameFragment groupNicknameFragment = new GroupNicknameFragment();
        groupNicknameFragment.setArguments(args);
        return groupNicknameFragment;
    }

    /**
     * 返回 GroupNameFragment.
     *
     * @param args 创建 GroupNameFragment 提供的参数
     * @return {@link GroupNameFragment} since 1.0
     */
    @NonNull
    public GroupNameFragment newGroupNameFragment(@NonNull Bundle args) {
        GroupNameFragment groupNameFragment = new GroupNameFragment();
        groupNameFragment.setArguments(args);
        return groupNameFragment;
    }

    /**
     * 返回 GroupNoticeFragment.
     *
     * @param args 创建 GroupNoticeFragment 提供的参数
     * @return {@link GroupNoticeFragment} since 1.0
     */
    @NonNull
    public GroupNoticeFragment newGroupNoticeFragment(@NonNull Bundle args) {
        GroupNoticeFragment groupNoticeFragment = new GroupNoticeFragment();
        groupNoticeFragment.setArguments(args);
        return groupNoticeFragment;
    }

    /**
     * 返回 GroupMemberListFragment.
     *
     * @param args 创建 GroupMemberListFragment 提供的参数
     * @return {@link GroupMemberListFragment} since 1.0
     */
    @NonNull
    public GroupMemberListFragment newGroupMemberListFragment(@NonNull Bundle args) {
        GroupMemberListFragment groupMemberListFragment = new GroupMemberListFragment();
        groupMemberListFragment.setArguments(args);
        return groupMemberListFragment;
    }
}
