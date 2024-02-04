package com.example.humiditeettemperature

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class Database (context: Context, factory: SQLiteDatabase.CursorFactory?) : SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {

        val query = ("CREATE TABLE " + TABLE_NAME + " (" + POS_COL + " INTEGER PRIMARY KEY, " + DATE_COL + " TEXT," +
                TEMP_COL + " TEXT," + HUMID_COL + " TEXT" + ")")

        db.execSQL(query)
    }



    override fun onUpgrade(db: SQLiteDatabase, p1: Int, p2: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }


    fun addData(date : String, temp : String, humid : String ){

        val values = ContentValues()

        values.put(DATE_COL, date)
        values.put(TEMP_COL, temp)
        values.put(HUMID_COL, humid)

        val db = this.writableDatabase

        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    fun getData(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_NAME", null)
    }


    fun eraseDatabase () {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, null, null);
    }

    fun getPosCount(): Long {
        val db = this.readableDatabase
        val count = DatabaseUtils.queryNumEntries(db, TABLE_NAME)
        db.close()
        return count
    }


    companion object{
        private const val DATABASE_NAME = "Database temp+humid"
        private const val DATABASE_VERSION = 1
        const val TABLE_NAME = "table0"
        const val POS_COL = "position"
        const val DATE_COL = "date"
        const val TEMP_COL = "temperature"
        const val HUMID_COL = "humidity"
    }
}