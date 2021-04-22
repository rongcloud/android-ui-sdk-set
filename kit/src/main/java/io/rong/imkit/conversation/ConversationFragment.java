package io.rong.imkit.conversation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import io.rong.common.RLog;
import io.rong.imkit.MessageItemLongClickAction;
import io.rong.imkit.MessageItemLongClickActionManager;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.InputMode;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.RongExtensionViewModel;
import io.rong.imkit.conversation.messgelist.processor.IConversationUIRenderer;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageItemLongClickBean;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.event.Event;
import io.rong.imkit.event.uievent.PageDestroyEvent;
import io.rong.imkit.event.uievent.PageEvent;
import io.rong.imkit.event.uievent.ScrollEvent;
import io.rong.imkit.event.uievent.ScrollMentionEvent;
import io.rong.imkit.event.uievent.ScrollToEndEvent;
import io.rong.imkit.event.uievent.ShowLongClickDialogEvent;
import io.rong.imkit.event.uievent.ShowWarningDialogEvent;
import io.rong.imkit.event.uievent.SmoothScrollEvent;
import io.rong.imkit.event.uievent.ToastEvent;
import io.rong.imkit.manager.hqvoicemessage.HQVoiceMsgDownloadManager;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.widget.adapter.BaseAdapter;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imkit.widget.dialog.OptionsPopupDialog;
import io.rong.imkit.widget.refresh.SmartRefreshLayout;
import io.rong.imkit.widget.refresh.api.RefreshLayout;
import io.rong.imkit.widget.refresh.constant.RefreshState;
import io.rong.imkit.widget.refresh.listener.OnLoadMoreListener;
import io.rong.imkit.widget.refresh.listener.OnRefreshListener;
import io.rong.imkit.widget.refresh.wrapper.RongRefreshHeader;
import io.rong.imlib.model.Conversation;

public class ConversationFragment extends Fragment implements OnRefreshListener, View.OnClickListener, OnLoadMoreListener, IViewProviderListener<UiMessage> {
    private final String TAG = ConversationFragment.class.getSimpleName();
    private static final int REQUEST_MSG_DOWNLOAD_PERMISSION = 1000;
    protected SmartRefreshLayout mRefreshLayout;

    protected RecyclerView mList;
    protected LinearLayoutManager mLinearLayoutManager;
    protected MessageListAdapter mAdapter;
    protected MessageViewModel mMessageViewModel;
    protected RongExtensionViewModel mRongExtensionViewModel;
    protected RongExtension mRongExtension;
    protected TextView mNewMessageNum;
    protected TextView mUnreadHistoryMessageNum;
    protected TextView mUnreadMentionMessageNum;
    // 开启合并转发的选择会话界面
    public static final int REQUEST_CODE_FORWARD = 104;
    private LinearLayout mNotificationContainer;
    private boolean onViewCreated = false;
    private String mTargetId;
    private Bundle mBundle;
    private Conversation.ConversationType mConversationType;

    {
        mAdapter = onResolveAdapter();
    }

