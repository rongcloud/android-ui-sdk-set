package io.rong.imkit.config;

import android.text.TextUtils;
import io.rong.imlib.model.Conversation;
import java.util.LinkedList;
import java.util.List;

/** @author jenny_zhou */
public class DefaultConversationListProcessor extends BaseDataProcessor<Conversation> {
    @Override
    public List<Conversation> filtered(List<Conversation> data) {
        List<Conversation> invalidConversation = new LinkedList<>();
        for (Conversation item : data) {
            if (TextUtils.isEmpty(item.getTargetId()) || item.getConversationType() == null) {
                invalidConversation.add(item);
            }
        }
        data.removeAll(invalidConversation);
        return data;
    }
}
