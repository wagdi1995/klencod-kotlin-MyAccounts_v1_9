package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** ملخص يظهر في مربعات أعلى الكشف. kind: 1 أخضر، -1 أحمر، 0 عادي */
data class StatementTotal(val label: String, val value: String, val kind: Int = 0)

/** بيانات كشف الحساب / التقرير بشكل جدول شبيه بالفاتورة */
data class StatementDoc(
  val title: String,
  val partyName: String?,
  val infoLines: List<Pair<String, String>>,
  val headers: List<String>,
  val rows: List<List<String>>,
  val colWeights: List<Float>,
  val colColors: List<Int?>,
  val totals: List<StatementTotal>,
  val footerRow: List<String>? = null
)

object StatementPdf {
  private const val W = 595
  private const val H = 842
  private const val M = 28f
  private const val FOOTER_TOP = H - 96f
  private const val CONTENT_BOTTOM = H - 104f

  private val NAVY = Color.parseColor("#06154A")
  private val ROYAL = Color.parseColor("#0B3FB8")
  private val BLUE = Color.parseColor("#1E64F0")
  private val ELECTRIC = Color.parseColor("#2F8BFF")
  private val CYAN = Color.parseColor("#37D4F5")
  private val GOLD = Color.parseColor("#F5C04A")
  private val SOFT = Color.parseColor("#EAF1FF")
  private val ZEBRA = Color.parseColor("#F1F6FF")
  private val LINE = Color.parseColor("#C5D5F5")
  private val TEXT = Color.parseColor("#0A1B45")
  private val MUTED = Color.parseColor("#3C4F7C")
  val GREEN: Int = Color.parseColor("#0E9F6E")
  val RED: Int = Color.parseColor("#E02D3C")

  private val numberRegex = Regex("^[-+—]?[\\d,.]*[\\d—]$")

  private fun paint(size: Float, color: Int, align: Paint.Align = Paint.Align.RIGHT): Paint {
    return Paint(Paint.ANTI_ALIAS_FLAG).apply {
      textSize = size
      this.color = color
      textAlign = align
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
  }

  private fun wrap(text: String, p: Paint, maxW: Float, maxLines: Int): List<String> {
    if (text.isEmpty()) return listOf("")
    val lines = mutableListOf<String>()
    var cur = ""
    for (word in text.split(" ")) {
      val cand = if (cur.isEmpty()) word else "$cur $word"
      if (p.measureText(cand) <= maxW) {
        cur = cand
      } else {
        if (cur.isNotEmpty()) { lines.add(cur); cur = "" }
        var w = word
        while (w.isNotEmpty() && p.measureText(w) > maxW) {
          val n = p.breakText(w, true, maxW, null)
          if (n <= 0) break
          lines.add(w.substring(0, n))
          w = w.substring(n)
        }
        cur = w
      }
    }
    if (cur.isNotEmpty()) lines.add(cur)
    if (lines.size > maxLines) {
      val cut = lines.take(maxLines).toMutableList()
      cut[maxLines - 1] = cut[maxLines - 1] + "…"
      return cut
    }
    return if (lines.isEmpty()) listOf("") else lines
  }

  private fun hGrad(l: Float, r: Float, a: Int, b: Int) =
    LinearGradient(l, 0f, r, 0f, a, b, Shader.TileMode.CLAMP)

  // ---------- رسومات مساعدة ----------

  private fun drawWave(c: Canvas, yb: Float, bottom: Float, color: Int, shift: Float = 0f) {
    val path = Path().apply {
      moveTo(0f, yb + 12f + shift)
      cubicTo(W * 0.28f, yb - 20f + shift, W * 0.62f, yb + 30f + shift, W.toFloat(), yb - 8f + shift)
      lineTo(W.toFloat(), bottom)
      lineTo(0f, bottom)
      close()
    }
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    c.drawPath(path, p)
  }

  private fun drawLogo(c: Canvas, cx: Float, cy: Float) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    // مجلد خلفي
    p.color = Color.argb(150, 170, 210, 255)
    c.drawRoundRect(RectF(cx - 30f, cy - 24f, cx + 24f, cy + 26f), 8f, 8f, p)
    // مجلد أمامي
    p.shader = LinearGradient(cx - 24f, cy - 18f, cx + 30f, cy + 30f, Color.WHITE, Color.parseColor("#9CC4FF"), Shader.TileMode.CLAMP)
    c.drawRoundRect(RectF(cx - 24f, cy - 16f, cx + 30f, cy + 32f), 8f, 8f, p)
    p.shader = null
    // أعمدة بيانية
    p.color = BLUE
    c.drawRoundRect(RectF(cx - 14f, cy + 8f, cx - 6f, cy + 26f), 2f, 2f, p)
    p.color = ELECTRIC
    c.drawRoundRect(RectF(cx - 2f, cy - 2f, cx + 6f, cy + 26f), 2f, 2f, p)
    p.color = ROYAL
    c.drawRoundRect(RectF(cx + 10f, cy - 10f, cx + 18f, cy + 26f), 2f, 2f, p)
  }

