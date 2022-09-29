package io.rong.sticker.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import io.rong.sticker.model.Sticker;
import java.util.ArrayList;
import java.util.List;

/** Created by luoyanlong on 2018/08/08. */
public class StickerTable implements BaseColumns {

    private static final String NAME = "sticker";

    private static final String COLUMN_STICKER_ID = "stickerId";
    private static final String COLUMN_PACKAGE_ID = "packageId";
    private static final String COLUMN_DIGEST = "digest";
    private static final String COLUMN_THUMB_URL = "thumbUrl";
    private static final String COLUMN_URL = "url";
    private static final String COLUMN_ORDER = "stickerOrder";
    private static final String COLUMN_LOCAL_THUMB_URL = "localThumbUrl";
    private static final String COLUMN_LOCAL_URL = "localUrl";
    private static final String COLUMN_WIDTH = "width";
    private static final String COLUMN_HEIGHT = "height";

    static final String CREATE =
            "CREATE TABLE "
                    + NAME
                    + " ("
                    + _ID
                    + " INTEGER PRIMARY KEY, "
                    + COLUMN_STICKER_ID
                    + " TEXT, "
                    + COLUMN_PACKAGE_ID
                    + " TEXT, "
                    + COLUMN_DIGEST
                    + " TEXT, "
                    + COLUMN_THUMB_URL
                    + " TEXT, "
                    + COLUMN_URL
                    + " TEXT, "
                    + COLUMN_ORDER
                    + " TEXT, "
                    + COLUMN_LOCAL_THUMB_URL
                    + " TEXT, "
                    + COLUMN_LOCAL_URL
                    + " TEXT, "
                    + COLUMN_WIDTH
                    + " INTEGER, "
                    + COLUMN_HEIGHT
                    + " INTEGER)";

    public static void insert(SQLiteDatabase db, String packageId, Sticker sticker) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_STICKER_ID, sticker.getStickerId());
        cv.put(COLUMN_PACKAGE_ID, packageId);
        cv.put(COLUMN_DIGEST, sticker.getDigest());
        cv.put(COLUMN_THUMB_URL, sticker.getThumbUrl());
        cv.put(COLUMN_URL, sticker.getUrl());
        cv.put(COLUMN_ORDER, sticker.getOrder());
        cv.put(COLUMN_LOCAL_THUMB_URL, sticker.getLocalThumbUrl());
        cv.put(COLUMN_LOCAL_URL, sticker.getLocalUrl());
        cv.put(COLUMN_WIDTH, sticker.getWidth());
        cv.put(COLUMN_HEIGHT, sticker.getHeight());
        db.insert(NAME, null, cv);
    }

    public static void deleteByPackageId(SQLiteDatabase db, String packageId) {
        String selection = COLUMN_PACKAGE_ID + " = ?";
        String[] selectionArgs = {packageId};
        db.delete(NAME, selection, selectionArgs);
    }

    public static List<Sticker> getStickersByPackageId(SQLiteDatabase db, String packageId) {
        String sql =
                "SELECT * FROM "
                        + StickerTable.NAME
                        + " WHERE "
                        + StickerTable.COLUMN_PACKAGE_ID
                        + " = ?"
                        + " ORDER BY "
                        + StickerTable.COLUMN_ORDER;
        Cursor cursor = db.rawQuery(sql, new String[] {packageId});
        List<Sticker> stickers = new ArrayList<>();
        while (cursor.moveToNext()) {
            Sticker sticker = new Sticker();
            sticker.setStickerId(cursor.getString(cursor.getColumnIndex(COLUMN_STICKER_ID)));
            sticker.setPackageId(cursor.getString(cursor.getColumnIndex(COLUMN_PACKAGE_ID)));
            sticker.setDigest(cursor.getString(cursor.getColumnIndex(COLUMN_DIGEST)));
            sticker.setThumbUrl(cursor.getString(cursor.getColumnIndex(COLUMN_THUMB_URL)));
            sticker.setUrl(cursor.getString(cursor.getColumnIndex(COLUMN_URL)));
            sticker.setLocalThumbUrl(
                    cursor.getString(cursor.getColumnIndex(COLUMN_LOCAL_THUMB_URL)));
            sticker.setLocalUrl(cursor.getString(cursor.getColumnIndex(COLUMN_LOCAL_URL)));
            sticker.setOrder(cursor.getInt(cursor.getColumnIndex(COLUMN_ORDER)));
            sticker.setWidth(cursor.getInt(cursor.getColumnIndex(COLUMN_WIDTH)));
            sticker.setHeight(cursor.getInt(cursor.getColumnIndex(COLUMN_HEIGHT)));
            stickers.add(sticker);
        }
        cursor.close();
        return stickers;
    }
}
