package io.rong.imkit.feature.reference;

import android.content.Context;
import android.net.Uri;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imkit.utils.FileTypeUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.QuoteInfo;
import io.rong.imlib.model.UserInfo;
import io.rong.message.FileMessage;
import io.rong.message.ImageMessage;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.ReferenceMessage;
import io.rong.message.RichContentMessage;
import io.rong.message.SightMessage;

/**
 * V2 引用消息卡片 View（对标 iOS RCReferencedContentView）。
 *
 * <p>四种互斥布局模式：
 *
 * <ul>
 *   <li>{@link Mode#INLINE}：左侧 compact 竖线 + 单 TextView 显示 "昵称: 内容"，最多两行。 适用于文本/语音/位置/小视频/未知/已编辑的引用。
 *   <li>{@link Mode#IMAGE_PREVIEW}：顶部 sender 行 + 缩略图。适用于图片且非删除/撤回。
 *   <li>{@link Mode#FILE_CARD}：顶部 sender 行 + 白底带边框文件卡片（icon + name + size）。
 *   <li>{@link Mode#STATUS}：与 INLINE 相同，但内容固定为"已撤回/已删除"，颜色用次级色。
 * </ul>
 *
 * @since 5.36.0
 */
public class QuoteCardView extends LinearLayout {

    /** 布局模式。互斥，由 {@link #applyMode(Mode)} 切换 visibility。 */
    private enum Mode {
        INLINE,
        IMAGE_PREVIEW,
        FILE_CARD,
        STATUS
    }

    private View rootContainer;

    // inline 行（模式 INLINE / STATUS）
    private View inlineRow;
    private View inlineLeftLine;
    private TextView inlineText;

    // header 行（模式 IMAGE_PREVIEW / FILE_CARD）
    private View headerRow;
    private View headerLeftLine;
    private TextView headerSenderName;

    // 缩略图（模式 IMAGE_PREVIEW）
    private ImageView thumbnailIv;

    // 文件卡（模式 FILE_CARD）
    private View fileCard;
    private ImageView fileCardIcon;
    private TextView fileCardName;
    private TextView fileCardSize;
    private int fileCardMinWidthOverridePx;

    private final QuoteCardBindingState bindingState = new QuoteCardBindingState();

    /** 当前绑定的会话信息，用于远端查询 */
    private Conversation.ConversationType conversationType;

    private String targetId;
    private String channelId;

    /** 缓存当前显示用的发送者昵称，用于 inline 模式拼接 "昵称: 内容"。 */
    private String currentSenderDisplayName = "";

    public QuoteCardView(@NonNull Context context) {
        this(context, null);
    }

