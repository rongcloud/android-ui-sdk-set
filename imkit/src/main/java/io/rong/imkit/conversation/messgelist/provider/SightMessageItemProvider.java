package io.rong.imkit.conversation.messgelist.provider;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.feature.reference.QuoteCardView;
import io.rong.imkit.feature.resend.ResendManager;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.picture.tools.ScreenUtils;
import io.rong.imkit.utils.RongOperationPermissionUtils;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imkit.widget.CircleProgressView;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.SightMessage;
import java.io.File;
import java.util.List;

public class SightMessageItemProvider extends BaseMessageItemProvider<SightMessage> {

    private Integer minShortSideSize;

    @Override
    protected int getQuoteCardWrapperMinWidth(Context context) {
        // 小视频消息本身足够宽，不需要额外最小宽度撑大气泡
        return 0;
    }

    public SightMessageItemProvider() {
        mConfig.showReadState = true;
        mConfig.showContentBubble = false;
        mConfig.showProgress = false;
    }

    static int resolveSightRootGravity(boolean hasQuoteCard, boolean isSender) {
        return hasQuoteCard ? Gravity.START : -1;
    }

    static int resolveSightRootMargin(boolean hasQuoteCard, int quoteLineStartMarginPx) {
        return hasQuoteCard ? quoteLineStartMarginPx : 0;
    }

    static int resolveSightRootEndMargin(boolean hasQuoteCard, int quoteLineEndMarginPx) {
        return hasQuoteCard ? quoteLineEndMarginPx : 0;
    }

    static int resolveSightRootBottomMargin(boolean hasQuoteCard, int quoteLineBottomMarginPx) {
        return hasQuoteCard ? quoteLineBottomMarginPx : 0;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.rc_item_sight_message, parent, false);
        return new ViewHolder(view.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(
            ViewHolder holder,
            ViewHolder parentHolder,
            SightMessage sightMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        int progress = uiMessage.getProgress();
        final Message.SentStatus status = uiMessage.getMessage().getSentStatus();
        holder.setVisible(R.id.rc_sight_thumb, true);
        Uri thumbUri = sightMessage.getThumbUri();
        if (thumbUri != null && thumbUri.getPath() != null) {
            final ImageView imageView = holder.getView(R.id.rc_sight_thumb);
            final ImageView readyButton = holder.getView(R.id.rc_sight_tag);
            if (!checkViewsValid(imageView)) {
                RLog.e(TAG, "checkViewsValid error," + uiMessage.getObjectName());
                return;
            }
            // 设置图片圆角角度
            RoundedCorners roundedCorners =
                    new RoundedCorners(ScreenUtils.dip2px(holder.getContext(), 6));
            RequestOptions options =
                    RequestOptions.bitmapTransform(roundedCorners).override(300, 300);
            Glide.with(imageView)
                    .load(new File(thumbUri.getPath()))
                    .apply(options)
                    .listener(
                            new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(
                                        @Nullable GlideException e,
                                        Object model,
                                        Target<Drawable> target,
                                        boolean isFirstResource) {
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(
                                        Drawable resource,
                                        Object model,
                                        Target<Drawable> target,
                                        DataSource dataSource,
                                        boolean isFirstResource) {

                                    measureLayoutParams(imageView, readyButton, resource);
                                    return false;
                                }
                            })
                    .into(imageView);
        }
        holder.setText(R.id.rc_sight_duration, getSightDuration(sightMessage.getDuration()));
        CircleProgressView loadingProgress = holder.getView(R.id.rc_sight_progress);
        ProgressBar compressProgress = holder.getView(R.id.compressVideoBar);
        if (!checkViewsValid(loadingProgress, compressProgress)) {
            RLog.e(TAG, "checkViewsValid error," + uiMessage.getObjectName());
            return;
        }
        if (progress > 0 && progress < 100) {
            loadingProgress.setProgress(progress, true);
            holder.setVisible(R.id.rc_sight_tag, false);
            loadingProgress.setVisibility(View.VISIBLE);
            compressProgress.setVisibility(View.GONE);
            // 发送小视频时取消发送，这个功能暂时未打开
            // handleSendingView(message, holder);
        } else if (status.equals(Message.SentStatus.SENDING)) {
            holder.setVisible(R.id.rc_sight_tag, false);
            loadingProgress.setVisibility(View.GONE);
            compressProgress.setVisibility(View.VISIBLE);
        } else if (status.equals(Message.SentStatus.FAILED)
                && ResendManager.getInstance().needResend(uiMessage.getMessage().getMessageId())) {
            holder.setVisible(R.id.rc_sight_tag, false);
            loadingProgress.setVisibility(View.GONE);
            compressProgress.setVisibility(View.VISIBLE);
        } else {
            holder.setVisible(R.id.rc_sight_tag, true);
            loadingProgress.setVisibility(View.GONE);
            compressProgress.setVisibility(View.GONE);
            // handleSendingView(message, holder);
        }

        // 有引用卡片时给视频容器添加内边距，并与引用卡片左侧对齐
        boolean hasQuoteCard = QuoteCardView.shouldShowQuoteCard(uiMessage.getMessage());
        boolean isSender =
                uiMessage.getMessage().getMessageDirection() == Message.MessageDirection.SEND;
        View sightRoot = holder.itemView;
        ViewGroup.LayoutParams lp = sightRoot.getLayoutParams();
        if (lp instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) lp;
            if (hasQuoteCard) {
                int bodySpacing = ScreenUtils.dip2px(sightRoot.getContext(), 6);
                int startMargin =
                        resolveSightRootMargin(
                                true,
                                sightRoot
                                        .getResources()
                                        .getDimensionPixelSize(
                                                R.dimen.rc_quote_v2_card_horizontal_margin));
                int endMargin =
                        resolveSightRootEndMargin(
                                true,
                                sightRoot
                                        .getResources()
                                        .getDimensionPixelSize(
                                                R.dimen.rc_quote_v2_card_horizontal_margin));
                int bottomMargin =
                        resolveSightRootBottomMargin(
                                true,
                                sightRoot
                                        .getResources()
                                        .getDimensionPixelSize(
                                                R.dimen.rc_quote_v2_card_horizontal_margin));
                llp.setMargins(startMargin, bodySpacing, endMargin, bottomMargin);
                llp.setMarginStart(startMargin);
                llp.setMarginEnd(endMargin);
                llp.gravity = resolveSightRootGravity(true, isSender);
            } else {
                llp.setMargins(0, 0, 0, 0);
                llp.setMarginStart(0);
                llp.setMarginEnd(0);
                llp.gravity = resolveSightRootGravity(false, isSender);
            }
            sightRoot.setLayoutParams(llp);
        }
    }

