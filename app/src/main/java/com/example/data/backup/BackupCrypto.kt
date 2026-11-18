package com.example.data.backup

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/** نتيجة عملية الاستعادة من ملف */
enum class RestoreStatus { SUCCESS, NEED_KEY, INVALID_FILE, FAILED }

/**
 * صيغة ملف النسخة الاحتياطية (مطابقة لصيغة الملف w.db):
 *   [8 بايت: "MYENCDB:"] + AES/ECB/PKCS5Padding( ملف قاعدة بيانات SQLite كاملاً )
 */
object BackupCrypto {

  private val MAGIC = "MYENCDB:".toByteArray(Charsets.US_ASCII)
  private val SQLITE_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

  /** المفتاح المستخدم لتشفير النسخ التي ينشئها هذا التطبيق (16 بايت = AES-128) */
  private val DEFAULT_KEY = "MyAccountsKey#16".toByteArray(Charsets.US_ASCII)

  private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"

  fun isEncrypted(data: ByteArray): Boolean = startsWith(data, MAGIC)

  fun isPlainSqlite(data: ByteArray): Boolean = startsWith(data, SQLITE_MAGIC)

  fun encrypt(plain: ByteArray): ByteArray {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(DEFAULT_KEY, "AES"))
    return MAGIC + cipher.doFinal(plain)
  }

  /**
   * يفك تشفير الملف. يجرّب مفتاح التطبيق أولاً، ثم مفاتيح مشتقة من [password] إن وُجدت
   * (النص كما هو، أو MD5، أو SHA-256). يرجع null إذا لم ينجح أي مفتاح.
   */
  fun decrypt(data: ByteArray, password: String?): ByteArray? {
    if (!isEncrypted(data)) return null
    val body = data.copyOfRange(MAGIC.size, data.size)
    if (body.isEmpty() || body.size % 16 != 0) return null

    for (key in candidateKeys(password)) {
      try {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
        val plain = cipher.doFinal(body)
        if (isPlainSqlite(plain)) return plain
      } catch (e: Exception) {
        // مفتاح غير صحيح، جرّب التالي
      }
    }
    return null
  }

  private fun candidateKeys(password: String?): List<ByteArray> {
    val keys = mutableListOf<ByteArray>()
    keys.add(DEFAULT_KEY)
    val p = password?.trim().orEmpty()
    if (p.isEmpty()) return keys

    val raw = p.toByteArray(Charsets.UTF_8)
    for (n in intArrayOf(16, 24, 32)) {
      keys.add(raw.copyOf(n)) // اقتطاع أو تتميم بأصفار
    }
    val md5 = MessageDigest.getInstance("MD5").digest(raw)
    val sha1 = MessageDigest.getInstance("SHA-1").digest(raw)
    val sha256 = MessageDigest.getInstance("SHA-256").digest(raw)
    keys.add(md5)
    keys.add(sha1.copyOf(16))
    keys.add(sha256)
    keys.add(sha256.copyOf(16))
    return keys
  }

  private fun startsWith(data: ByteArray, prefix: ByteArray): Boolean {
    if (data.size < prefix.size) return false
    for (i in prefix.indices) {
      if (data[i] != prefix[i]) return false
    }
    return true
  }
}
