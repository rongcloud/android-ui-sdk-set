package io.rong.sticker.util;

import com.google.gson.Gson;
import io.rong.common.rlog.RLog;
import io.rong.imlib.common.NetUtils;
import io.rong.sticker.model.FullResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;

/** Created by luoyanlong on 2018/08/09. */
public class HttpUtil {

    private static final String TAG = HttpUtil.class.getSimpleName();
    private static Gson gson = new Gson();

    public static <T> void get(String urlString, Callback<T> callback) {
        get(urlString, null, callback);
    }

    public static <T> void get(String urlString, Map<String, String> map, Callback<T> callback) {
        try {
            HttpURLConnection connection = NetUtils.createURLConnection(urlString);
            if (map != null) {
                Set<Map.Entry<String, String>> set = map.entrySet();
                for (Map.Entry<String, String> entry : set) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            InputStream inputStream = connection.getInputStream();
            String result = inputStreamToString(inputStream);
            Type[] types = callback.getClass().getGenericInterfaces();
            Type type = ((ParameterizedType) types[0]).getActualTypeArguments()[0];
            FullResponse response = gson.fromJson(result, new ResultType(type));
            callback.onSuccess((T) response.getData());
        } catch (MalformedURLException e) {
            RLog.e(TAG, e.toString());
            callback.onError(e);
        } catch (IOException e) {
            RLog.e(TAG, e.toString());
            callback.onError(e);
        }
    }

    public static <T> T get(String urlString, Map<String, String> map, Type typeOfT)
            throws IOException {
        HttpURLConnection connection = NetUtils.createURLConnection(urlString);
        if (map != null) {
            Set<Map.Entry<String, String>> set = map.entrySet();
            for (Map.Entry<String, String> entry : set) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            InputStream inputStream = connection.getInputStream();
            String result = inputStreamToString(inputStream);
            FullResponse response = gson.fromJson(result, new ResultType(typeOfT));
            return (T) response.getData();
        } else {
            throw new RuntimeException("http error: " + responseCode);
        }
    }

    private static String inputStreamToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }

    public interface Callback<T> {

        void onSuccess(T result);

        void onError(Exception e);
    }

    private static class ResultType implements ParameterizedType {
        private final Type type;

        public ResultType(Type type) {
            this.type = type;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[] {type};
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

        @Override
        public Type getRawType() {
            return FullResponse.class;
        }
    }
}
