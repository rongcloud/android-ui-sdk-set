package io.rong.sticker.businesslogic;

import android.content.Context;
import com.google.gson.Gson;
import io.rong.sticker.model.Sticker;
import io.rong.sticker.model.StickerPackage;
import io.rong.sticker.model.StickerPackageInfo;
import io.rong.sticker.util.FileUtil;
import java.io.File;
import java.util.List;
import java.util.Locale;

/** Created by luoyanlong on 2018/08/15. 表情包文件存储逻辑 */
public class StickerPackageStorageTask {

    private static final String STICKER_DIR = "sticker";

    /** 所有表情包配置文件 */
    private static final String STICKER_PACKAGES_CONFIG_FILE = "StickerPackagesConfig.json";

    /** 单个表情包配置文件 */
    private static final String STICKER_CONFIG_FILE = "meta.json";

    private static final String IMAGE_FORMAT = "image_%s.gif";
    private static final String THUMB_FORMAT = "thumb_%s.png";

    private static String sStickerHomeDir;

    public static void init(Context context, String appKey, String userId) {
        sStickerHomeDir = getPath(context, appKey, userId);
    }

    private static String getPath(Context context, String appKey, String userId) {
        String[] pathArray =
                new String[] {context.getFilesDir().toString(), appKey, userId, STICKER_DIR};
        StringBuilder sb = new StringBuilder();
        for (String path : pathArray) {
            sb.append(path).append(File.separator);
        }
        return sb.toString();
    }

    public static void saveStickerPackagesConfig(String json) {
        File configFile = getStickerPackagesConfigFile();
        FileUtil.writeStringToFile(json, configFile);
    }

    public static String getStickerHomeDir() {
        return sStickerHomeDir;
    }

    private static File getStickerPackagesConfigFile() {
        String filePath = getStickerHomeDir() + STICKER_PACKAGES_CONFIG_FILE;
        File file = new File(filePath);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
        return file;
    }

    /** 获取一个表情包的配置文件 */
    private static File getStickerPackageConfigFile(String packageId) {
        String filePath = getStickerPackageFolderPath(packageId) + STICKER_CONFIG_FILE;
        return new File(filePath);
    }

    private static String getStickerPackageFolderPath(String packageId) {
        return getStickerHomeDir() + packageId + File.separator;
    }

    public static String getStickerImageFilePath(String packageId, String stickerId) {
        return getStickerPackageFolderPath(packageId) + getStickerImageFileName(stickerId);
    }

    private static String getStickerImageFileName(String stickerId) {
        return String.format(Locale.getDefault(), IMAGE_FORMAT, stickerId);
    }

    public static boolean isStickerExist(String packageId, String stickerId) {
        String path = getStickerImageFilePath(packageId, stickerId);
        File file = new File(path);
        return file.exists();
    }

    public static String getStickerThumbFilePath(String packageId, String stickerId) {
        return getStickerPackageFolderPath(packageId) + getStickerThumbFileName(stickerId);
    }

    private static String getStickerThumbFileName(String stickerId) {
        return String.format(Locale.getDefault(), THUMB_FORMAT, stickerId);
    }

    public static void deleteStickerPackage(String packageId) {
        String folder = getStickerPackageFolderPath(packageId);
        FileUtil.recursiveDelete(new File(folder));
    }

    public static StickerPackage loadStickerPackage(String packageId) {
        File file = StickerPackageStorageTask.getStickerPackageConfigFile(packageId);
        String json = FileUtil.toString(file);
        StickerPackageInfo info = new Gson().fromJson(json, StickerPackageInfo.class);
        StickerPackage stickerPackage = info.getMetainfo();
        List<Sticker> stickers = info.getStickers();
        for (Sticker sticker : stickers) {
            sticker.setPackageId(packageId);
            sticker.setLocalUrl(
                    getStickerImageFilePath(sticker.getPackageId(), sticker.getStickerId()));
            sticker.setLocalThumbUrl(
                    getStickerThumbFilePath(sticker.getPackageId(), sticker.getStickerId()));
        }
        stickerPackage.setStickers(stickers);
        stickerPackage.setDownload(true);
        return stickerPackage;
    }

    public static String getStickerPackageIconFilePath(StickerPackage stickerPackage) {
        String packageDir = getStickerPackageFolderPath(stickerPackage.getPackageId());
        return packageDir + getUrlLastPath(stickerPackage.getIcon());
    }

    private static String getUrlLastPath(String url) {
        int index = url.lastIndexOf("/");
        return url.substring(index + 1);
    }
}
