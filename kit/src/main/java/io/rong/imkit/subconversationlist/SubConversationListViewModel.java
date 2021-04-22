package io.rong.imkit.subconversationlist;

import android.app.Application;

import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.rong.common.RLog;
import io.rong.imkit.conversationlist.model.BaseUiConversation;
import io.rong.imkit.conversationlist.model.GroupConversation;
import io.rong.imkit.conversationlist.model.PublicServiceConversation;
import io.rong.imkit.conversationlist.model.SingleConversation;
import io.rong.imkit.conversationlist.viewmodel.ConversationListViewModel;
import io.rong.imkit.event.Event;
import io.rong.imkit.widget.refresh.constant.RefreshState;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;

class SubConversationListViewModel extends ConversationListViewModel {
    private final String TAG = SubConversationListViewModel.class.getSimpleName();

    SubConversationListViewModel(Application application, Conversation.ConversationType conversationType) {
        super(application);
        mSupportedTypes = new Conversation.ConversationType[]{conversationType};
    }

    /**
     * 从本地数据库获取会话列表。
     *
     * @param loadMore 是否根据上次同步的时间戳拉取更多会话。
     *                 false: 从数据库拉取最新 N 条会话。true: 根据 UI 上最后一条会话的时间戳，继续拉取之前的 N 条会话。
     */
    @Override
    public void getConversationList(final boolean loadMore) {

        long timestamp = 0;
        if (loadMore) {
            timestamp = mLastSyncTime;
        }
        RongIMClient.getInstance().getConversationListByPage(new RongIMClient.ResultCallback<List<Conversation>>() {
            @Override
            public void onSuccess(List<Conversation> conversations) {
                RLog.d(TAG, "getConversationListByPage.");
                if (loadMore) {
                    ((MutableLiveData<Event.RefreshEvent>) getRefreshEventLiveData()).postValue(new Event.RefreshEvent(RefreshState.LoadFinish));
                } else {
                    ((MutableLiveData<Event.RefreshEvent>) getRefreshEventLiveData()).postValue(new Event.RefreshEvent(RefreshState.RefreshFinish));
                }
                if (conversations == null || conversations.size() == 0) {
                    return;
                }
                RLog.d(TAG, "getConversationListByPage. size:" + conversations.size());
                mLastSyncTime = conversations.get(conversations.size() - 1).getSentTime();

                for (Conversation conversation : conversations) {
//                        boolean isGathered = mDataFilter.isGathered(conversation.getConversationType());
                    BaseUiConversation oldItem = findConversationFromList(conversation.getConversationType(), conversation.getTargetId(), false);
                    if (oldItem != null) {
                        oldItem.onConversationUpdate(conversation);
                    } else {
                        if (conversation.getConversationType().equals(Conversation.ConversationType.GROUP)) {
                            mUiConversationList.add(new GroupConversation(mApplication.getApplicationContext(), conversation));
                        } else if (conversation.getConversationType().equals(Conversation.ConversationType.PUBLIC_SERVICE)
                                || conversation.getConversationType().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE)) {
                            mUiConversationList.add(new PublicServiceConversation(mApplication.getApplicationContext(), conversation));
                        } else {
                            mUiConversationList.add(new SingleConversation(mApplication.getApplicationContext(), conversation));
                        }
                    }
                }
                sort();
                mConversationListLiveData.postValue(mUiConversationList);

            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                //Todo 通知上层数据获取失败，刷新重试？
            }
        }, timestamp, mSizePerPage, mSupportedTypes);
    }

    @Override
    protected void updateByConversation(Conversation conversation) {
        if (conversation == null) {
            return;
        }
        List<Conversation> list = new CopyOnWriteArrayList<>();
        list.add(conversation);
        List<Conversation> filterList = mDataFilter.filtered(list);
        if (filterList != null && filterList.size() > 0 && isSupported(conversation.getConversationType())) {
            BaseUiConversation oldItem = findConversationFromList(conversation.getConversationType(), conversation.getTargetId(), false);
            if (oldItem != null) {
                oldItem.onConversationUpdate(conversation);
            } else {
                if (conversation.getConversationType().equals(Conversation.ConversationType.GROUP)) {
                    mUiConversationList.add(new GroupConversation(mApplication.getApplicationContext(), conversation));
                } else if (conversation.getConversationType().equals(Conversation.ConversationType.PUBLIC_SERVICE)
                        || conversation.getConversationType().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE)) {
                    mUiConversationList.add(new PublicServiceConversation(mApplication.getApplicationContext(), conversation));
                } else {
                    mUiConversationList.add(new SingleConversation(mApplication.getApplicationContext(), conversation));
                }
            }
            sort();
            mConversationListLiveData.postValue(mUiConversationList);
        }
    }
}
