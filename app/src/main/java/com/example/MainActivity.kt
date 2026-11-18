package com.example

import android.content.Intent
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import com.example.ui.BiometricHelper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.withResumed
import kotlinx.coroutines.delay
import com.example.ui.AppSecurity
import com.example.data.model.Account
import com.example.data.model.TransactionItem
import com.example.data.model.ReportType
import com.example.data.model.VoucherType
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.AccountActionBottomSheet
import com.example.ui.components.AddEditAccountDialog
import com.example.ui.components.AddEditTransactionDialog
import com.example.ui.components.FloatingCapsuleNavigation
import com.example.ui.screens.AccountDetailsScreen
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.CurrenciesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransferScreen
import com.example.ui.screens.VouchersScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
  private val mainViewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestHighRefreshRate()
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainAppContent()
      }
    }
  }

  /** يطلب أعلى معدل تحديث تدعمه الشاشة (90/120Hz) دون تغيير الدقة */
  private fun requestHighRefreshRate() {
    try {
      @Suppress("DEPRECATION")
      val d = if (android.os.Build.VERSION.SDK_INT >= 30) display else windowManager.defaultDisplay
      d ?: return
      val current = d.mode
      val best = d.supportedModes
        .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
        .maxByOrNull { it.refreshRate } ?: return
      val lp = window.attributes
      lp.preferredDisplayModeId = best.modeId
      lp.preferredRefreshRate = best.refreshRate
      window.attributes = lp
    } catch (e: Exception) {
      // تجاهل: الجهاز لا يدعم تغيير معدل التحديث
    }
  }

  companion object {
    private const val REQ_EXPORT = 7101
    private const val REQ_IMPORT = 7102
  }

  /** نافذة اختيار مكان حفظ النسخة الاحتياطية (مع بديل المشاركة إن لم تتوفر النافذة) */
  fun launchExportPicker(fileName: String) {
    AppSecurity.suppressLock = true
    try {
      val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_TITLE, fileName)
      }
      @Suppress("DEPRECATION")
      startActivityForResult(intent, REQ_EXPORT)
    } catch (e: Exception) {
      shareBackupInstead()
    }
  }

  /** نافذة اختيار ملف الاستعادة */
  fun launchImportPicker() {
    AppSecurity.suppressLock = true
    try {
      val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
      }
      @Suppress("DEPRECATION")
      startActivityForResult(intent, REQ_IMPORT)
    } catch (e: Exception) {
      try {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
          addCategory(Intent.CATEGORY_OPENABLE)
          type = "*/*"
        }
        @Suppress("DEPRECATION")
        startActivityForResult(Intent.createChooser(intent, "اختيار ملف النسخة الاحتياطية"), REQ_IMPORT)
      } catch (e2: Exception) {
        AppSecurity.suppressLock = false
        Toast.makeText(this, "لا يوجد تطبيق لاختيار الملفات على هذا الجهاز", Toast.LENGTH_LONG).show()
      }
    }
  }

  /** بديل عند تعذر نافذة الحفظ: إنشاء الملف ومشاركته (واتساب، Drive، البريد...) */
  private fun shareBackupInstead() {
    lifecycleScope.launch {
      val file = mainViewModel.createShareableBackupFile()
      if (file == null) {
        AppSecurity.suppressLock = false
        Toast.makeText(this@MainActivity, "تعذر إنشاء النسخة الاحتياطية", Toast.LENGTH_LONG).show()
        return@launch
      }
      try {
        val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
          type = "application/octet-stream"
          putExtra(Intent.EXTRA_STREAM, uri)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        AppSecurity.startExternal(this@MainActivity, Intent.createChooser(send, "حفظ / مشاركة النسخة الاحتياطية"))
      } catch (e: Exception) {
        AppSecurity.suppressLock = false
        Toast.makeText(this@MainActivity, "تعذر مشاركة النسخة الاحتياطية", Toast.LENGTH_LONG).show()
      }
    }
  }

  @Deprecated("Deprecated in Java")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode != RESULT_OK) return
    val uri = data?.data ?: return
    when (requestCode) {
      REQ_EXPORT -> mainViewModel.exportBackupTo(uri)
      REQ_IMPORT -> mainViewModel.setPickedRestoreUri(uri)
    }
  }

  override fun onResume() {
    super.onResume()
    // عدنا للتطبيق: نلغي استثناء القفل المؤقت (فتح شاشة خارجية كاختيار ملف أو مشاركة)
    AppSecurity.suppressLock = false
  }

  override fun onStop() {
    super.onStop()
    // الخروج من التطبيق (زر الرئيسية، التبديل لتطبيق آخر، إطفاء الشاشة): نعيد القفل
    if (!isChangingConfigurations && !AppSecurity.suppressLock) {
      mainViewModel.lockApp()
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MainViewModel = viewModel()) {
  val context = LocalContext.current
  val currentScreen by viewModel.currentScreen.collectAsState()
  val currentNavTab by viewModel.currentNavTab.collectAsState()
  val isAppLocked by viewModel.isAppLocked.collectAsState()
  val securitySettings by viewModel.securitySettings.collectAsState()
  val lockSession by viewModel.lockSession.collectAsState()
  val pendingRestoreUri by viewModel.pendingRestoreUri.collectAsState()
  val pickedRestoreUri2 by viewModel.pickedRestoreUri.collectAsState()

  val dashboardOverview by viewModel.dashboardOverview.collectAsState()
  val accounts by viewModel.accounts.collectAsState()
  val categories by viewModel.categories.collectAsState()
  val currencies by viewModel.currencies.collectAsState()
  val selectedCategoryNo by viewModel.selectedCategoryNo.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()

  val accountSummary by viewModel.accountDetailsSummary.collectAsState()
  val accountTransactions by viewModel.accountTransactions.collectAsState()
  val selectedDetailsCurrencyId by viewModel.selectedDetailsCurrencyId.collectAsState()
  val selectedDetailsFilterType by viewModel.selectedDetailsFilterType.collectAsState()

  val currentReportResult by viewModel.currentReportResult.collectAsState()
  val restorePoints by viewModel.restorePoints.collectAsState()
  val notificationMessage by viewModel.notificationMessage.collectAsState()

  val selectedAccountForAction by viewModel.selectedAccountForAction.collectAsState()

  // Dialog States
  var showAddAccountDialog by remember { mutableStateOf(false) }
  var accountToEdit by remember { mutableStateOf<Account?>(null) }
  var showAddTransactionDialog by remember { mutableStateOf(false) }
  var targetAccountNoForTx by remember { mutableStateOf<Long?>(null) }
  var showMoveAccountDialog by remember { mutableStateOf(false) }
  var showCopyAccountDialog by remember { mutableStateOf(false) }
  var accountToDelete by remember { mutableStateOf<Account?>(null) }
  var transactionToDelete by remember { mutableStateOf<TransactionItem?>(null) }

  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  // Handle toast / notification message
  LaunchedEffect(notificationMessage) {
    notificationMessage?.let { msg ->
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
      viewModel.clearNotification()
    }
  }

  // BackHandler to handle sub-screen back navigation smoothly
  BackHandler(enabled = currentScreen != AppScreen.Home) {
    if (!viewModel.navigateBack()) {
      (context as? ComponentActivity)?.finish()
    }
  }

  // App Lock Screen Gate
  val hasPin = securitySettings.isPinEnabled && securitySettings.pinCode.isNotBlank()
  // القفل بالبصمة وحدها (بدون PIN) ما دامت البصمة متاحة في الجهاز، حتى لا يُحبس المستخدم
  val fingerprintOnly = !hasPin && securitySettings.isFingerprintEnabled &&
    BiometricHelper.unavailableReason(context) == null
  val isLockActive = isAppLocked && (hasPin || fingerprintOnly)

  val promptFingerprint: () -> Unit = {
    BiometricHelper.findActivity(context)?.let { act ->
      BiometricHelper.authenticate(
        activity = act,
        onSuccess = { viewModel.unlockApp() },
        onFailure = { cancelled, msg ->
          if (!cancelled && msg.isNotBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
          }
        }
      )
    }
  }

  // عند كل قفل (فتح التطبيق أو العودة إليه): انتظر حتى تصبح الواجهة جاهزة ثم اطلب البصمة تلقائياً
  LaunchedEffect(isLockActive, lockSession, securitySettings.isFingerprintEnabled) {
    if (isLockActive && securitySettings.isFingerprintEnabled) {
      val act = BiometricHelper.findActivity(context)
      if (act != null) {
        act.lifecycle.withResumed { }
        delay(350)
        if (viewModel.isAppLocked.value) promptFingerprint()
      }
    }
  }

  if (isLockActive) {
    PinLockScreen(
      expectedPin = if (hasPin) securitySettings.pinCode else "----",
      isFingerprintEnabled = securitySettings.isFingerprintEnabled,
      onUnlockSuccess = { viewModel.unlockApp() },
      onFingerprintClick = promptFingerprint
    )
    return
  }

  // Whether floating bottom capsule should be visible
  val showFloatingNav = when (currentScreen) {
    is AppScreen.Home, is AppScreen.Accounts, is AppScreen.Reports, is AppScreen.Settings -> true
    else -> false
  }

  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        currentScreen.let { screen ->
          when (screen) {
            is AppScreen.Home -> {
              dashboardOverview?.let { overview ->
                HomeScreen(
                  overview = overview,
                  onQuickAction = { actionId ->
                    when (actionId) {
                      "add_account" -> showAddAccountDialog = true
                      "add_transaction" -> {
                        targetAccountNoForTx = null
                        showAddTransactionDialog = true
                      }
                      "transfer" -> viewModel.navigateTo(AppScreen.Transfer)
                      "receipt_voucher" -> viewModel.navigateTo(AppScreen.Vouchers(VoucherType.RECEIPT))
                      "payment_voucher" -> viewModel.navigateTo(AppScreen.Vouchers(VoucherType.PAYMENT))
                      "reports" -> {
                        viewModel.navigateTo(AppScreen.Reports)
                        viewModel.generateReport(ReportType.ACCOUNTS_BALANCE_SUMMARY)
                      }
                    }
                  },
                  onViewAllTransactions = {
                    viewModel.navigateTo(AppScreen.Reports)
                    viewModel.generateReport(ReportType.DETAILED_MOVEMENTS)
                  },
                  onTransactionClick = { /* detail */ },
                  onNotificationClick = {
                    Toast.makeText(context, "لا توجد تنبيهات جديدة معلقة", Toast.LENGTH_SHORT).show()
                  },
                  onAccountClick = { accNo ->
                    viewModel.navigateTo(AppScreen.AccountDetails(accNo))
                  }
                )
              }
            }

            is AppScreen.Accounts -> {
              AccountsScreen(
                accounts = accounts,
                categories = categories,
                selectedCategoryNo = selectedCategoryNo,
                searchQuery = searchQuery,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onCategorySelect = { viewModel.selectCategoryFilter(it) },
                onAccountClick = { accNo ->
                  viewModel.navigateTo(AppScreen.AccountDetails(accNo))
                },
                onAccountLongClick = { acc ->
                  viewModel.setSelectedAccountForAction(acc)
                },
                onAddAccountClick = {
                  accountToEdit = null
                  showAddAccountDialog = true
                },
                onSortClick = {
                  viewModel.toggleSortOrder()
                  Toast.makeText(context, "تم تغيير ترتيب الحسابات", Toast.LENGTH_SHORT).show()
                },
                onNotificationClick = {
                  Toast.makeText(context, "جميع الإشعارات مفعلة ومحدثة", Toast.LENGTH_SHORT).show()
                }
              )
            }

            is AppScreen.AccountDetails -> {
              accountSummary?.let { summary ->
                AccountDetailsScreen(
                  summary = summary,
                  transactions = accountTransactions,
                  currencies = currencies,
                  selectedCurrencyId = selectedDetailsCurrencyId,
                  selectedFilterType = selectedDetailsFilterType,
                  onCurrencySelect = { viewModel.setDetailsCurrencyFilter(it) },
                  onFilterTypeSelect = { viewModel.setDetailsTypeFilter(it) },
                  onBackClick = { viewModel.navigateBack() },
                  onAddTransactionClick = {
                    targetAccountNoForTx = screen.accountNo
                    showAddTransactionDialog = true
                  },
                  onTransactionClick = { tx ->
                    // Open confirmation to delete
                    transactionToDelete = tx
                  }
                )
              }
            }

            is AppScreen.Transfer -> {
              TransferScreen(
                accounts = accounts,
                currencies = currencies,
                onBackClick = { viewModel.navigateBack() },
                onExecuteTransfer = { src, dst, amt, cur, dt, det ->
                  viewModel.executeTransfer(src, dst, amt, cur, dt, det)
                }
              )
            }

            is AppScreen.Vouchers -> {
              VouchersScreen(
                accounts = accounts,
                currencies = currencies,
                initialType = screen.initialType,
                onBackClick = { viewModel.navigateBack() },
                onCreateVoucher = { type, accNo, amt, cur, dt, det, done ->
                  viewModel.createVoucher(type, accNo, amt, cur, dt, det, done)
                }
              )
            }

            is AppScreen.Reports -> {
              ReportsScreen(
                accounts = accounts,
                categories = categories,
                currencies = currencies,
                reportResult = currentReportResult,
                onGenerateReport = { type, from, to, acc, cat, cur ->
                  viewModel.generateReport(type, from, to, acc, cat, cur)
                },
                onExportCsv = { report ->
                  viewModel.exportReportCsv(report)
                },
                onBackClick = { viewModel.navigateBack() }
              )
            }

            is AppScreen.Categories -> {
              CategoriesScreen(
                categories = categories,
                onBackClick = { viewModel.navigateBack() },
                onAddCategory = { viewModel.addCategory(it) },
                onUpdateCategory = { id, name, enabled -> viewModel.updateCategory(id, name, enabled) },
                onDeleteCategory = { viewModel.deleteCategory(it) }
              )
            }

            is AppScreen.Currencies -> {
              CurrenciesScreen(
                currencies = currencies,
                onBackClick = { viewModel.navigateBack() },
                onAddCurrency = { viewModel.addCurrency(it) },
                onUpdateCurrency = { id, name -> viewModel.updateCurrency(id, name) },
                onDeleteCurrency = { viewModel.deleteCurrency(it) }
              )
            }

            is AppScreen.Settings -> {
              SettingsScreen(
                securitySettings = securitySettings,
                restorePoints = restorePoints,
                onUpdateSecurity = { viewModel.updateSecuritySettings(it) },
                onNavigateToAccounts = { viewModel.navigateTo(AppScreen.Accounts) },
                onNavigateToCategories = { viewModel.navigateTo(AppScreen.Categories) },
                onNavigateToCurrencies = { viewModel.navigateTo(AppScreen.Currencies) },
                onExportBackup = { viewModel.exportBackupTo(it) },
                onPickExportFile = { name -> (BiometricHelper.findActivity(context) as? MainActivity)?.launchExportPicker(name) },
                onPickImportFile = { (BiometricHelper.findActivity(context) as? MainActivity)?.launchImportPicker() },
                pickedRestoreUri = pickedRestoreUri2,
                onPickedRestoreHandled = { viewModel.setPickedRestoreUri(null) },
                onRestoreFromUri = { viewModel.restoreFromUri(it) },
                pendingRestoreUri = pendingRestoreUri,
                onRestoreWithKey = { uri, key -> viewModel.restoreFromUri(uri, key) },
                onCancelPendingRestore = { viewModel.cancelPendingRestore() },
                onRestoreBackup = { viewModel.restoreBackup(it) },
                onDeleteBackup = { viewModel.deleteBackup(it) },
                onAnnualClosing = { viewModel.performAnnualClosing(it) },
                onBackClick = { viewModel.navigateBack() }
              )
            }
          }
        }

        // Floating Capsule Bottom Navigation
        if (showFloatingNav) {
          FloatingCapsuleNavigation(
            currentDestination = currentNavTab,
            onDestinationSelected = { dest -> viewModel.selectNavTab(dest) },
            modifier = Modifier.align(Alignment.BottomCenter)
          )
        }

        // Add / Edit Account Dialog
        if (showAddAccountDialog) {
          AddEditAccountDialog(
            categories = categories,
            currencies = currencies,
            existingAccount = accountToEdit,
            onDismiss = {
              showAddAccountDialog = false
              accountToEdit = null
            },
            onConfirm = { name, catNo, mobile, openBal, credLim, debLim, reminder, curId ->
              if (accountToEdit == null) {
                viewModel.addAccount(name, catNo, mobile, openBal, credLim, debLim, reminder, curId)
              } else {
                viewModel.updateAccount(accountToEdit!!.accountNo, name, catNo, mobile, credLim, debLim, reminder)
              }
              showAddAccountDialog = false
              accountToEdit = null
            }
          )
        }

        // Add / Edit Transaction Dialog
        if (showAddTransactionDialog) {
          AddEditTransactionDialog(
            accounts = accounts,
            currencies = currencies,
            selectedAccountNo = targetAccountNoForTx,
            onDismiss = { showAddTransactionDialog = false },
            onConfirm = { accNo, amt, dt, det, curId ->
              viewModel.addTransaction(accNo, amt, dt, det, curId)
              showAddTransactionDialog = false
            }
          )
        }

        // Contextual Account Action Bottom Sheet
        selectedAccountForAction?.let { account ->
          AccountActionBottomSheet(
            account = account,
            onDismiss = { viewModel.setSelectedAccountForAction(null) },
            onOpenDetails = { viewModel.navigateTo(AppScreen.AccountDetails(account.accountNo)) },
            onEdit = {
              accountToEdit = account
              showAddAccountDialog = true
            },
            onDelete = { accountToDelete = account },
            onCopy = { viewModel.copyAccount(account.accountNo, "${account.accountName} (نسخة)") },
            onMove = {
              // Move to next category
              val nextCat = categories.find { it.ctgryNo != account.ctgryNo } ?: categories.first()
              viewModel.moveAccount(account.accountNo, nextCat.ctgryNo)
            },
            onTransfer = { viewModel.navigateTo(AppScreen.Transfer) },
            onAddTransaction = {
              targetAccountNoForTx = account.accountNo
              showAddTransactionDialog = true
            }
          )
        }

        // Delete Account Confirmation Dialog
        accountToDelete?.let { account ->
          AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("حذف الحساب") },
            text = { Text("هل أنت متأكد من رغبتك بحذف حساب '${account.accountName}'؟ سيتم حذف جميع عملياته.") },
            confirmButton = {
              TextButton(onClick = {
                viewModel.deleteAccount(account.accountNo)
                accountToDelete = null
                viewModel.setSelectedAccountForAction(null)
              }) {
                Text("حذف", color = androidx.compose.ui.graphics.Color.Red)
              }
            },
            dismissButton = {
              TextButton(onClick = { accountToDelete = null }) {
                Text("إلغاء")
              }
            }
          )
        }

        // Delete Transaction Confirmation Dialog
        transactionToDelete?.let { tx ->
          AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("حذف الحركة") },
            text = { Text("هل أنت متأكد من رغبتك بحذف هذه الحركة؟") },
            confirmButton = {
              TextButton(onClick = {
                viewModel.deleteTransaction(tx.transId)
                transactionToDelete = null
              }) {
                Text("حذف", color = androidx.compose.ui.graphics.Color.Red)
              }
            },
            dismissButton = {
              TextButton(onClick = { transactionToDelete = null }) {
                Text("إلغاء")
              }
            }
          )
        }
      }
    }
  }
}