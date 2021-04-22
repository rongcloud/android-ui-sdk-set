package io.rong.imkit.conversationlist;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.config.ConversationListBehaviorListener;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversationlist.model.BaseUiConversation;
import io.rong.imkit.conversationlist.model.GatheredConversation;
import io.rong.imkit.conversationlist.viewmodel.ConversationListViewModel;
import io.rong.imkit.event.Event;
import io.rong.imkit.model.NoticeContent;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.widget.adapter.BaseAdapter;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imkit.widget.dialog.OptionsPopupDialog;
import io.rong.imkit.widget.refresh.SmartRefreshLayout;
import io.rong.imkit.widget.refresh.api.RefreshLayout;
import io.rong.imkit.widget.refresh.constant.RefreshState;
import io.rong.imkit.widget.refresh.listener.OnLoadMoreListener;
import io.rong.imkit.widget.refresh.listener.OnRefreshListener;
import io.rong.imkit.widget.refresh.wrapper.RongRefreshHeader;
import io.rong.imlib.RongIMClient;

public class ConversationListFragment extends Fragment implements BaseAdapter.OnItemClickListener {
    private final String TAG = ConversationListFragment.class.getSimpleName();
    /*
     * 连接通知状态延迟显示时间。
     * 为了防止连接闪断，不会在断开连接时立即显示连接通知状态，而是在延迟一定时间后显示。
     */
    protected final long NOTICE_SHOW_DELAY_MILLIS = 4000L;
    protected ConversationListAdapter mAdapter;
    private RecyclerView mList;
    protected View mNoticeContainerView;
    private TextView mNoticeContentTv;
    private ImageView mNoticeIconIv;
    private ConversationListViewModel mConversationListViewModel;
    protected SmartRefreshLayout mRefreshLayout;
    protected Handler mHandler = new Handler(Looper.getMainLooper());

