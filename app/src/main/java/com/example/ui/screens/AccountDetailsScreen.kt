package com.example.ui.screens

import com.example.ui.fmtMoney
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountDetailsSummary
import com.example.data.model.Currency
import com.example.data.model.TransactionItem
import com.example.ui.StatementDoc
import com.example.ui.StatementPdf
import com.example.ui.StatementTotal
import com.example.ui.theme.CardBackground
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.NegativeRedSoft
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenSoft
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SecondaryBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftBlue
import java.util.Locale
import kotlin.math.abs

@Composable
fun AccountDetailsScreen(
  summary: AccountDetailsSummary,
  transactions: List<TransactionItem>,
  currencies: List<Currency>,
  selectedCurrencyId: Long?,
  selectedFilterType: String, // "ALL", "INCOMING", "OUTGOING"
  onCurrencySelect: (Long?) -> Unit,
  onFilterTypeSelect: (String) -> Unit,
  onBackClick: () -> Unit,
  onAddTransactionClick: () -> Unit,
  onTransactionClick: (TransactionItem) -> Unit,
  modifier: Modifier = Modifier
) {
  var filterMenuExpanded by remember { mutableStateOf(false) }
  var currencyMenuExpanded by remember { mutableStateOf(false) }

  val account = summary.account
  val context = LocalContext.current

  var previewFile by remember { mutableStateOf<java.io.File?>(null) }

  fun buildFile(): java.io.File? = try {
    StatementPdf.create(context, buildStatementDoc(summary, transactions, currencies, selectedCurrencyId))
  } catch (e: Exception) {
    android.widget.Toast.makeText(context, "تعذر إنشاء كشف الحساب", android.widget.Toast.LENGTH_SHORT).show()
    null
  }

  val shareStatement: () -> Unit = {
    buildFile()?.let { StatementPdf.share(context, it, "مشاركة كشف الحساب") }
  }
  val previewStatement: () -> Unit = {
    previewFile = buildFile()
  }

  previewFile?.let { pf ->
    com.example.ui.StatementPreviewDialog(
      file = pf,
      onDismiss = { previewFile = null },
      onShare = { StatementPdf.share(context, pf, "مشاركة كشف الحساب") }
    )
  }

  val icon = when {
    account.accountName.contains("شركة") -> Icons.Default.Business
    account.accountName.contains("مؤسسة") || account.accountName.contains("متجر") -> Icons.Default.Storefront
    else -> Icons.Default.Person
  }

  val formattedBalance = fmtMoney(summary.currentBalance) + " ر.ي"

  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Box(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .testTag("account_details_screen")
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 90.dp)
      ) {
        // Top Bar
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = onBackClick,
              modifier = Modifier.testTag("back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "رجوع",
                tint = PrimaryText
              )
            }
            Text(
              text = "كشف حساب",
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              color = PrimaryText,
              modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
              onClick = previewStatement,
              modifier = Modifier.testTag("preview_statement_button")
            ) {
              Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = "معاينة الكشف",
                tint = RoyalBlue
              )
            }
            IconButton(
              onClick = shareStatement,
              modifier = Modifier.testTag("share_statement_button")
            ) {
              Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "مشاركة كشف الحساب",
                tint = RoyalBlue
              )
            }
          }
        }

        // Account Main Header Card matching Screenshot 3
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 6.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
              ) {
                Column(
                  horizontalAlignment = Alignment.End,
                  modifier = Modifier.padding(end = 14.dp)
                ) {
                  Text(
                    text = account.accountName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                  )
                  Text(
                    text = account.ctgryName.ifBlank { "عملاء" },
                    fontSize = 12.sp,
                    color = SecondaryText
                  )
                  if (account.mobile.isNotBlank()) {
                    Text(
                      text = account.mobile,
                      fontSize = 12.sp,
                      color = SecondaryText
                    )
                  }
                }

                Box(
                  modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SoftBlue),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = RoyalBlue,
                    modifier = Modifier.size(26.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(18.dp))

              // Current Balance Row
              Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
              ) {
                Text(
                  text = "الرصيد الحالي",
                  fontSize = 12.sp,
                  color = SecondaryText,
                  fontWeight = FontWeight.Medium
                )
                Text(
                  text = formattedBalance,
                  fontSize = 26.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = if (summary.currentBalance < 0) NegativeRed else PrimaryText
                )
              }
            }
          }
        }

        // 4-pill Balance Summary Row matching Screenshot 3
        item {
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Opening balance
            SummaryPill(
              title = "رصيد افتتاحي",
              amount = fmtMoney(summary.openingBalance),
              icon = Icons.Default.AccountBalanceWallet,
              iconBg = PositiveGreenSoft,
              iconTint = PositiveGreen,
              modifier = Modifier.weight(1f)
            )

            // Incoming (وارد)
            SummaryPill(
              title = "وارد",
              amount = fmtMoney(summary.totalIncoming),
              icon = Icons.Default.ArrowDownward,
              iconBg = SoftBlue,
              iconTint = RoyalBlue,
              modifier = Modifier.weight(1f)
            )

            // Outgoing (صادر)
            SummaryPill(
              title = "صادر",
              amount = fmtMoney(summary.totalOutgoing),
              icon = Icons.Default.ArrowUpward,
              iconBg = NegativeRedSoft,
              iconTint = NegativeRed,
              modifier = Modifier.weight(1f)
            )

            // Currency breakdown pill (e.g. Dollar)
            val secondaryCur = summary.currencyBreakdowns.firstOrNull { it.currencyId != 1L }
            SummaryPill(
              title = secondaryCur?.currencyName ?: "دولار",
              amount = fmtMoney(secondaryCur?.balance ?: 0.0),
              icon = Icons.Default.AttachMoney,
              iconBg = SoftBlue,
              iconTint = RoyalBlue,
              modifier = Modifier.weight(1f)
            )
          }
        }

        // Filters row (الفترة, النوع, العملة)
        item {
          Spacer(modifier = Modifier.height(14.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Filter by Type
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { filterMenuExpanded = true }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              val label = when (selectedFilterType) {
                "INCOMING" -> "وارد فقط"
                "OUTGOING" -> "صادر فقط"
                else -> "النوع: الكل"
              }
              Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PrimaryText)

              DropdownMenu(
                expanded = filterMenuExpanded,
                onDismissRequest = { filterMenuExpanded = false }
              ) {
                DropdownMenuItem(text = { Text("الكل") }, onClick = { onFilterTypeSelect("ALL"); filterMenuExpanded = false })
                DropdownMenuItem(text = { Text("وارد فقط") }, onClick = { onFilterTypeSelect("INCOMING"); filterMenuExpanded = false })
                DropdownMenuItem(text = { Text("صادر فقط") }, onClick = { onFilterTypeSelect("OUTGOING"); filterMenuExpanded = false })
              }
            }

            // Filter by Currency
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { currencyMenuExpanded = true }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              val curName = currencies.find { it.cId == selectedCurrencyId }?.cName ?: "العملة: الكل"
              Text(text = curName, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PrimaryText)

              DropdownMenu(
                expanded = currencyMenuExpanded,
                onDismissRequest = { currencyMenuExpanded = false }
              ) {
                DropdownMenuItem(text = { Text("كل العملات") }, onClick = { onCurrencySelect(null); currencyMenuExpanded = false })
                currencies.forEach { cur ->
                  DropdownMenuItem(text = { Text(cur.cName) }, onClick = { onCurrencySelect(cur.cId); currencyMenuExpanded = false })
                }
              }
            }
          }
        }

        // Movements Header
        item {
          Spacer(modifier = Modifier.height(14.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 22.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "تفاصيل الحركات (${transactions.size})",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = PrimaryText
            )
          }
        }

        // Statement table: header, then totals row on top, then detailed entries
        item {
          StatementTableHeader()
          val totalIn = transactions.filter { it.amount > 0 }.sumOf { it.amount }
          val totalOut = transactions.filter { it.amount < 0 }.sumOf { -it.amount }
          StatementCells(
            date = "الإجمالي",
            details = "${transactions.size} حركة",
            incoming = fmtAmount(totalIn),
            outgoing = fmtAmount(totalOut),
            balance = fmtAmount(summary.currentBalance),
            background = SoftBlue,
            bold = true,
            balanceColor = if (summary.currentBalance < 0) NegativeRed else PrimaryText,
            onClick = null
          )
        }

        if (transactions.isEmpty()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(text = "لا توجد حركات مسجلة لهذا الحساب", color = SecondaryText, fontSize = 14.sp)
            }
          }
        } else {
          itemsIndexed(transactions, key = { _, t -> t.transId }) { index, tx ->
            StatementCells(
              date = tx.transDate.take(10),
              details = buildString {
                append(tx.details)
                if (!tx.transferAccountName.isNullOrBlank()) {
                  if (isNotEmpty()) append(" - ")
                  append(tx.transferAccountName)
                }
                if (tx.cName.isNotBlank()) {
                  if (isNotEmpty()) append("\n")
                  append(tx.cName)
                }
              },
              incoming = if (tx.amount > 0) fmtAmount(tx.amount) else "",
              outgoing = if (tx.amount < 0) fmtAmount(-tx.amount) else "",
              balance = fmtAmount(tx.totalBalance),
              background = if (index % 2 == 1) Color(0xFFF5F8FD) else Color.White,
              bold = false,
              balanceColor = if (tx.totalBalance < 0) NegativeRed else PrimaryText,
              onClick = { onTransactionClick(tx) },
              incomingColor = PositiveGreen,
              outgoingColor = NegativeRed
            )
          }
        }
      }

      // Add Transaction FAB
      FloatingActionButton(
        onClick = onAddTransactionClick,
        containerColor = RoyalBlue,
        contentColor = Color.White,
        shape = CircleShape,
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(start = 24.dp, bottom = 24.dp)
          .testTag("add_transaction_fab")
      ) {
        Icon(Icons.Default.Add, contentDescription = "إضافة حركة")
      }
    }
  }
}

