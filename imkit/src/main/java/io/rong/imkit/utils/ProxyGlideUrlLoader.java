package io.rong.imkit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import java.io.InputStream;

/** @author gusd @Date 2022/09/08 */
public class ProxyGlideUrlLoader implements ModelLoader<GlideUrl, InputStream> {
    private static final String TAG = "ProxyGlideUrlLoader";
    public static final Option<Integer> TIMEOUT =
            Option.memory("com.bumptech.glide.load.model.stream.HttpGlideUrlLoader.Timeout", 5000);

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(
            @NonNull GlideUrl glideUrl, int width, int height, @NonNull Options options) {
        int timeout = options.get(TIMEOUT);
        return new LoadData<>(glideUrl, new ProxyHttpUrlFetcher(glideUrl, timeout));
    }

    @Override
    public boolean handles(@NonNull GlideUrl glideUrl) {
        return true;
    }

    public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {

        @Override
        public ModelLoader<GlideUrl, InputStream> build(MultiModelLoaderFactory multiFactory) {
            return new ProxyGlideUrlLoader();
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }
}
