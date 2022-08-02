package io.rong.sticker.businesslogic;

import io.rong.sticker.model.Sticker;
import io.rong.sticker.util.DownloadUtil;

/**
 * Created by luoyanlong on 2018/08/22.
 * 下载一个表情
 */
public class StickerDownloadTask {

    private String packageId;
    private String stickerId;

    StickerDownloadTask(String packageId, String stickerId) {
        this.packageId = packageId;
        this.stickerId = stickerId;
    }

    public void downloadSticker() {
        Sticker sticker = StickerPackageApiTask.getStickerSync(packageId, stickerId);
        if (sticker != null) {
            DownloadUtil downloadThumb = new DownloadUtil(sticker.getThumbUrl());
            String savePath = StickerPackageStorageTask.getStickerThumbFilePath(packageId, stickerId);
            downloadThumb.download(savePath);
            sticker.setLocalThumbUrl(savePath);
            DownloadUtil downloadImage = new DownloadUtil(sticker.getUrl());
            savePath = StickerPackageStorageTask.getStickerImageFilePath(packageId, stickerId);
            downloadImage.download(savePath);
            sticker.setLocalUrl(savePath);
        }
    }

}
