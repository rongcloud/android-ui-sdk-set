package io.rong.imkit.conversation.extension.component.moreaction;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.conversation.extension.InputMode;
import io.rong.imkit.conversation.extension.RongExtensionViewModel;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.model.UiMessage;

/**
 * Created by zwfang on 2018/3/30.
 */
public class DeleteClickActions implements IClickActions {

    @Override
    public Drawable obtainDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_more_action_multi_delete);
    }

    @Override
    public void onClick(Fragment curFragment) {
        MessageViewModel messageViewModel = new ViewModelProvider(curFragment).get(MessageViewModel.class);
        List<UiMessage> messages = messageViewModel.getSelectedUiMessages();
        if (messages != null && messages.size() > 0) {
            int[] messageIds;
            messageIds = new int[messages.size()];
            for (int i = 0; i < messages.size(); ++i) {
                messageIds[i] = messages.get(i).getMessage().getMessageId();
            }
            IMCenter.getInstance().deleteMessages(messageViewModel.getCurConversationType(), messageViewModel.getCurTargetId(), messageIds, null);
            messageViewModel.quitEditMode();
        }
    }

    @Override
    public boolean filter(UiMessage message) {
        return false;
    }
}