    /**
     * findId，绑定监听
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.rc_conversation_fragment, container, false);
        mList = rootView.findViewById(R.id.rc_message_list);
        mRongExtension = rootView.findViewById(R.id.rc_extension);
        mRefreshLayout = rootView.findViewById(R.id.rc_refresh);
        mNewMessageNum = rootView.findViewById(R.id.rc_new_message_number);
        mUnreadHistoryMessageNum = rootView.findViewById(R.id.rc_unread_message_count);
        mUnreadMentionMessageNum = rootView.findViewById(R.id.rc_mention_message_count);
        mNotificationContainer = rootView.findViewById(R.id.rc_notification_container);
        mNewMessageNum.setOnClickListener(this);
        mUnreadHistoryMessageNum.setOnClickListener(this);
        mUnreadMentionMessageNum.setOnClickListener(this);
        mLinearLayoutManager = new LinearLayoutManager(getContext());
        mLinearLayoutManager.setStackFromEnd(true);
        mList.setLayoutManager(mLinearLayoutManager);
        mRefreshLayout.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                closeExpand();
                return false;
            }
        });
        mAdapter = new MessageListAdapter(this);
        mAdapter.setItemClickListener(new BaseAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, ViewHolder holder, int position) {
                closeExpand();
            }

            @Override
            public boolean onItemLongClick(View view, ViewHolder holder, int position) {
                return false;
            }
        });
        //关闭动画
        if (mList != null) {
            mList.setAdapter(mAdapter);
            mList.addOnScrollListener(mScrollListener);
            mList.setItemAnimator(null);
            final GestureDetector gd = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    closeExpand();
                    return super.onScroll(e1, e2, distanceX, distanceY);

                }
            });
            mList.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
                @Override
                public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                    return gd.onTouchEvent(e);
                }

                @Override
                public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {

                }

                @Override
                public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

                }
            });
        }

        mRefreshLayout.setNestedScrollingEnabled(false);
        mRefreshLayout.setRefreshHeader(new RongRefreshHeader(getContext()));
        mRefreshLayout.setRefreshFooter(new RongRefreshHeader(getContext()));
        mRefreshLayout.setEnableRefresh(true);
        mRefreshLayout.setOnRefreshListener(this);
        mRefreshLayout.setOnLoadMoreListener(this);
        return rootView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getActivity() == null || getActivity().getIntent() == null) {
            RLog.e(TAG, "Must put targetId and conversation type to intent when start conversation.");
            return;
        }
        super.onViewCreated(view, savedInstanceState);
        Intent intent = getActivity().getIntent();
        if (mTargetId == null)
            mTargetId = intent.getStringExtra(RouteUtils.TARGET_ID);
        if (mConversationType == null) {
            String type = intent.getStringExtra(RouteUtils.CONVERSATION_TYPE);
            if (type != null) {
                mConversationType = Conversation.ConversationType.valueOf(type.toUpperCase(Locale.US));
            }
        }
        if (mBundle == null) {
            mBundle = intent.getExtras();
        }
        if (Conversation.ConversationType.SYSTEM.equals(mConversationType)) {
            mRongExtension.setVisibility(View.GONE);
        } else {
            mRongExtension.setVisibility(View.VISIBLE);
        }
        mMessageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        mRongExtensionViewModel = new ViewModelProvider(this).get(RongExtensionViewModel.class);
        bindConversation(mTargetId, mConversationType, mBundle);
        if (!PermissionCheckUtil.checkPermissions(getContext(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})) {
            PermissionCheckUtil.requestPermissions(ConversationFragment.this.getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_MSG_DOWNLOAD_PERMISSION);
        }
        onViewCreated = true;
    }

    public void initConversation(String targetId, Conversation.ConversationType conversationType, Bundle bundle) {
        if (onViewCreated) {
            bindConversation(targetId, conversationType, bundle);
        } else {
            mTargetId = targetId;
            mConversationType = conversationType;
            mBundle = bundle;
        }
    }

    private void bindConversation(String targetId, Conversation.ConversationType conversationType, Bundle bundle) {
        if (conversationType != null && !TextUtils.isEmpty(targetId)) {
            for (IConversationUIRenderer processor : RongConfigCenter.conversationConfig().getViewProcessors()) {
                processor.init(this, mRongExtension, conversationType, targetId);
            }
            mRongExtension.bindToConversation(this, conversationType, targetId);
            mMessageViewModel.bindConversation(conversationType, targetId, bundle);
            subscribeUi();
        } else {
            RLog.e(TAG, "Invalid intent data !!! Must put targetId and conversation type to intent.");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() == null) {
            return;
        }
        mMessageViewModel.onResume();
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    return onBackPressed();
                }
                return false;
            }
        });
        mRongExtension.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMessageViewModel.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMessageViewModel.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (IConversationUIRenderer processor : RongConfigCenter.conversationConfig().getViewProcessors()) {
            processor.onDestroy();
        }
        mList.removeOnScrollListener(mScrollListener);
        mMessageViewModel.getPageEventLiveData().removeObserver(mPageObserver);
        mMessageViewModel.getUiMessageLiveData().removeObserver(mListObserver);
        mMessageViewModel.getNewMentionMessageUnreadLiveData().removeObserver(mNewMentionMessageUnreadObserver);
        if (mMessageViewModel != null) {
            mMessageViewModel.onDestroy();
        }
        if (mRongExtension != null) {
            mRongExtension.onDestroy();
            mRongExtension = null;
        }
    }


    private void subscribeUi() {
        mMessageViewModel.getPageEventLiveData().observeForever(mPageObserver);
        mMessageViewModel.getUiMessageLiveData().observeForever(mListObserver);
        mMessageViewModel.getNewMessageUnreadLiveData().observe(getViewLifecycleOwner(), mNewMessageUnreadObserver);
        mMessageViewModel.getHistoryMessageUnreadLiveData().observe(getViewLifecycleOwner(), mHistoryMessageUnreadObserver);
        mMessageViewModel.getNewMentionMessageUnreadLiveData().observe(getViewLifecycleOwner(), mNewMentionMessageUnreadObserver);
        mRongExtensionViewModel.getExtensionBoardState().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(final Boolean value) {
                RLog.d(TAG, "scroll to the bottom");
                mList.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        InputMode inputMode = mRongExtensionViewModel.getInputModeLiveData().getValue();
                        if (!inputMode.equals(InputMode.MoreInputMode) && true == value) {
                            mList.scrollToPosition(mAdapter.getItemCount() - 1);
                        }
                    }
                }, 150);
            }
        });
    }

    @Override
    public void onViewClick(int clickType, UiMessage data) {
        mMessageViewModel.onViewClick(clickType, data);
    }

    @Override
    public boolean onViewLongClick(int clickType, UiMessage data) {
        return mMessageViewModel.onViewLongClick(clickType, data);
    }

    /**
     * 获取顶部通知栏容器
     *
     * @return 通知栏容器
     */
    public LinearLayout getNotificationContainer() {
        return mNotificationContainer;
    }

