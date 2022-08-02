package io.rong.sticker.businesslogic;

import android.content.Context;

import java.util.List;

import io.rong.sticker.model.StickerPackage;

/**
 * Created by luoyanlong on 2018/08/22.
 * 下载所有未下载且需要预加载的表情包
 */
public class PreloadPackageDownloadTask {

    private Context context;
    private List<StickerPackage> preloadPackages;

    PreloadPackageDownloadTask(Context context, List<StickerPackage> preloadPackages) {
        this.context = context;
        this.preloadPackages = preloadPackages;
    }

    public void execute() {
        for (StickerPackage preloadPackage : preloadPackages) {
            String packageId = preloadPackage.getPackageId();
            if (StickerPackageDbTask.getInstance().isPreloadPackageNeedDownload(packageId)) {
                StickerPackageDownloadTask task = new StickerPackageDownloadTask(context, packageId);
                task.downloadStickerPackage(null, null);
            }
        }
    }

}
