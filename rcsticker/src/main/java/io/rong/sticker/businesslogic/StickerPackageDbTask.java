package io.rong.sticker.businesslogic;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import io.rong.common.RLog;
import io.rong.sticker.db.PreloadStickerPackageDeleteTable;
import io.rong.sticker.db.StickerDbHelper;
import io.rong.sticker.db.StickerPackageTable;
import io.rong.sticker.db.StickerTable;
import io.rong.sticker.model.Sticker;
import io.rong.sticker.model.StickerPackage;
import java.util.ArrayList;
import java.util.List;

/** Created by luoyanlong on 2018/08/15. */
public class StickerPackageDbTask {

    private SQLiteDatabase db;

    private static StickerPackageDbTask instance;

    private StickerPackageDbTask() {
        StickerDbHelper dbManager = StickerDbHelper.getInstance();
        try {
            db = dbManager.getWritableDatabase();
        } catch (SQLiteException e) {
            RLog.e("StickerPackageDbTask", "SQLiteException occur :" + e.toString());
        }
    }

    public static void init(Context context, String appKey, String userId) {
        StickerDbHelper.init(context, appKey, userId);
    }

    public static void destroy() {
        StickerDbHelper.destroy();
        instance = null;
    }

    public static synchronized StickerPackageDbTask getInstance() {
        if (instance == null) {
            instance = new StickerPackageDbTask();
        }
        return instance;
    }

    private void savePackage(StickerPackage stickerPackage) {
        if (db == null) {
            return;
        }
        if (StickerPackageTable.exist(db, stickerPackage.getPackageId())) {
            StickerPackageTable.update(db, stickerPackage);
        } else {
            StickerPackageTable.insert(db, stickerPackage);
        }
    }

    /** 更新表情包下载状态 */
    public void setPackageDownload(StickerPackage stickerPackage) {
        if (db == null) {
            return;
        }
        StickerPackageTable.setDownload(
                db, stickerPackage.getPackageId(), stickerPackage.isDownload());
        saveStickers(stickerPackage);
        if (stickerPackage.isPreload() == 1) {
            PreloadStickerPackageDeleteTable.update(db, stickerPackage.getPackageId(), false);
        }
    }

    public boolean isPreloadPackageNeedDownload(String packageId) {
        return !StickerPackageDbTask.getInstance().isPackageDownload(packageId)
                && !StickerPackageDbTask.getInstance().isPreloadPackageDeleted(packageId);
    }

    private boolean isPackageDownload(String packageId) {
        if (db == null) {
            return false;
        }
        return StickerPackageTable.isDownload(db, packageId);
    }

    private boolean isPreloadPackageDeleted(String packageId) {
        if (db == null) {
            return false;
        }
        return PreloadStickerPackageDeleteTable.isDelete(db, packageId);
    }

    public void savePackages(List<StickerPackage> stickerPackages) {
        if (stickerPackages != null) {
            for (StickerPackage stickerPackage : stickerPackages) {
                savePackage(stickerPackage);
            }
        }
    }

    public void deletePackage(String packageId, boolean isPreload) {
        if (db == null) {
            return;
        }
        StickerTable.deleteByPackageId(db, packageId);
        StickerPackageTable.setDownload(db, packageId, false);
        if (isPreload) {
            PreloadStickerPackageDeleteTable.update(db, packageId, true);
        }
    }

    public List<StickerPackage> getDownloadPackages() {
        if (db == null) {
            return new ArrayList<>();
        }
        return StickerPackageTable.getDownloadPackages(db);
    }

    /** 获取推荐表情包，即非预加载且未下载的表情包 */
    public List<StickerPackage> getRecommendPackages() {
        if (db == null) {
            return new ArrayList<>();
        }
        return StickerPackageTable.getRecommendPackages(db);
    }

    private void saveSticker(String packageId, Sticker sticker) {
        if (db == null) {
            return;
        }
        StickerTable.insert(db, packageId, sticker);
    }

    private void saveStickers(StickerPackage stickerPackage) {
        List<Sticker> stickers = stickerPackage.getStickers();
        for (Sticker sticker : stickers) {
            saveSticker(stickerPackage.getPackageId(), sticker);
        }
    }

    /** 排序规则：先按是否预加载排序，预加载（preload=1）在前； 再按order排序，order值小的在前 */
    public List<StickerPackage> getStickerPackages() {
        if (db == null) {
            return new ArrayList<>();
        }
        List<StickerPackage> packages = StickerPackageTable.getAllPackages(db);
        for (StickerPackage stickerPackage : packages) {
            List<Sticker> stickers =
                    StickerTable.getStickersByPackageId(db, stickerPackage.getPackageId());
            stickerPackage.setStickers(stickers);
        }
        return packages;
    }
}
