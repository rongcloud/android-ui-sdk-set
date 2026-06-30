package io.rong.imkit.conversation.messgelist.viewmodel;

import static org.junit.Assert.assertEquals;

import io.rong.imkit.R;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import org.junit.Test;

public class MessageViewModelReactionTest {

    @Test
    public void reactionLimitErrorsUseLimitReachedMessage() {
        assertEquals(
                R.string.rc_reaction_limit_exceeded,
                MessageViewModel.getReactionOperationErrorMessageRes(
                        IRongCoreEnum.CoreErrorCode.RC_MSG_REACTION_LIMIT_REACHED));
        assertEquals(
                R.string.rc_reaction_limit_exceeded,
                MessageViewModel.getReactionOperationErrorMessageRes(
                        IRongCoreEnum.CoreErrorCode.RC_MSG_REACTION_USER_LIMIT_REACHED));
    }

    @Test
    public void otherReactionErrorsUseGenericFailureMessage() {
        assertEquals(
                R.string.rc_reaction_operation_failed,
                MessageViewModel.getReactionOperationErrorMessageRes(
                        IRongCoreEnum.CoreErrorCode.RC_NET_UNAVAILABLE));
    }

    @Test
    public void preserveMessageStateForRefreshKeepsExistingNonSendingState() {
        Message refreshedMessage = newMessage(Message.SentStatus.SENDING);
        refreshedMessage.setMessageDirection(Message.MessageDirection.SEND);
        Message currentMessage = newMessage(Message.SentStatus.SENT);

        MessageViewModel.preserveMessageStateForRefresh(refreshedMessage, currentMessage);

        assertEquals(Message.SentStatus.SENT, refreshedMessage.getSentStatus());
    }

    @Test
    public void preserveMessageStateForRefreshKeepsReactionEntryMetadata() {
        Message refreshedMessage = newMessage(Message.SentStatus.SENDING);
        refreshedMessage.setMessageDirection(Message.MessageDirection.SEND);
        Message currentMessage = newMessage(Message.SentStatus.SENT);
        currentMessage.setUId("sent-uid");
        currentMessage.setObjectName("RC:TxtMsg");

        MessageViewModel.preserveMessageStateForRefresh(refreshedMessage, currentMessage);

        assertEquals("sent-uid", refreshedMessage.getUId());
        assertEquals("RC:TxtMsg", refreshedMessage.getObjectName());
        assertEquals(Message.SentStatus.SENT, refreshedMessage.getSentStatus());
    }

    private static Message newMessage(Message.SentStatus sentStatus) {
        Message message = new Message();
        message.setConversationType(Conversation.ConversationType.PRIVATE);
        message.setTargetId("target");
        message.setMessageId(1);
        message.setSentStatus(sentStatus);
        return message;
    }
}