    /**
     * 隐藏调用showNotificationView所显示的通知view
     *
     * @param notificationView 通知栏 view
     */
    public void hideNotificationView(View notificationView) {
        if (notificationView == null) {
            return;
        }
        View view = mNotificationContainer.findViewById(notificationView.getId());
        if (view != null) {
            mNotificationContainer.removeView(view);
            if (mNotificationContainer.getChildCount() == 0) {
                mNotificationContainer.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 在通知区域显示一个view
     */
    public void showNotificationView(View notificationView) {
        if (notificationView == null) {
            return;
        }
        mNotificationContainer.removeAllViews();
        if (notificationView.getParent() != null) {
            ((ViewGroup) notificationView.getParent()).removeView(notificationView);
        }
        mNotificationContainer.addView(notificationView);
        mNotificationContainer.setVisibility(View.VISIBLE);
    }

    Observer<List<UiMessage>> mListObserver = new Observer<List<UiMessage>>() {
        @Override
        public void onChanged(List<UiMessage> uiMessages) {
            refreshList(uiMessages);
        }
    };

    Observer<Integer> mNewMessageUnreadObserver = new Observer<Integer>() {
        @Override
        public void onChanged(Integer count) {
            if (RongConfigCenter.conversationConfig().isShowNewMessageBar(mMessageViewModel.getCurConversationType())) {
                if (count != null && count > 0) {
                    mNewMessageNum.setVisibility(View.VISIBLE);
                    mNewMessageNum.setText(count > 99 ? "99+" : String.valueOf(count));
                } else {
                    mNewMessageNum.setVisibility(View.INVISIBLE);
                }
            }
        }
    };

    Observer<Integer> mHistoryMessageUnreadObserver = new Observer<Integer>() {
        @Override
        public void onChanged(Integer count) {
            if (RongConfigCenter.conversationConfig().isShowHistoryMessageBar(mMessageViewModel.getCurConversationType())) {
                if (count != null && count > 0) {
                    mUnreadHistoryMessageNum.setVisibility(View.VISIBLE);
                    mUnreadHistoryMessageNum.setText(MessageFormat.format(getString(R.string.rc_unread_message), count > 99 ? "99+" : count));
                } else {
                    mUnreadHistoryMessageNum.setVisibility(View.GONE);
                }
            }
        }
    };

    Observer<Integer> mNewMentionMessageUnreadObserver = new Observer<Integer>() {
        @Override
        public void onChanged(Integer count) {
            if (RongConfigCenter.conversationConfig().isShowNewMentionMessageBar(mMessageViewModel.getCurConversationType())) {
                if (count != null && count > 0) {
                    mUnreadMentionMessageNum.setVisibility(View.VISIBLE);
                    mUnreadMentionMessageNum.setText(getContext().getResources().getString(R.string.rc_mention_messages, count));
                } else {
                    mUnreadMentionMessageNum.setVisibility(View.GONE);
                }
            }
        }
    };

    Observer<PageEvent> mPageObserver = new Observer<PageEvent>() {
        @Override
        public void onChanged(PageEvent event) {
            //优先透传给各模块的 view 处理中心进行处理，如果返回 true, 代表事件被消费，不再处理。
            for (IConversationUIRenderer processor : RongConfigCenter.conversationConfig().getViewProcessors()) {
                if (processor.handlePageEvent(event)) {
                    return;
                }
            }
            if (event instanceof Event.RefreshEvent) {
                if (((Event.RefreshEvent) event).state.equals(RefreshState.RefreshFinish)) {
                    mRefreshLayout.finishRefresh();
                } else if (((Event.RefreshEvent) event).state.equals(RefreshState.LoadFinish)) {
                    mRefreshLayout.finishLoadMore();
                }
            } else if (event instanceof ToastEvent) {
                String msg = ((ToastEvent) event).getMessage();
                if (!TextUtils.isEmpty(msg)) {
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                }
            } else if (event instanceof ScrollToEndEvent) {
                mList.scrollToPosition(mAdapter.getItemCount() - 1);
            } else if (event instanceof ScrollMentionEvent) {
                mMessageViewModel.onScrolled(mList, 0, 0);
            } else if (event instanceof ScrollEvent) {
                mList.scrollToPosition(mAdapter.getHeadersCount() + ((ScrollEvent) event).getPosition());
            } else if (event instanceof SmoothScrollEvent) {
                mList.scrollToPosition(mAdapter.getHeadersCount() + ((SmoothScrollEvent) event).getPosition());
            } else if (event instanceof ShowLongClickDialogEvent) {
                final MessageItemLongClickBean bean = ((ShowLongClickDialogEvent) event).getBean();
                final List<MessageItemLongClickAction> messageItemLongClickActions = bean.getMessageItemLongClickActions();
                Collections.sort(
                        messageItemLongClickActions,
                        new Comparator<MessageItemLongClickAction>() {
                            @Override
                            public int compare(
                                    MessageItemLongClickAction lhs, MessageItemLongClickAction rhs) {
                                // desc sort
                                return rhs.priority - lhs.priority;
                            }
                        });
                List<String> titles = new ArrayList<>();
                for (MessageItemLongClickAction action : messageItemLongClickActions) {
                    titles.add(action.getTitle(getContext()));
                }

                OptionsPopupDialog dialog =
                        OptionsPopupDialog.newInstance(
                                getContext(), titles.toArray(new String[titles.size()]))
                                .setOptionsPopupDialogListener(
                                        new OptionsPopupDialog.OnOptionsItemClickedListener() {
                                            @Override
                                            public void onOptionsItemClicked(int which) {
                                                messageItemLongClickActions
                                                        .get(which)
                                                        .listener
                                                        .onMessageItemLongClick(
                                                                getContext(), bean.getUiMessage());
                                            }
                                        });
                MessageItemLongClickActionManager.getInstance().setLongClickDialog(dialog);
                MessageItemLongClickActionManager.getInstance()
                        .setLongClickMessage(bean.getUiMessage().getMessage());
                dialog.setOnDismissListener(
                        new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                MessageItemLongClickActionManager.getInstance()
                                        .setLongClickDialog(null);
                                MessageItemLongClickActionManager.getInstance()
                                        .setLongClickMessage(null);
                            }
                        });
                dialog.show();
            } else if (event instanceof PageDestroyEvent) {
                FragmentManager fm = getChildFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                } else {
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                }
            } else if (event instanceof ShowWarningDialogEvent) {
                onWarningDialog(((ShowWarningDialogEvent) event).getMessage());
            }
        }
    };

