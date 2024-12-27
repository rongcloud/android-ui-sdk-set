package io.rong.imkit.usermanage.component;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseComponent;
import io.rong.imkit.base.adapter.HeaderAndFooterWrapper;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.usermanage.adapter.ContactListAdapter;
import io.rong.imkit.usermanage.interfaces.OnActionClickListener;
import io.rong.imkit.usermanage.interfaces.OnPagedDataLoader;
import io.rong.imkit.widget.SideBar;
import io.rong.imkit.widget.refresh.SmartRefreshLayout;
import io.rong.imkit.widget.refresh.wrapper.RongRefreshHeader;
import java.util.ArrayList;
import java.util.List;

public class ContactListComponent extends BaseComponent {

    private RecyclerView rvContactList;
    private SideBar sideBarContact;
    private boolean showSideBar;
    private boolean showItemSelectButton;
    private boolean showItemRightArrow;
    private boolean showItemRightText;
    private boolean showDivider;
    private boolean showItemSelectAutoUpdate;
    private boolean showItemRemoveButton;
    private SmartRefreshLayout refreshLayout;
    private ContactListAdapter contactListAdapter;
    private HeaderAndFooterWrapper headerAndFooterWrapper;
    private OnActionClickListener<ContactModel> onItemClickListener;
    private OnPagedDataLoader onPagedDataLoader;
    private OnActionClickListener<ContactModel> onItemRemoveClickListener;

    public ContactListComponent(@NonNull Context context) {
        super(context);
    }

