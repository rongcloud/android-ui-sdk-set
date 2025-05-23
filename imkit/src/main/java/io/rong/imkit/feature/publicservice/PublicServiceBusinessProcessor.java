package io.rong.imkit.feature.publicservice;

import android.os.Bundle;
import io.rong.imkit.IMCenter;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.messgelist.processor.BaseBusinessProcessor;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.model.UiMessage;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.publicservice.message.PublicServiceCommandMessage;
import io.rong.imlib.publicservice.model.PublicServiceMenu;
import io.rong.imlib.publicservice.model.PublicServiceProfile;

public class PublicServiceBusinessProcessor extends BaseBusinessProcessor {

    @Override
    public void init(MessageViewModel messageViewModel, Bundle bundle) {
        if (messageViewModel == null
                || !(messageViewModel
                                .getCurConversationType()
                                .equals(Conversation.ConversationType.PUBLIC_SERVICE)
                        || messageViewModel
                                .getCurConversationType()
                                .equals(Conversation.ConversationType.APP_PUBLIC_SERVICE))) {
            return;
        }
        PublicServiceCommandMessage msg = new PublicServiceCommandMessage();
        msg.setCommand(PublicServiceMenu.PublicServiceMenuItemType.Entry.getMessage());
        Message message =
                Message.obtain(
                        messageViewModel.getCurTargetId(),
                        messageViewModel.getCurConversationType(),
                        msg);
        IMCenter.getInstance().sendMessage(message, null, null, null);
        Conversation.PublicServiceType publicServiceType;
        if (messageViewModel
                .getCurConversationType()
                .equals(Conversation.ConversationType.PUBLIC_SERVICE)) {
            publicServiceType = Conversation.PublicServiceType.PUBLIC_SERVICE;
        } else {
            publicServiceType = Conversation.PublicServiceType.APP_PUBLIC_SERVICE;
        }
        PublicServiceProfile publicServiceProfile =
                PublicServiceManager.getInstance()
                        .getPublicServiceProfile(
                                publicServiceType, messageViewModel.getCurTargetId());
        if (publicServiceProfile != null) {
            PublicServiceManager.getInstance()
                    .getExtensionModule()
                    .updateMenu(publicServiceProfile);
        } else {
            PublicServiceManager.getInstance()
                    .getPublicServiceProfile(
                            publicServiceType,
                            messageViewModel.getCurTargetId(),
                            new RongIMClient.ResultCallback<PublicServiceProfile>() {
                                @Override
                                public void onSuccess(PublicServiceProfile publicServiceProfile) {
                                    PublicServiceManager.getInstance()
                                            .getExtensionModule()
                                            .updateMenu(publicServiceProfile);
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode errorCode) {
                                    // do nothing
                                }
                            });
        }
        super.init(messageViewModel, bundle);
    }

    @Override
    public boolean onReceived(
            MessageViewModel viewModel,
            UiMessage message,
            int left,
            boolean hasPackage,
            boolean offline) {
        if (left == 0 && !hasPackage) {
            boolean syncReadStatus =
                    RongConfigCenter.conversationConfig()
                            .isEnableMultiDeviceSync(viewModel.getCurConversationType());
            if (syncReadStatus) {
                IMCenter.getInstance()
                        .syncConversationReadStatus(
                                viewModel.getConversationIdentifier(), message.getSentTime(), null);
            }
        }
        return super.onReceived(viewModel, message, left, hasPackage, offline);
    }

    @Override
    public void onExistUnreadMessage(
            MessageViewModel viewModel, Conversation conversation, int unreadMessageCount) {
        boolean syncReadStatus =
                RongConfigCenter.conversationConfig()
                        .isEnableMultiDeviceSync(viewModel.getCurConversationType());
        if (syncReadStatus) {
            IMCenter.getInstance()
                    .syncConversationReadStatus(
                            viewModel.getConversationIdentifier(),
                            conversation.getSentTime(),
                            null);
        }
        IMCenter.getInstance()
                .syncConversationReadStatus(
                        viewModel.getCurConversationType(), viewModel.getCurTargetId(), 0, null);
    }
}