    @Override
    protected boolean onItemClick(
            ViewHolder holder,
            SightMessage sightMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        if (sightMessage != null) {
            if (!RongOperationPermissionUtils.isMediaOperationPermit(holder.getContext())) {
                return true;
            }

            // KNOTE: 2021/8/24  点击进入SightPlayerActivity下载播放,下载保存目录是应用私有目录  不需要存储权限
            //            String[] permissions = {
            //                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            //            };
            //            if (!PermissionCheckUtil.checkPermissions(holder.getContext(),
            // permissions)) {
            //                Activity activity = (Activity) holder.getContext();
            //                PermissionCheckUtil.requestPermissions(activity, permissions, 100);
            //                return true;
            //            }

            Uri.Builder builder = new Uri.Builder();
            builder.scheme("rong")
                    .authority(holder.getContext().getPackageName())
                    .appendPath("sight")
                    .appendPath("player");
            String intentUrl = builder.build().toString();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(intentUrl));
            intent.setPackage(holder.getContext().getPackageName());
            intent.putExtra("SightMessage", sightMessage);
            intent.putExtra("Message", uiMessage.getMessage());
            intent.putExtra("Progress", uiMessage.getProgress());
            if (intent.resolveActivity(holder.getContext().getPackageManager()) != null) {
                holder.getContext().startActivity(intent);
            } else {
                ToastUtils.show(
                        holder.getContext(), "Sight Module does not exist.", Toast.LENGTH_SHORT);
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof SightMessage && !messageContent.isDestruct();
    }

    private void measureLayoutParams(View view, ImageView readyButton, Drawable drawable) {
        float width = drawable.getIntrinsicWidth();
        float height = drawable.getIntrinsicHeight();
        int finalWidth;
        int finalHeight;
        int minSize = 100;
        if (minShortSideSize == null) {
            minShortSideSize = ScreenUtils.dip2px(view.getContext(), 140);
        }
        if (minShortSideSize > 0) {
            if (width >= minShortSideSize || height >= minShortSideSize) {
                float scale = width / height;

                if (scale > 1) {
                    finalHeight = (int) (minShortSideSize / scale);
                    if (finalHeight < minSize) {
                        finalHeight = minSize;
                    }
                    finalWidth = (int) minShortSideSize;
                } else {
                    finalHeight = (int) minShortSideSize;
                    finalWidth = (int) (minShortSideSize * scale);
                    if (finalWidth < minSize) {
                        finalWidth = minSize;
                    }
                }

                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.height = finalHeight;
                params.width = finalWidth;
                view.setLayoutParams(params);
                measureReadyButton(readyButton, drawable, finalWidth, finalHeight);
            } else {
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.height = (int) height;
                params.width = (int) width;
                view.setLayoutParams(params);
                measureReadyButton(readyButton, drawable, width, height);
            }
        }
    }

    private void measureReadyButton(
            ImageView readyButton, Drawable drawable, float finalWidth, float finalHeight) {
        if (readyButton == null || drawable == null) {
            return;
        }
        int intrinsicHeight = drawable.getIntrinsicHeight();
        int intrinsicWidth = drawable.getIntrinsicWidth();
        if (intrinsicHeight == 0 || intrinsicWidth == 0 || finalHeight == 0 || finalWidth == 0) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = readyButton.getLayoutParams();
        int readyButtonSize;
        if ((intrinsicWidth / (finalWidth * 1.0)) > (intrinsicHeight / (finalHeight * 1.0))) {
            readyButtonSize = (int) (finalHeight * (intrinsicHeight / (intrinsicWidth * 1.0)));
        } else {
            readyButtonSize = (int) (finalWidth * (intrinsicWidth / (intrinsicHeight * 1.0)));
        }
        int min =
                Math.min(
                        readyButtonSize,
                        readyButton
                                .getResources()
                                .getDimensionPixelSize(R.dimen.rc_sight_play_size));
        layoutParams.width = min;
        layoutParams.height = min;
        readyButton.setLayoutParams(layoutParams);
    }

    private String getSightDuration(int time) {
        String recordTime;
        int hour, minute, second;
        if (time <= 0) {
            return "00:00";
        } else {
            minute = time / 60;
            if (minute < 60) {
                second = time % 60;
                recordTime = unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99) {
                    return "99:59:59";
                }
                minute = minute % 60;
                second = time - hour * 3600 - minute * 60;
                recordTime = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
            }
        }
        return recordTime;
    }

    private String unitFormat(int time) {
        String formatTime;
        if (time >= 0 && time < 10) {
            formatTime = "0" + time;
        } else {
            formatTime = "" + time;
        }
        return formatTime;
    }

    @Override
    public Spannable getSummarySpannable(Context context, SightMessage sightMessage) {
        return new SpannableString(
                context.getString(R.string.rc_conversation_summary_content_sight));
    }
}
