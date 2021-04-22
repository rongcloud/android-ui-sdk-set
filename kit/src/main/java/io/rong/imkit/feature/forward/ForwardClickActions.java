package io.rong.imkit.feature.forward;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.conversation.ConversationFragment;
import io.rong.imkit.conversation.extension.component.moreaction.IClickActions;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;

public class ForwardClickActions implements IClickActions {
    private static final String TAG = ForwardClickActions.class.getSimpleName();

    @Override
    public Drawable obtainDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_selector_multi_forward);
    }

    @Override
    public void onClick(final Fragment fragment) {
        if (fragment == null || fragment.getActivity() == null || fragment.getActivity().isFinishing()) {
            RLog.e(TAG, "onClick activity is null or finishing.");
            return;
        }
        final WeakReference<Fragment> fragmentWeakReference = new WeakReference<>(fragment);

        final BottomMenuDialog dialog = new BottomMenuDialog();
        dialog.setConfirmListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(View arg0) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                if (fragmentWeakReference.get() != null) {
                    startSelectConversationActivity(fragmentWeakReference.get(), 0);
                }
            }
        });
        dialog.setMiddleListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                if (fragmentWeakReference.get() != null) {
                    startSelectConversationActivity(fragmentWeakReference.get(), 1);
                }
            }
        });
        dialog.setCancelListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        dialog.show(fragment.getChildFragmentManager());
    }

    @Override
    public boolean filter(UiMessage message) {
        return !(Conversation.ConversationType.PRIVATE.equals(message.getConversationType())
                || Conversation.ConversationType.GROUP.equals(message.getConversationType()));
    }

    // index: 0:逐步转发  1:合并转发
    private void startSelectConversationActivity(Fragment pFragment, int index) {
        if (pFragment == null) {
            return;
        }
        final ConversationFragment fragment = (ConversationFragment) pFragment;
        MessageViewModel messageViewModel = new ViewModelProvider(pFragment).get(MessageViewModel.class);
        List<Message> messageList = new ArrayList<>();
        for (UiMessage uiMessage : messageViewModel.getSelectedUiMessages()) {
            messageList.add(uiMessage.getMessage());
        }
        List<Message> messages = ForwardManager.filterMessagesList(
                fragment.getContext(), messageList, index);
        if (messages.size() == 0) {
            RLog.e(TAG, "startSelectConversationActivity the size of messages is 0!");
            return;
        }
        ArrayList<Integer> messageIds = new ArrayList<>();
        for (Message msg : messages) {
            messageIds.add(msg.getMessageId());
        }
        RouteUtils.routeToForwardSelectConversationActivity(pFragment, index == 0 ? ForwardType.SINGLE : ForwardType.MULTI, messageIds);
    }

    public enum ForwardType {
        SINGLE(0),
        MULTI(1);

        int value;

        ForwardType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public ForwardType valueOf(int value) {
            if (value == SINGLE.value) {
                return SINGLE;
            } else {
                return MULTI;
            }
        }
    }
}
