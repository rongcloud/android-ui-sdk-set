package io.rong.imkit.config;

import android.net.Uri;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

import io.rong.imlib.model.Conversation;

public class GatheredConversationConfig {
    private Map<Conversation.ConversationType, Integer> mTitleMap;
    private Map<Conversation.ConversationType, Uri> mPortraitMap;

    public GatheredConversationConfig() {
        mTitleMap = new HashMap<>();
        mPortraitMap = new HashMap<>();
    }

    public void setConversationTitle(Conversation.ConversationType conversationType, int title) {
        mTitleMap.put(conversationType, title);
    }

    public Integer getConversationTitle(Conversation.ConversationType conversationType) {
        return mTitleMap.get(conversationType);
    }

    public void setGatherConversationPortrait(Conversation.ConversationType conversationType, Uri resUri) {
        mPortraitMap.put(conversationType, resUri);
    }

    public Uri getGatherConversationPortrait(Conversation.ConversationType conversationType) {
        return mPortraitMap.get(conversationType);
    }
}
