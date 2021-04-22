package io.rong.imkit.conversationlist.model;

import android.content.Context;

import io.rong.imkit.feature.publicservice.PublicServiceManager;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.publicservice.model.PublicServiceProfile;

public class PublicServiceConversation extends SingleConversation {

    public PublicServiceConversation(Context context, Conversation conversation) {
        super(context, conversation);
        onConversationUpdate(conversation);
    }

    @Override
    public void onConversationUpdate(Conversation conversation) {
        mCore = conversation;
        Conversation.PublicServiceType publicServiceType = null;
        if (mCore.getConversationType().equals(Conversation.ConversationType.PUBLIC_SERVICE)) {
            publicServiceType = Conversation.PublicServiceType.PUBLIC_SERVICE;
        } else if (mCore.getConversationType().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE)) {
            publicServiceType = Conversation.PublicServiceType.APP_PUBLIC_SERVICE;
        }
        if (publicServiceType != null) {
            PublicServiceProfile profile = PublicServiceManager.getInstance().getPublicServiceProfile(publicServiceType, mCore.getTargetId());
            if (profile != null) {
                mCore.setConversationTitle(profile.getName());
                mCore.setPortraitUrl(profile.getPortraitUri().toString());
                buildConversationContent();
            } else {
                PublicServiceManager.getInstance().getPublicServiceProfile(publicServiceType, mCore.getTargetId(), new RongIMClient.ResultCallback<PublicServiceProfile>() {
                    @Override
                    public void onSuccess(PublicServiceProfile publicServiceProfile) {
                        mCore.setConversationTitle(publicServiceProfile == null ? mCore.getTargetId() : publicServiceProfile.getName());
                        mCore.setPortraitUrl(publicServiceProfile == null ? "" : publicServiceProfile.getPortraitUri().toString());
                        buildConversationContent();
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode errorCode) {

                    }
                });
            }
        }
    }
}