    public ContactListComponent(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ContactListComponent(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View onCreateView(
            Context context,
            LayoutInflater inflater,
            @NonNull ViewGroup parent,
            AttributeSet attrs) {
        // Inflate the layout and attach it to the parent ViewGroup
        View view = inflater.inflate(R.layout.rc_contact_list_component, parent, false);

        // Process custom attributes
        if (attrs != null) {
            TypedArray a =
                    context.getTheme()
                            .obtainStyledAttributes(attrs, R.styleable.ContactListComponent, 0, 0);
            try {
                showSideBar = a.getBoolean(R.styleable.ContactListComponent_show_side_bar, false);
                showItemSelectButton =
                        a.getBoolean(R.styleable.ContactListComponent_show_item_select_icon, false);
                showItemRightArrow =
                        a.getBoolean(R.styleable.ContactListComponent_show_item_right_arrow, false);
                showItemRightText =
                        a.getBoolean(R.styleable.ContactListComponent_show_item_right_text, false);
                showDivider = a.getBoolean(R.styleable.ContactListComponent_show_divider, false);
                showItemSelectAutoUpdate =
                        a.getBoolean(
                                R.styleable.ContactListComponent_show_item_select_auto_update,
                                false);
                showItemRemoveButton =
                        a.getBoolean(
                                R.styleable.ContactListComponent_show_item_remove_button, false);
            } finally {
                if (a != null) {
                    a.recycle();
                }
            }
        }

        refreshLayout = view.findViewById(R.id.rc_refresh);
        refreshLayout.setNestedScrollingEnabled(false);
        refreshLayout.setRefreshHeader(new RongRefreshHeader(context));
        refreshLayout.setRefreshFooter(new RongRefreshHeader(context));
        refreshLayout.setEnableRefresh(false);
        refreshLayout.setEnableLoadMore(false);
        refreshLayout.setOnLoadMoreListener(
                refreshLayout -> {
                    if (onPagedDataLoader != null) {
                        onPagedDataLoader.loadNext(
                                aBoolean -> {
                                    refreshLayout.finishLoadMore();
                                    if (!onPagedDataLoader.hasNext()) {
                                        refreshLayout.setEnableLoadMore(false);
                                    }
                                });
                    }
                });

        // Initialize SideBar and its related views
        sideBarContact = view.findViewById(R.id.side_bar_contact);
        sideBarContact.setVisibility(showSideBar ? VISIBLE : GONE);
        TextView groupDialogTextView = view.findViewById(R.id.tv_group_overlay);
        groupDialogTextView.setVisibility(GONE);
        sideBarContact.setTextView(groupDialogTextView);
        sideBarContact.setOnTouchingLetterChangedListener(
                s -> {
                    if (contactListAdapter != null && rvContactList != null && s != null) {
                        int position = contactListAdapter.getPositionForSection(s.charAt(0));
                        if (position != -1) {
                            LinearLayoutManager layoutManager =
                                    (LinearLayoutManager) rvContactList.getLayoutManager();
                            if (layoutManager != null) {
                                // 将指定位置的项滚动到 RecyclerView 的顶部
                                layoutManager.scrollToPositionWithOffset(position, 0);
                            }
                        }
                    }
                });

        // Initialize RecyclerView
        rvContactList = view.findViewById(R.id.rv_contact_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        rvContactList.setLayoutManager(layoutManager);

        // Set item divider if enabled
        if (showDivider) {
            DividerItemDecoration itemDecoration =
                    new DividerItemDecoration(context, layoutManager.getOrientation());
            rvContactList.addItemDecoration(itemDecoration);
        }

        // Initialize and set the adapter
        contactListAdapter =
                new ContactListAdapter(
                        showItemSelectButton,
                        showItemRightArrow,
                        showItemRightText,
                        showItemSelectAutoUpdate,
                        showItemRemoveButton);
        contactListAdapter.setOnItemClickListener(
                new OnActionClickListener<ContactModel>() {
                    @Override
                    public void onActionClick(ContactModel contactModel) {}

                    @Override
                    public <E> void onActionClickWithConfirm(
                            ContactModel contactModel, OnConfirmClickListener<E> listener) {
                        OnActionClickListener.super.onActionClickWithConfirm(
                                contactModel, listener);
                        if (onItemClickListener != null) {
                            onItemClickListener.onActionClickWithConfirm(contactModel, listener);
                        }
                    }
                });
        contactListAdapter.setOnItemRemoveClickListener(
                contactModel -> {
                    if (onItemRemoveClickListener != null) {
                        onItemRemoveClickListener.onActionClick(contactModel);
                    }
                });
        headerAndFooterWrapper = new HeaderAndFooterWrapper(contactListAdapter);
        rvContactList.setAdapter(headerAndFooterWrapper);
        return view;
    }

    /**
     * 设置联系人列表
     *
     * @param data 联系人列表数据
     */
    public void setContactList(List<ContactModel> data) {
        if (contactListAdapter != null) {
            contactListAdapter.setData(data);
        }
        if (headerAndFooterWrapper != null) {
            headerAndFooterWrapper.notifyDataSetChanged();
        }

        if (onPagedDataLoader != null) {
            refreshLayout.setEnableLoadMore(onPagedDataLoader.hasNext());
        }

        if (data != null && !data.isEmpty()) {
            List<String> lettersList = new ArrayList<>();
            for (ContactModel contactModel : data) {
                if (contactModel.getContactType() == ContactModel.ItemType.TITLE
                        && contactModel.getBean() instanceof String) {
                    lettersList.add((String) contactModel.getBean());
                }
            }
            setSideBarContactLetters(lettersList.toArray(new String[0]));
        }
    }

    /**
     * 设置联系人列表点击事件监听器
     *
     * @param listener {@link OnActionClickListener}
     */
    public void setOnItemClickListener(OnActionClickListener<ContactModel> listener) {
        this.onItemClickListener = listener;
    }

    /**
     * 设置联系人列表移除按钮点击事件监听器
     *
     * @param listener {@link OnActionClickListener}
     */
    public void setOnItemRemoveClickListener(OnActionClickListener<ContactModel> listener) {
        this.onItemRemoveClickListener = listener;
    }

    public void addHeaderView(View view) {
        if (headerAndFooterWrapper != null) {
            headerAndFooterWrapper.addHeaderView(view);
        }
    }

    public void addFootView(View view) {
        if (headerAndFooterWrapper != null) {
            headerAndFooterWrapper.addFootView(view);
        }
    }

    public void setEnableLoadMore(boolean isEnable) {
        if (refreshLayout != null) {
            refreshLayout.setEnableLoadMore(isEnable);
        }
    }

    private void setSideBarContactLetters(String[] letters) {
        if (sideBarContact != null && showSideBar) {
            sideBarContact.setLetters(letters);
        }
    }

    public void setShowItemRemoveButton(boolean isShow) {
        if (contactListAdapter != null) {
            contactListAdapter.setShowItemRemoveButton(isShow);
        }
    }

    /**
     * 设置分页数据加载器
     *
     * @param onPageLoader 分页数据加载器
     */
    public void setOnPageDataLoader(OnPagedDataLoader onPageLoader) {
        this.onPagedDataLoader = onPageLoader;
    }
}
