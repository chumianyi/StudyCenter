package com.chumianyi.studycenter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 汉字拼音数据库。
 * 表结构：pinyin(_id, character, pinyin, radical, strokes, meaning)
 * 首次启动从 assets/pinyin.json 导入。
 * 同一汉字可能有多条记录（多音字），查询时返回全部。
 */
public class PinyinDBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "pinyin.db";
    private static final int DB_VERSION = 1;
    public static final String TABLE = "pinyin";

    private final Context context;

    public PinyinDBHelper(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
        this.context = ctx.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "character TEXT, "
                + "pinyin TEXT, "
                + "radical TEXT, "
                + "strokes INTEGER, "
                + "meaning TEXT"
                + ")";
        db.execSQL(sql);
        db.execSQL("CREATE INDEX idx_char ON " + TABLE + "(character)");
        importFromAssets(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    private void importFromAssets(SQLiteDatabase db) {
        try {
            InputStream is = context.getAssets().open("pinyin.json");
            int size = is.available();
            byte[] buf = new byte[size];
            is.read(buf);
            is.close();
            String json = new String(buf, "UTF-8");
            JSONArray arr = new JSONArray(json);
            db.beginTransaction();
            try {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    ContentValues cv = new ContentValues();
                    cv.put("character", o.optString("character"));
                    cv.put("pinyin", o.optString("pinyin"));
                    cv.put("radical", o.optString("radical"));
                    cv.put("strokes", o.optInt("strokes", 0));
                    cv.put("meaning", o.optString("meaning"));
                    db.insert(TABLE, null, cv);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /** 查询一个或多个汉字，返回所有匹配记录（含多音字）。 */
    public List<PinyinItem> query(String chars) {
        List<PinyinItem> list = new ArrayList<PinyinItem>();
        if (chars == null) return list;
        chars = chars.trim();
        if (chars.length() == 0) return list;

        // 把每个汉字拆开，逐个查
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = getReadableDatabase();
            for (int i = 0; i < chars.length(); i++) {
                String ch = chars.substring(i, i + 1);
                // 跳过空白和标点
                if (ch.trim().length() == 0) continue;
                c = db.rawQuery("SELECT * FROM " + TABLE
                        + " WHERE character=? ORDER BY _id", new String[]{ch});
                while (c.moveToNext()) {
                    PinyinItem it = new PinyinItem();
                    it.character = c.getString(c.getColumnIndex("character"));
                    it.pinyin = c.getString(c.getColumnIndex("pinyin"));
                    it.radical = c.getString(c.getColumnIndex("radical"));
                    it.strokes = c.getInt(c.getColumnIndex("strokes"));
                    it.meaning = c.getString(c.getColumnIndex("meaning"));
                    list.add(it);
                }
                c.close();
                c = null;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (c != null) {
                try { c.close(); } catch (Throwable ignore) {}
            }
            if (db != null) {
                try { db.close(); } catch (Throwable ignore) {}
            }
        }
        return list;
    }

    /** 总数（用于调试）。 */
    public int countAll() {
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = getReadableDatabase();
            c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE, null);
            if (c.moveToFirst()) return c.getInt(0);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignore) {}
            if (db != null) try { db.close(); } catch (Throwable ignore) {}
        }
        return 0;
    }

    /** 拼音条目 POJO。 */
    public static class PinyinItem {
        public String character;
        public String pinyin;
        public String radical;
        public int strokes;
        public String meaning;
    }
}
