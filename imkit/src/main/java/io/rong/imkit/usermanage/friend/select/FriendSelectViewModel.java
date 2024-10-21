package io.rong.imkit.usermanage.friend.select;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.usermanage.handler.FriendInfoHandler;
import io.rong.imkit.utils.StringUtils;
import io.rong.imlib.model.FriendInfo;
import io.rong.imlib.model.QueryFriendsDirectionType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 功能描述: 创建群组ViewModel
 *
 * @author rongcloud
 * @since 5.10.4
 */
public class FriendSelectViewModel extends BaseViewModel {

    private final MutableLiveData<List<ContactModel>> allContactsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ContactModel>> filteredContactsLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<List<ContactModel>> selectedContactsLiveData =
            new MutableLiveData<>();
    private final FriendInfoHandler friendInfoHandler;

    public FriendSelectViewModel(@NonNull Bundle arguments) {
        super(arguments);
        friendInfoHandler = new FriendInfoHandler();
        friendInfoHandler.addDataChangeListener(
                FriendInfoHandler.KEY_GET_FRIENDS,
                new SafeDataHandler<List<FriendInfo>>() {
                    @Override
                    public void onDataChange(List<FriendInfo> friendInfos) {
                        List<ContactModel> contactModels = sortAndCategorizeContacts(friendInfos);
                        allContactsLiveData.postValue(contactModels);
                        filteredContactsLiveData.postValue(contactModels); // 初始状态下，展示所有联系人
                    }
                });
        friendInfoHandler.addDataChangeListener(
                FriendInfoHandler.KEY_SEARCH_FRIENDS,
                friendInfos ->
                        filteredContactsLiveData.postValue(sortAndCategorizeContacts(friendInfos)));
        friendInfoHandler.getFriends(QueryFriendsDirectionType.Both);
    }

    public LiveData<List<ContactModel>> getFilteredContactsLiveData() {
        return filteredContactsLiveData;
    }

    public LiveData<List<ContactModel>> getSelectedContactsLiveData() {
        return selectedContactsLiveData;
    }

    public LiveData<List<ContactModel>> getAllContactsLiveData() {
        return allContactsLiveData;
    }

    /**
     * 更新联系人状态
     *
     * @param updatedContact 更新后的联系人
     */
    public void updateContact(ContactModel updatedContact) {
        List<ContactModel> allContacts = filteredContactsLiveData.getValue();
        if (allContacts == null) {
            return;
        }

        boolean allContactsUpdated = false;

        // 更新选中联系人列表
        List<ContactModel> selectedContactsValue = selectedContactsLiveData.getValue();
        if (selectedContactsValue == null) {
            selectedContactsValue = new ArrayList<>();
        }

        // 更新所有联系人列表中的相应联系人状态
        for (int i = 0; i < allContacts.size(); i++) {
            ContactModel contact = allContacts.get(i);
            if (contact.getBean() instanceof FriendInfo
                    && updatedContact.getBean() instanceof FriendInfo) {
                FriendInfo friendInfo = (FriendInfo) contact.getBean();
                FriendInfo updatedFriendInfo = (FriendInfo) updatedContact.getBean();
                if (friendInfo != null
                        && friendInfo.getUserId().equals(updatedFriendInfo.getUserId())) {
                    allContacts.get(i).setCheckType(updatedContact.getCheckType());
                    allContactsUpdated = true;

                    if (updatedContact.getCheckType() == ContactModel.CheckType.CHECKED) {
                        selectedContactsValue.add(updatedContact);
                    } else {
                        // 使用迭代器进行移除操作，确保兼容性
                        Iterator<ContactModel> iterator = selectedContactsValue.iterator();
                        while (iterator.hasNext()) {
                            ContactModel contactModel = iterator.next();
                            if (contactModel.getBean() instanceof FriendInfo) {
                                FriendInfo selectedFriendInfo = (FriendInfo) contactModel.getBean();
                                if (selectedFriendInfo
                                        .getUserId()
                                        .equals(selectedFriendInfo.getUserId())) {
                                    iterator.remove();
                                    break;
                                }
                            }
                        }
                    }

                    break;
                }
            }
        }

        if (allContactsUpdated) {
            selectedContactsLiveData.postValue(selectedContactsValue);
            filteredContactsLiveData.postValue(allContacts);
        }
    }

    /**
     * 查询联系人
     *
     * @param query 查询关键字
     */
    public void queryContacts(String query) {
        if (TextUtils.isEmpty(query)) {
            filteredContactsLiveData.postValue(allContactsLiveData.getValue());
            return;
        }
        if (allContactsLiveData.getValue() == null || query == null) {
            return;
        }
        friendInfoHandler.searchFriendsInfo(query);
    }

    private List<ContactModel> sortAndCategorizeContacts(List<FriendInfo> friendInfos) {
        // 排序处理，中文按首字母，英文按整个名字排序
        Collections.sort(
                friendInfos,
                (friend1, friend2) -> {
                    String name1 = getValidName(friend1);
                    String name2 = getValidName(friend2);
                    char firstChar1 = StringUtils.getFirstChar(name1.charAt(0));
                    char firstChar2 = StringUtils.getFirstChar(name2.charAt(0));
                    // 如果 firstChar1 是 #，把它放到最后
                    if (firstChar1 == '#') {
                        return 1;
                    }
                    // 如果 firstChar2 是 #，把它放到最后
                    if (firstChar2 == '#') {
                        return -1;
                    }
                    if (Character.isLetter(firstChar1) && Character.isLetter(firstChar2)) {
                        return firstChar1 - firstChar2;
                    } else {
                        return name1.compareToIgnoreCase(name2);
                    }
                });

        List<ContactModel> contactModels = new ArrayList<>();
        char lastCategory = '\0';

        for (FriendInfo friendInfo : friendInfos) {
            String name = getValidName(friendInfo);
            char firstChar = StringUtils.getFirstChar(name.charAt(0));

            // 如果首字母不同，添加一个标题 ContactModel
            if (firstChar != lastCategory) {
                contactModels.add(
                        ContactModel.obtain(
                                String.valueOf(firstChar),
                                ContactModel.ItemType.TITLE,
                                ContactModel.CheckType.NONE));
                lastCategory = firstChar;
            }

            // 添加联系人 ContactModel
            contactModels.add(
                    ContactModel.obtain(
                            friendInfo,
                            ContactModel.ItemType.CONTENT,
                            ContactModel.CheckType.UNCHECKED));
        }

        return contactModels;
    }

    private String getValidName(FriendInfo friendInfo) {
        // 返回有效的名称，优先使用备注，其次是姓名，最后使用 "#" 作为占位符
        String name =
                !TextUtils.isEmpty(friendInfo.getRemark())
                        ? friendInfo.getRemark()
                        : friendInfo.getName();
        return !TextUtils.isEmpty(name) ? name : "#";
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        friendInfoHandler.stop();
    }
}