@Composable
private fun SummaryPill(
  title: String,
  amount: String,
  icon: ImageVector,
  iconBg: Color,
  iconTint: Color,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp, horizontal = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = title,
        fontSize = 10.sp,
        color = SecondaryText,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = amount,
        fontSize = 14.sp,
        color = PrimaryText,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(6.dp))
      Box(
        modifier = Modifier
          .size(24.dp)
          .clip(CircleShape)
          .background(iconBg),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(14.dp)
        )
      }
    }
  }
}


private fun fmtAmount(v: Double): String = fmtMoney(v)

@Composable
private fun StatementTableHeader() {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp)
      .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
      .background(Color(0xFF14243A))
      .padding(vertical = 10.dp, horizontal = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    HeaderCell("التاريخ", 1.15f)
    HeaderCell("البيان", 2f)
    HeaderCell("وارد", 1f)
    HeaderCell("صادر", 1f)
    HeaderCell("الرصيد", 1.2f)
  }
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
  Text(
    text = text,
    modifier = Modifier.weight(weight),
    color = Color.White,
    fontSize = 11.sp,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center
  )
}

@Composable
private fun StatementCells(
  date: String,
  details: String,
  incoming: String,
  outgoing: String,
  balance: String,
  background: Color,
  bold: Boolean,
  balanceColor: Color,
  onClick: (() -> Unit)?,
  incomingColor: Color = PrimaryText,
  outgoingColor: Color = PrimaryText
) {
  val weight = if (bold) FontWeight.Bold else FontWeight.Normal
  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(background)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(vertical = 9.dp, horizontal = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = date, modifier = Modifier.weight(1.15f), fontSize = 10.sp, fontWeight = weight, color = SecondaryText, textAlign = TextAlign.Center)
      Text(text = details, modifier = Modifier.weight(2f).padding(horizontal = 4.dp), fontSize = 10.5.sp, fontWeight = weight, color = PrimaryText, textAlign = TextAlign.Start)
      Text(text = incoming, modifier = Modifier.weight(1f), fontSize = 10.5.sp, fontWeight = weight, color = incomingColor, textAlign = TextAlign.Center)
      Text(text = outgoing, modifier = Modifier.weight(1f), fontSize = 10.5.sp, fontWeight = weight, color = outgoingColor, textAlign = TextAlign.Center)
      Text(text = balance, modifier = Modifier.weight(1.2f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = balanceColor, textAlign = TextAlign.Center)
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE3E8F2)))
  }
}

