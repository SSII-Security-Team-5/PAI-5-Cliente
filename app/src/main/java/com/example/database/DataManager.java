package com.example.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DataManager {
    private Context context;
    private DatabaseHelper databaseHelper;
    private SQLiteDatabase database;

    public DataManager(Context context) {
        this.context = context;
        databaseHelper = new DatabaseHelper(context);
    }

    public void open() {
        database = databaseHelper.getWritableDatabase();
    }

    public void close() {
        databaseHelper.close();
    }

    public void createDatabase() {
        String dropTableQuery = "DROP TABLE IF EXISTS mytable";
        database.execSQL(dropTableQuery);
        String createTableQuery = "CREATE TABLE IF NOT EXISTS publickeys " +
                "(id INTEGER PRIMARY KEY, username TEXT UNIQUE, password TEXT, pkey TEXT)";
        database.execSQL(createTableQuery);
    }

    public String getData(String username, String password) {
        String query = "SELECT " + "*" + " FROM " + "publickeys WHERE username" + " = ?" + " AND " + "password" + " = ?";
        Cursor cursor = database.rawQuery(query, new String[]{username, password});
        String pkey = null;
        if (cursor.moveToFirst()) {
            pkey = cursor.getString(cursor.getColumnIndex("pkey"));
        }
        cursor.close();

        return pkey;
    }

    public void insertData(int id, String username, String hashedPassword, String pkey) {
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("username", username);
        values.put("password", hashedPassword);
        values.put("pkey", pkey);
        database.insert("publickeys", null, values);
    }
}