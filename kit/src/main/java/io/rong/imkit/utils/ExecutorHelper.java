package io.rong.imkit.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ExecutorHelper {
    private final Executor mDiskIO;
    private final Executor mNetworkIO;
    private final Executor mUiExecutor;
    private final Executor mCompressExecutor;

    ExecutorHelper(Executor diskIO, Executor networkIO, Executor mainThread, Executor compressExecutor) {
        this.mDiskIO = diskIO;
        this.mNetworkIO = networkIO;
        this.mUiExecutor = mainThread;
        this.mCompressExecutor = compressExecutor;
    }

    public ExecutorHelper() {
        this(new DisIOExecutor(), new NetExecutor(),
                new MainThreadExecutor(), new CompressExecutor());
    }

    private static class SingletonHolder {
        static ExecutorHelper sInstance = new ExecutorHelper();
    }

    public static ExecutorHelper getInstance() {
        return SingletonHolder.sInstance;
    }

    public Executor diskIO() {
        return mDiskIO;
    }

    public Executor networkIO() {
        return mNetworkIO;
    }

    public Executor compressExecutor() {
        return mCompressExecutor;
    }

    public Executor mainThread() {
        return mUiExecutor;
    }

    private static class MainThreadExecutor implements Executor {
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }

    private static class DisIOExecutor implements Executor {
        private final Executor mDiskIO;

        public DisIOExecutor() {
            mDiskIO = Executors.newSingleThreadExecutor();
        }

        @Override
        public void execute(@NonNull Runnable command) {
            mDiskIO.execute(command);
        }
    }

    private static class NetExecutor implements Executor {
        private final Executor mNetExecutor;

        public NetExecutor() {
            mNetExecutor = Executors.newSingleThreadExecutor();
        }

        @Override
        public void execute(@NonNull Runnable command) {
            mNetExecutor.execute(command);
        }
    }

    /**
     * 无核心线程，使用后 60 秒自动释放,视频压缩，正则替换
     */
    private static class CompressExecutor implements Executor {
        private final Executor mCompressExecutor;

        public CompressExecutor() {
            mCompressExecutor = Executors.newCachedThreadPool();
        }

        @Override
        public void execute(@NonNull Runnable command) {
            mCompressExecutor.execute(command);
        }
    }

}