  private fun drawCalendarIcon(c: Canvas, cx: Float, cy: Float, color: Int) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    c.drawRoundRect(RectF(cx - 9f, cy - 8f, cx + 9f, cy + 9f), 3f, 3f, p)
    p.color = Color.WHITE
    c.drawRect(cx - 7f, cy - 2f, cx + 7f, cy + 7f, p)
    p.color = color
    for (i in 0..2) for (j in 0..1) {
      c.drawRect(cx - 5.5f + i * 4.2f, cy + 0f + j * 3.6f, cx - 3.5f + i * 4.2f, cy + 2f + j * 3.6f, p)
    }
  }

  private fun drawCoinIcon(c: Canvas, cx: Float, cy: Float) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GOLD }
    c.drawOval(RectF(cx - 8f, cy - 1f, cx + 8f, cy + 7f), p)
    p.color = Color.parseColor("#FFD978")
    c.drawOval(RectF(cx - 8f, cy - 5f, cx + 8f, cy + 3f), p)
    p.color = Color.parseColor("#E3A92B")
    c.drawOval(RectF(cx - 4.5f, cy - 3f, cx + 4.5f, cy + 1f), p)
  }

  private fun drawUserIcon(c: Canvas, cx: Float, cy: Float) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    c.drawCircle(cx, cy - 5f, 5f, p)
    c.drawArc(RectF(cx - 10f, cy + 1f, cx + 10f, cy + 19f), 180f, 180f, true, p)
  }

  private fun drawSkyline(c: Canvas) {
    val heights = floatArrayOf(26f, 40f, 32f, 58f, 36f, 70f, 44f, 30f, 52f, 64f, 38f, 28f)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    var x = W * 0.46f
    val bw = 22f
    for (h in heights) {
      p.shader = LinearGradient(0f, FOOTER_TOP + 62f - h, 0f, FOOTER_TOP + 62f,
        Color.argb(90, 46, 120, 240), Color.argb(10, 46, 120, 240), Shader.TileMode.CLAMP)
      c.drawRect(x, FOOTER_TOP + 62f - h, x + bw - 3f, FOOTER_TOP + 62f, p)
      x += bw
      if (x > W - M) break
    }
    p.shader = null
  }

  private fun drawFooter(c: Canvas, pageNo: Int) {
    drawSkyline(c)
    drawWave(c, H - 52f, H.toFloat(), Color.parseColor("#D9E7FF"))
    drawWave(c, H - 38f, H.toFloat(), Color.parseColor("#9DC0FF"), 4f)
    drawWave(c, H - 26f, H.toFloat(), NAVY)
    // عبارة ختامية
    c.drawText("مع تحياتنا", M + 76f, H - 52f, paint(15f, ROYAL, Paint.Align.CENTER))
    val sw = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE; color = CYAN; strokeWidth = 2.2f
    }
    val swoosh = Path().apply {
      moveTo(M, H - 44f)
      quadTo(M + 70f, H - 36f, M + 150f, H - 47f)
    }
    c.drawPath(swoosh, sw)
    c.drawText("صفحة $pageNo", W / 2f, H - 9f, paint(9.5f, Color.WHITE, Paint.Align.CENTER))
  }

  fun create(context: Context, doc: StatementDoc): File {
    val pdf = PdfDocument()
    val tableW = W - 2 * M
    val weights = if (doc.colWeights.size == doc.headers.size) doc.colWeights else List(doc.headers.size) { 1f }
    val sum = weights.sum()
    val colW = weights.map { it / sum * tableW }

    val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE; color = LINE; strokeWidth = 1f
    }
    val whiteLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb(110, 255, 255, 255); strokeWidth = 1f
    }

    var pageNo = 0
    var page: PdfDocument.Page? = null
    var canvas: Canvas? = null
    var y = 0f
    var tableTop = 0f

    fun closeTableBorder(c: Canvas) {
      if (y > tableTop + 34f) {
        c.drawRoundRect(RectF(M, tableTop, W - M, y), 10f, 10f, stroke)
      }
    }

    fun drawTableHeader(c: Canvas) {
      tableTop = y
      fill.shader = hGrad(M, W - M, ROYAL, BLUE)
      val path = Path().apply {
        addRoundRect(RectF(M, y, W - M, y + 36f), floatArrayOf(10f, 10f, 10f, 10f, 0f, 0f, 0f, 0f), Path.Direction.CW)
      }
      c.drawPath(path, fill)
      fill.shader = null
      val pHead = paint(12f, Color.WHITE, Paint.Align.CENTER)
      var right = W - M
      for (i in doc.headers.indices) {
        val left = right - colW[i]
        c.drawText(doc.headers[i], (left + right) / 2, y + 23f, pHead)
        if (i < doc.headers.size - 1) c.drawLine(left, y + 7f, left, y + 29f, whiteLine)
        right = left
      }
      y += 36f
    }

    fun startPage(first: Boolean, withTableHeader: Boolean = true) {
      page?.let { closeTableBorder(it.canvas); pdf.finishPage(it) }
      pageNo++
      val pg = pdf.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
      page = pg
      val c = pg.canvas
      canvas = c
      val wf = W.toFloat()

      if (first) {
        // ===== الترويسة =====
        fill.shader = LinearGradient(0f, 0f, wf, 150f, NAVY, BLUE, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, wf, 156f, fill)
        fill.shader = RadialGradient(wf - 40f, 20f, 190f, Color.argb(120, 55, 212, 245), Color.argb(0, 55, 212, 245), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, wf, 156f, fill)
        fill.shader = RadialGradient(40f, 140f, 160f, Color.argb(90, 47, 139, 255), Color.argb(0, 47, 139, 255), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, wf, 156f, fill)
        fill.shader = null
        drawWave(c, 128f, 170f, Color.argb(120, 150, 190, 255), -8f)
        drawWave(c, 134f, 170f, Color.WHITE)

        drawLogo(c, 72f, 62f)
        c.drawText("مدونة الحسابات", wf / 2f, 66f, paint(32f, Color.WHITE, Paint.Align.CENTER))
        val tp = paint(19f, GOLD, Paint.Align.CENTER)
        c.drawText(doc.title, wf / 2f, 100f, tp)
        val tw = tp.measureText(doc.title) / 2f
        val gl = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GOLD; strokeWidth = 1.6f }
        c.drawLine(wf / 2f - tw - 70f, 94f, wf / 2f - tw - 12f, 94f, gl)
        c.drawLine(wf / 2f + tw + 12f, 94f, wf / 2f + tw + 70f, 94f, gl)

        y = 168f

        // ===== بطاقة البيانات =====
        val infos = doc.infoLines.filter { it.second.isNotBlank() }
        val rowsN = maxOf(infos.size, 2)
        val cardH = 18f + rowsN * 22f
        fill.color = SOFT
        c.drawRoundRect(RectF(M, y, W - M, y + cardH), 16f, 16f, fill)
        val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = LINE; strokeWidth = 1f }
        c.drawRoundRect(RectF(M, y, W - M, y + cardH), 16f, 16f, bp)
        var iy = y + 14f
        for ((label, value) in infos) {
          iy += 8f
          val cy = iy
          when (label) {
            "العملة" -> drawCoinIcon(c, W - M - 22f, cy)
            "الفترة" -> drawCalendarIcon(c, W - M - 22f, cy, BLUE)
            else -> { fill.color = BLUE; c.drawCircle(W - M - 22f, cy, 4f, fill) }
          }
          c.drawText("$label : $value", W - M - 40f, cy + 4.5f, paint(12f, TEXT))
          iy += 14f
        }
        // تاريخ الإصدار على اليسار
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date())
        drawCalendarIcon(c, M + 24f, y + cardH / 2f, ROYAL)
        c.drawText(stamp, M + 44f, y + cardH / 2f - 1f, paint(12f, TEXT, Paint.Align.LEFT))
        c.drawText("تاريخ الإصدار", M + 44f, y + cardH / 2f + 16f, paint(11f, MUTED, Paint.Align.LEFT))
        y += cardH + 16f

        // ===== كبسولة اسم الحساب =====
        val name = doc.partyName.orEmpty()
        if (name.isNotBlank()) {
          val np = paint(17f, Color.WHITE, Paint.Align.CENTER)
          val cw = (np.measureText(name) + 90f).coerceIn(150f, tableW - 40f)
          val l = wf / 2f - cw / 2f
          fill.shader = hGrad(l, l + cw, ROYAL, ELECTRIC)
          c.drawRoundRect(RectF(l, y, l + cw, y + 38f), 19f, 19f, fill)
          fill.shader = null
          drawUserIcon(c, l + cw - 28f, y + 17f)
          c.drawText(name, wf / 2f - 8f, y + 25f, np)
          y += 38f + 16f
        }

        // ===== بطاقات الملخص (اختيارية) =====
        if (doc.totals.isNotEmpty()) {
          val gap = 8f
          val bw = (tableW - gap * (doc.totals.size - 1)) / doc.totals.size
          var right = W - M
          for (t in doc.totals) {
            val left = right - bw
            fill.shader = hGrad(left, right, ROYAL, ELECTRIC)
            c.drawRoundRect(RectF(left, y, right, y + 48f), 12f, 12f, fill)
            fill.shader = null
            c.drawText(t.label, (left + right) / 2, y + 18f, paint(10f, Color.WHITE, Paint.Align.CENTER))
            c.drawText(t.value, (left + right) / 2, y + 38f, paint(14f, Color.WHITE, Paint.Align.CENTER))
            right = left - gap
          }
          y += 48f + 14f
        }
      } else {
        // ترويسة مصغّرة للصفحات التالية
        fill.shader = hGrad(0f, wf, NAVY, BLUE)
        c.drawRect(0f, 0f, wf, 46f, fill)
        fill.shader = null
        drawWave(c, 40f, 60f, Color.WHITE)
        c.drawText("مدونة الحسابات", W - M, 30f, paint(16f, Color.WHITE))
        c.drawText(doc.title + if (!doc.partyName.isNullOrBlank()) " - ${doc.partyName}" else "", M, 30f, paint(12f, GOLD, Paint.Align.LEFT))
        y = 66f
      }
      if (withTableHeader) drawTableHeader(c)
      drawFooter(c, pageNo)
    }

    startPage(true)

    fun rowHeight(cells: List<String>, p: Paint): Pair<Float, List<List<String>>> {
      val wrapped = cells.mapIndexed { i, s -> wrap(s, p, colW[i] - 14f, 3) }
      val lines = wrapped.maxOfOrNull { it.size } ?: 1
      return Pair(maxOf(34f, lines * 15f + 16f), wrapped)
    }

    fun displayCells(cells: List<String>): List<String> =
      cells.mapIndexed { i, s -> if (s.isBlank() && doc.colColors.getOrNull(i) != null) "—" else s }

    fun drawRow(raw: List<String>, index: Int) {
      val cells = displayCells(raw)
      val measure = paint(11.5f, TEXT)
      val (h, wrapped) = rowHeight(cells, measure)
      if (y + h > CONTENT_BOTTOM) startPage(false)
      val c = canvas!!
      fill.color = if (index % 2 == 1) ZEBRA else Color.WHITE
      c.drawRect(M, y, W - M, y + h, fill)
      var right = W - M
      for (i in cells.indices) {
        val left = right - colW[i]
        val isNum = numberRegex.matches(cells[i].trim())
        val p = paint(11.5f, TEXT, if (isNum) Paint.Align.CENTER else Paint.Align.RIGHT)
        val blockH = wrapped[i].size * 15f
        var ly = y + (h - blockH) / 2f + 11.5f
        for (line in wrapped[i]) {
          val x = if (isNum) (left + right) / 2 else right - 8f
          c.drawText(line, x, ly, p)
          ly += 15f
        }
        if (i < cells.size - 1) c.drawLine(left, y, left, y + h, stroke)
        right = left
      }
      c.drawLine(M, y + h, W - M, y + h, stroke)
      y += h
    }

    if (doc.rows.isEmpty()) {
      drawRow(listOf("لا توجد حركات") + List(doc.headers.size - 1) { "" }, 0)
    } else {
      doc.rows.forEachIndexed { idx, r -> drawRow(r, idx) }
    }

    // إغلاق حدود الجدول ثم صف الإجمالي
    closeTableBorder(canvas!!)
    tableTop = y + 100000f // لا نرسم حدوداً إضافية بعد هذه النقطة
    y += 10f
    doc.footerRow?.let { cells ->
      val h = 42f
      if (y + h > CONTENT_BOTTOM) {
        startPage(false, false)
        y = 66f
      }
      val c = canvas!!
      fill.shader = hGrad(M, W - M, ROYAL, ELECTRIC)
      c.drawRoundRect(RectF(M, y, W - M, y + h), 12f, 12f, fill)
      fill.shader = null
      var right = W - M
      for (i in cells.indices) {
        val left = right - colW[i]
        if (i == 0) {
          fill.color = NAVY
          c.drawRoundRect(RectF(left + 6f, y + 6f, right - 6f, y + h - 6f), 9f, 9f, fill)
        }
        val isNum = i >= 2
        val p = paint(13f, Color.WHITE, Paint.Align.CENTER)
        c.drawText(cells[i], (left + right) / 2, y + 26f, p)
        if (i < cells.size - 1 && i > 0) c.drawLine(left, y + 8f, left, y + h - 8f, whiteLine)
        right = left
      }
      y += h
    }

    page?.let { pdf.finishPage(it) }

    val dir = File(context.cacheDir, "reports").apply { mkdirs() }
    val file = File(dir, "statement_${System.currentTimeMillis()}.pdf")
    FileOutputStream(file).use { pdf.writeTo(it) }
    pdf.close()
    return file
  }

  fun share(context: Context, file: File, chooserTitle: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "application/pdf"
      putExtra(Intent.EXTRA_STREAM, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    AppSecurity.startExternal(context, Intent.createChooser(intent, chooserTitle))
  }
}
