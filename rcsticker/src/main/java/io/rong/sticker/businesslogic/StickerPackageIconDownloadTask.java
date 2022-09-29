package io.rong.sticker.businesslogic;

import io.rong.sticker.model.StickerPackage;
import io.rong.sticker.util.DownloadUtil;

/** Created by luoyanlong on 2018/08/24. 下载表情包底部tab图标 */
public class StickerPackageIconDownloadTask {

    private StickerPackage stickerPackage;

    StickerPackageIconDownloadTask(StickerPackage stickerPackage) {
        this.stickerPackage = stickerPackage;
    }

    public void execute() {
        DownloadUtil downloadUtil = new DownloadUtil(stickerPackage.getIcon());
        String savePath = StickerPackageStorageTask.getStickerPackageIconFilePath(stickerPackage);
        downloadUtil.download(savePath);
    }
}