    {
        mAdapter = onResolveAdapter();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.rc_conversationlist_fragment, null, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mList = view.findViewById(R.id.rc_conversation_list);
        mRefreshLayout = view.findViewById(R.id.rc_refresh);

        mAdapter.setItemClickListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mList.setLayoutManager(layoutManager);
        mList.setAdapter(mAdapter);

        mNoticeContainerView = view.findViewById(R.id.rc_conversationlist_notice_container);
        mNoticeContentTv = view.findViewById(R.id.rc_conversationlist_notice_tv);
        mNoticeIconIv = view.findViewById(R.id.rc_conversationlist_notice_icon_iv);

        initRefreshView();
        subscribeUi();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mConversationListViewModel != null) {
            mConversationListViewModel.clearAllNotification();
        }
    }

    /**
     * 初始化刷新模块
     */
    protected void initRefreshView() {
        mRefreshLayout.setNestedScrollingEnabled(false);
        mRefreshLayout.setRefreshHeader(new RongRefreshHeader(getContext()));
        mRefreshLayout.setRefreshFooter(new RongRefreshHeader(getContext()));
        mRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                onConversationListRefresh(refreshLayout);
            }
        });
        mRefreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                onConversationListLoadMore();
            }
        });
    }

    protected void onConversationListLoadMore() {
        if (mConversationListViewModel != null) {
            mConversationListViewModel.getConversationList(true);
        }
    }

    protected void onConversationListRefresh(RefreshLayout refreshLayout) {
        if (mConversationListViewModel != null) {
            mConversationListViewModel.getConversationList(false);
        }
    }

    /**
     * 获取 adapter. 可复写此方法实现自定义 adapter.
     *
     * @return 会话列表 adapter
     */
    protected ConversationListAdapter onResolveAdapter() {
        mAdapter = new ConversationListAdapter();
        mAdapter.setEmptyView(R.layout.rc_conversationlist_empty_view);
        return mAdapter;
    }

    /**
     * 观察 view model 各数据以便进行页面刷新操作。
     */
    protected void subscribeUi() {
        //会话列表数据监听
        mConversationListViewModel = new ViewModelProvider(this)
                .get(ConversationListViewModel.class);
        mConversationListViewModel.getConversationList(false);
        mConversationListViewModel.getConversationListLiveData().observe(getViewLifecycleOwner(), new Observer<List<BaseUiConversation>>() {
            @Override
            public void onChanged(List<BaseUiConversation> uiConversations) {
                RLog.d(TAG, "conversation list onChanged.");
                mAdapter.setDataCollection(uiConversations);
            }
        });
        //连接状态监听
        mConversationListViewModel.getNoticeContentLiveData().observe(getViewLifecycleOwner(), new Observer<NoticeContent>() {
            @Override
            public void onChanged(NoticeContent noticeContent) {
                // 当连接通知没有显示时，延迟进行显示，防止连接闪断造成画面闪跳。
                if (mNoticeContainerView.getVisibility() == View.GONE) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // 刷新时使用最新的通知内容
                            updateNoticeContent(mConversationListViewModel.getNoticeContentLiveData().getValue());
                        }
                    }, NOTICE_SHOW_DELAY_MILLIS);
                } else {
                    updateNoticeContent(noticeContent);
                }
            }
        });
        //刷新事件监听
        mConversationListViewModel.getRefreshEventLiveData().observe(getViewLifecycleOwner(), new Observer<Event.RefreshEvent>() {
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

    /**
     * 会话列表点击事件回调
     *
     * @param view     点击 view
     * @param holder   {@link ViewHolder}
     * @param position 点击位置
     */
    @Override
    public void onItemClick(View view, ViewHolder holder, int position) {
        if (position < 0) {
            return;
        }
        BaseUiConversation baseUiConversation = mAdapter.getItem(position);
        ConversationListBehaviorListener listBehaviorListener = RongConfigCenter.conversationListConfig().getListener();
        if (listBehaviorListener != null && listBehaviorListener.onConversationClick(view.getContext(), view, baseUiConversation)) {
            RLog.d(TAG, "ConversationList item click event has been intercepted by App.");
            return;
        }
        if (baseUiConversation != null && baseUiConversation.mCore != null) {
            if (baseUiConversation instanceof GatheredConversation) {
                RouteUtils.routeToSubConversationListActivity(view.getContext(), ((GatheredConversation) baseUiConversation).mGatheredType, baseUiConversation.mCore.getConversationTitle());
            } else {
                RouteUtils.routeToConversationActivity(view.getContext(), baseUiConversation.mCore.getConversationType(), baseUiConversation.mCore.getTargetId());
            }
        } else {
            RLog.e(TAG, "invalid conversation.");
        }
    }

    /**
     * 会话列表长按事件回调
     *
     * @param view     点击 view
     * @param holder   {@link ViewHolder}
     * @param position 点击位置
     * @return 事件是否被消费
     */
    @Override
    public boolean onItemLongClick(final View view, ViewHolder holder, int position) {
        if (position < 0) {
            return false;
        }
        final BaseUiConversation baseUiConversation = mAdapter.getItem(position);
        ConversationListBehaviorListener listBehaviorListener = RongConfigCenter.conversationListConfig().getListener();
        if (listBehaviorListener != null && listBehaviorListener.onConversationLongClick(view.getContext(), view, baseUiConversation)) {
            RLog.d(TAG, "ConversationList item click event has been intercepted by App.");
            return true;
        }
        final ArrayList<String> items = new ArrayList<>();
        final String removeItem = view.getContext().getResources().getString(R.string.rc_conversation_list_dialog_remove);
        final String setTopItem = view.getContext().getResources().getString(R.string.rc_conversation_list_dialog_set_top);
        final String cancelTopItem = view.getContext().getResources().getString(R.string.rc_conversation_list_dialog_cancel_top);

        if (!(baseUiConversation instanceof GatheredConversation)) {
            if (baseUiConversation.mCore.isTop()) {
                items.add(cancelTopItem);
            } else {
                items.add(setTopItem);
            }
        }
        items.add(removeItem);
        int size = items.size();
        OptionsPopupDialog.newInstance(view.getContext(), items.toArray(new String[size]))
                .setOptionsPopupDialogListener(new OptionsPopupDialog.OnOptionsItemClickedListener() {
                    @Override
                    public void onOptionsItemClicked(final int which) {
                        if (items.get(which).equals(setTopItem) || items.get(which).equals(cancelTopItem)) {
                            IMCenter.getInstance().setConversationToTop(baseUiConversation.mCore.getConversationType(), baseUiConversation.mCore.getTargetId(),
                                    !baseUiConversation.mCore.isTop(), false, new RongIMClient.ResultCallback<Boolean>() {
                                        @Override
                                        public void onSuccess(Boolean value) {
                                            Toast.makeText(view.getContext(), items.get(which), Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onError(RongIMClient.ErrorCode errorCode) {

                                        }
                                    });
                        } else if (items.get(which).equals(removeItem)) {
                            IMCenter.getInstance().removeConversation(baseUiConversation.mCore.getConversationType(), baseUiConversation.mCore.getTargetId(), null);
                        }
                    }
                }).show();
        return true;
    }

    /**
     * 更新连接状态通知栏
     *
     * @param content
     */
    protected void updateNoticeContent(NoticeContent content) {
        if (content == null) return;

        if (content.isShowNotice()) {
            mNoticeContainerView.setVisibility(View.VISIBLE);
            mNoticeContentTv.setText(content.getContent());
            if (content.getIconResId() != 0) {
                mNoticeIconIv.setImageResource(content.getIconResId());
            }
        } else {
            mNoticeContainerView.setVisibility(View.GONE);
        }
    }

    /**
     * @param view 自定义列表 header view
     */
    public void addHeaderView(View view) {
        mAdapter.addHeaderView(view);
    }

    /**
     * @param view 自定义列表 footer view
     */
    public void addFooterView(View view) {
        mAdapter.addFootView(view);
    }

    /**
     * @param view 自定义列表 空数据 view
     */
    public void setEmptyView(View view) {
        mAdapter.setEmptyView(view);
    }

    /**
     * @param emptyId 自定义列表 空数据的 LayoutId
     */
    public void setEmptyView(@LayoutRes int emptyId) {
        mAdapter.setEmptyView(emptyId);
    }
}
