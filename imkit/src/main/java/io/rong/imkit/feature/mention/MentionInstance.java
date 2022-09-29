package io.rong.imkit.feature.mention;

import android.widget.EditText;
import io.rong.imlib.model.Conversation;
import java.util.List;

public class MentionInstance {
    public Conversation.ConversationType conversationType;
    public String targetId;
    public EditText inputEditText;
    public List<MentionBlock> mentionBlocks;
}
