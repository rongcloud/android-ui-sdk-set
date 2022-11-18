package io.rong.sticker.businesslogic;

import android.content.Context;
import com.google.gson.Gson;
import io.rong.sticker.model.StickerPackagesConfigInfo;
import io.rong.sticker.util.HttpUtil;

/** Created by luoyanlong on 2018/08/23. 所有表情包配置文件的下载与保存/更新 */
public class StickerPackagesConfigTask {

    private StickerPackagesConfigInfo mInfo;

    public void getConfig() {
        StickerPackageApiTask.getAllConfig(
                new HttpUtil.Callback<StickerPackagesConfigInfo>() {
                    @Override
                    public void onSuccess(StickerPackagesConfigInfo result) {
                        mInfo = result;
                    }

                    @Override
                    public void onError(Exception e) {
                        // default implementation ignored
                    }
                });
    }

    public void saveConfig(Context context) {
        saveStickerPackagesInfo();
        refreshInfo();
        if (mInfo != null) {
            PreloadPackageDownloadTask task =
                    new PreloadPackageDownloadTask(context, mInfo.getPreload());
            task.execute();
        }
    }

    private void refreshInfo() {
        StickerPackageApiTask.getAllConfig(
                new HttpUtil.Callback<StickerPackagesConfigInfo>() {
                    @Override
                    public void onSuccess(StickerPackagesConfigInfo result) {
                        mInfo = result;
                        saveStickerPackagesInfo();
                    }

                    @Override
                    public void onError(Exception e) {
                        // default implementation ignored
                    }
                });
    }

    private void saveStickerPackagesInfo() {
        if (mInfo != null) {
            String json = new Gson().toJson(mInfo);
            StickerPackageStorageTask.saveStickerPackagesConfig(json);
            StickerPackageDbTask.getInstance().savePackages(mInfo.getPreload());
            StickerPackageDbTask.getInstance().savePackages(mInfo.getManualLoad());
        }
    }
}
