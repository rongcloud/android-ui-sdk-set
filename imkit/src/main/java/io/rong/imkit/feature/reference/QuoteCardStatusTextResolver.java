package io.rong.imkit.feature.reference;

import io.rong.imkit.R;

final class QuoteCardStatusTextResolver {

    private QuoteCardStatusTextResolver() {}

    static int resolveUnavailableTextResId() {
        return R.string.rc_reference_status_delete;
    }

    static int resolveRecalledTextResId() {
        return R.string.rc_reference_status_recall;
    }

    static int resolveLoadingTextResId() {
        return R.string.rc_reference_status_loading;
    }

    static int resolveFailedTextResId() {
        return R.string.rc_reference_status_failed;
    }
}
