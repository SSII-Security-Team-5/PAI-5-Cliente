package com.example.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "mydatabase.db";
    private static final int DATABASE_VERSION = 2;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create your database tables here
        String dropTableQuery = "DROP TABLE IF EXISTS mytable";
        db.execSQL(dropTableQuery);
        String createTableQuery = "CREATE TABLE IF NOT EXISTS publickeys " +
                "(id INTEGER PRIMARY KEY, username TEXT UNIQUE, password TEXT, pkey TEXT)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Perform database upgrade operations here
        if (oldVersion < 2) {
            // Upgrade the schema or migrate data as needed
        }
    }
}
