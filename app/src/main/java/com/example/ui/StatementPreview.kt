package com.example.ui

import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/** يرسم صفحات ملف PDF كصور تماماً كما تظهر في الملف */
private fun renderPdf(file: File): List<Bitmap> {
  val result = mutableListOf<Bitmap>()
  val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
  val renderer = PdfRenderer(pfd)
  try {
    for (i in 0 until renderer.pageCount) {
      val page = renderer.openPage(i)
      val w = 1000
      val h = (w.toFloat() * page.height / page.width).toInt()
      val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
      bmp.eraseColor(AColor.WHITE)
      page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
      page.close()
      result.add(bmp)
    }
  } finally {
    renderer.close()
    pfd.close()
  }
  return result
}

@Composable
fun StatementPreviewDialog(
  file: File,
  onDismiss: () -> Unit,
  onShare: () -> Unit
) {
  var pages by remember(file) { mutableStateOf<List<Bitmap>?>(null) }
  LaunchedEffect(file) {
    pages = try {
      withContext(Dispatchers.Default) { renderPdf(file) }
    } catch (e: Exception) {
      emptyList()
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF2B3246))
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
        }
        Text("معاينة الكشف", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onShare) {
          Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = Color.White)
        }
      }

      val list = pages
      if (list == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = Color.White)
        }
      } else if (list.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text("تعذر عرض المعاينة", color = Color.White, fontWeight = FontWeight.Bold)
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          itemsIndexed(list) { index, bmp ->
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
              delay(index * 120L)
              shown = true
            }
            AnimatedVisibility(
              visible = shown,
              enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 10 }
            ) {
              Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
        }
      }
    }
  }
}
