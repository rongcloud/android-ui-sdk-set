package io.rong.imkit.feature.reaction;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import java.util.ArrayList;
import java.util.List;

/**
 * 回应表情选择器弹层，底部弹出式样式，支持上滑展开。
 *
 * @since 5.42.0
 */
public class ReactionEmojiPickerDialog extends DialogFragment {

    public static final String TAG = "ReactionEmojiPickerDialog";
    private static final String ARG_MESSAGE_UID = "messageUId";
    private static final int GRID_COLUMNS = 9;
    private static final int ANIMATION_DURATION_MS = 200;
    private static final float COLLAPSED_HEIGHT_RATIO = 0.5f;
    private static boolean showing;

    private OnEmojiSelectedListener listener;
    private String messageUId;
    private View root;
    private View sheet;
    private View dragHandle;
    private RecyclerView rvEmoji;

    private int collapsedHeight;
    private int expandedHeight;
    private int collapsedTransY;
    private float downRawY;
    private float downTransY;
    private int touchSlop;
    private ValueAnimator sheetAnimator;
    private SheetState sheetState = SheetState.COLLAPSED;
    private boolean dismissing;

    private enum SheetState {
        COLLAPSED,
        EXPANDED,
        DISMISSING
    }

    public static ReactionEmojiPickerDialog newInstance(String messageUId) {
        ReactionEmojiPickerDialog dialog = new ReactionEmojiPickerDialog();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE_UID, messageUId);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            messageUId = getArguments().getString(ARG_MESSAGE_UID);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rc_reaction_emoji_picker, container, false);
        findView(view);
        initView();
        bindData();
        return view;
    }

    private void findView(View view) {
        root = view.findViewById(R.id.rc_reaction_root);
        sheet = view.findViewById(R.id.rc_reaction_sheet);
        dragHandle = view.findViewById(R.id.rc_drag_handle);
        rvEmoji = view.findViewById(R.id.rc_rv_emoji);
    }

    private void initView() {
        touchSlop = ViewConfiguration.get(requireContext()).getScaledTouchSlop();

        updateSheetHeights();
        sheet.setTranslationY(expandedHeight);
        sheetState = SheetState.COLLAPSED;
        root.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    if (bottom - top == oldBottom - oldTop) {
                        return;
                    }
                    updateSheetHeights();
                    if (sheetAnimator == null) {
                        syncSheetTranslationWithState();
                    }
                });
        root.post(
                () -> {
                    updateSheetHeights();
                    animateSheetTo(collapsedTransY, SheetState.COLLAPSED, false);
                });

        root.setOnClickListener(v -> dismissSheet());
        sheet.setOnClickListener(v -> {});

        EmojiSectionAdapter adapter = new EmojiSectionAdapter();
        adapter.setOnItemClickListener(this::onEmojiSelected);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), GRID_COLUMNS);
        layoutManager.setSpanSizeLookup(
                new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return adapter.isTitle(position) ? GRID_COLUMNS : 1;
                    }
                });
        rvEmoji.setLayoutManager(layoutManager);
        rvEmoji.setAdapter(adapter);
        rvEmoji.setOverScrollMode(View.OVER_SCROLL_NEVER);

        setupDragHandle();
        setupRecyclerDrag();
    }

    private void updateSheetHeights() {
        int availableHeight = root == null ? 0 : root.getHeight();
        if (availableHeight <= 0 && getDialog() != null && getDialog().getWindow() != null) {
            View decorView = getDialog().getWindow().getDecorView();
            availableHeight = decorView == null ? 0 : decorView.getHeight();
        }
        if (availableHeight <= 0) {
            availableHeight = getResources().getDisplayMetrics().heightPixels;
        }
        collapsedHeight = (int) (availableHeight * COLLAPSED_HEIGHT_RATIO);
        int expandedTopRevealHeight = resolveExpandedTopRevealHeight(availableHeight);
        expandedHeight = Math.max(collapsedHeight, availableHeight - expandedTopRevealHeight);
        collapsedTransY = expandedHeight - collapsedHeight;

        ViewGroup.LayoutParams lp = sheet.getLayoutParams();
        lp.height = expandedHeight;
        sheet.setLayoutParams(lp);
    }

    private int resolveExpandedTopRevealHeight(int availableHeight) {
        int anchorTop = resolveHostAnchorTopOnScreen();
        int rootTop = getViewTopOnScreen(root);
        if (anchorTop > rootTop) {
            return clampTopRevealHeight(anchorTop - rootTop, availableHeight);
        }
        int headComponentHeight = getResources().getDimensionPixelSize(R.dimen.rc_title_bar_height);
        return clampTopRevealHeight(headComponentHeight, availableHeight);
    }

    private int resolveHostAnchorTopOnScreen() {
        if (getActivity() == null) {
            return 0;
        }
        int[] anchorIds = {R.id.rc_message_list, R.id.rc_refresh, R.id.rc_base_container};
        for (int anchorId : anchorIds) {
            View anchor = getActivity().findViewById(anchorId);
            if (anchor != null && anchor.getHeight() > 0) {
                return getViewTopOnScreen(anchor);
            }
        }
        return 0;
    }

    private int clampTopRevealHeight(int topRevealHeight, int availableHeight) {
        if (topRevealHeight <= 0 || availableHeight <= 0) {
            return 0;
        }
        int minExpandedHeight = (int) (availableHeight * COLLAPSED_HEIGHT_RATIO);
        return Math.min(topRevealHeight, Math.max(0, availableHeight - minExpandedHeight));
    }

    private int getViewTopOnScreen(View view) {
        if (view == null) {
            return 0;
        }
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return location[1];
    }

    private void syncSheetTranslationWithState() {
        if (sheet == null) {
            return;
        }
        switch (sheetState) {
            case EXPANDED:
                sheet.setTranslationY(0);
                break;
            case COLLAPSED:
                sheet.setTranslationY(collapsedTransY);
                break;
            case DISMISSING:
                sheet.setTranslationY(expandedHeight);
                break;
            default:
                break;
        }
    }

    private List<SectionItem> buildSectionItems() {
        List<SectionItem> items = new ArrayList<>();
        if (getContext() != null) {
            ReactionFrequentManager manager = new ReactionFrequentManager(getContext());
            int displayCount =
                    RongConfigCenter.conversationConfig().getMessageReactionFrequentDisplayCount();
            List<String> frequentIds = manager.getFrequentReactionIds(displayCount);
            if (!frequentIds.isEmpty()) {
                items.add(SectionItem.title(getString(R.string.rc_reaction_frequent_title)));
                for (String id : frequentIds) {
                    items.add(
                            SectionItem.emoji(
                                    new ReactionEmoji(
                                            ReactionEmojiProvider.findUnicodeById(id), id)));
                }
            }
        }
        items.add(SectionItem.title(getString(R.string.rc_reaction_default_title)));
        for (ReactionEmoji emoji : ReactionEmojiProvider.getEmojiList()) {
            items.add(SectionItem.emoji(emoji));
        }
        return items;
    }

    private void onEmojiSelected(ReactionEmoji emoji) {
        if (listener != null) {
            listener.onEmojiSelected(messageUId, emoji.getReactionId());
        }
        dismissSheet();
    }

    private void setupDragHandle() {
        dragHandle.setOnTouchListener(
                (v, event) -> {
                    if (dismissing) {
                        return true;
                    }
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            beginSheetDrag(event);
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            dragSheet(event);
                            return true;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            settleSheet();
                            return true;
                        default:
                            return false;
                    }
                });
    }

    private void setupRecyclerDrag() {
        rvEmoji.addOnItemTouchListener(
                new RecyclerView.SimpleOnItemTouchListener() {
                    private boolean dragging;

                    @Override
                    public boolean onInterceptTouchEvent(
                            @NonNull RecyclerView rv, @NonNull MotionEvent event) {
                        if (dismissing) {
                            return true;
                        }
                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                beginSheetDrag(event);
                                dragging = false;
                                return false;
                            case MotionEvent.ACTION_MOVE:
                                if (shouldDragSheet(rv, event)) {
                                    dragging = true;
                                    dragSheet(event);
                                    return true;
                                }
                                return false;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                if (dragging) {
                                    dragging = false;
                                    settleSheet();
                                    return true;
                                }
                                return false;
                            default:
                                return false;
                        }
                    }

                    @Override
                    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent event) {
                        if (!dragging) {
                            return;
                        }
                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_MOVE:
                                dragSheet(event);
                                break;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                dragging = false;
                                settleSheet();
                                break;
                            default:
                                break;
                        }
                    }
                });
    }

    private void beginSheetDrag(MotionEvent event) {
        cancelSheetAnimator();
        downRawY = event.getRawY();
        downTransY = sheet.getTranslationY();
    }

    private void dragSheet(MotionEvent event) {
        float dy = event.getRawY() - downRawY;
        setSheetTranslation(downTransY + dy);
    }

    private boolean shouldDragSheet(RecyclerView rv, MotionEvent event) {
        float dy = event.getRawY() - downRawY;
        if (Math.abs(dy) < touchSlop) {
            return false;
        }
        if (dy < 0) {
            return sheet.getTranslationY() > 0;
        }
        return !rv.canScrollVertically(-1);
    }

    private void setSheetTranslation(float translationY) {
        float clamped = Math.max(0, Math.min(translationY, expandedHeight));
        sheet.setTranslationY(clamped);
    }

    private void settleSheet() {
        float current = sheet.getTranslationY();
        if (current > collapsedTransY + collapsedHeight * 0.25f) {
            dismissSheet();
        } else if (current < collapsedTransY / 2f) {
            animateSheetTo(0, SheetState.EXPANDED, false);
        } else {
            animateSheetTo(collapsedTransY, SheetState.COLLAPSED, false);
        }
    }

    private void animateSheetTo(float target, SheetState targetState, boolean dismissOnEnd) {
        cancelSheetAnimator();
        sheetAnimator = ValueAnimator.ofFloat(sheet.getTranslationY(), target);
        sheetAnimator.setDuration(ANIMATION_DURATION_MS);
        sheetAnimator.addUpdateListener(a -> sheet.setTranslationY((float) a.getAnimatedValue()));
        sheetAnimator.addListener(
                new AnimatorListenerAdapter() {
                    private boolean canceled;

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        canceled = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        sheetAnimator = null;
                        if (canceled) {
                            return;
                        }
                        sheetState = targetState;
                        if (dismissOnEnd) {
                            if (sheet != null) {
                                sheet.setVisibility(View.GONE);
                            }
                            dismissAllowingStateLoss();
                        }
                    }
                });
        sheetAnimator.start();
    }

    private void dismissSheet() {
        if (dismissing) {
            return;
        }
        dismissing = true;
        sheetState = SheetState.DISMISSING;
        if (root != null) {
            root.setOnClickListener(null);
        }
        if (sheet == null) {
            dismissAllowingStateLoss();
            return;
        }
        animateSheetTo(expandedHeight, SheetState.DISMISSING, true);
    }

    private void cancelSheetAnimator() {
        if (sheetAnimator != null) {
            sheetAnimator.cancel();
            sheetAnimator = null;
        }
    }

    private void bindData() {
        if (rvEmoji != null && rvEmoji.getAdapter() instanceof EmojiSectionAdapter) {
            ((EmojiSectionAdapter) rvEmoji.getAdapter()).setItems(buildSectionItems());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setCanceledOnTouchOutside(true);
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setDimAmount(0f);
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                WindowManager.LayoutParams params = window.getAttributes();
                params.gravity = Gravity.BOTTOM;
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                window.setAttributes(params);
                if (window.getDecorView() != null) {
                    window.getDecorView().setBackgroundColor(Color.TRANSPARENT);
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        cancelSheetAnimator();
        root = null;
        sheet = null;
        dragHandle = null;
        rvEmoji = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        resetShowing();
        super.onDestroy();
    }

    public void show(FragmentManager manager) {
        if (manager == null || manager.isStateSaved()) {
            return;
        }
        synchronized (ReactionEmojiPickerDialog.class) {
            if (showing) {
                return;
            }
            Fragment existing = manager.findFragmentByTag(TAG);
            if (existing != null) {
                manager.beginTransaction().remove(existing).commitNowAllowingStateLoss();
            }
            showing = true;
        }
        try {
            super.show(manager, TAG);
        } catch (IllegalStateException e) {
            resetShowing();
            RLog.e(TAG, "show failed, e:" + e);
        }
    }

    public void setOnEmojiSelectedListener(OnEmojiSelectedListener listener) {
        this.listener = listener;
    }

    public interface OnEmojiSelectedListener {
        void onEmojiSelected(String messageUId, String reactionId);
    }

    private static void resetShowing() {
        synchronized (ReactionEmojiPickerDialog.class) {
            showing = false;
        }
    }

    private static int dp2px(View view, int dp) {
        float density = view.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    private static int resolveColor(View view, int attrId, int fallbackColorResId) {
        TypedValue typedValue = new TypedValue();
        boolean resolved = view.getContext().getTheme().resolveAttribute(attrId, typedValue, true);
        if (resolved) {
            if (typedValue.resourceId != 0) {
                return view.getResources().getColor(typedValue.resourceId);
            }
            return typedValue.data;
        }
        return view.getResources().getColor(fallbackColorResId);
    }

    private static class SectionItem {
        static final int TYPE_TITLE = 0;
        static final int TYPE_EMOJI = 1;

        final int type;
        final String title;
        final ReactionEmoji emoji;

        private SectionItem(int type, String title, ReactionEmoji emoji) {
            this.type = type;
            this.title = title;
            this.emoji = emoji;
        }

        static SectionItem title(String title) {
            return new SectionItem(TYPE_TITLE, title, null);
        }

        static SectionItem emoji(ReactionEmoji emoji) {
            return new SectionItem(TYPE_EMOJI, null, emoji);
        }
    }

    private static class EmojiSectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final List<SectionItem> items = new ArrayList<>();
        private OnItemClickListener clickListener;

        void setItems(List<SectionItem> newItems) {
            items.clear();
            if (newItems != null) {
                items.addAll(newItems);
            }
            notifyDataSetChanged();
        }

        boolean isTitle(int position) {
            return position >= 0
                    && position < items.size()
                    && items.get(position).type == SectionItem.TYPE_TITLE;
        }

        void setOnItemClickListener(OnItemClickListener listener) {
            this.clickListener = listener;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).type;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            if (viewType == SectionItem.TYPE_TITLE) {
                tv.setTextSize(13);
                tv.setTextColor(
                        resolveColor(
                                parent, R.attr.rc_text_primary_color, R.color.rc_edit_reference));
                tv.setGravity(Gravity.CENTER_VERTICAL);
                RecyclerView.LayoutParams lp =
                        new RecyclerView.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.leftMargin = dp2px(parent, 4);
                lp.rightMargin = dp2px(parent, 16);
                lp.topMargin = dp2px(parent, 8);
                lp.bottomMargin = dp2px(parent, 0);
                tv.setLayoutParams(lp);
                return new TitleViewHolder(tv);
            }
            tv.setTextSize(24);
            tv.setGravity(Gravity.CENTER);
            tv.setTextColor(
                    resolveColor(parent, R.attr.rc_text_primary_color, R.color.rc_edit_reference));
            int size = dp2px(parent, 48);
            RecyclerView.LayoutParams lp =
                    new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, size);
            tv.setLayoutParams(lp);
            return new EmojiViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            SectionItem item = items.get(position);
            TextView tv = (TextView) holder.itemView;
            if (item.type == SectionItem.TYPE_TITLE) {
                tv.setText(item.title);
                tv.setOnClickListener(null);
                return;
            }
            ReactionEmoji emoji = item.emoji;
            tv.setText(emoji == null ? "" : emoji.getUnicode());
            tv.setOnClickListener(
                    v -> {
                        if (clickListener != null && emoji != null) {
                            clickListener.onItemClick(emoji);
                        }
                    });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        interface OnItemClickListener {
            void onItemClick(ReactionEmoji emoji);
        }

        private static class TitleViewHolder extends RecyclerView.ViewHolder {
            TitleViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }

        private static class EmojiViewHolder extends RecyclerView.ViewHolder {
            EmojiViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
