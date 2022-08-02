package io.rong.sticker.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

/**
 * Created by luoyanlong on 2018/08/08.
 * 表情包数据库
 * 负责 Sticker.db 数据库内表的创建和更新
 * 里面的表包括：
 * 1. 表情包表(sticker_package)
 * 2. 表情表(sticker)
 * 每一个用户有自己的一份 Sticker.db，实现多账户体系
 */
public class StickerDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "Sticker.db";
    private static final int DB_VERSION = 1;

    private static StickerDbHelper instance;
    private static String sDbPath;

    public static void init(Context context, String appKey, String userId) {
        String dbPath = getDbPath(context, appKey, userId);
        if (!dbPath.equals(sDbPath) || instance == null) {
            sDbPath = dbPath;
            instance = new StickerDbHelper(context);
        }
    }

    public static void destroy() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }

    /**
     * /data/data/包名/files/appKey/userId/Sticker.db
     */
    private static String getDbPath(Context context, String appKey, String userId) {
        String[] pathArray = new String[] {context.getFilesDir().toString(), appKey, userId, DB_NAME};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pathArray.length; i++) {
            sb.append(pathArray[i]);
            if (i < pathArray.length - 1) {
                sb.append(File.separator);
            }
        }
        return sb.toString();
    }

    public static StickerDbHelper getInstance() {
        return instance;
    }

    private StickerDbHelper(Context context) {
        super(context.getApplicationContext(), sDbPath, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(StickerPackageTable.CREATE);
        db.execSQL(StickerTable.CREATE);
        db.execSQL(PreloadStickerPackageDeleteTable.CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
