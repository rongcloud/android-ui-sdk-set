package io.rong.imkit.conversation.messgelist.provider;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.feature.location.AMapRealTimeActivity;
import io.rong.imkit.feature.location.AMapRealTimeActivity2D;
import io.rong.imkit.feature.location.LocationDelegate2D;
import io.rong.imkit.feature.location.LocationDelegate3D;
import io.rong.imkit.manager.IMessageProviderPermissionHandler;
import io.rong.imkit.manager.MessageProviderPermissionHandler;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imkit.widget.dialog.PromptPopupDialog;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.location.message.RealTimeLocationStartMessage;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;

public class RealTimeLocationMessageItemProvider extends BaseMessageItemProvider<RealTimeLocationStartMessage> implements IMessageProviderPermissionHandler {

    String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_PHONE_STATE};

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_share_location_message, parent, false);
        return new ViewHolder(parent.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(ViewHolder holder, ViewHolder parentHolder, RealTimeLocationStartMessage realTimeLocationStartMessage, final UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        final Message message = uiMessage.getMessage();
        final TextView view = holder.getView(R.id.rc_location);
        if (message.getMessageDirection() == Message.MessageDirection.SEND) {
            Drawable drawable = view.getResources().getDrawable(R.drawable.rc_icon_rt_message_right);
            drawable.setBounds(0, 0, 29, 41);
            view.setBackgroundResource(R.drawable.rc_ic_bubble_right);
            view.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null);
            view.setText(holder.getContext()
                    .getResources()
                    .getString(R.string.rc_real_time_location_sharing));
        } else {
            Drawable drawable = view.getResources().getDrawable(R.drawable.rc_icon_rt_message_left);
            drawable.setBounds(0, 0, 29, 41);
            view.setBackgroundResource(R.drawable.rc_ic_bubble_left);
            view.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null);
            view.setText(holder.getContext()
                    .getResources()
                    .getString(R.string.rc_real_time_location_sharing));
        }
    }

    @Override
    protected boolean onItemClick(ViewHolder holder, RealTimeLocationStartMessage realTimeLocationStartMessage, final UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        if (!PermissionCheckUtil.checkPermissions(holder.getContext(), permissions)) {
            MessageProviderPermissionHandler.getInstance().addMessageContent(RealTimeLocationStartMessage.class, permissions);
            return false;
        }

        final Message message = uiMessage.getMessage();
        final View view = holder.getView(R.id.rc_location);
        if (message.getMessageDirection() == Message.MessageDirection.SEND) {
            joinMap(view.getContext(), uiMessage);
        } else {
            showPromptPopupDialog(view.getContext(), uiMessage);

        }

        return true;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof RealTimeLocationStartMessage;
    }

    @Override
    public Spannable getSummarySpannable(Context context, RealTimeLocationStartMessage data) {
        if (data != null && data.getContent() != null) {
            return new SpannableString(context.getResources().getString(R.string.rc_real_time_location_start));
        }

        return null;
    }

    private void showPromptPopupDialog(final Context context, final UiMessage uiMessage) {
        PromptPopupDialog dialog = PromptPopupDialog.newInstance(context, "",
                context.getResources().getString(R.string.rc_real_time_join_notification));
        dialog.setPromptButtonClickedListener(new PromptPopupDialog.OnPromptButtonClickedListener() {
            @Override
            public void onPositiveButtonClicked() {
                joinMap(context, uiMessage);
            }
        });
        dialog.show();
    }

    private void joinMap(Context context, UiMessage uiMessage) {
        Message message = uiMessage.getMessage();
        List<String> mLocationShareParticipants = RongIMClient.getInstance()
                .getRealTimeLocationParticipants(message.getConversationType(), message.getTargetId());
        //int result = LocationManager.getInstance().joinLocationSharing();
        int result;
        if (context.getResources().getBoolean(R.bool.rc_location_2D)) {
            result = LocationDelegate2D.getInstance().joinLocationSharing();
        } else {
            result = LocationDelegate3D.getInstance().joinLocationSharing();
        }

        if (result == 0) {
            Intent intent;
            if (context.getResources().getBoolean(R.bool.rc_location_2D)) {
                intent = new Intent(context, AMapRealTimeActivity2D.class);
            } else {
                intent = new Intent(context, AMapRealTimeActivity.class);
            }
            if (mLocationShareParticipants != null) {
                intent.putStringArrayListExtra("participants", (ArrayList<String>) mLocationShareParticipants);
            }
            context.startActivity(intent);
        } else if (result == 1) {
            Toast.makeText(context, R.string.rc_network_exception, Toast.LENGTH_SHORT).show();
        } else if ((result == 2)) {
            Toast.makeText(context, R.string.rc_location_sharing_exceed_max, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void handleRequestPermissionsResult(Activity activity, UiMessage uiMessage, String[] permissions, int[] grantResults) {
        if (!PermissionCheckUtil.checkPermissions(activity, permissions)) {
            PermissionCheckUtil.showRequestPermissionFailedAlter(activity, permissions, grantResults);
        } else {
            showPromptPopupDialog(activity, uiMessage);
        }
    }
}
