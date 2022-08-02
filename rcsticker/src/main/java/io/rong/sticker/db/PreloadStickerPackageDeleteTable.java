package io.rong.sticker.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by luoyanlong on 2018/08/28.
 */
public class PreloadStickerPackageDeleteTable {

    public static final String NAME = "preload_sticker_package_delete";

    private static final String COLUMN_PACKAGE_ID = "packageId";

    private static final String COLUMN_IS_DELETE = "isDelete";

    static final String CREATE =
            "CREATE TABLE " + NAME + " (" +
                    COLUMN_PACKAGE_ID + " TEXT PRIMARY KEY, " +
                    COLUMN_IS_DELETE + " BOOLEAN)";

    public static void update(SQLiteDatabase db, String packageId, boolean isDelete) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PACKAGE_ID, packageId);
        cv.put(COLUMN_IS_DELETE, isDelete);
        db.replace(NAME, null, cv);
    }

    public static boolean isDelete(SQLiteDatabase db, String packageId) {
        String sql = "SELECT * FROM " + NAME +
                " WHERE " + COLUMN_PACKAGE_ID + " = ?";
        Cursor cursor = db.rawQuery(sql, new String[]{packageId});
        boolean isDelete = false;
        if (cursor.moveToNext()) {
            isDelete = cursor.getInt(cursor.getColumnIndex(COLUMN_IS_DELETE)) == 1;
        }
        cursor.close();
        return isDelete;
    }

}
