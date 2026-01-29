package io.rong.imkit.conversation.readreceipt;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModelFragment;
import io.rong.imkit.base.adapter.CommonAdapter;
import io.rong.imkit.base.adapter.ViewHolder;
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.model.ReadReceiptData;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.component.CommonListComponent;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.utils.RongDateUtils;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.ReadReceiptInfoV5;
import java.util.List;

/**
 * 功能描述: 群组消息阅读状态详情页面
 *
 * @author rongcloud
 * @since 5.30.0
 */
public class MessageReadDetailFragment extends BaseViewModelFragment<MessageReadDetailViewModel> {

    protected HeadComponent headComponent;
    protected TextView readTabText;
    protected TextView readUnderLine;
    protected TextView unReadTabText;
    protected TextView unReadUnderLine;
    protected CommonListComponent readList;
    protected CommonListComponent unreadList;
    protected TextView readReceiptNumberNone;
    protected int currentPosition = 0;
    private RecyclerView.OnScrollListener readScrollListener;
    private RecyclerView.OnScrollListener unreadScrollListener;

    protected CommonAdapter<ReadReceiptData> readAdapter =
            new CommonAdapter<ReadReceiptData>(R.layout.rc_read_receipt_member_item) {
                @Override
                public void bindData(ViewHolder holder, ReadReceiptData data, int position) {
                    MessageReadDetailFragment.this.onAdapterBindData(holder, data);
                }
            };

    protected CommonAdapter<ReadReceiptData> unreadAdapter =
            new CommonAdapter<ReadReceiptData>(R.layout.rc_read_receipt_member_item) {
                @Override
                public void bindData(ViewHolder holder, ReadReceiptData data, int position) {
                    MessageReadDetailFragment.this.onAdapterBindData(holder, data);
                }
            };

