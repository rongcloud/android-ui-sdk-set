package io.rong.imkit.feature.destruct.provider;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.conversation.messgelist.provider.BaseMessageItemProvider;
import io.rong.imkit.feature.destruct.DestructManager;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.utils.RongOperationPermissionUtils;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.SightMessage;

public class DestructSightMessageItemProvider extends BaseMessageItemProvider<SightMessage> {
    private static final String TAG = DestructSightMessageItemProvider.class.getSimpleName();

    public DestructSightMessageItemProvider() {
        mConfig.showContentBubble = false;
    }


    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_item_destruct_sight_message, parent, false);
        return new ViewHolder(view.getContext(), view);
    }


    @Override
    protected void bindMessageContentViewHolder(ViewHolder holder,ViewHolder parentHolder, SightMessage sightMessage, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        boolean isSender = uiMessage.getMessage().getMessageDirection() == Message.MessageDirection.SEND;
        holder.setText(R.id.rc_sight_duration, getSightDuration(sightMessage.getDuration()));
        holder.getConvertView().setTag(uiMessage.getMessage().getUId());
        holder.setBackgroundRes(R.id.rc_destruct_click, isSender ? R.drawable.rc_ic_bubble_right : R.drawable.rc_ic_bubble_left);
        holder.setVisible(R.id.fl_send_fire, isSender);
        holder.setVisible(R.id.fl_receiver_fire, !isSender);
        TextView clickHint = holder.getView(R.id.rc_destruct_click_hint);
        if (!isSender) {
            DestructManager.getInstance().addListener(uiMessage.getMessage().getUId(), new DestructListener(holder, uiMessage), TAG);
            boolean isRead = uiMessage.getMessage().getReadTime() > 0;
            holder.setVisible(R.id.tv_receiver_fire, isRead);
            holder.setVisible(R.id.iv_receiver_fire, !isRead);
            if (isRead) {
                String unFinishTime;
                if (TextUtils.isEmpty(uiMessage.getDestructTime())) {
                    unFinishTime = DestructManager.getInstance().getUnFinishTime(uiMessage.getMessage().getUId());
                } else {
                    unFinishTime = uiMessage.getDestructTime();
                }
                holder.setText(R.id.tv_receiver_fire, unFinishTime);
                DestructManager.getInstance().startDestruct(uiMessage.getMessage());
            }
        }
    }

    @Override
    protected boolean onItemClick(ViewHolder holder, SightMessage sightMessage, UiMessage uiMessage, int position,List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        if (sightMessage != null) {
            if (!RongOperationPermissionUtils.isMediaOperationPermit(holder.getContext())) {
                return true;
            }
            String[] permissions = {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            if (!PermissionCheckUtil.checkPermissions(holder.getContext(), permissions)) {
                Activity activity = (Activity) holder.getContext();
                PermissionCheckUtil.requestPermissions(activity, permissions, 100);
                return true;
            }

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
                Toast.makeText(holder.getContext(), "Sight Module does not exist.", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }

    @Override
    public Spannable getSummarySpannable(Context context, SightMessage imageMessage) {
        return new SpannableString(context.getString(R.string.rc_conversation_summary_content_burn));
    }


    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof SightMessage && messageContent.isDestruct();
    }

    private static class DestructListener implements RongIMClient.DestructCountDownTimerListener {
        private WeakReference<ViewHolder> mHolder;
        private UiMessage mUIMessage;

        public DestructListener(ViewHolder pHolder, UiMessage pUIMessage) {
            mHolder = new WeakReference<>(pHolder);
            mUIMessage = pUIMessage;
        }

        @Override
        public void onTick(long millisUntilFinished, String messageId) {
            if (mUIMessage.getMessage().getUId().equals(messageId)) {
                ViewHolder holder = mHolder.get();
                if (holder != null && messageId.equals(holder.getConvertView().getTag())) {
                    holder.setVisible(R.id.tv_receiver_fire, true);
                    holder.setVisible(R.id.iv_receiver_fire, false);
                    String unDestructTime = String.valueOf(Math.max(millisUntilFinished, 1));
                    holder.setText(R.id.tv_receiver_fire, unDestructTime);
                    mUIMessage.setDestructTime(unDestructTime);
                }
            }
        }

        @Override
        public void onStop(String messageId) {
            if (mUIMessage.getMessage().getUId().equals(messageId)) {
                ViewHolder holder = mHolder.get();
                if (holder != null && messageId.equals(holder.getConvertView().getTag())) {
                    holder.setVisible(R.id.tv_receiver_fire, false);
                    holder.setVisible(R.id.iv_receiver_fire, true);
                    mUIMessage.setDestructTime(null);
                }
            }
        }
    }

    private String getSightDuration(int time) {
        String recordTime;
        int hour, minute, second;
        if (time <= 0)
            return "00:00";
        else {
            minute = time / 60;
            if (minute < 60) {
                second = time % 60;
                recordTime = unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99)
                    return "99:59:59";
                minute = minute % 60;
                second = time - hour * 3600 - minute * 60;
                recordTime = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
            }
        }
        return recordTime;
    }

    private String unitFormat(int time) {
        String formatTime;
        if (time >= 0 && time < 10)
            formatTime = "0" + time;
        else
            formatTime = "" + time;
        return formatTime;
    }
}
