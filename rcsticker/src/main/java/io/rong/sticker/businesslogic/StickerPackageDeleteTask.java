package io.rong.sticker.businesslogic;

/**
 * Created by luoyanlong on 2018/08/22.
 * 删除一个表情包
 */
public class StickerPackageDeleteTask {

    private String packageId;
    private boolean isPreload;

    public StickerPackageDeleteTask(String packageId, boolean isPreload) {
        this.packageId = packageId;
        this.isPreload = isPreload;
    }

    public void delete() {
        StickerPackageDbTask.getInstance().deletePackage(packageId, isPreload);
        StickerPackageStorageTask.deleteStickerPackage(packageId);
    }
}
