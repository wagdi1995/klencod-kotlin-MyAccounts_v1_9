package com.example.ui

import android.content.Context
import android.content.Intent

/**
 * حالة مشتركة لقفل التطبيق.
 * عند فتح شاشة خارجية (اختيار ملف، مشاركة، اتصال...) يخرج التطبيق مؤقتاً إلى الخلفية،
 * ولا نريد أن يُقفل في هذه الحالة. يتم تصفير العلامة تلقائياً عند العودة (onResume).
 */
object AppSecurity {
  @Volatile
  var suppressLock: Boolean = false

  fun startExternal(context: Context, intent: Intent) {
    suppressLock = true
    context.startActivity(intent)
  }
}
