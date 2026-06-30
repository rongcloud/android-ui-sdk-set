package io.rong.imkit.feature.reaction;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.handler.ReactionUsersPagedHandler;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.picture.widget.BaseDialogFragment;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MessageReaction;
import io.rong.imlib.model.MessageReactionUser;
import java.util.List;

public class ReactionDetailDialog extends BaseDialogFragment {

    private static final String ARG_MESSAGE_UID = "messageUId";
    private static final String ARG_CONVERSATION_TYPE = "conversationType";
    private static final String ARG_TARGET_ID = "targetId";
    private static final String ARG_SELECTED_REACTION_ID = "selectedReactionId";
    private static final int MAX_REACTION_COUNT_DISPLAY = 99;
    private static final int USER_PAGE_SIZE = 50;
    private String messageUId;
    private Conversation.ConversationType conversationType;
    private String targetId;
    private String selectedReactionId;
    private List<MessageReaction> reactions;
    private RecyclerView rvTabs;
    private RecyclerView rvUsers;
    private TextView tvTitle;
    private ImageView ivClose;
    private int selectedTabIndex = 0;
    private UserAdapter userAdapter;
    private ReactionUsersPagedHandler usersPagedHandler;
    private boolean loadingFirstPage;
    private UiMessage uiMessage;

    public static ReactionDetailDialog newInstance(String messageUId) {
        return newInstance(messageUId, null, null, null);
    }

    public static ReactionDetailDialog newInstance(
            String messageUId, Conversation.ConversationType conversationType, String targetId) {
        return newInstance(messageUId, conversationType, targetId, null);
    }

