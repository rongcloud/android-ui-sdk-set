package io.rong.imkit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;

import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;
import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.text.MetadataRepo;

/**
 * 用于本地加载字体库.
 *
 * @see <a href="https://juejin.cn/post/6966858553583730718#heading-15">https://juejin.cn/post/6966858553583730718#heading-15</a>
 */
public class LocalEmojiCompatConfig extends EmojiCompat.Config {

    public LocalEmojiCompatConfig(@NonNull Context context) {
        super(new LocalMetadataLoader(context));
    }

    private static class LocalMetadataLoader implements EmojiCompat.MetadataRepoLoader {
        private final Context mContext;

        LocalMetadataLoader(@NonNull Context context) {
            mContext = context.getApplicationContext();
        }

        @SuppressLint("RestrictedApi")
        @Override
        @RequiresApi(19)
        public void load(@NonNull EmojiCompat.MetadataRepoLoaderCallback loaderCallback) {
            Preconditions.checkNotNull(loaderCallback, "loaderCallback cannot be null");
            //开启子线程执行任务
            final InitRunnable runnable = new InitRunnable(mContext, loaderCallback);
            final Thread thread = new Thread(runnable);
            thread.setDaemon(false);
            thread.start();
        }
    }

    @RequiresApi(19)
    private static class InitRunnable implements Runnable {
        private final EmojiCompat.MetadataRepoLoaderCallback mLoaderCallback;
        private final Context mContext;

        InitRunnable(Context context, EmojiCompat.MetadataRepoLoaderCallback loaderCallback) {
            mContext = context;
            mLoaderCallback = loaderCallback;
        }

        @Override
        public void run() {
            try {
                //构建 MetadataRepo
                final Typeface typeface = Typeface.createFromAsset(mContext.getAssets(), "NotoColorEmojiCompat.ttf");
                final InputStream inputStream = mContext.getResources().getAssets().open("NotoColorEmojiCompat.ttf");
                final MetadataRepo metadataRepo = MetadataRepo.create(typeface, inputStream);
                mLoaderCallback.onLoaded(metadataRepo);
            } catch (Throwable t) {
                mLoaderCallback.onFailed(t);
            }
        }
    }
}