private fun buildStatementDoc(
  summary: AccountDetailsSummary,
  transactions: List<TransactionItem>,
  currencies: List<Currency>,
  selectedCurrencyId: Long?
): StatementDoc {
  val account = summary.account
  val sorted = transactions.sortedWith(compareBy({ it.transDate }, { it.transId }))
  val totalIn = sorted.filter { it.amount > 0 }.sumOf { it.amount }
  val totalOut = sorted.filter { it.amount < 0 }.sumOf { -it.amount }
  val curName = currencies.find { it.cId == selectedCurrencyId }?.cName ?: "كل العملات"
  val from = sorted.firstOrNull()?.transDate?.take(10).orEmpty()
  val to = sorted.lastOrNull()?.transDate?.take(10).orEmpty()

  val rows = sorted.map { tx ->
    val det = buildString {
      append(tx.details)
      if (!tx.transferAccountName.isNullOrBlank()) {
        if (isNotEmpty()) append(" - ")
        append(tx.transferAccountName)
      }
      if (tx.cName.isNotBlank()) {
        if (isNotEmpty()) append(" - ")
        append(tx.cName)
      }
    }
    listOf(
      tx.transDate.take(10),
      det,
      if (tx.amount > 0) fmtAmount(tx.amount) else "",
      if (tx.amount < 0) fmtAmount(-tx.amount) else "",
      fmtAmount(tx.totalBalance)
    )
  }

  return StatementDoc(
    title = "كشف حساب",
    partyName = account.accountName,
    infoLines = listOf(
      "العملة" to curName,
      "الفترة" to if (from.isNotBlank()) "من $from إلى $to" else "",
      "الجوال" to account.mobile
    ),
    headers = listOf("التاريخ", "البيان", "وارد", "صادر", "الرصيد"),
    rows = rows,
    colWeights = listOf(1.15f, 2.4f, 1f, 1f, 1.2f),
    colColors = listOf(null, null, StatementPdf.GREEN, StatementPdf.RED, null),
    totals = emptyList(),
    footerRow = listOf("الإجمالي", "${sorted.size} حركة", fmtAmount(totalIn), fmtAmount(totalOut), fmtAmount(summary.currentBalance))
  )
}