    private void refreshList(final List<UiMessage> data) {
        if (mList.isComputingLayout()) {
            mList.post(new Runnable() {
                @Override
                public void run() {
                    refreshList(data);
                }
            });
        } else {
            mAdapter.setDataCollection(data);
        }

    }

    public boolean onBackPressed() {
        boolean result = false;
        for (IConversationUIRenderer processor : RongConfigCenter.conversationConfig().getViewProcessors()) {
            boolean temp = processor.onBackPressed();
            if (temp) {
                result = true;
            }
        }
        if (mMessageViewModel != null) {
            boolean temp = mMessageViewModel.onBackPressed();
            if (temp) {
                result = true;
            }
        }
        mRongExtensionViewModel.exitMoreInputMode(this.getContext());
        mRongExtensionViewModel.collapseExtensionBoard();
        return result;
    }


    @Override
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {
        if (mMessageViewModel != null) {
            mMessageViewModel.onRefresh();
        }
    }

    public RongExtension getRongExtension() {
        return mRongExtension;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_MSG_DOWNLOAD_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                HQVoiceMsgDownloadManager.getInstance().resumeDownloadService();
            } else {
                PermissionCheckUtil.showRequestPermissionFailedAlter(this.getContext(), permissions, grantResults);
            }
            return;
        }
        if (requestCode == PermissionCheckUtil.REQUEST_CODE_ASK_PERMISSIONS && grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            PermissionCheckUtil.showRequestPermissionFailedAlter(this.getContext(), permissions, grantResults);
        } else {
            mRongExtension.onRequestPermissionResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FORWARD) {
            mMessageViewModel.forwardMessage(data);
            return;
        }
        mRongExtension.onActivityPluginResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.rc_new_message_number) {
            mMessageViewModel.newMessageBarClick();
        } else if (id == R.id.rc_unread_message_count) {
            mMessageViewModel.unreadBarClick();
        } else if (id == R.id.rc_mention_message_count) {
            mMessageViewModel.newMentionMessageBarClick();
        }
    }

    @Override
    public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
        if (mMessageViewModel != null) {
            mMessageViewModel.onLoadMore();
        }
    }

    private RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            mMessageViewModel.onScrolled(recyclerView, dx, dy);
        }

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }
    };

    /**
     * 提示dialog.
     * 例如"加入聊天室失败"的dialog
     * 用户自定义此dialog的步骤:
     * 1.定义一个类继承自 ConversationFragment
     * 2.重写 onWarningDialog
     *
     * @param msg dialog 提示
     */
    public void onWarningDialog(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        Window window = alertDialog.getWindow();
        if (window == null) {
            return;
        }
        window.setContentView(R.layout.rc_cs_alert_warning);
        TextView tv = window.findViewById(R.id.rc_cs_msg);
        tv.setText(msg);

        window.findViewById(R.id.rc_btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                FragmentManager fm = getChildFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                } else {
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                }
            }
        });
    }

    private void closeExpand() {
        if (mRongExtensionViewModel != null) {
            mRongExtensionViewModel.collapseExtensionBoard();
        }
    }

    /**
     * 获取 adapter. 可复写此方法实现自定义 adapter.
     *
     * @return 会话列表 adapter
     */
    protected MessageListAdapter onResolveAdapter() {
        MessageListAdapter adapter = new MessageListAdapter(this);
        return adapter;
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