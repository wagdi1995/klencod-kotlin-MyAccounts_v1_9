package com.example.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.data.model.Account
import com.example.data.model.Category
import com.example.data.model.Currency
import com.example.data.model.CurrencyBalance
import com.example.data.model.MonthlyStat
import com.example.data.model.TransactionItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccountingDatabaseHelper(private val context: Context) : SQLiteOpenHelper(
  context,
  DATABASE_NAME,
  null,
  DATABASE_VERSION
) {

  companion object {
    const val DATABASE_NAME = "myaccounts.db"
    const val DATABASE_VERSION = 3

    @Volatile
    private var instance: AccountingDatabaseHelper? = null

    fun getInstance(context: Context): AccountingDatabaseHelper {
      return instance ?: synchronized(this) {
        instance ?: AccountingDatabaseHelper(context.applicationContext).also { instance = it }
      }
    }
  }

  override fun onCreate(db: SQLiteDatabase) {
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS Currency_tbl (
        c_id INTEGER PRIMARY KEY AUTOINCREMENT,
        c_name VARCHAR UNIQUE NOT NULL
      );
      """.trimIndent()
    )

    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS gategory_tbl (
        ctgry_no INTEGER PRIMARY KEY AUTOINCREMENT,
        ctgry_name VARCHAR UNIQUE,
        is_enabled INTEGER DEFAULT 1
      );
      """.trimIndent()
    )

    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS accounts_tbl (
        account_name VARCHAR,
        ctgry_no INTEGER,
        total_balance NUMERIC DEFAULT 0,
        trans_date DATETIME,
        account_no INTEGER UNIQUE,
        mobile TEXT,
        alarm_date TEXT,
        alram_req_id INTEGER,
        alarm_status INTEGER DEFAULT 0,
        PRIMARY KEY(account_name, ctgry_no, account_no),
        CONSTRAINT fk_ctgry_no FOREIGN KEY(ctgry_no) REFERENCES gategory_tbl(ctgry_no) ON DELETE CASCADE ON UPDATE CASCADE
      );
      """.trimIndent()
    )

    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS details_tbl (
        account_no INTEGER,
        amount NUMERIC,
        trans_date DATETIME,
        details TEXT,
        trans_id INTEGER PRIMARY KEY AUTOINCREMENT,
        total_balance NUMERIC,
        transfer_id INTEGER,
        tranfer_account_no INTEGER,
        img_path TEXT,
        c_id INTEGER DEFAULT 1,
        CONSTRAINT fk_account_no FOREIGN KEY(account_no) REFERENCES accounts_tbl(account_no) ON DELETE CASCADE ON UPDATE CASCADE,
        CONSTRAINT fk_c_id FOREIGN KEY(c_id) REFERENCES Currency_tbl(c_id)
      );
      """.trimIndent()
    )

    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS account_limit_tbl (
        l_trans_id INTEGER PRIMARY KEY AUTOINCREMENT,
        c_id INTEGER,
        account_no INTEGER,
        account_name VARCHAR,
        credit_limit_enbled INTEGER DEFAULT 0,
        debit_limit_enbled INTEGER DEFAULT 0,
        credit_limit NUMERIC,
        debit_limit NUMERIC,
        CONSTRAINT fk_account_no FOREIGN KEY(account_no) REFERENCES accounts_tbl(account_no) ON DELETE CASCADE ON UPDATE CASCADE
      );
      """.trimIndent()
    )

    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS alarm_tbl (
        account_name VARCHAR,
        c_name VARCHAR,
        ctgry_no INTEGER,
        total_balance NUMERIC DEFAULT 0,
        trans_date DATETIME,
        account_no INTEGER UNIQUE,
        mobile TEXT,
        alarm_date TEXT,
        alram_req_id INTEGER,
        alarm_status INTEGER DEFAULT 0,
        c_id INTEGER
      );
      """.trimIndent()
    )

    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS notification_preferences_tbl (
        account_no INTEGER PRIMARY KEY,
        notification_type INTEGER,
        send_via INTEGER,
        shr_btn_aply_auto INTEGER,
        FOREIGN KEY (account_no) REFERENCES accounts_tbl(account_no) ON DELETE CASCADE
      );
      """.trimIndent()
    )

    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS app_info_tbl (
        app_name VARCHAR,
        app_version VARCHAR
      );
      """.trimIndent()
    )

    db.execSQL("CREATE INDEX IF NOT EXISTS idx_accounts_account_no ON accounts_tbl (account_no);")
    db.execSQL("CREATE INDEX IF NOT EXISTS idx_accounts_ctgry_no ON accounts_tbl (ctgry_no);")

    db.execSQL("CREATE INDEX IF NOT EXISTS idx_details_account_no ON details_tbl (account_no);")
    db.execSQL("CREATE INDEX IF NOT EXISTS idx_details_trans_date ON details_tbl (trans_date);")
    db.execSQL("CREATE INDEX IF NOT EXISTS idx_details_account_date ON details_tbl (account_no, trans_date);")
    db.execSQL("CREATE INDEX IF NOT EXISTS idx_details_transfer_id ON details_tbl (transfer_id);")
    db.execSQL("CREATE INDEX IF NOT EXISTS idx_details_c_id ON details_tbl (c_id);")
    seedInitialData(db)
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    // Non-destructive upgrade preserving existing data
    db.execSQL("CREATE TABLE IF NOT EXISTS Currency_tbl (c_id INTEGER PRIMARY KEY AUTOINCREMENT, c_name VARCHAR UNIQUE NOT NULL);")
    db.execSQL("CREATE TABLE IF NOT EXISTS gategory_tbl (ctgry_no INTEGER PRIMARY KEY AUTOINCREMENT, ctgry_name VARCHAR UNIQUE, is_enabled INTEGER DEFAULT 1);")
    db.execSQL("CREATE TABLE IF NOT EXISTS account_limit_tbl (l_trans_id INTEGER PRIMARY KEY AUTOINCREMENT, c_id INTEGER, account_no INTEGER, account_name VARCHAR, credit_limit_enbled INTEGER DEFAULT 0, debit_limit_enbled INTEGER DEFAULT 0, credit_limit NUMERIC, debit_limit NUMERIC);")
    db.execSQL("CREATE INDEX IF NOT EXISTS idx_accounts_account_no ON accounts_tbl (account_no);")
    db.execSQL("CREATE INDEX IF NOT EXISTS idx_accounts_ctgry_no ON accounts_tbl (ctgry_no);")
    db.execSQL("CREATE INDEX IF NOT EXISTS idx_details_account_no ON details_tbl (account_no);")
    db.execSQL("CREATE INDEX IF NOT EXISTS idx_details_trans_date ON details_tbl (trans_date);")
    db.execSQL("CREATE INDEX IF NOT EXISTS idx_details_account_date ON details_tbl (account_no, trans_date);")
    db.execSQL("CREATE INDEX IF NOT EXISTS idx_details_transfer_id ON details_tbl (transfer_id);")
    db.execSQL("CREATE INDEX IF NOT EXISTS idx_details_c_id ON details_tbl (c_id);")
  }

  override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    // نسمح بفتح قاعدة بإصدار أعلى بدل رمي استثناء
  }

  override fun onConfigure(db: SQLiteDatabase) {
    super.onConfigure(db)
    db.setForeignKeyConstraintsEnabled(true)
    // الوضع WAL يسرّع القراءة والكتابة ويمنع تجمّد الواجهة أثناء الحفظ
    db.enableWriteAheadLogging()
  }

  private fun seedInitialData(db: SQLiteDatabase) {
    db.execSQL("INSERT OR IGNORE INTO Currency_tbl (c_id, c_name) VALUES (1, 'ريال يمني (ر.ي)');")
    db.execSQL("INSERT OR IGNORE INTO Currency_tbl (c_id, c_name) VALUES (2, 'دولار أمريكي ($)');")
    db.execSQL("INSERT OR IGNORE INTO Currency_tbl (c_id, c_name) VALUES (3, 'درهم إماراتي (د.إ)');")

    db.execSQL("INSERT OR IGNORE INTO gategory_tbl (ctgry_no, ctgry_name, is_enabled) VALUES (1, 'عام', 1);")
    db.execSQL("INSERT OR IGNORE INTO gategory_tbl (ctgry_no, ctgry_name, is_enabled) VALUES (2, 'عملاء', 1);")
    db.execSQL("INSERT OR IGNORE INTO gategory_tbl (ctgry_no, ctgry_name, is_enabled) VALUES (3, 'موردون', 1);")
    db.execSQL("INSERT OR IGNORE INTO gategory_tbl (ctgry_no, ctgry_name, is_enabled) VALUES (4, 'موظفون', 1);")

    db.execSQL("INSERT OR IGNORE INTO app_info_tbl (app_name, app_version) VALUES ('myaccounts_paid', '3.4.1');")

  }

  private fun insertAccountWithTx(
    db: SQLiteDatabase,
    accountNo: Long,
    accountName: String,
    ctgryNo: Long,
    mobile: String,
    txs: List<Triple<String, Double, String>>
  ) {
    var runningBalance = 0.0
    for ((_, amount, _) in txs) {
      runningBalance += amount
    }

    val accValues = ContentValues().apply {
      put("account_no", accountNo)
      put("account_name", accountName)
      put("ctgry_no", ctgryNo)
      put("mobile", mobile)
      put("total_balance", runningBalance)
      put("trans_date", txs.lastOrNull()?.third ?: SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date()))
      put("alarm_status", 0)
    }
    db.insertWithOnConflict("accounts_tbl", null, accValues, SQLiteDatabase.CONFLICT_REPLACE)

    var currentRunning = 0.0
    for ((details, amount, date) in txs) {
      currentRunning += amount
      val txValues = ContentValues().apply {
        put("account_no", accountNo)
        put("amount", amount)
        put("trans_date", date)
        put("details", details)
        put("total_balance", currentRunning)
        put("c_id", 1)
      }
      db.insert("details_tbl", null, txValues)
    }
  }

  // --- Core Query Operations ---

  fun recalculateAccountBalance(accountNo: Long) {
    val db = writableDatabase
    val cursor = db.rawQuery(
      "SELECT COALESCE(SUM(amount), 0) FROM details_tbl WHERE account_no = ?",
      arrayOf(accountNo.toString())
    )
    var newBal = 0.0
    if (cursor.moveToFirst()) {
      newBal = cursor.getDouble(0)
    }
    cursor.close()

    val values = ContentValues().apply {
      put("total_balance", newBal)
    }
    db.update("accounts_tbl", values, "account_no = ?", arrayOf(accountNo.toString()))
  }

  fun updateRunningBalancesForAccount(accountNo: Long) {
    val db = writableDatabase
    val cursor = db.rawQuery(
      "SELECT trans_id, amount FROM details_tbl WHERE account_no = ? ORDER BY trans_date ASC, trans_id ASC",
      arrayOf(accountNo.toString())
    )
    var run = 0.0
    val updates = mutableListOf<Pair<Long, Double>>()
    while (cursor.moveToNext()) {
      val tId = cursor.getLong(0)
      val amt = cursor.getDouble(1)
      run += amt
      updates.add(tId to run)
    }
    cursor.close()

    for ((tId, b) in updates) {
      val cv = ContentValues().apply { put("total_balance", b) }
      db.update("details_tbl", cv, "trans_id = ?", arrayOf(tId.toString()))
    }
    recalculateAccountBalance(accountNo)
  }

  /** يقرأ ملف قاعدة البيانات كاملاً (بعد تفريغ ملف WAL داخله) لعمل نسخة احتياطية. */
  fun readDatabaseBytes(): ByteArray? {
    return try {
      // تفريغ ملف WAL داخل الملف الرئيسي دون إغلاق الاتصال (الإغلاق كان يسبب انهيار العمليات الجارية)
      writableDatabase.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
      val dbFile = context.getDatabasePath(DATABASE_NAME)
      if (dbFile.exists()) dbFile.readBytes() else null
    } catch (e: Exception) {
      null
    }
  }

  /** ينسخ قاعدة البيانات كاملة إلى ملف [dest] (نسخة احتياطية محلية بصيغة SQLite عادية). */
  fun backupDatabaseTo(dest: File): Boolean {
    return try {
      val bytes = readDatabaseBytes() ?: return false
      dest.parentFile?.mkdirs()
      dest.writeBytes(bytes)
      true
    } catch (e: Exception) {
      false
    }
  }

  /**
   * يستعيد قاعدة البيانات من محتوى SQLite صريح (غير مشفّر).
   * يتحقق من سلامة الملف ووجود الجداول الأساسية قبل استبدال البيانات الحالية.
   */
  fun restoreFromPlainBytes(bytes: ByteArray): Boolean {
    val dbFile = context.getDatabasePath(DATABASE_NAME)
    val tmp = File(dbFile.parentFile, "restore_tmp.db")
    return try {
      tmp.writeBytes(bytes)

      val check = SQLiteDatabase.openDatabase(tmp.path, null, SQLiteDatabase.OPEN_READWRITE)
      try {
        val quick = check.rawQuery("PRAGMA quick_check", null)
        val integrityOk = quick.use { it.moveToFirst() && it.getString(0) == "ok" }
        if (!integrityOk) return false

        for (table in listOf("accounts_tbl", "details_tbl", "Currency_tbl", "gategory_tbl")) {
          val c = check.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(table)
          )
          val exists = c.use { it.moveToFirst() }
          if (!exists) return false
        }

        // ضبط رقم إصدار القاعدة حتى لا يُعاد إنشاؤها ولا يحدث خطأ تراجع عن الإصدار
        val version = check.version
        if (version == 0) check.version = 2
        else if (version > DATABASE_VERSION) check.version = DATABASE_VERSION
      } finally {
        check.close()
      }

      replaceDataFromAttached(tmp)
      true
    } catch (e: Exception) {
      false
    } finally {
      tmp.delete()
      deleteSidecarFiles(tmp)
    }
  }

  /**
   * يستبدل بيانات القاعدة الحالية ببيانات ملف النسخة الاحتياطية دون إغلاق القاعدة الجارية.
   * يربط الملف (ATTACH) وينسخ الأعمدة المشتركة لكل جدول داخل معاملة واحدة، فإن فشل شيء
   * تُلغى العملية كلها وتبقى البيانات الحالية كما هي.
   */
  private fun replaceDataFromAttached(backupFile: File) {
    val db = writableDatabase
    val path = backupFile.path.replace("'", "''")
    db.execSQL("PRAGMA foreign_keys = OFF")
    db.execSQL("ATTACH DATABASE '$path' AS bk")
    try {
      fun tables(schema: String): List<String> {
        val list = mutableListOf<String>()
        db.rawQuery(
          "SELECT name FROM $schema.sqlite_master WHERE type = 'table' " +
            "AND name NOT LIKE 'sqlite_%' AND name <> 'android_metadata'", null
        ).use { c -> while (c.moveToNext()) list.add(c.getString(0)) }
        return list
      }
      fun columns(schema: String, table: String): List<String> {
        val list = mutableListOf<String>()
        db.rawQuery("PRAGMA $schema.table_info(\"$table\")", null).use { c ->
          val idx = c.getColumnIndexOrThrow("name")
          while (c.moveToNext()) list.add(c.getString(idx))
        }
        return list
      }

      val mainTables = tables("main")
      val backupTables = tables("bk").toSet()

      db.beginTransaction()
      try {
        for (t in mainTables) {
          if (t !in backupTables) continue
          val common = columns("main", t).filter { it in columns("bk", t) }
          if (common.isEmpty()) continue
          db.execSQL("DELETE FROM main.\"$t\"")
          val cols = common.joinToString(",") { "\"$it\"" }
          db.execSQL("INSERT INTO main.\"$t\" ($cols) SELECT $cols FROM bk.\"$t\"")
        }
        db.setTransactionSuccessful()
      } finally {
        db.endTransaction()
      }
    } finally {
      try { db.execSQL("DETACH DATABASE bk") } catch (e: Exception) { }
      try { db.execSQL("PRAGMA foreign_keys = ON") } catch (e: Exception) { }
    }
  }

  private fun deleteSidecarFiles(dbFile: File) {
    for (suffix in listOf("-wal", "-shm", "-journal")) {
      File(dbFile.path + suffix).delete()
    }
  }
}
