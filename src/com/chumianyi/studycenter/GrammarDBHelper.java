package com.chumianyi.studycenter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 英语语法知识点数据库。
 * 表结构：grammar(_id, category, title, explanation, example,
 *         exercise_question, exercise_A, exercise_B, exercise_C, exercise_D,
 *         exercise_answer, exercise_explain, is_learned)
 * 首次启动时从 assets/grammar.json 导入全部数据。
 */
public class GrammarDBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "grammar.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE = "grammar";

    private final Context context;

    public GrammarDBHelper(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
        this.context = ctx.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "category TEXT, "
                + "title TEXT, "
                + "explanation TEXT, "
                + "example TEXT, "
                + "exercise_question TEXT, "
                + "exercise_A TEXT, "
                + "exercise_B TEXT, "
                + "exercise_C TEXT, "
                + "exercise_D TEXT, "
                + "exercise_answer TEXT, "
                + "exercise_explain TEXT, "
                + "is_learned INTEGER DEFAULT 0"
                + ")";
        db.execSQL(sql);
        importFromAssets(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    /** 从 assets/grammar.json 读取并写入数据库。 */
    private void importFromAssets(SQLiteDatabase db) {
        try {
            InputStream is = context.getAssets().open("grammar.json");
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
                    JSONArray opts = o.getJSONArray("exercise_options");
                    ContentValues cv = new ContentValues();
                    cv.put("category", o.optString("category"));
                    cv.put("title", o.optString("title"));
                    cv.put("explanation", o.optString("explanation"));
                    cv.put("example", o.optString("example"));
                    cv.put("exercise_question", o.optString("exercise_question"));
                    cv.put("exercise_A", opts.optString(0));
                    cv.put("exercise_B", opts.optString(1));
                    cv.put("exercise_C", opts.optString(2));
                    cv.put("exercise_D", opts.optString(3));
                    cv.put("exercise_answer", o.optString("exercise_answer"));
                    cv.put("exercise_explain", o.optString("exercise_explain"));
                    cv.put("is_learned", 0);
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

    /** 取所有不重复分类。 */
    public List<String> getCategories() {
        List<String> list = new ArrayList<String>();
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = getReadableDatabase();
            c = db.rawQuery("SELECT DISTINCT category FROM " + TABLE
                    + " ORDER BY _id", null);
            while (c.moveToNext()) {
                list.add(c.getString(0));
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            closeCursor(c);
            closeDb(db);
        }
        return list;
    }

    /** 取某分类下的所有知识点。 */
    public List<GrammarItem> getByCategory(String category) {
        List<GrammarItem> list = new ArrayList<GrammarItem>();
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = getReadableDatabase();
            c = db.rawQuery("SELECT * FROM " + TABLE
                    + " WHERE category=? ORDER BY _id", new String[]{category});
            while (c.moveToNext()) {
                list.add(cursorToItem(c));
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            closeCursor(c);
            closeDb(db);
        }
        return list;
    }

    /** 按 id 取单个知识点。 */
    public GrammarItem getById(long id) {
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = getReadableDatabase();
            c = db.rawQuery("SELECT * FROM " + TABLE + " WHERE _id=?",
                    new String[]{String.valueOf(id)});
            if (c.moveToFirst()) {
                return cursorToItem(c);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            closeCursor(c);
            closeDb(db);
        }
        return null;
    }

    /** 标记为已学。 */
    public void markLearned(long id) {
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("is_learned", 1);
            db.update(TABLE, cv, "_id=?", new String[]{String.valueOf(id)});
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            closeDb(db);
        }
    }

    /** 已学数量。 */
    public int countLearned() {
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = getReadableDatabase();
            c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE + " WHERE is_learned=1", null);
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            closeCursor(c);
            closeDb(db);
        }
        return 0;
    }

    /** 总数。 */
    public int countAll() {
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = getReadableDatabase();
            c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE, null);
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            closeCursor(c);
            closeDb(db);
        }
        return 0;
    }

    /** 清空学习进度（所有 is_learned 归零）。 */
    public void resetLearned() {
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            db.execSQL("UPDATE " + TABLE + " SET is_learned=0");
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            closeDb(db);
        }
    }

    private GrammarItem cursorToItem(Cursor c) {
        GrammarItem it = new GrammarItem();
        it.id = c.getLong(c.getColumnIndex("_id"));
        it.category = c.getString(c.getColumnIndex("category"));
        it.title = c.getString(c.getColumnIndex("title"));
        it.explanation = c.getString(c.getColumnIndex("explanation"));
        it.example = c.getString(c.getColumnIndex("example"));
        it.question = c.getString(c.getColumnIndex("exercise_question"));
        it.optA = c.getString(c.getColumnIndex("exercise_A"));
        it.optB = c.getString(c.getColumnIndex("exercise_B"));
        it.optC = c.getString(c.getColumnIndex("exercise_C"));
        it.optD = c.getString(c.getColumnIndex("exercise_D"));
        it.answer = c.getString(c.getColumnIndex("exercise_answer"));
        it.explain = c.getString(c.getColumnIndex("exercise_explain"));
        it.isLearned = c.getInt(c.getColumnIndex("is_learned")) == 1;
        return it;
    }

    private void closeCursor(Cursor c) {
        if (c != null) {
            try { c.close(); } catch (Throwable ignore) {}
        }
    }
    private void closeDb(SQLiteDatabase db) {
        if (db != null) {
            try { db.close(); } catch (Throwable ignore) {}
        }
    }

    /** 语法知识点 POJO。 */
    public static class GrammarItem {
        public long id;
        public String category;
        public String title;
        public String explanation;
        public String example;
        public String question;
        public String optA;
        public String optB;
        public String optC;
        public String optD;
        public String answer;
        public String explain;
        public boolean isLearned;
    }
}
