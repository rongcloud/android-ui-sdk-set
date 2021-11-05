package io.rong.imkit.feature.mention;

import android.widget.EditText;

import java.util.List;

import io.rong.imlib.model.Conversation;

public class MentionInstance {
    public Conversation.ConversationType conversationType;
    public String targetId;
    public EditText inputEditText;
    public List<MentionBlock> mentionBlocks;
}