    public QuoteCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuoteCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.rc_quote_card_view, this, true);
        rootContainer = findViewById(R.id.rc_quote_card_root);

        inlineRow = findViewById(R.id.rc_quote_inline_row);
        inlineLeftLine = findViewById(R.id.rc_quote_inline_left_line);
        inlineText = findViewById(R.id.rc_quote_inline_text);

        headerRow = findViewById(R.id.rc_quote_header_row);
        headerLeftLine = findViewById(R.id.rc_quote_header_left_line);
        headerSenderName = findViewById(R.id.rc_quote_header_sender_name);

        thumbnailIv = findViewById(R.id.rc_quote_thumbnail);

        fileCard = findViewById(R.id.rc_quote_file_card);
        fileCardIcon = findViewById(R.id.rc_quote_file_card_icon);
        fileCardName = findViewById(R.id.rc_quote_file_card_name);
        fileCardSize = findViewById(R.id.rc_quote_file_card_size);

        applyInlineLeftLineMargin();
    }

    /**
     * inline 模式下 leftLine 需垂直居中于首行文字（行高 18dp，中心 9dp 处）， 高度 9dp，故 marginTop = (18 - 9) / 2 = 4.5dp。
     */
    private void applyInlineLeftLineMargin() {
        if (inlineLeftLine == null) {
            return;
        }
        ViewGroup.LayoutParams lp = inlineLeftLine.getLayoutParams();
        if (lp instanceof LinearLayout.LayoutParams) {
            int margin =
                    Math.round(
                            TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_DIP,
                                    4.5f,
                                    getResources().getDisplayMetrics()));
            ((LinearLayout.LayoutParams) lp).topMargin = margin;
            inlineLeftLine.setLayoutParams(lp);
        }
    }

    /**
     * 判断消息是否应该显示 V2 引用卡片。
     *
     * <p>条件：{@code quoteInfo.messageUId} 非空 && 消息内容不是 V1 {@link ReferenceMessage}。
     *
     * <p>不再依赖 {@code RongConfigCenter.featureConfig().isQuoteV2Enable()}：该开关仅控制发送侧是否走 V2 协议，
     * 收到的消息只要带 {@code quoteInfo} 就需要渲染卡片。
     */
    public static boolean shouldShowQuoteCard(@Nullable Message message) {
        if (message == null) {
            return false;
        }
        QuoteInfo quoteInfo = message.getQuoteInfo();
        if (quoteInfo == null || TextUtils.isEmpty(quoteInfo.getMessageUId())) {
            return false;
        }
        return !(message.getContent() instanceof ReferenceMessage);
    }

    static boolean shouldQueryQuotedMessage(@Nullable QuoteInfo quoteInfo) {
        return quoteInfo != null
                && quoteInfo.getMessageUId() != null
                && quoteInfo.getMessageUId().length() > 0
                && quoteInfo.getQuoteMessageStatus() == QuoteInfo.QuoteMessageStatus.DEFAULT;
    }

    /** 绑定消息数据并触发被引用消息查询。 需在 RecyclerView.onBindViewHolder 中调用。 */
    public void setMessage(@NonNull Message message) {
        setMessage(message, false);
    }

    /**
     * 绑定消息数据并触发被引用消息查询。
     *
     * @param forceReload true 时忽略相同 quoted uid 的缓存结果，重新查询被引用消息状态。
     */
    public void setMessage(@NonNull Message message, boolean forceReload) {
        QuoteInfo quoteInfo = message.getQuoteInfo();
        if (quoteInfo == null) {
            return;
        }

        String quotedMsgUId = quoteInfo.getMessageUId();
        this.conversationType = message.getConversationType();
        this.targetId = message.getTargetId();
        this.channelId = message.getChannelId();

        // 先用 quoteInfo 中的 senderId 展示昵称
        currentSenderDisplayName =
                resolveDisplayName(
                        quoteInfo.getSenderId(),
                        message.getConversationType(),
                        message.getTargetId(),
                        null);

        bindingState.bind(quotedMsgUId);
        QuoteMessageBatchLoader.Result result =
                QuoteMessageBatchLoader.getInstance().getResult(message);
        renderBatchResult(result);
    }

    /** 获取已查到的被引用消息，可能为 null（加载中/不可用）。 */
    @Nullable
    public Message getQuotedMessage() {
        return bindingState.getQuotedMessage();
    }

    /**
     * V1 路径：直接根据 {@link ReferenceMessage} 自带的本地引用内容渲染卡片，不查询 DB。
     *
     * <p>这样 V1 引用消息 cell 与 V2 引用卡片视觉一致，对齐 iOS 的"两端共用 RCReferencedContentView"。
     */
    public void setReferenceMessage(@NonNull Message wrappingMessage) {
        if (!(wrappingMessage.getContent() instanceof ReferenceMessage)) {
            return;
        }
        ReferenceMessage refMsg = (ReferenceMessage) wrappingMessage.getContent();
        // 同一条 V1 消息复用，避免重复绑定
        String syntheticUid = "v1:" + wrappingMessage.getMessageId();
        if (bindingState.shouldReuse(syntheticUid, false)) {
            return;
        }
        bindingState.bind(syntheticUid);
        this.conversationType = wrappingMessage.getConversationType();
        this.targetId = wrappingMessage.getTargetId();
        this.channelId = wrappingMessage.getChannelId();

        MessageContent referContent = refMsg.getReferenceContent();
        UserInfo embeddedUserInfo = referContent != null ? referContent.getUserInfo() : null;
        currentSenderDisplayName =
                resolveDisplayName(
                        refMsg.getUserId(), conversationType, targetId, embeddedUserInfo);

        ReferenceMessage.ReferenceMessageStatus status = refMsg.getReferMsgStatus();
        if (status == ReferenceMessage.ReferenceMessageStatus.DELETE) {
            applyMode(Mode.STATUS);
            applyAccentColor(true);
            setInlineDisplayText(
                    currentSenderDisplayName,
                    getContext()
                            .getString(QuoteCardStatusTextResolver.resolveUnavailableTextResId()));
            return;
        }
        if (status == ReferenceMessage.ReferenceMessageStatus.RECALLED) {
            applyMode(Mode.STATUS);
            applyAccentColor(true);
            setInlineDisplayText(
                    currentSenderDisplayName,
                    getContext().getString(QuoteCardStatusTextResolver.resolveRecalledTextResId()));
            return;
        }

        renderLocalContent(referContent, null);
    }

    private void showNormalLoadingState() {
        applyMode(Mode.INLINE);
        applyAccentColor(false);
        setInlineDisplayText(
                currentSenderDisplayName,
                getContext().getString(QuoteCardStatusTextResolver.resolveLoadingTextResId()));
        if (thumbnailIv != null) {
            Glide.with(getContext()).clear(thumbnailIv);
            thumbnailIv.setImageDrawable(null);
        }
        if (fileCardIcon != null) fileCardIcon.setImageDrawable(null);
        if (fileCardName != null) fileCardName.setText("");
        if (fileCardSize != null) fileCardSize.setText("");
    }

    /** 重置为空白状态（RecyclerView 复用时调用）。 */
    public void reset() {
        bindingState.reset();
        conversationType = null;
        targetId = null;
        channelId = null;
        currentSenderDisplayName = "";
        if (inlineText != null) inlineText.setText("");
        if (headerSenderName != null) headerSenderName.setText("");
        if (fileCardName != null) fileCardName.setText("");
        if (fileCardSize != null) fileCardSize.setText("");
        if (thumbnailIv != null) {
            thumbnailIv.setVisibility(GONE);
            thumbnailIv.setImageDrawable(null);
        }
        if (fileCardIcon != null) fileCardIcon.setImageDrawable(null);
        fileCardMinWidthOverridePx = 0;
        applyMode(Mode.INLINE);
    }

    /** 文件消息本体带引用时，引用文件卡需要与下方文件卡保持同宽。 */
    public void setFileCardMinWidthOverride(int widthPx) {
        int newWidth = Math.max(0, widthPx);
        if (fileCardMinWidthOverridePx == newWidth) {
            return;
        }
        fileCardMinWidthOverridePx = newWidth;
        if (fileCard != null && fileCard.getVisibility() == VISIBLE) {
            applyFileCardWidth(
                    fileCardName != null ? safe(fileCardName.getText().toString()) : "",
                    fileCardSize != null ? safe(fileCardSize.getText().toString()) : "");
        }
    }

    // ---- 渲染被引用消息内容 ----

    private void renderBatchResult(@NonNull QuoteMessageBatchLoader.Result result) {
        switch (result.getState()) {
            case LOADED:
                applyQuotedMessageInternal(result.getMessage());
                break;
            case RECALLED:
                showStatusText(QuoteCardStatusTextResolver.resolveRecalledTextResId(), true);
                bindingState.markUnavailable();
                break;
            case DELETED:
                showStatusText(QuoteCardStatusTextResolver.resolveUnavailableTextResId(), true);
                bindingState.markUnavailable();
                break;
            case FAILED:
                showStatusText(QuoteCardStatusTextResolver.resolveFailedTextResId(), true);
                bindingState.markUnavailable();
                break;
            case LOADING:
            default:
                showNormalLoadingState();
                bindingState.markUnavailable();
                break;
        }
    }

    private void applyQuotedMessageInternal(Message quotedMsg) {
        if (quotedMsg == null || quotedMsg.getContent() == null) {
            showStatusText(QuoteCardStatusTextResolver.resolveUnavailableTextResId(), true);
            bindingState.markUnavailable();
            return;
        }
        bindingState.setQuotedMessage(quotedMsg);

        // 更新发送者昵称（用完整 UserInfo）
        currentSenderDisplayName =
                resolveDisplayName(
                        quotedMsg.getSenderUserId(),
                        quotedMsg.getConversationType() != null
                                ? quotedMsg.getConversationType()
                                : conversationType,
                        quotedMsg.getTargetId() != null ? quotedMsg.getTargetId() : targetId,
                        quotedMsg.getContent() != null
                                ? quotedMsg.getContent().getUserInfo()
                                : null);

        // 已撤回
        if (quotedMsg.getContent() instanceof RecallNotificationMessage) {
            applyMode(Mode.STATUS);
            applyAccentColor(true);
            setInlineDisplayText(
                    currentSenderDisplayName,
                    getContext().getString(QuoteCardStatusTextResolver.resolveRecalledTextResId()));
            requestParentRelayout();
            return;
        }

        // 按类型渲染
        renderContent(quotedMsg);
    }

    private void showStatusText(int textResId, boolean isErrorStatus) {
        applyMode(Mode.STATUS);
        applyAccentColor(isErrorStatus);
        setInlineDisplayText(currentSenderDisplayName, getContext().getString(textResId));
        requestParentRelayout();
    }

    // ---- 渲染被引用消息内容 ----

    private void renderContent(Message quotedMsg) {
        renderLocalContent(quotedMsg.getContent(), quotedMsg.getObjectName());
    }

    /** V1/V2 共用的内容渲染逻辑，仅依赖 {@link MessageContent} 自身。 */
    private void renderLocalContent(@Nullable MessageContent content, @Nullable String objectName) {
        if (content == null) {
            applyMode(Mode.INLINE);
            applyAccentColor(false);
            setInlineDisplayText(
                    currentSenderDisplayName, getContext().getString(R.string.rc_message_unknown));
            return;
        }
        if (content instanceof ImageMessage) {
            ImageMessage imgMsg = (ImageMessage) content;
            applyMode(Mode.IMAGE_PREVIEW);
            applyAccentColor(false);
            setHeaderSenderName(currentSenderDisplayName);
            showThumbnail(imgMsg.getThumUri());
        } else if (content instanceof SightMessage) {
            applyMode(Mode.INLINE);
            applyAccentColor(false);
            setInlineDisplayText(
                    currentSenderDisplayName,
                    resolveInlineSummaryText(
                            content, TextUtils.isEmpty(objectName) ? "RC:SightMsg" : objectName));
        } else if (content instanceof FileMessage) {
            FileMessage fileMsg = (FileMessage) content;
            applyMode(Mode.FILE_CARD);
            applyAccentColor(false);
            setHeaderSenderName(currentSenderDisplayName);
            applyFileCard(fileMsg);
        } else if (content instanceof RichContentMessage) {
            RichContentMessage richMsg = (RichContentMessage) content;
            String text =
                    getContext().getString(R.string.rc_message_content_rich_text)
                            + " "
                            + safe(richMsg.getTitle());
            applyMode(Mode.INLINE);
            applyAccentColor(false);
            setInlineDisplayText(currentSenderDisplayName, sanitizePreviewText(text));
        } else {
            // 文本、语音、位置、合并转发、流式等：使用通用摘要拼接成 inline 文本
            applyMode(Mode.INLINE);
            applyAccentColor(false);
            setInlineDisplayText(
                    currentSenderDisplayName, resolveInlineSummaryText(content, objectName));
        }
    }

    private String resolveInlineSummaryText(
            @NonNull MessageContent content, @Nullable String objectName) {
        Spannable summary =
                RongConfigCenter.conversationConfig().getMessageSummary(getContext(), content);
        if (summary != null && summary.length() > 0) {
            return sanitizePreviewText(summary.toString());
        }
        return fallbackPreviewText(objectName);
    }

    private void showThumbnail(Uri thumbUri) {
        if (thumbnailIv == null) {
            return;
        }
        if (thumbUri != null) {
            int thumbnailSize = getDimenPx(R.dimen.rc_quote_v2_image_thumbnail_size);
            ViewGroup.LayoutParams layoutParams = thumbnailIv.getLayoutParams();
            if (layoutParams != null
                    && (layoutParams.width != thumbnailSize
                            || layoutParams.height != thumbnailSize)) {
                layoutParams.width = thumbnailSize;
                layoutParams.height = thumbnailSize;
                thumbnailIv.setLayoutParams(layoutParams);
            }
            thumbnailIv.setVisibility(VISIBLE);
            Glide.with(getContext())
                    .load(thumbUri)
                    .apply(
                            RequestOptions.bitmapTransform(new RoundedCorners(6))
                                    .override(thumbnailSize, thumbnailSize))
                    .dontAnimate()
                    .into(thumbnailIv);
        } else {
            thumbnailIv.setVisibility(GONE);
            thumbnailIv.setImageDrawable(null);
        }
    }

    private void applyFileCard(FileMessage fileMessage) {
        if (fileCardIcon != null) {
            int iconResId =
                    FileTypeUtils.fileTypeImageId(
                            getContext(),
                            fileMessage.getName() != null ? fileMessage.getName() : "");
            if (iconResId != 0) {
                fileCardIcon.setImageResource(iconResId);
            } else {
                fileCardIcon.setImageDrawable(null);
            }
        }
        if (fileCardName != null) {
            fileCardName.setText(safe(fileMessage.getName()));
        }
        String fileSizeText = FileTypeUtils.formatFileSize(fileMessage.getSize());
        if (fileCardSize != null) {
            fileCardSize.setText(fileSizeText);
        }
        applyFileCardWidth(safe(fileMessage.getName()), fileSizeText);
    }

    /** iOS 的文件引用卡宽度由发送者行、文件名、文件大小三者中最长的内容决定， 仅在超过最大宽度时才取最大值。 */
    private void applyFileCardWidth(@NonNull String fileName, @NonNull String fileSize) {
        if (fileCard == null) {
            return;
        }
        int width = resolveFileCardWidth(fileName, fileSize);
        ViewGroup.LayoutParams lp = fileCard.getLayoutParams();
        if (lp == null) {
            return;
        }
        int height = getDimenPx(R.dimen.rc_quote_v2_file_card_height);
        if (lp.width == width && lp.height == height) {
            return;
        }
        lp.width = width;
        lp.height = height;
        fileCard.setLayoutParams(lp);
    }

    private int resolveFileCardWidth(@NonNull String fileName, @NonNull String fileSize) {
        int maxWidth = getQuoteContentMaxWidth();
        if (maxWidth <= 0) {
            return ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        int minWidth =
                Math.min(
                        Math.max(
                                getDimenPx(R.dimen.rc_quote_v2_file_card_min_width),
                                fileCardMinWidthOverridePx),
                        maxWidth);
        int horizontalPadding = getDimenPx(R.dimen.rc_quote_v2_file_card_padding);
        int iconSize = getDimenPx(R.dimen.rc_quote_v2_file_card_icon_size);
        int iconTextSpacing = getDimenPx(R.dimen.rc_quote_v2_file_card_icon_text_spacing);
        int textMaxWidth =
                Math.max(0, maxWidth - horizontalPadding * 2 - iconSize - iconTextSpacing);

        int fileNameWidth = measureTextWidth(fileCardName, fileName);
        int fileSizeWidth = measureTextWidth(fileCardSize, fileSize);
        int fileTextWidth = Math.min(Math.max(fileNameWidth, fileSizeWidth), textMaxWidth);
        int filePreviewWidth = horizontalPadding * 2 + iconSize + iconTextSpacing + fileTextWidth;
        int senderWidth = resolveHeaderWidth();

        return clamp(Math.max(filePreviewWidth, senderWidth), minWidth, maxWidth);
    }

    private int resolveHeaderWidth() {
        if (headerSenderName == null) {
            return 0;
        }
        return getDimenPx(R.dimen.rc_quote_v2_inline_left_line_width)
                + getDimenPx(R.dimen.rc_quote_v2_inline_left_line_margin_end)
                + measureTextWidth(headerSenderName, headerSenderName.getText());
    }

    private int getQuoteContentMaxWidth() {
        return getDimenPx(R.dimen.rc_file_item_width);
    }

    private int getDimenPx(int dimenResId) {
        return getResources().getDimensionPixelSize(dimenResId);
    }

    private static int measureTextWidth(@Nullable TextView textView, @Nullable CharSequence text) {
        if (textView == null || TextUtils.isEmpty(text)) {
            return 0;
        }
        return (int) Math.ceil(textView.getPaint().measureText(text.toString()));
    }

    private static int clamp(int value, int minValue, int maxValue) {
        return Math.min(Math.max(value, minValue), maxValue);
    }

    /** 切换布局模式，互斥地切换各子节点 visibility。 */
    private void applyMode(Mode mode) {
        if (inlineRow != null) {
            inlineRow.setVisibility((mode == Mode.INLINE || mode == Mode.STATUS) ? VISIBLE : GONE);
        }
        if (headerRow != null) {
            headerRow.setVisibility(
                    (mode == Mode.IMAGE_PREVIEW || mode == Mode.FILE_CARD) ? VISIBLE : GONE);
        }
        if (thumbnailIv != null) {
            thumbnailIv.setVisibility(mode == Mode.IMAGE_PREVIEW ? VISIBLE : GONE);
        }
        if (fileCard != null) {
            fileCard.setVisibility(mode == Mode.FILE_CARD ? VISIBLE : GONE);
        }
    }

    /**
     * 调整重点元素的强调色：
     *
     * <ul>
     *   <li>非状态：使用 {@code rc_reference_title_color}（深主文色，对应 iOS 0x020814）。
     *   <li>状态（撤回/删除/不可用）：使用 {@code rc_secondary_color}（次级灰）。
     * </ul>
     */
    private void applyAccentColor(boolean isStatus) {
        int color =
                ContextCompat.getColor(
                        getContext(),
                        isStatus ? R.color.rc_secondary_color : R.color.rc_reference_title_color);
        if (inlineLeftLine != null) inlineLeftLine.setBackgroundColor(color);
        if (headerLeftLine != null) headerLeftLine.setBackgroundColor(color);
        if (inlineText != null) inlineText.setTextColor(color);
        if (headerSenderName != null) headerSenderName.setTextColor(color);
    }

    /** inline 模式拼接 "昵称: 内容"，RTL 下颠倒。 */
    private void setInlineDisplayText(@Nullable String senderName, @Nullable String previewText) {
        if (inlineText == null) {
            return;
        }
        String name = safe(senderName);
        String text = safe(previewText);
        String display;
        if (name.length() == 0) {
            display = text;
        } else if (text.length() == 0) {
            display = name;
        } else if (isRtl()) {
            display = text + " :" + name;
        } else {
            display = name + ": " + text;
        }
        inlineText.setText(display);
    }

    private void setHeaderSenderName(@Nullable String senderName) {
        if (headerSenderName == null) {
            return;
        }
        String name = safe(senderName);
        if (name.length() == 0) {
            headerSenderName.setText("");
            return;
        }
        headerSenderName.setText(isRtl() ? ":" + name : name + ":");
    }

    private boolean isRtl() {
        return getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }

    /** 去除换行符，避免 inline 拼接出多行。 */
    private static String sanitizePreviewText(String text) {
        if (text == null) return "";
        return text.replace("\r\n", " ").replace("\n", " ").replace("\r", " ");
    }

    private static String safe(@Nullable String s) {
        return s == null ? "" : s;
    }

    private String fallbackPreviewText(@Nullable String objectName) {
        if (TextUtils.isEmpty(objectName)) {
            return getContext().getString(R.string.rc_message_unknown);
        }
        int resId = getFallbackResId(objectName);
        if (resId != 0) {
            return getContext().getString(resId);
        }
        return getContext().getString(R.string.rc_message_unknown);
    }

    private static int getFallbackResId(String objectName) {
        if ("RC:ImgMsg".equals(objectName)) {
            return R.string.rc_message_content_image;
        } else if ("RC:VcMsg".equals(objectName) || "RC:HQVCMsg".equals(objectName)) {
            return R.string.rc_message_content_voice;
        } else if ("RC:SightMsg".equals(objectName)) {
            return R.string.rc_message_content_sight;
        } else if ("RC:FileMsg".equals(objectName)) {
            return R.string.rc_message_content_file;
        } else if ("RC:LBSMsg".equals(objectName)) {
            return R.string.rc_message_content_location;
        } else if ("RC:ImgTextMsg".equals(objectName)) {
            return R.string.rc_message_content_rich_text;
        }
        return 0;
    }

    // ---- 发送者昵称解析 ----

    /**
     * 解析发送者昵称：
     *
     * <ol>
     *   <li>用户信息托管模式 + senderId 与消息内容携带的 UserInfo 一致 → 使用消息内 UserInfo。
     *   <li>群会话 → 优先群昵称。
     *   <li>普通会话 → UserInfoCache。
     *   <li>兜底 → senderId 字符串。
     * </ol>
     */
    private String resolveDisplayName(
            @Nullable String senderId,
            @Nullable Conversation.ConversationType convType,
            @Nullable String tId,
            @Nullable UserInfo contentUserInfo) {
        if (TextUtils.isEmpty(senderId)) {
            return "";
        }

        boolean isInfoManagement =
                RongUserInfoManager.getInstance().getDataSourceType()
                        == RongUserInfoManager.DataSourceType.INFO_MANAGEMENT;
        UserInfo userInfo;
        if (isInfoManagement
                && contentUserInfo != null
                && senderId.equals(contentUserInfo.getUserId())) {
            userInfo = contentUserInfo;
        } else {
            userInfo = RongUserInfoManager.getInstance().getUserInfo(senderId);
        }

        String groupMemberName = "";
        if (convType == Conversation.ConversationType.GROUP && !TextUtils.isEmpty(tId)) {
            GroupUserInfo groupUserInfo =
                    RongUserInfoManager.getInstance().getGroupUserInfo(tId, senderId);
            if (groupUserInfo != null && !TextUtils.isEmpty(groupUserInfo.getNickname())) {
                groupMemberName = groupUserInfo.getNickname();
            }
        }

        String displayName =
                RongUserInfoManager.getInstance().getUserDisplayName(userInfo, groupMemberName);
        if (TextUtils.isEmpty(displayName)) {
            displayName = senderId;
        }
        return displayName;
    }

    // ---- 工具方法 ----

    /**
     * 状态文本（撤回/删除）切换后，通知父容器重新测量布局。
     *
     * <p>初始化时使用 INLINE 兜底文案，异步回调后切换到 STATUS/IMAGE_PREVIEW/FILE_CARD 等模式， 此时需要让 wrapper
     * 重新测量以适应新内容尺寸。
     */
    private void requestParentRelayout() {
        // 先让 QuoteCardView 自己重新测量
        requestLayout();
        // 强制让父容器（wrapper）重新测量，并清除其宽度缓存
        ViewParent parent = getParent();
        if (parent instanceof View) {
            View parentView = (View) parent;
            // 强制清除父容器的测量缓存
            parentView.forceLayout();
            // 请求重新布局
            parentView.requestLayout();
        }
    }
}