    public static ReactionDetailDialog newInstance(
            String messageUId,
            Conversation.ConversationType conversationType,
            String targetId,
            String selectedReactionId) {
        ReactionDetailDialog dialog = new ReactionDetailDialog();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE_UID, messageUId);
        if (conversationType != null) {
            args.putInt(ARG_CONVERSATION_TYPE, conversationType.getValue());
        }
        args.putString(ARG_TARGET_ID, targetId);
        args.putString(ARG_SELECTED_REACTION_ID, selectedReactionId);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            messageUId = getArguments().getString(ARG_MESSAGE_UID);
            if (getArguments().containsKey(ARG_CONVERSATION_TYPE)) {
                conversationType =
                        Conversation.ConversationType.setValue(
                                getArguments().getInt(ARG_CONVERSATION_TYPE));
            }
            targetId = getArguments().getString(ARG_TARGET_ID);
            selectedReactionId = getArguments().getString(ARG_SELECTED_REACTION_ID);
        }
    }

    @Override
    protected int getContentView() {
        return R.layout.rc_reaction_detail_dialog;
    }

    @Override
    protected void findView() {
        tvTitle = mRootView.findViewById(R.id.rc_tv_detail_title);
        ivClose = mRootView.findViewById(R.id.rc_iv_detail_close);
        rvTabs = mRootView.findViewById(R.id.rc_rv_reaction_tabs);
        rvUsers = mRootView.findViewById(R.id.rc_rv_reaction_users);
    }

    @Override
    protected void initView() {
        usersPagedHandler = new ReactionUsersPagedHandler(USER_PAGE_SIZE);
        usersPagedHandler.addDataChangeListener(
                ReactionUsersPagedHandler.KEY_GET_REACTION_USERS,
                new io.rong.imkit.usermanage.interfaces.OnDataChangeListener<
                        ReactionUsersPagedHandler.UserPageResult>() {
                    @Override
                    public void onDataChange(ReactionUsersPagedHandler.UserPageResult result) {
                        loadingFirstPage = false;
                        if (result == null
                                || reactions == null
                                || selectedTabIndex >= reactions.size()) {
                            return;
                        }
                        MessageReaction selectedReaction = reactions.get(selectedTabIndex);
                        if (selectedReaction == null
                                || !TextUtils.equals(
                                        selectedReaction.getReactionId(), result.getReactionId())) {
                            return;
                        }
                        if (result.isFirstPage()) {
                            bindUsers(result.getUsers(), selectedReaction);
                        } else if (userAdapter != null) {
                            userAdapter.appendUsers(result.getUsers());
                        }
                        maybeLoadMoreUsers();
                    }

                    @Override
                    public void onDataError(
                            io.rong.imlib.IRongCoreEnum.CoreErrorCode coreErrorCode,
                            String errorMsg) {
                        if (!loadingFirstPage
                                || reactions == null
                                || selectedTabIndex >= reactions.size()) {
                            return;
                        }
                        loadingFirstPage = false;
                        MessageReaction reaction = reactions.get(selectedTabIndex);
                        bindUsers(reaction == null ? null : reaction.getUsers(), reaction);
                    }
                });
        rvTabs.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUsers.addItemDecoration(new UserDividerDecoration());
        rvUsers.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                        if (dy <= 0
                                || usersPagedHandler == null
                                || usersPagedHandler.isLoading()
                                || !usersPagedHandler.hasNext()) {
                            return;
                        }
                        LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                        if (lm != null
                                && lm.findLastVisibleItemPosition() >= lm.getItemCount() - 3) {
                            loadingFirstPage = false;
                            usersPagedHandler.loadNext(null);
                        }
                    }
                });
        ivClose.setOnClickListener(v -> dismiss());
    }

    @Override
    public void bindData() {
        if (reactions == null || reactions.isEmpty()) {
            return;
        }
        selectedTabIndex = resolveSelectedTabIndex();
        TabAdapter tabAdapter = new TabAdapter(reactions, selectedTabIndex);
        tabAdapter.setOnTabClickListener(
                index -> {
                    selectedTabIndex = index;
                    tabAdapter.setSelectedIndex(index);
                    tabAdapter.notifyDataSetChanged();
                    loadUsersForReaction(index);
                });
        rvTabs.setAdapter(tabAdapter);
        rvTabs.scrollToPosition(selectedTabIndex);
        loadUsersForReaction(selectedTabIndex);
    }

    private void loadUsersForReaction(int index) {
        if (reactions == null
                || index < 0
                || index >= reactions.size()
                || usersPagedHandler == null) {
            return;
        }
        MessageReaction reaction = reactions.get(index);
        if (reaction == null) {
            return;
        }
        loadingFirstPage = true;
        usersPagedHandler.query(messageUId, reaction.getReactionId());
    }

    private int resolveSelectedTabIndex() {
        if (!TextUtils.isEmpty(selectedReactionId)) {
            for (int i = 0; i < reactions.size(); i++) {
                MessageReaction reaction = reactions.get(i);
                if (reaction != null
                        && TextUtils.equals(selectedReactionId, reaction.getReactionId())) {
                    return i;
                }
            }
        }
        if (selectedTabIndex < 0 || selectedTabIndex >= reactions.size()) {
            return 0;
        }
        return selectedTabIndex;
    }

    private void bindUsers(
            List<io.rong.imlib.model.MessageReactionUser> users, MessageReaction reaction) {
        List<io.rong.imlib.model.MessageReactionUser> data = users;
        if ((data == null || data.isEmpty()) && reaction != null) {
            data = reaction.getUsers();
        }
        userAdapter = new UserAdapter(data, conversationType, targetId, uiMessage, reaction);
        rvUsers.setAdapter(userAdapter);
    }

    private void maybeLoadMoreUsers() {
        if (rvUsers == null || usersPagedHandler == null) {
            return;
        }
        rvUsers.post(
                () -> {
                    if (usersPagedHandler == null
                            || usersPagedHandler.isLoading()
                            || !usersPagedHandler.hasNext()
                            || rvUsers == null) {
                        return;
                    }
                    if (!rvUsers.canScrollVertically(1) || isNearUserListBottom()) {
                        loadingFirstPage = false;
                        usersPagedHandler.loadNext(null);
                    }
                });
    }

    private boolean isNearUserListBottom() {
        if (rvUsers == null || !(rvUsers.getLayoutManager() instanceof LinearLayoutManager)) {
            return false;
        }
        LinearLayoutManager lm = (LinearLayoutManager) rvUsers.getLayoutManager();
        return lm.findLastVisibleItemPosition() >= lm.getItemCount() - 3;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            android.view.Window window = getDialog().getWindow();
            window.setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(
                            android.graphics.Color.TRANSPARENT));
            window.setDimAmount(0.4f);
        }
        if (getDialog() != null) {
            getDialog().setCanceledOnTouchOutside(true);
        }
    }

    @Override
    protected int getGravity() {
        return Gravity.BOTTOM;
    }

    @Override
    protected float getScreenWidthProportion() {
        return 1f;
    }

    @Override
    protected int getScreenHeightProportion() {
        if (getContext() != null) {
            int screenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
            return (int) (screenHeight * 0.6f);
        }
        return ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    public void setReactions(List<MessageReaction> reactions) {
        this.reactions = reactions;
    }

    public void setUiMessage(UiMessage uiMessage) {
        this.uiMessage = uiMessage;
    }

    @Override
    public void onDestroyView() {
        if (usersPagedHandler != null) {
            usersPagedHandler.stop();
            usersPagedHandler = null;
        }
        super.onDestroyView();
    }

    private static class TabAdapter extends RecyclerView.Adapter<TabAdapter.VH> {
        private final List<MessageReaction> reactions;
        private int selectedIndex;
        private OnTabClickListener tabClickListener;

        TabAdapter(List<MessageReaction> reactions, int selectedIndex) {
            this.reactions = reactions;
            this.selectedIndex = selectedIndex;
        }

        void setSelectedIndex(int index) {
            this.selectedIndex = index;
        }

        void setOnTabClickListener(OnTabClickListener listener) {
            this.tabClickListener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout container = new LinearLayout(parent.getContext());
            container.setOrientation(LinearLayout.HORIZONTAL);
            container.setGravity(Gravity.CENTER);
            int h = dp2px(parent.getContext(), 32);
            int pad = dp2px(parent.getContext(), 12);
            container.setPadding(pad, 0, pad, 0);
            container.setLayoutParams(
                    new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, h));

            TextView tvEmoji = new TextView(parent.getContext());
            tvEmoji.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvEmoji.setGravity(Gravity.CENTER);
            container.addView(
                    tvEmoji,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));

            View divider = new View(parent.getContext());
            LinearLayout.LayoutParams dividerLp =
                    new LinearLayout.LayoutParams(
                            dp2px(parent.getContext(), 1), dp2px(parent.getContext(), 12));
            dividerLp.setMarginStart(dp2px(parent.getContext(), 6));
            dividerLp.setMarginEnd(dp2px(parent.getContext(), 6));
            container.addView(divider, dividerLp);

            TextView tvCount = new TextView(parent.getContext());
            tvCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvCount.setGravity(Gravity.CENTER);
            container.addView(
                    tvCount,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
            return new VH(container, tvEmoji, divider, tvCount);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            MessageReaction reaction = reactions.get(position);
            String emoji = ReactionEmojiProvider.findUnicodeById(reaction.getReactionId());
            int count = reaction.getTotalCount();
            holder.tvEmoji.setText(emoji != null ? emoji : reaction.getReactionId());
            holder.tvCount.setText(formatReactionCount(holder.itemView, count));

            RecyclerView.LayoutParams lp =
                    (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
            lp.setMarginEnd(
                    position == getItemCount() - 1 ? 0 : dp2px(holder.itemView.getContext(), 10));
            holder.itemView.setLayoutParams(lp);

            int textColor;
            if (position == selectedIndex) {
                holder.itemView.setBackgroundResource(
                        R.drawable.rc_lively_reaction_tab_selected_bg);
                textColor =
                        resolveThemeColor(
                                holder.itemView.getContext(),
                                R.attr.rc_control_title_white_color,
                                R.color.rc_white_color);
            } else {
                holder.itemView.setBackgroundResource(R.drawable.rc_lively_reaction_tab_normal_bg);
                textColor =
                        resolveThemeColor(
                                holder.itemView.getContext(),
                                R.attr.rc_text_secondary_color,
                                R.color.rc_edit_reference);
            }
            holder.tvEmoji.setTextColor(textColor);
            holder.divider.setBackgroundColor(textColor);
            holder.tvCount.setTextColor(textColor);
            holder.itemView.setOnClickListener(
                    v -> {
                        int adapterPosition = holder.getAdapterPosition();
                        if (tabClickListener != null
                                && adapterPosition != RecyclerView.NO_POSITION) {
                            tabClickListener.onTabClick(adapterPosition);
                        }
                    });
        }

        @Override
        public int getItemCount() {
            return reactions != null ? reactions.size() : 0;
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tvEmoji;
            final View divider;
            final TextView tvCount;

            VH(@NonNull View v, TextView tvEmoji, View divider, TextView tvCount) {
                super(v);
                this.tvEmoji = tvEmoji;
                this.divider = divider;
                this.tvCount = tvCount;
            }
        }

        interface OnTabClickListener {
            void onTabClick(int index);
        }
    }

    private static class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {
        private final List<MessageReactionUser> users;
        private final Conversation.ConversationType conversationType;
        private final String targetId;
        private final UiMessage uiMessage;
        private final MessageReaction reaction;

        UserAdapter(
                List<MessageReactionUser> users,
                Conversation.ConversationType conversationType,
                String targetId,
                UiMessage uiMessage,
                MessageReaction reaction) {
            this.users =
                    users == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(users);
            this.conversationType = conversationType;
            this.targetId = targetId;
            this.uiMessage = uiMessage;
            this.reaction = reaction;
        }

        /** 追加下一页用户（分页加载）。 */
        void appendUsers(List<MessageReactionUser> more) {
            if (more == null || more.isEmpty()) {
                return;
            }
            int start = users.size();
            users.addAll(more);
            notifyItemRangeInserted(start, more.size());
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v =
                    android.view.LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.rc_reaction_user_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            MessageReactionUser user = users.get(position);
            String userId = user == null ? null : user.getUserId();
            ReactionUserInfoHelper.DisplayInfo displayInfo =
                    ReactionUserInfoHelper.getDisplayInfo(
                            holder.itemView.getContext(),
                            conversationType,
                            targetId,
                            userId,
                            false);
            holder.tvName.setText(displayInfo.name);
            holder.ivAvatar.setImageResource(R.drawable.rc_default_portrait);
            if (!TextUtils.isEmpty(displayInfo.portraitUrl)) {
                RongConfigCenter.featureConfig()
                        .getKitImageEngine()
                        .loadUserPortrait(
                                holder.itemView.getContext(),
                                displayInfo.portraitUrl,
                                holder.ivAvatar);
            }
            // 点击用户项：透出给开发者回调（默认无操作）
            final MessageReactionUser clickedUser = user;
            holder.itemView.setOnClickListener(
                    v -> {
                        OnMessageReactionClickListener listener =
                                RongConfigCenter.featureConfig()
                                        .getOnMessageReactionClickListener();
                        if (listener != null && clickedUser != null) {
                            listener.onMessageReactionUserClicked(clickedUser, uiMessage, reaction);
                        }
                    });
        }

        @Override
        public int getItemCount() {
            return users != null ? users.size() : 0;
        }

        static class VH extends RecyclerView.ViewHolder {
            final ImageView ivAvatar;
            final TextView tvName;

            VH(@NonNull View v) {
                super(v);
                ivAvatar = v.findViewById(R.id.rc_iv_user_avatar);
                tvName = v.findViewById(R.id.rc_tv_user_name);
            }
        }
    }

    private static class UserDividerDecoration extends RecyclerView.ItemDecoration {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float lineHeight;
        private int lineOffset;
        private int horizontalInset;

        @Override
        public void onDraw(
                @NonNull Canvas canvas,
                @NonNull RecyclerView parent,
                @NonNull RecyclerView.State state) {
            ensureConfig(parent);
            int left = parent.getPaddingLeft() + horizontalInset;
            int right = parent.getWidth() - parent.getPaddingRight() - horizontalInset;
            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);
                int position = parent.getChildAdapterPosition(child);
                RecyclerView.Adapter<?> adapter = parent.getAdapter();
                if (adapter == null
                        || position == RecyclerView.NO_POSITION
                        || position >= adapter.getItemCount() - 1) {
                    continue;
                }
                RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
                int top = child.getBottom() + lp.bottomMargin;
                canvas.drawRect(left, top, right, top + lineHeight, paint);
            }
        }

        @Override
        public void getItemOffsets(
                @NonNull Rect outRect,
                @NonNull View view,
                @NonNull RecyclerView parent,
                @NonNull RecyclerView.State state) {
            ensureConfig(parent);
            int position = parent.getChildAdapterPosition(view);
            RecyclerView.Adapter<?> adapter = parent.getAdapter();
            if (adapter != null
                    && position != RecyclerView.NO_POSITION
                    && position < adapter.getItemCount() - 1) {
                outRect.bottom = lineOffset;
            }
        }

        private void ensureConfig(@NonNull RecyclerView parent) {
            if (lineOffset == 0) {
                lineHeight = dp2pxFloat(parent.getContext(), 0.5f);
                lineOffset = Math.max(1, (int) Math.ceil(lineHeight));
                horizontalInset = dp2px(parent.getContext(), 16);
            }
            paint.setColor(
                    resolveThemeColor(
                            parent.getContext(),
                            R.attr.rc_line_background_color,
                            R.color.rc_divider_color));
        }
    }

    private static String formatReactionCount(View view, int count) {
        String countText =
                count > MAX_REACTION_COUNT_DISPLAY
                        ? view.getResources().getString(R.string.rc_reaction_count_overflow)
                        : String.valueOf(count);
        return view.getResources().getString(R.string.rc_reaction_count_format, countText);
    }

    private static int dp2px(android.content.Context ctx, float dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    private static float dp2pxFloat(android.content.Context ctx, float dp) {
        return dp * ctx.getResources().getDisplayMetrics().density;
    }

    private static int resolveThemeColor(
            android.content.Context ctx, int attr, int fallbackColorResId) {
        TypedValue tv = new TypedValue();
        if (ctx.getTheme().resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) {
                return ctx.getResources().getColor(tv.resourceId);
            }
            return tv.data;
        }
        return ctx.getResources().getColor(fallbackColorResId);
    }
}
