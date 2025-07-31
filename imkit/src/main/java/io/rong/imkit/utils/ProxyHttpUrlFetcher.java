package io.rong.imkit.utils;

import android.text.TextUtils;
import android.util.Base64;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.HttpException;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.util.ContentLengthInputStream;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Synthetic;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.RCIMProxy;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** @author gusd @Date 2022/09/08 */
public class ProxyHttpUrlFetcher implements DataFetcher<InputStream> {

    private static final String TAG = "ProxyHttpUrlFetcher";
    private static final int MAXIMUM_REDIRECTS = 5;
    static final HttpUrlConnectionFactory DEFAULT_CONNECTION_FACTORY =
            new DefaultHttpUrlConnectionFactory();

    private final GlideUrl glideUrl;
    private final int timeout;
    private final HttpUrlConnectionFactory connectionFactory;

    private HttpURLConnection urlConnection;
    private InputStream stream;
    private volatile boolean isCancelled;

    public ProxyHttpUrlFetcher(GlideUrl glideUrl, int timeout) {
        this(glideUrl, timeout, DEFAULT_CONNECTION_FACTORY);
    }

    // Visible for testing.
    ProxyHttpUrlFetcher(
            GlideUrl glideUrl, int timeout, HttpUrlConnectionFactory connectionFactory) {
        this.glideUrl = glideUrl;
        this.timeout = timeout;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void loadData(Priority priority, DataCallback<? super InputStream> callback) {
        long startTime = LogTime.getLogTime();
        final InputStream result;
        try {
            result =
                    loadDataWithRedirects(
                            glideUrl.toURL(),
                            0 /*redirects*/,
                            null /*lastUrl*/,
                            glideUrl.getHeaders());
        } catch (IOException e) {
            callback.onLoadFailed(e);
            return;
        }

        callback.onDataReady(result);
    }

    private InputStream loadDataWithRedirects(
            URL url, int redirects, URL lastUrl, Map<String, String> headers) throws IOException {
        if (redirects >= MAXIMUM_REDIRECTS) {
            throw new HttpException("Too many (> " + MAXIMUM_REDIRECTS + ") redirects!");
        } else {
            // Comparing the URLs using .equals performs additional network I/O and is generally
            // broken.
            // See
            // http://michaelscharf.blogspot.com/2006/11/javaneturlequals-and-hashcode-make.html.
            try {
                if (lastUrl != null && url.toURI().equals(lastUrl.toURI())) {
                    throw new HttpException("In re-direct loop");
                }
            } catch (URISyntaxException e) {
                // Do nothing, this is best effort.
            }
        }

        urlConnection = connectionFactory.build(url);
        for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
            urlConnection.addRequestProperty(headerEntry.getKey(), headerEntry.getValue());
        }

        RCIMProxy currentProxy = RongIMClient.getInstance().getCurrentProxy();
        if (currentProxy != null
                && currentProxy.isValid()
                && !TextUtils.isEmpty(currentProxy.getUserName())
                && !TextUtils.isEmpty(currentProxy.getHost())) {
            String headerKey = "Proxy-Authorization";
            String headerValue =
                    "Basic "
                            + Base64.encode(
                                            (currentProxy.getUserName()
                                                            + ":"
                                                            + currentProxy.getPassword())
                                                    .getBytes(StandardCharsets.UTF_8),
                                            Base64.DEFAULT)
                                    .toString();
            urlConnection.setRequestProperty(headerKey, headerValue);
        }

        urlConnection.setConnectTimeout(timeout);
        urlConnection.setReadTimeout(timeout);
        urlConnection.setUseCaches(false);
        urlConnection.setDoInput(true);

        // Stop the urlConnection instance of HttpUrlConnection from following redirects so that
        // redirects will be handled by recursive calls to this method, loadDataWithRedirects.
        urlConnection.setInstanceFollowRedirects(false);

        // Connect explicitly to avoid errors in decoders if connection fails.
        urlConnection.connect();
        if (isCancelled) {
            return null;
        }
        final int statusCode = urlConnection.getResponseCode();
        if (statusCode / 100 == 2) {
            return getStreamForSuccessfulRequest(urlConnection);
        } else if (statusCode / 100 == 3) {
            String redirectUrlString = urlConnection.getHeaderField("Location");
            if (TextUtils.isEmpty(redirectUrlString)) {
                throw new HttpException("Received empty or null redirect url");
            }
            URL redirectUrl = new URL(url, redirectUrlString);
            return loadDataWithRedirects(redirectUrl, redirects + 1, url, headers);
        } else if (statusCode == -1) {
            throw new HttpException(statusCode);
        } else {
            throw new HttpException(urlConnection.getResponseMessage(), statusCode);
        }
    }

    private InputStream getStreamForSuccessfulRequest(HttpURLConnection urlConnection)
            throws IOException {
        if (TextUtils.isEmpty(urlConnection.getContentEncoding())) {
            int contentLength = urlConnection.getContentLength();
            stream = ContentLengthInputStream.obtain(urlConnection.getInputStream(), contentLength);
        } else {

            stream = urlConnection.getInputStream();
        }
        return stream;
    }

    @Override
    public void cleanup() {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        if (urlConnection != null) {
            urlConnection.disconnect();
        }
    }

    @Override
    public void cancel() {
        // TODO: we should consider disconnecting the url connection here, but we can't do so
        // directly because cancel is often called on the main thread.
        isCancelled = true;
    }

    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @Override
    public DataSource getDataSource() {
        return DataSource.REMOTE;
    }

    interface HttpUrlConnectionFactory {
        HttpURLConnection build(URL url) throws IOException;
    }

    private static class DefaultHttpUrlConnectionFactory implements HttpUrlConnectionFactory {

        @Synthetic
        DefaultHttpUrlConnectionFactory() {
            // default implementation ignored
        }

        @Override
        public HttpURLConnection build(URL url) throws IOException {
            // 代理服务器配置
            RCIMProxy currentProxy = RongIMClient.getInstance().getCurrentProxy();
            if (currentProxy != null && currentProxy.isValid()) {
                Proxy proxy =
                        new Proxy(
                                Proxy.Type.SOCKS,
                                new InetSocketAddress(
                                        currentProxy.getHost(), currentProxy.getPort()));
                return (HttpURLConnection) url.openConnection(proxy);
            } else {
                return (HttpURLConnection) url.openConnection();
            }
        }
    }
}
