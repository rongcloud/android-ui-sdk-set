package io.rong.imkit.feature.reaction;

import android.content.Context;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import io.rong.imkit.R;
import io.rong.imkit.config.MessageReactionDisplayMode;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MessageReaction;
import io.rong.imlib.model.MessageReactionUser;
import java.util.ArrayList;
import java.util.List;

/** 消息回应展示组件，采用 FlowLayout 布局。 */
public class ReactionView extends ViewGroup {

    private static final int MAX_REACTION_COUNT_DISPLAY = 99;
    private static final int DETAIL_NAME_PREVIEW_LIMIT = 10;
    private static final int DETAIL_NAME_MAX_WIDTH_DP = 180;
    private static final int DETAIL_TEXT_WIDTH_BUFFER_DP = 6;
    private static final int DETAIL_ITEM_SAFE_INSET_DP = 6;
    private int horizontalGap;
    private int verticalGap;
    private OnMoreClickListener onMoreClickListener;
    private OnReactionItemClickListener onReactionItemClickListener;
    private List<MessageReaction> allReactions;
    private Conversation.ConversationType conversationType;
    private String targetId;

    public ReactionView(Context context) {
        super(context);
        init();
    }

    public ReactionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ReactionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        horizontalGap = dp2px(4);
        verticalGap = dp2px(4);
    }

    public void setReactions(List<MessageReaction> reactions) {
        removeAllViews();
        allReactions = reactions;
        if (reactions == null || reactions.isEmpty()) {
            return;
        }
        MessageReactionDisplayMode mode =
                RongConfigCenter.conversationConfig().getMessageReactionDisplayMode();
        for (MessageReaction reaction : reactions) {
            addReactionItem(reaction, mode);
        }
        requestLayout();
    }

    public void setMessageContext(Conversation.ConversationType conversationType, String targetId) {
        this.conversationType = conversationType;
        this.targetId = targetId;
    }

    public void setOnMoreClickListener(OnMoreClickListener listener) {
        this.onMoreClickListener = listener;
    }

    public void setOnReactionItemClickListener(OnReactionItemClickListener listener) {
        this.onReactionItemClickListener = listener;
    }

    private void addReactionItem(MessageReaction reaction, MessageReactionDisplayMode mode) {
        View itemView =
                LayoutInflater.from(getContext()).inflate(R.layout.rc_reaction_item, this, false);
        TextView tvEmoji = itemView.findViewById(R.id.rc_tv_reaction_emoji);
        TextView tvCount = itemView.findViewById(R.id.rc_tv_reaction_count);
        TextView tvOverflowCount = itemView.findViewById(R.id.rc_tv_reaction_overflow_count);
        View divider = itemView.findViewById(R.id.rc_reaction_divider);

        String emojiUnicode = findEmojiUnicode(reaction.getReactionId());
        tvEmoji.setText(emojiUnicode != null ? emojiUnicode : reaction.getReactionId());
        bindReactionText(itemView, reaction, mode, dp2px(DETAIL_NAME_MAX_WIDTH_DP));
        // 竖线分隔符两种展示模式均显示（2026-06-10 评审裁决）
        divider.setVisibility(View.VISIBLE);

        applyItemBackground(itemView, reaction.isHasCurrentUserReacted());
        applyCountTextColor(tvCount);
        applyCountTextColor(tvOverflowCount);
        itemView.setOnClickListener(v -> {});
        tvEmoji.setOnClickListener(v -> performReactionClick(reaction));
        View.OnClickListener detailClickListener =
                v -> {
                    if (onMoreClickListener != null) {
                        onMoreClickListener.onMoreClick(allReactions, reaction);
                    }
                };
        if (mode == MessageReactionDisplayMode.DETAIL) {
            tvOverflowCount.setOnClickListener(detailClickListener);
        } else {
            tvCount.setOnClickListener(detailClickListener);
        }
        addView(itemView);
    }

    private void performReactionClick(MessageReaction reaction) {
        if (onReactionItemClickListener != null) {
            onReactionItemClickListener.onReactionItemClick(reaction);
        }
    }

    private void applyItemBackground(View itemView, boolean selected) {
        if (selected) {
            itemView.setBackgroundResource(R.drawable.rc_lively_reaction_item_bg_selected);
        } else {
            itemView.setBackgroundResource(R.drawable.rc_lively_reaction_item_bg_normal);
        }
    }

    private void applyCountTextColor(TextView tvCount) {
        tvCount.setTextColor(
                resolveColor(R.attr.rc_text_secondary_color, R.color.rc_edit_reference));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int availableWidth = resolveAvailableWidth(widthMeasureSpec);

        int itemCount = getChildCount();
        int childWidthSpec = MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        MessageReactionDisplayMode mode =
                RongConfigCenter.conversationConfig().getMessageReactionDisplayMode();
        for (int i = 0; i < itemCount; i++) {
            View child = getChildAt(i);
            if (allReactions != null && i < allReactions.size()) {
                bindReactionText(
                        child,
                        allReactions.get(i),
                        mode,
                        calculateLabelMaxWidth(child, availableWidth));
            }
            measureChild(child, childWidthSpec, childHeightSpec);
        }

        LayoutResult result = calculateLayoutResult(itemCount, availableWidth);

        int measuredWidth =
                widthMode == MeasureSpec.EXACTLY
                        ? widthSize
                        : result.maxLineWidth + getPaddingLeft() + getPaddingRight();
        int measuredHeight = result.totalHeight + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    private int resolveAvailableWidth(int widthMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int maxWidth = getResources().getDimensionPixelSize(R.dimen.rc_message_content_max_width);
        if (widthMode == MeasureSpec.UNSPECIFIED || widthSize <= 0) {
            widthSize = maxWidth;
        }
        int availableWidth = Math.min(widthSize, maxWidth);
        return Math.max(0, availableWidth - getPaddingLeft() - getPaddingRight());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int availableWidth = r - l - paddingLeft - getPaddingRight();
        if (availableWidth <= 0) {
            availableWidth = getMeasuredWidth() - paddingLeft - getPaddingRight();
        }
        if (availableWidth <= 0) {
            availableWidth =
                    getResources().getDimensionPixelSize(R.dimen.rc_message_content_max_width);
        }

        int currentX = paddingLeft;
        int currentY = paddingTop;
        int lineHeight = 0;
        int childCount = getChildCount();

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (currentX - paddingLeft + childWidth > availableWidth && currentX > paddingLeft) {
                currentX = paddingLeft;
                currentY += lineHeight + verticalGap;
                lineHeight = 0;
            }
            child.layout(currentX, currentY, currentX + childWidth, currentY + childHeight);
            currentX += childWidth + horizontalGap;
            lineHeight = Math.max(lineHeight, childHeight);
        }
    }

    private void bindReactionText(
            View itemView,
            MessageReaction reaction,
            MessageReactionDisplayMode mode,
            int maxWidth) {
        TextView tvCount = itemView.findViewById(R.id.rc_tv_reaction_count);
        TextView tvOverflowCount = itemView.findViewById(R.id.rc_tv_reaction_overflow_count);

        int count = reaction.getTotalCount();
        if (mode == MessageReactionDisplayMode.DETAIL) {
            NameListText nameListText =
                    buildNameListText(reaction, count, tvCount.getPaint(), maxWidth);
            tvCount.setText(nameListText.nameText);
            if (TextUtils.isEmpty(nameListText.overflowText)) {
                tvCount.setEllipsize(TextUtils.TruncateAt.END);
                tvCount.setMaxWidth(maxWidth);
                resetOverflowTextView(tvOverflowCount);
            } else {
                // Keep the "...N人" suffix out of the ellipsized name TextView.
                tvCount.setEllipsize(null);
                tvCount.setMaxWidth(nameListText.nameMaxWidth);
                tvOverflowCount.setEllipsize(null);
                tvOverflowCount.setMinWidth(nameListText.overflowMinWidth);
                tvOverflowCount.setMinimumWidth(nameListText.overflowMinWidth);
                setTextViewWidth(tvOverflowCount, nameListText.overflowMinWidth);
                tvOverflowCount.setText(nameListText.overflowText);
                tvOverflowCount.setVisibility(View.VISIBLE);
            }
        } else {
            tvCount.setEllipsize(TextUtils.TruncateAt.END);
            tvCount.setMaxWidth(dp2px(DETAIL_NAME_MAX_WIDTH_DP));
            tvCount.setText(formatReactionCount(count));
            resetOverflowTextView(tvOverflowCount);
        }
    }

    private void resetOverflowTextView(TextView tvOverflowCount) {
        tvOverflowCount.setText(null);
        tvOverflowCount.setMinWidth(0);
        tvOverflowCount.setMinimumWidth(0);
        setTextViewWidth(tvOverflowCount, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvOverflowCount.setVisibility(View.GONE);
    }

    private NameListText buildNameListText(
            MessageReaction reaction, int count, TextPaint textPaint, int maxWidth) {
        List<String> names = new ArrayList<>();
        String currentUserId = ReactionUserInfoHelper.getCurrentUserId();
        if (reaction.isHasCurrentUserReacted() && !TextUtils.isEmpty(currentUserId)) {
            names.add(resolveDisplayName(currentUserId));
        }
        List<MessageReactionUser> users = reaction.getUsers();
        if (users == null || users.isEmpty()) {
            if (names.isEmpty()) {
                return new NameListText(formatReactionCount(count), null);
            }
            return buildNameListText(names, count, textPaint, maxWidth);
        }
        for (MessageReactionUser user : users) {
            if (user == null) {
                continue;
            }
            String userId = user.getUserId();
            if (TextUtils.isEmpty(userId)
                    || (!TextUtils.isEmpty(currentUserId) && currentUserId.equals(userId))) {
                continue;
            }
            if (names.size() >= DETAIL_NAME_PREVIEW_LIMIT) {
                break;
            }
            names.add(resolveDisplayName(userId));
        }
        if (names.isEmpty()) {
            return new NameListText(formatReactionCount(count), null);
        }
        return buildNameListText(names, count, textPaint, maxWidth);
    }

    private NameListText buildNameListText(
            List<String> names, int count, TextPaint textPaint, int maxWidth) {
        int show = Math.min(names.size(), DETAIL_NAME_PREVIEW_LIMIT);
        String nameText = joinNames(names, show);
        if (count > show || isTextWiderThan(textPaint, nameText, maxWidth)) {
            String overflowText = formatReactionOverflowCount(count);
            int overflowMinWidth = measureTextWidth(textPaint, overflowText);
            int nameMaxWidth = calculateNameMaxWidth(maxWidth, overflowMinWidth);
            return new NameListText(
                    trimNameTextToWidth(nameText, textPaint, nameMaxWidth),
                    overflowText,
                    nameMaxWidth,
                    overflowMinWidth);
        }
        return new NameListText(nameText, null);
    }

    private String joinNames(List<String> names, int show) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < show; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(names.get(i));
        }
        return sb.toString();
    }

    private boolean isTextWiderThan(TextPaint textPaint, String text, int maxWidth) {
        return textPaint != null
                && !TextUtils.isEmpty(text)
                && maxWidth > 0
                && textPaint.measureText(text) > maxWidth;
    }

    private String trimNameTextToWidth(String text, TextPaint textPaint, int maxWidth) {
        if (TextUtils.isEmpty(text) || maxWidth <= 0) {
            return "";
        }
        if (!isTextWiderThan(textPaint, text, maxWidth)) {
            return text;
        }
        String bestPrefix = "";
        int low = 0;
        int high = text.length();
        while (low <= high) {
            int mid = (low + high) / 2;
            String prefix = normalizeNameTextPrefix(text, mid);
            if (textPaint.measureText(prefix) <= maxWidth) {
                bestPrefix = prefix;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return bestPrefix;
    }

    private int calculateNameMaxWidth(int maxWidth, int overflowMinWidth) {
        return Math.max(0, maxWidth - overflowMinWidth);
    }

    private int measureTextWidth(TextPaint textPaint, String text) {
        if (textPaint == null || TextUtils.isEmpty(text)) {
            return 0;
        }
        return (int) Math.ceil(textPaint.measureText(text)) + dp2px(DETAIL_TEXT_WIDTH_BUFFER_DP);
    }

    private int calculateLabelMaxWidth(View itemView, int availableWidth) {
        if (availableWidth <= 0) {
            return dp2px(DETAIL_NAME_MAX_WIDTH_DP);
        }
        int safeAvailableWidth = Math.max(0, availableWidth - dp2px(DETAIL_ITEM_SAFE_INSET_DP));
        int labelMaxWidth = Math.min(dp2px(DETAIL_NAME_MAX_WIDTH_DP), safeAvailableWidth);
        int chromeWidth = calculateReactionItemChromeWidth(itemView);
        return Math.max(0, Math.min(labelMaxWidth, safeAvailableWidth - chromeWidth));
    }

    private int calculateReactionItemChromeWidth(View itemView) {
        TextView tvEmoji = itemView.findViewById(R.id.rc_tv_reaction_emoji);
        TextView tvCount = itemView.findViewById(R.id.rc_tv_reaction_count);
        View divider = itemView.findViewById(R.id.rc_reaction_divider);

        int width = itemView.getPaddingLeft() + itemView.getPaddingRight();
        width += measureTextWidth(tvEmoji.getPaint(), tvEmoji.getText().toString());
        width += getHorizontalMargins(tvEmoji);
        width += getLayoutWidth(divider);
        width += getHorizontalMargins(divider);
        width += getHorizontalMargins(tvCount);
        return width;
    }

    private int getLayoutWidth(View view) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams != null && layoutParams.width > 0) {
            return layoutParams.width;
        }
        return view.getMeasuredWidth();
    }

    private int getHorizontalMargins(View view) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams =
                    (ViewGroup.MarginLayoutParams) layoutParams;
            return marginLayoutParams.leftMargin + marginLayoutParams.rightMargin;
        }
        return 0;
    }

    private void setTextViewWidth(TextView textView, int width) {
        ViewGroup.LayoutParams layoutParams = textView.getLayoutParams();
        if (layoutParams != null && layoutParams.width != width) {
            layoutParams.width = width;
            textView.setLayoutParams(layoutParams);
        }
    }

    private String normalizeNameTextPrefix(String text, int length) {
        if (TextUtils.isEmpty(text) || length <= 0) {
            return "";
        }
        String prefix = text.substring(0, Math.min(length, text.length())).trim();
        while (prefix.endsWith(",")) {
            prefix = prefix.substring(0, prefix.length() - 1).trim();
        }
        return prefix;
    }

    private String formatReactionCount(int count) {
        return getResources()
                .getString(R.string.rc_reaction_count_format, formatReactionCountText(count));
    }

    private String formatReactionOverflowCount(int count) {
        return getResources()
                .getString(
                        R.string.rc_reaction_name_overflow_format, formatReactionCountText(count));
    }

    private String formatReactionCountText(int count) {
        if (count > MAX_REACTION_COUNT_DISPLAY) {
            return getResources().getString(R.string.rc_reaction_count_overflow);
        }
        return String.valueOf(Math.max(count, 0));
    }

    private String findEmojiUnicode(String reactionId) {
        for (ReactionEmoji emoji : ReactionEmojiProvider.getEmojiList()) {
            if (emoji.getReactionId().equals(reactionId)) {
                return emoji.getUnicode();
            }
        }
        return null;
    }

    private String resolveDisplayName(String userId) {
        return ReactionUserInfoHelper.getDisplayInfo(
                        getContext(), conversationType, targetId, userId, false)
                .name;
    }

    private int dp2px(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    private LayoutResult calculateLayoutResult(int itemCount, int availableWidth) {
        int currentLineWidth = 0;
        int lineCount = 0;
        int totalHeight = 0;
        int lineHeight = 0;
        int maxLineWidth = 0;

        for (int i = 0; i < itemCount; i++) {
            View child = getChildAt(i);
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (currentLineWidth > 0 && currentLineWidth + childWidth > availableWidth) {
                maxLineWidth = Math.max(maxLineWidth, currentLineWidth - horizontalGap);
                totalHeight += lineHeight + verticalGap;
                currentLineWidth = 0;
                lineHeight = 0;
            }
            if (currentLineWidth == 0) {
                lineCount++;
            }
            currentLineWidth += childWidth + horizontalGap;
            lineHeight = Math.max(lineHeight, childHeight);
        }

        if (currentLineWidth > 0) {
            maxLineWidth = Math.max(maxLineWidth, currentLineWidth - horizontalGap);
            totalHeight += lineHeight;
        }
        return new LayoutResult(lineCount, maxLineWidth, totalHeight);
    }

    private int resolveColor(int attrId, int fallbackColorResId) {
        TypedValue typedValue = new TypedValue();
        boolean resolved = getContext().getTheme().resolveAttribute(attrId, typedValue, true);
        if (resolved) {
            if (typedValue.resourceId != 0) {
                return getResources().getColor(typedValue.resourceId);
            }
            return typedValue.data;
        }
        return getResources().getColor(fallbackColorResId);
    }

    private static class LayoutResult {
        final int lineCount;
        final int maxLineWidth;
        final int totalHeight;

        LayoutResult(int lineCount, int maxLineWidth, int totalHeight) {
            this.lineCount = lineCount;
            this.maxLineWidth = maxLineWidth;
            this.totalHeight = totalHeight;
        }
    }

    /** 点击回应详情入口的回调接口。 */
    public interface OnMoreClickListener {
        void onMoreClick(List<MessageReaction> reactions, MessageReaction selectedReaction);
    }

    /** 点击单个回应 item 的回调接口（用于 toggle 添加/移除）。 */
    public interface OnReactionItemClickListener {
        void onReactionItemClick(MessageReaction reaction);
    }

    private static class NameListText {
        final String nameText;
        final String overflowText;
        final int nameMaxWidth;
        final int overflowMinWidth;

        NameListText(String nameText, String overflowText) {
            this(nameText, overflowText, 0, 0);
        }

        NameListText(String nameText, String overflowText, int nameMaxWidth, int overflowMinWidth) {
            this.nameText = nameText;
            this.overflowText = overflowText;
            this.nameMaxWidth = nameMaxWidth;
            this.overflowMinWidth = overflowMinWidth;
        }
    }
}
