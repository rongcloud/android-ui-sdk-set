package io.rong.imkit.feature.reference;

final class QuoteCardUiThreadDispatcher {

    interface Scheduler {
        boolean isMainThread();

        void post(Runnable runnable);
    }

    private QuoteCardUiThreadDispatcher() {}

    static void dispatch(Scheduler scheduler, Runnable runnable) {
        if (scheduler.isMainThread()) {
            runnable.run();
            return;
        }
        scheduler.post(runnable);
    }
}
