package io.rong.imkit.conversation.messgelist.provider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.activity.GIFPreviewActivity;
import io.rong.imkit.config.ConversationConfig;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.feature.resend.ResendManager;
import io.rong.imkit.model.State;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.picture.tools.ScreenUtils;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.widget.CircleProgressView;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.GIFMessage;

public class GIFMessageItemProvider extends BaseMessageItemProvider<GIFMessage> {

    private Integer minSize = null;
    private Integer maxSize = null;

    public GIFMessageItemProvider() {
        mConfig.showProgress = false;
        mConfig.showContentBubble = false;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_item_gif_message, parent, false);
        return new ViewHolder(view.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(ViewHolder holder, ViewHolder parentHolder, GIFMessage gifMessage, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        final ImageView imageView = holder.getView(R.id.rc_img);
        measureLayoutParams(imageView, gifMessage.getWidth(), gifMessage.getHeight());
        CircleProgressView loadingProgress = holder.getView(R.id.rc_gif_progress);
        loadingProgress.setVisibility(View.GONE);
        holder.setVisible(R.id.rc_download_failed, false);
        holder.setVisible(R.id.rc_start_download, false);
        holder.setVisible(R.id.rc_pre_progress, false);
        holder.setVisible(R.id.rc_length, false);
        int progress = uiMessage.getProgress();
        if (uiMessage.getMessage().getMessageDirection() == Message.MessageDirection.SEND) {
            if ((progress > 0 && progress < 100)
                    || (uiMessage.getState() == State.ERROR) && ResendManager.getInstance().needResend(uiMessage.getMessage().getMessageId())) {
                loadingProgress.setProgress(progress, true);
                loadingProgress.setVisibility(View.VISIBLE);
                holder.setVisible(R.id.rc_pre_progress, false);
            } else if (uiMessage.getState() == State.PROGRESS) {
                loadingProgress.setVisibility(View.GONE);
            } else if (progress == -1) {
                loadingProgress.setVisibility(View.GONE);
                holder.setVisible(R.id.rc_pre_progress, false);
                holder.setVisible(R.id.rc_download_failed, true);
                holder.setVisible(R.id.rc_length, true);
            } else {
                loadingProgress.setVisibility(View.GONE);
                holder.setVisible(R.id.rc_pre_progress, false);
            }
        } else {
            if (uiMessage.getMessage().getReceivedStatus().isDownload()) {
                if (progress > 0 && progress < 100) {
                    loadingProgress.setProgress(progress, true);
                    loadingProgress.setVisibility(View.VISIBLE);
                    holder.setVisible(R.id.rc_pre_progress, false);
                    holder.setVisible(R.id.rc_start_download, false);
                } else if (progress == 100) {
                    loadingProgress.setVisibility(View.GONE);
                    holder.setVisible(R.id.rc_pre_progress, false);
                    holder.setVisible(R.id.rc_length, false);
                    holder.setVisible(R.id.rc_start_download, false);
                } else if (progress == -1) {
                    loadingProgress.setVisibility(View.GONE);
                    holder.setVisible(R.id.rc_pre_progress, false);
                    holder.setVisible(R.id.rc_download_failed, true);
                    holder.setVisible(R.id.rc_length, true);
                    holder.setText(R.id.rc_length, formatSize(gifMessage.getGifDataSize()));
                    holder.setVisible(R.id.rc_start_download, false);
                } else {
                    loadingProgress.setVisibility(View.GONE);
                    holder.setVisible(R.id.rc_pre_progress, true);
                    holder.setVisible(R.id.rc_length, true);
                    holder.setVisible(R.id.rc_start_download, false);
                }
            } else {
                loadingProgress.setVisibility(View.GONE);
                holder.setVisible(R.id.rc_pre_progress, false);
                holder.setVisible(R.id.rc_length, false);
                holder.setVisible(R.id.rc_start_download, false);

                // 下载失败
                if (progress == -1) {
                    holder.setVisible(R.id.rc_download_failed, true);
                    holder.setVisible(R.id.rc_length, true);
                    holder.setText(R.id.rc_length, formatSize(gifMessage.getGifDataSize()));
                }
            }
        }
        if (gifMessage.getLocalPath() != null) {
            loadGif(gifMessage, imageView);
        } else {
            imageView.setImageResource(R.drawable.def_gif_bg);
            final int size = RongConfigCenter.conversationConfig().rc_gifmsg_auto_download_size;
            if (gifMessage.getGifDataSize() <= size * 1024) {
                if (checkPermission(holder.getContext())) {
                    if (!uiMessage.getMessage().getReceivedStatus().isDownload()) {
                        uiMessage.getMessage().getReceivedStatus().setDownload();
                        downLoad(uiMessage.getMessage(), holder);
                    }
                } else {
                    if (progress != -1) {
                        // 显示图片下载的界面
                        holder.setVisible(R.id.rc_start_download, true);
                        holder.setVisible(R.id.rc_length, true);
                        holder.setText(R.id.rc_length, formatSize(gifMessage.getGifDataSize()));
                    }

                }
            } else {
                // 下载的时候
                if (progress > 0 && progress < 100) {
                    loadingProgress.setVisibility(View.VISIBLE);
                    loadingProgress.setProgress(progress, true);
                    holder.setVisible(R.id.rc_start_download, false);
                    holder.setVisible(R.id.rc_length, true);
                    holder.setText(R.id.rc_length, formatSize(gifMessage.getGifDataSize()));
                } else if (progress == 100) {
                    loadingProgress.setVisibility(View.GONE);
                    holder.setVisible(R.id.rc_pre_progress, false);
                    holder.setVisible(R.id.rc_length, false);
                    holder.setVisible(R.id.rc_start_download, false);
                } else if (progress != -1) {
                    // 显示图片下载的界面
                    holder.setVisible(R.id.rc_start_download, true);
                    holder.setVisible(R.id.rc_pre_progress, false);
                    loadingProgress.setVisibility(View.GONE);
                    holder.setVisible(R.id.rc_download_failed, false);
                    holder.setVisible(R.id.rc_length, true);
                    holder.setText(R.id.rc_length, formatSize(gifMessage.getGifDataSize()));
                }

            }

        }
    }

    private void loadGif(final GIFMessage gifMessage, final ImageView imageView) {
        Uri thumbUri = gifMessage.getLocalUri();
        if (thumbUri != null && thumbUri.getPath() != null) {
            Glide.with(imageView.getContext())
                    .asGif()
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .load(thumbUri.getPath())
                    .into(imageView);
        }
    }

    @Override
    protected boolean onItemClick(ViewHolder holder, GIFMessage gifMessage, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        ImageView startDownLoad = holder.getView(R.id.rc_start_download);
        ImageView downLoadFailed = holder.getView(R.id.rc_download_failed);
        TextView length = holder.getView(R.id.rc_length);
        ProgressBar preProgress = holder.getView(R.id.rc_pre_progress);
        CircleProgressView loadingProgress = holder.getView(R.id.rc_gif_progress);

        if (startDownLoad.getVisibility() == View.VISIBLE) {
            startDownLoad.setVisibility(View.GONE);
            if (checkPermission(holder.getContext())) {
                downLoad(uiMessage.getMessage(), holder);
            } else {
                downLoadFailed.setVisibility(View.VISIBLE);
                length.setVisibility(View.VISIBLE);
                length.setText(formatSize(gifMessage.getGifDataSize()));
                Toast.makeText(holder.getContext(), R.string.rc_ac_file_download_request_permission, Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (downLoadFailed.getVisibility() == View.VISIBLE) {
            downLoadFailed.setVisibility(View.GONE);
            if (checkPermission(holder.getContext())) {
                downLoad(uiMessage.getMessage(), holder);
            } else {
                // 显示图片下载的界面
                downLoadFailed.setVisibility(View.VISIBLE);
                length.setVisibility(View.VISIBLE);
                length.setText(formatSize(gifMessage.getGifDataSize()));
                Toast.makeText(holder.getContext(), R.string.rc_ac_file_download_request_permission, Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (preProgress.getVisibility() != View.VISIBLE && loadingProgress.getVisibility() != View.VISIBLE) {
            if (gifMessage != null) {
                Intent intent = new Intent(holder.getContext(), GIFPreviewActivity.class);
                intent.putExtra("message", uiMessage.getMessage());
                holder.getContext().startActivity(intent);
                return true;
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof GIFMessage && !messageContent.isDestruct();
    }

    private String formatSize(long length) {
        if (length > 1024 * 1024) { // M
            float size = Math.round(length / (1024f * 1024f) * 100) / 100f;
            return size + "M";
        } else if (length > 1024) {
            float size = Math.round(length / (1024f) * 100) / 100f;
            return size + "KB";
        } else {
            return length + "B";
        }
    }


    //图片消息最小值为 158 X 158，最大值为 240 X 240
    //缩放逻辑同普通图片一致
    private void measureLayoutParams(View view, int width, int height) {
        if (minSize == null) {
            minSize = ScreenUtils.dip2px(view.getContext(), 79);
        }
        if (maxSize == null) {
            maxSize = ScreenUtils.dip2px(view.getContext(), 120);
        }
        int finalWidth;
        int finalHeight;
        if (width < minSize || height < minSize) {
            if (width < height) {
                finalWidth = minSize;
                finalHeight = Math.min((int) (minSize * 1f / width * height), maxSize);
            } else {
                finalHeight = minSize;
                finalWidth = Math.min((int) (minSize * 1f / height * width), maxSize);
            }
        } else if (width < maxSize && height < maxSize) {
            if (width > height) {
                finalWidth = maxSize;
                finalHeight = (int) (maxSize * 1f / width * height);
            } else {
                finalHeight = maxSize;
                finalWidth = (int) (maxSize * 1f / height * width);
            }
        } else {
            if (width > height) {
                if (width * 1f / height <= 2.4) {
                    finalWidth = maxSize;
                    finalHeight = (int) (maxSize * 1f / width * height);
                } else {
                    finalWidth = maxSize;
                    finalHeight = minSize;
                }
            } else {
                if (height * 1f / width <= 2.4) {
                    finalHeight = maxSize;
                    finalWidth = (int) (maxSize * 1f / height * width);
                } else {
                    finalHeight = maxSize;
                    finalWidth = minSize;
                }
            }
        }
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = finalHeight;
        params.width = finalWidth;
        view.setLayoutParams(params);
    }

    private boolean checkPermission(Context context) {
        String[] permission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        return PermissionCheckUtil.checkPermissions(context, permission);
    }

    private void downLoad(final Message downloadMsg, final ViewHolder holder) {
        holder.setVisible(R.id.rc_pre_progress, true);
        IMCenter.getInstance().downloadMediaMessage(downloadMsg, null);
    }

    @Override
    public Spannable getSummarySpannable(Context context, GIFMessage gifMessage) {
        return new SpannableString(context.getString(R.string.rc_conversation_summary_content_image));
    }

    @Override
    protected boolean onItemLongClick(ViewHolder holder, GIFMessage gifMessage, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        if (holder.getView(R.id.rc_download_failed).getVisibility() == View.VISIBLE
                || holder.getView(R.id.rc_pre_progress).getVisibility() == View.VISIBLE) {
            return true;
        }
        return super.onItemLongClick(holder, gifMessage, uiMessage, position, list, listener);
    }
}
