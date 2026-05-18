package io.rong.imkit.feature.reference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.QuoteInfo;
import java.util.HashSet;
import java.util.Set;

public final class QuoteCardRefreshMatcher {

    private QuoteCardRefreshMatcher() {}

    public static boolean shouldRefresh(
            @Nullable Message message, @NonNull Set<String> affectedUids) {
        if (message == null || affectedUids.isEmpty()) {
            return false;
        }
        QuoteInfo quoteInfo = message.getQuoteInfo();
        if (quoteInfo == null
                || quoteInfo.getMessageUId() == null
                || quoteInfo.getMessageUId().isEmpty()) {
            return false;
        }
        return affectedUids.contains(quoteInfo.getMessageUId());
    }

    @NonNull
    public static Set<String> collectAffectedUids(@Nullable Message... messages) {
        Set<String> affectedUids = new HashSet<>();
        if (messages == null || messages.length == 0) {
            return affectedUids;
        }
        for (Message message : messages) {
            if (message == null || message.getUId() == null || message.getUId().isEmpty()) {
                continue;
            }
            affectedUids.add(message.getUId());
        }
        return affectedUids;
    }
}
