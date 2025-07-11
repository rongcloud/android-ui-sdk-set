package io.rong.imkit.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.model.AssetUriLoader;
import com.bumptech.glide.load.model.DataUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.UriLoader;
import com.bumptech.glide.load.model.UrlUriLoader;
import com.bumptech.glide.load.model.stream.MediaStoreImageThumbLoader;
import com.bumptech.glide.load.model.stream.MediaStoreVideoThumbLoader;
import com.bumptech.glide.load.model.stream.QMediaStoreUriLoader;
import com.bumptech.glide.module.LibraryGlideModule;
import java.io.InputStream;

/** @author gusd @Date 2022/09/08 */
@GlideModule
public class GlideProxyModule extends LibraryGlideModule {
    private static final String TAG = "ProxyModule";

    @Override
    public void registerComponents(
            @NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        ContentResolver contentResolver = context.getContentResolver();
        registry.replace(Uri.class, InputStream.class, new ProxyHttpLoader.Factory())
                .append(Uri.class, InputStream.class, new DataUrlLoader.StreamFactory<Uri>())
                .append(
                        Uri.class,
                        InputStream.class,
                        new AssetUriLoader.StreamFactory(context.getAssets()))
                .append(
                        Uri.class,
                        InputStream.class,
                        new MediaStoreImageThumbLoader.Factory(context))
                .append(
                        Uri.class,
                        InputStream.class,
                        new MediaStoreVideoThumbLoader.Factory(context));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            registry.append(
                    Uri.class,
                    InputStream.class,
                    new QMediaStoreUriLoader.InputStreamFactory(context));
        }
        registry.append(Uri.class, InputStream.class, new UriLoader.StreamFactory(contentResolver))
                .append(Uri.class, InputStream.class, new UrlUriLoader.StreamFactory());
        registry.replace(GlideUrl.class, InputStream.class, new ProxyGlideUrlLoader.Factory());
    }
}
