package io.rong.sticker.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import io.rong.sticker.model.StickerPackage;
import java.util.ArrayList;
import java.util.List;

/** Created by luoyanlong on 2018/08/08. */
public class StickerPackageTable implements BaseColumns {

    public static final String NAME = "sticker_package";

    private static final String COLUMN_PACKAGE_ID = "packageId";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_PRELOAD = "preload";
    private static final String COLUMN_AUTHOR = "author";
    private static final String COLUMN_URL = "url";
    private static final String COLUMN_ICON = "icon";
    private static final String COLUMN_COVER = "cover";
    private static final String COLUMN_COPYRIGHT = "copyright";
    private static final String COLUMN_CREATE_TIME = "createTime";
    private static final String COLUMN_DIGEST = "digest";
    private static final String COLUMN_ORDER = "packageOrder";

    private static final String COLUMN_IS_DOWNLOAD = "is_download";

    static final String CREATE =
            "CREATE TABLE "
                    + NAME
                    + " ("
                    + _ID
                    + " INTEGER PRIMARY KEY, "
                    + COLUMN_PACKAGE_ID
                    + " TEXT UNIQUE, "
                    + COLUMN_NAME
                    + " TEXT, "
                    + COLUMN_PRELOAD
                    + " INTEGER, "
                    + COLUMN_AUTHOR
                    + " TEXT, "
                    + COLUMN_URL
                    + " TEXT, "
                    + COLUMN_ICON
                    + " TEXT, "
                    + COLUMN_COVER
                    + " TEXT, "
                    + COLUMN_COPYRIGHT
                    + " TEXT, "
                    + COLUMN_CREATE_TIME
                    + " INTEGER, "
                    + COLUMN_DIGEST
                    + " TEXT, "
                    + COLUMN_ORDER
                    + " INTEGER, "
                    + COLUMN_IS_DOWNLOAD
                    + " BOOLEAN DEFAULT 0)";

    public static List<StickerPackage> getAllPackages(SQLiteDatabase db) {
        String sql =
                "SELECT * FROM "
                        + StickerPackageTable.NAME
                        + " ORDER BY "
                        + StickerPackageTable.COLUMN_PRELOAD
                        + " DESC, "
                        + StickerPackageTable.COLUMN_ORDER;
        Cursor cursor = db.rawQuery(sql, null);
        List<StickerPackage> packages = new ArrayList<>();
        while (cursor.moveToNext()) {
            StickerPackage stickerPackage = createStickerPackageFromCursor(cursor);
            packages.add(stickerPackage);
        }
        cursor.close();
        return packages;
    }

    /**
     * 表情包是否存在
     *
     * @param packageId 表情包id
     */
    public static boolean exist(SQLiteDatabase db, String packageId) {
        String sql =
                "SELECT "
                        + StickerPackageTable._ID
                        + " FROM "
                        + StickerPackageTable.NAME
                        + " WHERE "
                        + StickerPackageTable.COLUMN_PACKAGE_ID
                        + " = ?";
        Cursor cursor = db.rawQuery(sql, new String[] {packageId});
        boolean exist = cursor.getCount() == 1;
        cursor.close();
        return exist;
    }

