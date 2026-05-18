package io.rong.imkit.feature.reference;

import androidx.annotation.Nullable;
import io.rong.imlib.model.Message;

final class QuoteCardBindingState {

    private String boundMessageUId;
    private Message quotedMessage;

    void bind(@Nullable String messageUId) {
        boundMessageUId = messageUId;
        quotedMessage = null;
    }

    void reset() {
        bind(null);
    }

    void setQuotedMessage(@Nullable Message message) {
        quotedMessage = message;
    }

    void markUnavailable() {
        quotedMessage = null;
    }

    @Nullable
    Message getQuotedMessage() {
        return quotedMessage;
    }

    boolean shouldReuse(@Nullable String messageUId) {
        return shouldReuse(messageUId, false);
    }

    boolean shouldReuse(@Nullable String messageUId, boolean forceReload) {
        if (forceReload) {
            return false;
        }
        return messageUId != null && messageUId.equals(boundMessageUId) && quotedMessage != null;
    }

    boolean isStillBound(@Nullable String messageUId) {
        return messageUId != null && messageUId.equals(boundMessageUId);
    }
}
