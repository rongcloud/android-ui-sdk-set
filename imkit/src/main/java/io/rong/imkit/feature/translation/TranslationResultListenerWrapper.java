package io.rong.imkit.feature.translation;

/** @author gusd */
public interface TranslationResultListenerWrapper {
    void onTranslationResult(int code, RCTranslationResultWrapper result);
}
