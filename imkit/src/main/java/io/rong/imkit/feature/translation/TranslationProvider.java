package io.rong.imkit.feature.translation;

import io.rong.imlib.model.RCTranslationResult;
import io.rong.imlib.translation.TranslationClient;
import io.rong.imlib.translation.TranslationResultListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author gusd
 */
public class TranslationProvider implements TranslationResultListener {
    private static final String TAG = "TranslationProvider";

    private List<TranslationResultListenerWrapper> listenerWrapperList =
            new CopyOnWriteArrayList<>();

    public TranslationProvider() {
        TranslationClient.getInstance().addTranslationResultListener(this);
    }

    public void addListener(TranslationResultListenerWrapper listener) {
        listenerWrapperList.add(listener);
    }

    public void removeListener(TranslationResultListenerWrapper listener) {
        listenerWrapperList.remove(listener);
    }

    @Override
    public void onTranslationResult(int code, RCTranslationResult result) {
        RCTranslationResultWrapper resultWrapper = getTranslationResultWrapper(result);
        for (TranslationResultListenerWrapper translationResultListenerWrapper :
                listenerWrapperList) {
            translationResultListenerWrapper.onTranslationResult(code, resultWrapper);
        }
    }

    private RCTranslationResultWrapper getTranslationResultWrapper(RCTranslationResult result) {
        return new RCTranslationResultWrapper(
                result.getMessageId(),
                result.getSrcText(),
                result.getTranslatedText(),
                result.getSrcLanguage(),
                result.getTargetLanguage());
    }

    private static class SingletonHolder {
        static TranslationProvider instance = new TranslationProvider();
    }

    public static TranslationProvider getInstance() {
        return TranslationProvider.SingletonHolder.instance;
    }
}
