package com.copas.myapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

// script_name: ClipDbHelper
// author: brruham
// version: 1.0
// Penyimpanan riwayat clipboard. Kolom TEXT SQLite tidak ada limit praktis,
// jadi ribuan karakter aman disimpan tanpa trik khusus.
public class ClipDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "clip_history.db";
    private static final int DB_VERSION = 1;
    public static final String TABLE = "clips";

    public ClipDbHelper(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "content TEXT NOT NULL," +
                "created_at INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public long addClip(String content) {
        if (content == null || content.trim().isEmpty()) return -1;
        SQLiteDatabase db = getWritableDatabase();
        // hindari duplikat berurutan
        Cursor last = db.rawQuery("SELECT content FROM " + TABLE + " ORDER BY _id DESC LIMIT 1", null);
        if (last.moveToFirst() && last.getString(0).equals(content)) {
            last.close();
            return -1;
        }
        last.close();

        ContentValues cv = new ContentValues();
        cv.put("content", content);
        cv.put("created_at", System.currentTimeMillis());
        long id = db.insert(TABLE, null, cv);

        // batasi riwayat max 200 entri biar db tidak bengkak
        db.execSQL("DELETE FROM " + TABLE + " WHERE _id NOT IN " +
                "(SELECT _id FROM " + TABLE + " ORDER BY _id DESC LIMIT 200)");
        return id;
    }

    public List<String> getRecent(int limit) {
        List<String> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT content FROM " + TABLE + " ORDER BY _id DESC LIMIT ?",
                new String[]{String.valueOf(limit)});
        while (c.moveToNext()) {
            out.add(c.getString(0));
        }
        c.close();
        return out;
    }

    public void clearAll() {
        getWritableDatabase().execSQL("DELETE FROM " + TABLE);
    }
}
