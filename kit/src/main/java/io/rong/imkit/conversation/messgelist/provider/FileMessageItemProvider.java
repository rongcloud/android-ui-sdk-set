package io.rong.imkit.conversation.messgelist.provider;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.feature.resend.ResendManager;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.FileTypeUtils;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.widget.EllipsizeTextView;
import io.rong.imkit.widget.FileRectangleProgress;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.FileMessage;

public class FileMessageItemProvider extends BaseMessageItemProvider<FileMessage> {

    private int progress = 0;

    public FileMessageItemProvider() {
        mConfig.showContentBubble = false;
        mConfig.showProgress = false;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_item_file_message, parent, false);
        return new ViewHolder(view.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(final ViewHolder holder, ViewHolder parentHolder, FileMessage fileMessage, final UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        EllipsizeTextView tvFileName = holder.getView(R.id.rc_msg_tv_file_name);
        tvFileName.setAdaptiveText(fileMessage.getName());
        long fileSizeBytes = fileMessage.getSize();
        holder.setText(R.id.rc_msg_tv_file_size, FileTypeUtils.formatFileSize(fileSizeBytes));
        holder.setImageResource(R.id.rc_msg_iv_file_type_image, FileTypeUtils.fileTypeImageId(holder.getContext(), fileMessage.getName()));
        if (Message.MessageDirection.SEND.equals(uiMessage.getMessageDirection())) {
            holder.setBackgroundRes(R.id.rc_message, R.drawable.rc_bg_file_message_send);
        } else {
            holder.setBackgroundRes(R.id.rc_message, R.drawable.rc_bg_file_message_receive);
        }
        if (uiMessage.getMessage().getSentStatus().equals(Message.SentStatus.SENDING) && fileMessage.progress < 100) {
            holder.setVisible(R.id.rc_msg_pb_file_upload_progress, true);

            if (ResendManager.getInstance().needResend(uiMessage.getMessage().getMessageId())) {
                if (uiMessage.getProgress() == progress) {
                    holder.setVisible(R.id.rc_progress, true);
                    holder.setVisible(R.id.rc_btn_cancel, false);
                } else {
                    holder.setVisible(R.id.rc_progress, false);
                    holder.setVisible(R.id.rc_btn_cancel, true);
                }
            } else {
                progress = uiMessage.getProgress();
                holder.setVisible(R.id.rc_btn_cancel, true);
                holder.setVisible(R.id.rc_progress, false);
            }
            holder.setHoldVisible(R.id.rc_msg_canceled, false);
            ((FileRectangleProgress) holder.getView(R.id.rc_msg_pb_file_upload_progress)).setProgress(uiMessage.getProgress());
        } else if (uiMessage.getMessage().getSentStatus().equals(Message.SentStatus.FAILED) && ResendManager.getInstance().needResend(uiMessage.getMessage().getMessageId())) {
            holder.setVisible(R.id.rc_msg_pb_file_upload_progress, true);
            holder.setVisible(R.id.rc_btn_cancel, false);
            holder.setHoldVisible(R.id.rc_msg_canceled, false);
            holder.setVisible(R.id.rc_progress, true);
            ((FileRectangleProgress) holder.getView(R.id.rc_msg_pb_file_upload_progress)).setProgress(uiMessage.getProgress());
        } else {
            if (uiMessage.getMessage().getSentStatus().equals(Message.SentStatus.CANCELED)) {
                holder.setHoldVisible(R.id.rc_msg_canceled, true);
            } else {
                holder.setHoldVisible(R.id.rc_msg_canceled, false);
            }
            holder.setHoldVisible(R.id.rc_msg_pb_file_upload_progress, false);
            holder.setVisible(R.id.rc_btn_cancel, false);
            holder.setVisible(R.id.rc_progress, false);
        }

        holder.setOnClickListener(R.id.rc_btn_cancel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IMCenter.getInstance().cancelSendMediaMessage(uiMessage.getMessage(), new RongIMClient.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        ResendManager.getInstance().removeResendMessage(uiMessage.getMessage().getMessageId());
                        holder.setVisible(R.id.rc_msg_canceled, true);
                        holder.setHoldVisible(R.id.rc_msg_pb_file_upload_progress, false);
                        holder.setVisible(R.id.rc_btn_cancel, false);
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode errorCode) {

                    }
                });
            }
        });
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof FileMessage;
    }

    @Override
    public Spannable getSummarySpannable(Context context, FileMessage fileMessage) {
        return new SpannableString(context.getString(R.string.rc_conversation_summary_content_file));
    }

    @Override
    protected boolean onItemClick(ViewHolder holder, FileMessage fileMessage, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        RouteUtils.routeToFilePreviewActivity(holder.getContext(), uiMessage.getMessage(), fileMessage, uiMessage.getProgress());
        return true;
    }

}