    public static void insert(SQLiteDatabase db, StickerPackage stickerPackage) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PACKAGE_ID, stickerPackage.getPackageId());
        cv.put(COLUMN_NAME, stickerPackage.getName());
        cv.put(COLUMN_PRELOAD, stickerPackage.isPreload());
        cv.put(COLUMN_AUTHOR, stickerPackage.getAuthor());
        cv.put(COLUMN_URL, stickerPackage.getUrl());
        cv.put(COLUMN_ICON, stickerPackage.getIcon());
        cv.put(COLUMN_COVER, stickerPackage.getCover());
        cv.put(COLUMN_COPYRIGHT, stickerPackage.getCopyright());
        cv.put(COLUMN_CREATE_TIME, stickerPackage.getCreateTime());
        cv.put(COLUMN_DIGEST, stickerPackage.getDigest());
        cv.put(COLUMN_ORDER, stickerPackage.getOrder());
        db.insert(NAME, null, cv);
    }

    public static void update(SQLiteDatabase db, StickerPackage stickerPackage) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PACKAGE_ID, stickerPackage.getPackageId());
        cv.put(COLUMN_NAME, stickerPackage.getName());
        cv.put(COLUMN_PRELOAD, stickerPackage.isPreload());
        cv.put(COLUMN_AUTHOR, stickerPackage.getAuthor());
        cv.put(COLUMN_URL, stickerPackage.getUrl());
        cv.put(COLUMN_ICON, stickerPackage.getIcon());
        cv.put(COLUMN_COVER, stickerPackage.getCover());
        cv.put(COLUMN_COPYRIGHT, stickerPackage.getCopyright());
        cv.put(COLUMN_CREATE_TIME, stickerPackage.getCreateTime());
        cv.put(COLUMN_DIGEST, stickerPackage.getDigest());
        cv.put(COLUMN_ORDER, stickerPackage.getOrder());
        String where = COLUMN_PACKAGE_ID + " = ?";
        String[] whereArgs = new String[] {stickerPackage.getPackageId()};
        db.update(NAME, cv, where, whereArgs);
    }

    public static void setDownload(SQLiteDatabase db, String packageId, boolean isDownload) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_IS_DOWNLOAD, isDownload);
        String selection = COLUMN_PACKAGE_ID + " = ?";
        String[] selectionArgs = {packageId};
        db.update(NAME, cv, selection, selectionArgs);
    }

    public static boolean isDownload(SQLiteDatabase db, String packageId) {
        String sql =
                "SELECT * FROM "
                        + NAME
                        + " WHERE "
                        + COLUMN_PACKAGE_ID
                        + " = ?"
                        + " AND "
                        + COLUMN_IS_DOWNLOAD
                        + " = 1";
        Cursor cursor = db.rawQuery(sql, new String[] {packageId});
        boolean download = cursor.getCount() == 1;
        cursor.close();
        return download;
    }

    public static List<StickerPackage> getRecommendPackages(SQLiteDatabase db) {
        String sql =
                "SELECT * FROM "
                        + NAME
                        + " WHERE "
                        + COLUMN_PRELOAD
                        + " = 0 AND "
                        + COLUMN_IS_DOWNLOAD
                        + " = 0";
        Cursor cursor = db.rawQuery(sql, null);
        List<StickerPackage> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            StickerPackage stickerPackage = createStickerPackageFromCursor(cursor);
            list.add(stickerPackage);
        }
        return list;
    }

    public static List<StickerPackage> getDownloadPackages(SQLiteDatabase db) {
        String sql = "SELECT * FROM " + NAME + " WHERE " + COLUMN_IS_DOWNLOAD + " = 1";
        Cursor cursor = db.rawQuery(sql, null);
        List<StickerPackage> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            StickerPackage stickerPackage = createStickerPackageFromCursor(cursor);
            list.add(stickerPackage);
        }
        return list;
    }

    private static StickerPackage createStickerPackageFromCursor(Cursor cursor) {
        List<String> columns = new ArrayList<>();
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            columns.add(cursor.getColumnName(i));
        }
        StickerPackage stickerPackage = new StickerPackage();
        for (String column : columns) {
            switch (column) {
                case COLUMN_PACKAGE_ID:
                    stickerPackage.setPackageId(cursor.getString(cursor.getColumnIndex(column)));
                    break;
                case COLUMN_NAME:
                    stickerPackage.setName(cursor.getString(cursor.getColumnIndex(column)));
                    break;
                case COLUMN_PRELOAD:
                    stickerPackage.setPreload(cursor.getInt(cursor.getColumnIndex(column)));
                    break;
                case COLUMN_AUTHOR:
                    stickerPackage.setAuthor(cursor.getString(cursor.getColumnIndex(column)));
                    break;
                case COLUMN_URL:
                    stickerPackage.setUrl(cursor.getString(cursor.getColumnIndex(column)));
                    break;
                case COLUMN_ICON:
                    stickerPackage.setIcon(cursor.getString(cursor.getColumnIndex(column)));
                    break;
                case COLUMN_COVER:
                    stickerPackage.setCover(cursor.getString(cursor.getColumnIndex(column)));
                    break;
                case COLUMN_COPYRIGHT:
                    stickerPackage.setCopyright(cursor.getString(cursor.getColumnIndex(column)));
                    break;
                case COLUMN_CREATE_TIME:
                    stickerPackage.setCreateTime(cursor.getString(cursor.getColumnIndex(column)));
                    break;
                case COLUMN_DIGEST:
                    stickerPackage.setDigest(cursor.getString(cursor.getColumnIndex(column)));
                    break;
                case COLUMN_ORDER:
                    stickerPackage.setOrder(cursor.getInt(cursor.getColumnIndex(column)));
                    break;
                case COLUMN_IS_DOWNLOAD:
                    stickerPackage.setDownload(cursor.getInt(cursor.getColumnIndex(column)) == 1);
                default:
                    break;
            }
        }
        return stickerPackage;
    }
}
