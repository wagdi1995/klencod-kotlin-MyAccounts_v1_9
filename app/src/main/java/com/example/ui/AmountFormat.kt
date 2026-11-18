package com.example.ui

import java.util.Locale

/**
 * تنسيق المبالغ في كامل التطبيق: فواصل الآلاف وبدون أصفار زائدة.
 * 1000 -> 1,000   |   1000.5 -> 1,000.5   |   1000.25 -> 1,000.25
 */
fun fmtMoney(value: Double): String {
  if (value.isNaN() || value.isInfinite()) return "0"
  val r = Math.round(value * 100.0) / 100.0
  if (r == 0.0) return "0"
  return if (r == Math.floor(r)) {
    String.format(Locale.ENGLISH, "%,.0f", r)
  } else {
    String.format(Locale.ENGLISH, "%,.2f", r).trimEnd('0')
  }
}
