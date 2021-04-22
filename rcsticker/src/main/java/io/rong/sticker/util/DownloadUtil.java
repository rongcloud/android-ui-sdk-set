package io.rong.sticker.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import io.rong.common.rlog.RLog;
import io.rong.sticker.model.StickerPackage;

/**
 * Created by luoyanlong on 2018/08/07.
 * 下载器，没有做线程切换
 */
public class DownloadUtil {
    private static final String TAG = "DownloadUtil";
    private String urlString;
    private List<DownloadListener> listeners = new ArrayList<>();

    public DownloadUtil(String url) {
        urlString = url;
    }

    public void download(String savePath) {
        OutputStream output = null;
        InputStream input = null;
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.connect();
            int totalLength = connection.getContentLength();
            InputStream inputStream = connection.getInputStream();
            input = new BufferedInputStream(inputStream, 8192);
            File file = new File(savePath);
            if (!file.getParentFile().exists()) {
                boolean successMkdir = file.getParentFile().mkdirs();
                if (!successMkdir) {
                    RLog.e(TAG, "download created folders unSuccessfully");
                }
            }
            output = new FileOutputStream(savePath);
            int downloadSize = 0;
            byte[] data = new byte[1024];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
                downloadSize += count;
                int progress = (int) ((float) downloadSize / totalLength * 100);
                notifyListenersOnProgress(progress);
            }
            output.flush();
            notifyListenersOnComplete(savePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            notifyListenersOnError(e);
        } catch (IOException e) {
            e.printStackTrace();
            notifyListenersOnError(e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    RLog.e(TAG, e.toString());
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    RLog.e(TAG, e.toString());
                }
            }
        }
    }

    public void addDownloadListener(DownloadListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    private void notifyListenersOnProgress(int progress) {
        for (DownloadListener listener : listeners) {
            listener.onProgress(progress);
        }
    }

    private void notifyListenersOnComplete(String path) {
        for (DownloadListener listener : listeners) {
            listener.onComplete(path);
        }
    }

    private void notifyListenersOnError(Exception e) {
        for (DownloadListener listener : listeners) {
            listener.onError(e);
        }
    }

    public interface DownloadListener {
        void onProgress(int progress);

        void onComplete(String path);

        void onError(Exception e);
    }

}
