package io.rong.sticker.businesslogic;

import android.content.Context;

import io.rong.sticker.model.StickerPackage;
import io.rong.sticker.model.StickerPackageDownloadUrlInfo;
import io.rong.sticker.util.DownloadUtil;
import io.rong.sticker.util.HttpUtil;
import io.rong.sticker.util.ZipUtil;

/**
 * Created by luoyanlong on 2018/08/15.
 * 下载一个表情包
 */
public class StickerPackageDownloadTask {
    private String packageId;

    public StickerPackageDownloadTask(Context context, String packageId) {
        this.packageId = packageId;
    }

    public void downloadStickerPackage(final DownloadUtil.DownloadListener listener, final ZipListener zipListener) {
        StickerPackageApiTask.getStickerPackageDownloadUrl(packageId, new HttpUtil.Callback<StickerPackageDownloadUrlInfo>() {
            @Override
            public void onSuccess(StickerPackageDownloadUrlInfo result) { // 在工作线程中
                DownloadUtil downloadUtil = new DownloadUtil(result.getDownloadUrl());
                String savePath = StickerPackageStorageTask.getStickerHomeDir() + packageId + ".zip";
                downloadUtil.addDownloadListener(new DownloadUtil.DownloadListener() {
                    @Override
                    public void onProgress(int progress) {
                        if (listener != null) {
                            listener.onProgress(progress);
                        }
                    }

                    @Override
                    public void onComplete(String path) {
                        if (listener != null) {
                            listener.onComplete(path);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (listener != null) {
                            listener.onError(e);
                        }
                    }
                });
                downloadUtil.download(savePath);
                unzip(savePath, zipListener);
            }

            @Override
            public void onError(Exception e) {
                listener.onError(e);
            }
        });
    }

    private void unzip(String zipFilePath, ZipListener zipListener) {
        ZipUtil.unzip(zipFilePath);
        StickerPackage stickerPackage = StickerPackageStorageTask.loadStickerPackage(packageId);
        new StickerPackageIconDownloadTask(stickerPackage).execute();
        StickerPackageDbTask.getInstance().setPackageDownload(stickerPackage);
        if (zipListener != null) {
            zipListener.onUnzip(stickerPackage);
        }
    }

    public interface ZipListener {
        void onUnzip(StickerPackage stickerPackage);
    }

}
