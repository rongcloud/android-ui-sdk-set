package io.rong.imkit.utils;

import android.net.Uri;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** @author gusd @Date 2022/09/08 */
public class ProxyHttpLoader implements ModelLoader<Uri, InputStream> {

    private static final String TAG = "ProxyHttpLoader";

    private static final Set<String> SCHEMES =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList("http", "https")));
    public static final Option<Integer> TIMEOUT =
            Option.memory("com.bumptech.glide.load.model.stream.HttpGlideUrlLoader.Timeout", 5000);

    public ProxyHttpLoader() {
        // do nothing
    }

    @Override
    public LoadData<InputStream> buildLoadData(Uri model, int width, int height, Options options) {
        GlideUrl url = new GlideUrl(model.toString());
        int timeout = options.get(TIMEOUT);
        return new LoadData<>(url, new ProxyHttpUrlFetcher(url, timeout));
    }

    @Override
    public boolean handles(Uri model) {
        return SCHEMES.contains(model.getScheme());
    }

    /** Factory for loading {@link InputStream}s from http/https {@link Uri}s. */
    public static class Factory implements ModelLoaderFactory<Uri, InputStream> {

        @Override
        public ModelLoader<Uri, InputStream> build(MultiModelLoaderFactory multiFactory) {
            return new ProxyHttpLoader();
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }
}
