package io.github.karino2.itsunani

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.util.*


class DatabaseHolder(val context: Context) {
    companion object {
        val ENTRY_TABLE_NAME = "entry"
    }
    private val TAG = "ItsuNani"
    private val DATABASE_NAME = "itsunani.db"
    private val DATABASE_VERSION = 1

    val dbHelper : SQLiteOpenHelper by lazy {
        object : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL("CREATE TABLE " + ENTRY_TABLE_NAME + " ("
                        + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "BODY TEXT,"
                        + "DATE LONG"
                        + ");");
            }

            fun recreate(db: SQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS $ENTRY_TABLE_NAME")
                onCreate(db)
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
                recreate(db);
            }
        }
    }

    val database by lazy {
        dbHelper.writableDatabase
    }

    fun close() {
        dbHelper.close()
    }
}

class SelectBuilder(val tableName: String) {
    var distinct = false
    var columns = arrayOf<String>()
    var selection : String? = null
    var selectionArgs = arrayOf<String>()
    var groupBy : String? = null
    var having : String? = null
    var orderBy : String? = null

    var limit : String? = null

    fun select(vararg fields: String) {
        columns = arrayOf(*fields)
    }

    fun order(sentence: String) {
        orderBy = sentence
    }

    fun where(whereSentence: String, vararg args: String) {
        selection = whereSentence
        selectionArgs = arrayOf(*args)
    }


    fun exec(db: SQLiteDatabase) : Cursor {
        val columnsArg = if(columns.isEmpty()) null else columns
        val selectionArgsArg = if(selectionArgs.isEmpty()) null else selectionArgs

        return db.query(distinct, tableName, columnsArg, selection, selectionArgsArg, groupBy, having, orderBy, limit)
    }
}

fun DatabaseHolder.query(tableName: String, body: SelectBuilder.()->Unit) : Cursor{
    val builder = SelectBuilder(tableName)
    builder.body()
    return builder.exec(this.database)
}

fun DatabaseHolder.insertEntry(body: String) {
    val values = ContentValues()
    values.put("BODY", body)
    values.put("DATE", Date().time)

    this.database.insert(DatabaseHolder.ENTRY_TABLE_NAME, null, values)
}

fun DatabaseHolder.updateEntry(id: Long, body: String) {
    val (date, _) = getEntry(id)
    val values = ContentValues()
    values.put("BODY", body)
    values.put("DATE", date)
    database.update(DatabaseHolder.ENTRY_TABLE_NAME, values, "_id=?", arrayOf(id.toString()))
}

fun DatabaseHolder.deleteEntries(ids: List<Long>) {
    ids.forEach {
        database.delete(DatabaseHolder.ENTRY_TABLE_NAME, "_id=?", arrayOf(it.toString()))
    }
}

inline fun <reified T> Cursor.withClose(body: Cursor.()->T) : T{
    val res = body()
    this.close()
    return res
}

fun DatabaseHolder.getEntry(id: Long): Pair<Long, String> {
    return query(DatabaseHolder.ENTRY_TABLE_NAME) {
        where("_id=?", id.toString())
    }.withClose{
        moveToFirst()
       Pair(this.getLong(2), this.getString(1))
    }
}