    @NonNull
    @Override
    protected MessageReadDetailViewModel onCreateViewModel(@NonNull Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(getArguments()))
                .get(MessageReadDetailViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_message_read_detail, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        readTabText = view.findViewById(R.id.rc_read_tab);
        unReadTabText = view.findViewById(R.id.rc_unread_tab);
        readUnderLine = view.findViewById(R.id.rc_read_tab_underline);
        unReadUnderLine = view.findViewById(R.id.rc_unread_tab_underline);
        readList = view.findViewById(R.id.rc_read_list_component);
        unreadList = view.findViewById(R.id.rc_unread_list_component);
        readReceiptNumberNone = view.findViewById(R.id.rc_read_receipt_number_none);
        readList.setAdapter(readAdapter);
        unreadList.setAdapter(unreadAdapter);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull MessageReadDetailViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        headComponent.setRightTextViewEnable(false);
        // 设置滑动监听器
        setupScrollListener(viewModel);
        readTabText.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        currentPosition = 0;
                        readList.setVisibility(View.VISIBLE);
                        unreadList.setVisibility(View.GONE);
                        updateTabText(viewModel);
                        updateMemberNoneView();
                    }
                });
        unReadTabText.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        currentPosition = 1;
                        readList.setVisibility(View.GONE);
                        unreadList.setVisibility(View.VISIBLE);
                        updateTabText(viewModel);
                        updateMemberNoneView();
                    }
                });
        viewModel
                .getReadReceiptInfoV5LiveData()
                .observe(
                        getViewLifecycleOwner(),
                        new Observer<ReadReceiptInfoV5>() {
                            @Override
                            public void onChanged(ReadReceiptInfoV5 data) {
                                updateTabText(viewModel);
                                // 加载已读未读首屏数据
                                getViewModel().getMessagesReadReceiptUsersByPage(true);
                                getViewModel().getMessagesReadReceiptUsersByPage(false);
                            }
                        });
        viewModel
                .getReadUsersLiveData()
                .observe(getViewLifecycleOwner(), data -> updateMemberList(data, true));
        viewModel
                .getUnreadUsersLiveData()
                .observe(getViewLifecycleOwner(), data -> updateMemberList(data, false));
        viewModel
                .getMemberInfoUpdateLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        info -> {
                            if (!onMemberInfoUpdate(readAdapter, info)) {
                                onMemberInfoUpdate(unreadAdapter, info);
                            }
                        });
    }

    public void onAdapterBindData(ViewHolder holder, ReadReceiptData data) {
        Context context = holder.itemView.getContext();
        // name
        String name = data.getInfo().getName();
        if (TextUtils.isEmpty(name)) {
            name = data.getInfo().getUserId();
        }
        holder.setText(R.id.rc_member_name, name);
        // time
        long time = data.getUser().getTimestamp();
        if (time > 0) {
            holder.setVisible(R.id.rc_member_time, true);
            String timeTxt = RongDateUtils.getConversationFormatDate(time, context);
            holder.setText(R.id.rc_member_time, timeTxt);
        } else {
            holder.setVisible(R.id.rc_member_time, false);
        }
        // portrait
        RongConfigCenter.featureConfig()
                .getKitImageEngine()
                .loadUserPortrait(
                        context,
                        data.getInfo().getPortraitUri(),
                        holder.<ImageView>getView(R.id.rc_member_portrait));
    }

    private boolean onMemberInfoUpdate(
            CommonAdapter<ReadReceiptData> adapter, GroupMemberInfo info) {
        if (TextUtils.isEmpty(info.getUserId())) {
            return false;
        }
        int position = -1;
        List<ReadReceiptData> data = adapter.getData();
        for (int i = 0; i < data.size(); i++) {
            ReadReceiptData item = data.get(i);
            if (TextUtils.equals(info.getUserId(), item.getInfo().getUserId())) {
                position = i;
                break;
            }
        }
        if (position >= 0) {
            ReadReceiptData readReceiptData = data.get(position);
            readReceiptData.setInfo(info);
            adapter.setData(position, readReceiptData);
        }
        return position >= 0;
    }

    private void updateMemberList(List<ReadReceiptData> data, boolean isRead) {
        if (isRead) readAdapter.addData(data);
        else unreadAdapter.addData(data);

        updateMemberNoneView();
    }

    private void updateMemberNoneView() {
        ReadReceiptInfoV5 infoV5 = getViewModel().getReadReceiptInfoV5();
        if (currentPosition == 0) {
            if (infoV5 != null && infoV5.getReadCount() > 0) {
                readReceiptNumberNone.setVisibility(View.GONE);
            } else if (readAdapter.getItemCount() == 0) {
                readReceiptNumberNone.setVisibility(View.VISIBLE);
                readReceiptNumberNone.setText(R.string.rc_message_none_user_read);
            } else {
                readReceiptNumberNone.setVisibility(View.GONE);
            }
        } else if (currentPosition == 1) {
            if (infoV5 != null && infoV5.getUnreadCount() > 0) {
                readReceiptNumberNone.setVisibility(View.GONE);
            } else if (unreadAdapter.getItemCount() == 0) {
                readReceiptNumberNone.setVisibility(View.VISIBLE);
                readReceiptNumberNone.setText(R.string.rc_message_all_user_read);
            } else {
                readReceiptNumberNone.setVisibility(View.GONE);
            }
        } else {
            readReceiptNumberNone.setVisibility(View.GONE);
        }
    }

    private void setupScrollListener(MessageReadDetailViewModel viewModel) {
        readScrollListener =
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        onScrollToSecondLast(recyclerView, viewModel, true);
                    }
                };
        unreadScrollListener =
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        onScrollToSecondLast(recyclerView, viewModel, false);
                    }
                };
        // 为两个列表添加滑动监听
        if (readList.getRecyclerView() != null) {
            readList.getRecyclerView().addOnScrollListener(readScrollListener);
        }
        if (unreadList.getRecyclerView() != null) {
            unreadList.getRecyclerView().addOnScrollListener(unreadScrollListener);
        }
    }

    private void onScrollToSecondLast(
            RecyclerView recyclerView, MessageReadDetailViewModel viewModel, boolean isRead) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) {
            return;
        }
        int totalItemCount = layoutManager.getItemCount();
        int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
        // 当列表超过1屏且倒数第2个Item可见时触发回调
        if (totalItemCount > 0 && lastVisibleItemPosition >= totalItemCount - 2) {
            viewModel.getMessagesReadReceiptUsersByPage(isRead);
        }
    }

    private void updateTabText(@NonNull MessageReadDetailViewModel viewModel) {
        ReadReceiptInfoV5 infoV5 = viewModel.getReadReceiptInfoV5();
        if (infoV5 != null) {
            int readSize = infoV5.getReadCount();
            String readTxt = getString(R.string.rc_read_receipt) + "(" + readSize + ")";
            readTabText.setText(readTxt);
            int unreadSize = infoV5.getUnreadCount();
            String unreadTxt = getString(R.string.rc_unread_receipt) + "(" + unreadSize + ")";
            unReadTabText.setText(unreadTxt);
        } else {
            readTabText.setText(R.string.rc_read_receipt);
            unReadTabText.setText(R.string.rc_unread_receipt);
        }
        if (currentPosition == 0) {
            readTabText.setTextColor(
                    IMKitThemeManager.getColorFromAttrId(getContext(), R.attr.rc_primary_color));
            unReadTabText.setTextColor(
                    IMKitThemeManager.getColorFromAttrId(
                            getContext(), R.attr.rc_text_secondary_color));
            readUnderLine.setBackgroundColor(
                    IMKitThemeManager.getColorFromAttrId(getContext(), R.attr.rc_primary_color));
            unReadUnderLine.setBackgroundColor(
                    IMKitThemeManager.getColorFromAttrId(
                            getContext(), R.attr.rc_line_background_color));
        } else {
            readTabText.setTextColor(
                    IMKitThemeManager.getColorFromAttrId(
                            getContext(), R.attr.rc_text_secondary_color));
            unReadTabText.setTextColor(
                    IMKitThemeManager.getColorFromAttrId(getContext(), R.attr.rc_primary_color));
            readUnderLine.setBackgroundColor(
                    IMKitThemeManager.getColorFromAttrId(
                            getContext(), R.attr.rc_line_background_color));
            unReadUnderLine.setBackgroundColor(
                    IMKitThemeManager.getColorFromAttrId(getContext(), R.attr.rc_primary_color));
        }
    }

    @Override
    public void onDestroyView() {
        // 移除滑动监听器，避免内存泄漏
        if (readList != null && readList.getRecyclerView() != null && readScrollListener != null) {
            readList.getRecyclerView().removeOnScrollListener(readScrollListener);
        }
        if (unreadList != null
                && unreadList.getRecyclerView() != null
                && unreadScrollListener != null) {
            unreadList.getRecyclerView().removeOnScrollListener(unreadScrollListener);
        }
        super.onDestroyView();
    }
}
