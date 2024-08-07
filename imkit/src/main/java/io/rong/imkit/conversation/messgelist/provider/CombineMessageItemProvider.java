package io.rong.imkit.conversation.messgelist.provider;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.activity.CombineWebViewActivity;
import io.rong.imkit.feature.forward.CombineMessage;
import io.rong.imkit.feature.forward.CombineMessageUtils;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MessageContent;
import java.io.File;
import java.util.List;

public class CombineMessageItemProvider extends BaseMessageItemProvider<CombineMessage> {

    public CombineMessageItemProvider() {
        mConfig.showReadState = true;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.rc_item_combine_message, parent, false);
        return new ViewHolder(view.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(
            ViewHolder holder,
            ViewHolder parentHolder,
            CombineMessage combineMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        String title = getTitle(combineMessage);
        combineMessage.setTitle(title);
        holder.setText(R.id.title, combineMessage.getTitle());

        StringBuilder summary = new StringBuilder();
        List<String> summarys = combineMessage.getSummaryList();
        for (int i = 0; i < summarys.size() && i < 4; i++) {
            if (i == 0) {
                summary = new StringBuilder(summarys.get(i));
            } else {
                summary.append("\n").append(summarys.get(i));
            }
        }
        holder.setText(R.id.summary, summary.toString());
    }

    @Override
    protected boolean onItemClick(
            ViewHolder holder,
            CombineMessage combineMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        String uri = getUriString(combineMessage);

        String type = CombineWebViewActivity.TYPE_LOCAL;
        if (uri != null && uri.startsWith("http")) {
            type = CombineWebViewActivity.TYPE_MEDIA;
        }

        if (uri == null) {
            Context context = holder.getContext();
            new AlertDialog.Builder(context)
                    .setMessage(context.getString(R.string.rc_combine_history_deleted))
                    .setPositiveButton(context.getString(R.string.rc_dialog_ok), null)
                    .show();
            return false;
        }
        RouteUtils.routeToCombineWebViewActivity(
                holder.getContext(),
                uiMessage.getMessage().getMessageId(),
                uri,
                type,
                combineMessage.getTitle());
        return false;
    }

    private String getUriString(CombineMessage combineMessage) {
        if (combineMessage == null) {
            return null;
        }

        // 如果远端地址映射的本地路径存在，则直接 return
        Uri mediaUri = combineMessage.getMediaUrl();
        if (mediaUri != null) {
            String filePath =
                    CombineMessageUtils.getInstance().getCombineFilePath(mediaUri.toString());
            if (new File(filePath).exists()) {
                return "file://" + filePath;
            }
        }

        // 如果本地路径存在 return
        Uri localPathUri = combineMessage.getLocalPath();
        if (localPathUri != null) {
            String localPath = localPathUri.toString().replace("file://", "");
            if (new File(localPath).exists()) {
                return localPath;
            }
        }

        if (mediaUri == null) {
            return null;
        }
        return mediaUri.toString();
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof CombineMessage;
    }

    @Override
    public Spannable getSummarySpannable(Context context, CombineMessage combineMessage) {
        return new SpannableString(context.getString(R.string.rc_message_content_combine));
    }

    private String getTitle(CombineMessage content) {
        String title = "";
        Context context = IMCenter.getInstance().getContext();
        if (Conversation.ConversationType.GROUP.equals(content.getConversationType())) {
            title = context.getString(R.string.rc_combine_group_chat);
        } else {
            List<String> nameList = content.getNameList();
            if (nameList == null) {
                return title;
            }

            if (nameList.size() == 1) {
                title =
                        String.format(
                                context.getString(R.string.rc_combine_the_group_chat_of),
                                nameList.get(0));
            } else if (nameList.size() == 2) {
                title =
                        String.format(
                                context.getString(R.string.rc_combine_the_group_chat_of),
                                nameList.get(0)
                                        + " "
                                        + context.getString(R.string.rc_combine_and)
                                        + " "
                                        + nameList.get(1));
            }
        }
        if (TextUtils.isEmpty(title)) {
            title = context.getString(R.string.rc_combine_chat_history);
        }
        return title;
    }
}
