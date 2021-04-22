package io.rong.imkit.subconversationlist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import io.rong.imkit.conversationlist.model.BaseUiConversation;
import io.rong.imkit.conversationlist.ConversationListFragment;
import io.rong.imkit.event.Event;
import io.rong.imkit.model.NoticeContent;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.widget.refresh.api.RefreshLayout;
import io.rong.imkit.widget.refresh.constant.RefreshState;
import io.rong.imlib.model.Conversation;

public class SubConversationListFragment extends ConversationListFragment {
    private final String TAG = SubConversationListFragment.class.getSimpleName();
    private SubConversationListViewModel mSubConversationListViewModel;
    private Conversation.ConversationType mConversationType;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getActivity() != null && getActivity().getIntent() != null) {
            mConversationType = (Conversation.ConversationType) getActivity().getIntent().getSerializableExtra(RouteUtils.CONVERSATION_TYPE);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected void subscribeUi() {
        if (this.getActivity() != null) {
            SubConversationListVMFactory factory = new SubConversationListVMFactory(this.getActivity().getApplication(), mConversationType);
            mSubConversationListViewModel = new ViewModelProvider(this, factory).get(SubConversationListViewModel.class);
            mSubConversationListViewModel.getConversationList(false);
            mSubConversationListViewModel.getConversationListLiveData().observe(getViewLifecycleOwner(), new Observer<List<BaseUiConversation>>() {
                @Override
                public void onChanged(List<BaseUiConversation> baseUiConversations) {
                    mAdapter.setDataCollection(baseUiConversations);
                }
            });
        }

        mSubConversationListViewModel.getNoticeContentLiveData().observe(getViewLifecycleOwner(), new Observer<NoticeContent>() {
            @Override
            public void onChanged(final NoticeContent noticeContent) {
                // 当连接通知没有显示时，延迟进行显示，防止连接闪断造成画面闪跳。
                if (mNoticeContainerView.getVisibility() == View.GONE) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // 刷新时使用最新的通知内容
                            updateNoticeContent(noticeContent);
                        }
                    }, NOTICE_SHOW_DELAY_MILLIS);
                } else {
                    updateNoticeContent(noticeContent);
                }
            }
        });

        //刷新事件监听
        mSubConversationListViewModel.getRefreshEventLiveData().observe(getViewLifecycleOwner(), new Observer<Event.RefreshEvent>() {
            @Override
            public void onChanged(Event.RefreshEvent refreshEvent) {
                if (refreshEvent.state.equals(RefreshState.LoadFinish)) {
                    mRefreshLayout.finishLoadMore();
                } else if (refreshEvent.state.equals(RefreshState.RefreshFinish)) {
                    mRefreshLayout.finishRefresh();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mSubConversationListViewModel != null) {
            mSubConversationListViewModel.clearAllNotification();
        }
    }

    @Override
    protected void onConversationListRefresh(RefreshLayout refreshLayout) {
        if (mSubConversationListViewModel != null) {
            mSubConversationListViewModel.getConversationList(false);
        }
    }

    @Override
    protected void onConversationListLoadMore() {
        if (mSubConversationListViewModel != null) {
            mSubConversationListViewModel.getConversationList(true);
        }
    }
}
