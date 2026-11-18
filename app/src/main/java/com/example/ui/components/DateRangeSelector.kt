package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RoyalBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private data class QuickRange(val label: String, val from: String, val to: String)

private fun fmt(cal: Calendar): String =
  SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(cal.time)

private fun utcFmt(millis: Long): String =
  SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(millis)

private fun parseUtc(s: String): Long? = try {
  if (s.isBlank()) null
  else SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(s)?.time
} catch (e: Exception) {
  null
}

private fun buildQuickRanges(): List<QuickRange> {
  val today = Calendar.getInstance()
  val t = fmt(today)

  val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -1) }

  val week = (today.clone() as Calendar).apply {
    firstDayOfWeek = Calendar.SATURDAY
    while (get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) add(Calendar.DAY_OF_MONTH, -1)
  }

  val monthStart = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }

  val lastMonthStart = (today.clone() as Calendar).apply {
    set(Calendar.DAY_OF_MONTH, 1)
    add(Calendar.MONTH, -1)
  }
  val lastMonthEnd = (lastMonthStart.clone() as Calendar).apply {
    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
  }

  val yearStart = (today.clone() as Calendar).apply {
    set(Calendar.MONTH, Calendar.JANUARY)
    set(Calendar.DAY_OF_MONTH, 1)
  }

  return listOf(
    QuickRange("اليوم", t, t),
    QuickRange("أمس", fmt(yesterday), fmt(yesterday)),
    QuickRange("هذا الأسبوع", fmt(week), t),
    QuickRange("هذا الشهر", fmt(monthStart), t),
    QuickRange("الشهر الماضي", fmt(lastMonthStart), fmt(lastMonthEnd)),
    QuickRange("هذه السنة", fmt(yearStart), t)
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSelector(
  fromDate: String,
  toDate: String,
  onChange: (from: String, to: String) -> Unit,
  modifier: Modifier = Modifier
) {
  var showPicker by remember { mutableStateOf(false) }
  val quick = remember { buildQuickRanges() }

  Column(modifier = modifier.fillMaxWidth()) {
    // اختيارات سريعة
    Row(
      modifier = Modifier.horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FilterChip(
        selected = fromDate.isBlank() && toDate.isBlank(),
        onClick = { onChange("", "") },
        label = { Text("كل الفترات", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
      )
      quick.forEach { q ->
        FilterChip(
          selected = q.from == fromDate && q.to == toDate,
          onClick = { onChange(q.from, q.to) },
          label = { Text(q.label, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // حقلا التاريخ: الضغط يفتح التقويم
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      DateBox(label = "من تاريخ", value = fromDate, modifier = Modifier.weight(1f)) { showPicker = true }
      DateBox(label = "إلى تاريخ", value = toDate, modifier = Modifier.weight(1f)) { showPicker = true }
    }

    val a = parseUtc(fromDate)
    val b = parseUtc(toDate)
    if (a != null && b != null) {
      val days = ((b - a) / 86_400_000L) + 1
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "الفترة: من $fromDate إلى $toDate  •  $days يوم",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = RoyalBlue
      )
    }
  }

  if (showPicker) {
    val state = rememberDateRangePickerState(
      initialSelectedStartDateMillis = parseUtc(fromDate),
      initialSelectedEndDateMillis = parseUtc(toDate)
    )
    DatePickerDialog(
      onDismissRequest = { showPicker = false },
      confirmButton = {
        TextButton(
          enabled = state.selectedStartDateMillis != null,
          onClick = {
            val s = state.selectedStartDateMillis
            val e = state.selectedEndDateMillis ?: s
            if (s != null && e != null) onChange(utcFmt(s), utcFmt(e))
            showPicker = false
          }
        ) { Text("تأكيد", fontWeight = FontWeight.Bold) }
      },
      dismissButton = {
        TextButton(onClick = { showPicker = false }) { Text("إلغاء") }
      }
    ) {
      DateRangePicker(
        state = state,
        modifier = Modifier.height(520.dp)
      )
    }
  }
}

@Composable
private fun DateBox(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  val shape = RoundedCornerShape(14.dp)
  Row(
    modifier = modifier
      .clip(shape)
      .border(BorderStroke(1.dp, if (value.isBlank()) Color(0xFFC5D0E6) else RoyalBlue), shape)
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(label, fontSize = 11.sp, color = Color(0xFF6B7789), fontWeight = FontWeight.Bold)
      Text(
        text = if (value.isBlank()) "اختر التاريخ" else value,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = if (value.isBlank()) Color(0xFF9AA6BA) else Color(0xFF0F1B33)
      )
    }
    Icon(
      imageVector = Icons.Default.DateRange,
      contentDescription = null,
      tint = RoyalBlue,
      modifier = Modifier.size(22.dp)
    )
  }
}
