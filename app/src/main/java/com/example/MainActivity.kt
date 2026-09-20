package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BillInvoice
import com.example.data.model.Customer
import com.example.notification.DueReminderNotificationHelper
import com.example.ui.BillingViewModel
import com.example.ui.components.AddCustomerDialog
import com.example.ui.components.CreateInvoiceDialog
import com.example.ui.components.PayInvoiceDialog
import com.example.ui.components.ThermalPrintReceiptDialog
import com.example.ui.components.UpdateUsageDialog
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DataUsageScreen
import com.example.ui.screens.InvoicesScreen
import com.example.ui.screens.PrinterSettingsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: BillingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channel for due date alerts
        DueReminderNotificationHelper.createNotificationChannel(this)

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: BillingViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.checkAndSendAutomatedDueNotifications(context)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        viewModel.snackBarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    // Dialog States
    var invoiceToPay by remember { mutableStateOf<BillInvoice?>(null) }
    var customerToAddOrEdit by remember { mutableStateOf<Customer?>(null) }
    var isAddCustomerDialogOpen by remember { mutableStateOf(false) }
    var customerToUpdateUsage by remember { mutableStateOf<Customer?>(null) }
    var isCreateInvoiceDialogOpen by remember { mutableStateOf(false) }

    // State Flows
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val invoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val filteredInvoices by viewModel.filteredInvoices.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val printState by viewModel.printState.collectAsStateWithLifecycle()
    val currentFilter by viewModel.invoiceFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val totalPending by viewModel.totalPendingAmount.collectAsStateWithLifecycle()
    val unpaidCount by viewModel.unpaidCount.collectAsStateWithLifecycle()
    val overdueCount by viewModel.overdueCount.collectAsStateWithLifecycle()
    val totalBandwidthGb by viewModel.totalBandwidthUsedGb.collectAsStateWithLifecycle()

    val now = System.currentTimeMillis()
    val urgentInvoices = invoices.filter { !it.isPaid && (it.isOverdue(now) || it.daysUntilDue(now) <= 3) }

    val navTitles = listOf(
        "Beranda & Ringkasan",
        "Tagihan & Kuitansi",
        "Monitoring Kuota Data",
        "Data Pelanggan",
        "Printer & Pengaturan"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = navTitles[selectedTab],
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Trigger Automatic Due Date Reminder Notification Button
                    IconButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !DueReminderNotificationHelper.hasNotificationPermission(context)
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.checkAndSendAutomatedDueNotifications(context)
                            }
                        },
                        modifier = Modifier.testTag("btn_top_trigger_notification")
                    ) {
                        BadgedBox(
                            badge = {
                                if (overdueCount > 0) {
                                    Badge {
                                        Text("$overdueCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Kirim Pengingat Jatuh Tempo Otomatis",
                                tint = if (overdueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Beranda") },
                    label = { Text("Beranda") },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unpaidCount > 0) {
                                    Badge { Text("$unpaidCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = "Tagihan")
                        }
                    },
                    label = { Text("Tagihan") },
                    modifier = Modifier.testTag("nav_tab_invoices")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.DataUsage, contentDescription = "Kuota") },
                    label = { Text("Kuota Data") },
                    modifier = Modifier.testTag("nav_tab_data_usage")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.People, contentDescription = "Pelanggan") },
                    label = { Text("Pelanggan") },
                    modifier = Modifier.testTag("nav_tab_customers")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Print, contentDescription = "Printer") },
                    label = { Text("Printer") },
                    modifier = Modifier.testTag("nav_tab_printer")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    unpaidCount = unpaidCount,
                    overdueCount = overdueCount,
                    totalPendingAmount = totalPending,
                    totalBandwidthGb = totalBandwidthGb,
                    urgentInvoices = urgentInvoices,
                    onTriggerNotifications = {
                        viewModel.checkAndSendAutomatedDueNotifications(context)
                    },
                    onPayInvoice = { invoice -> invoiceToPay = invoice },
                    onPrintInvoice = { invoice -> viewModel.openPrintDialog(invoice) },
                    onSendWhatsAppReminder = { invoice ->
                        val intent = DueReminderNotificationHelper.createWhatsAppReminderIntent(invoice, settings)
                        context.startActivity(intent)
                    },
                    onNavigateToInvoices = { selectedTab = 1 },
                    onCreateInvoiceClick = { isCreateInvoiceDialogOpen = true }
                )

                1 -> InvoicesScreen(
                    invoices = filteredInvoices,
                    currentFilter = currentFilter,
                    searchQuery = searchQuery,
                    onFilterChanged = { viewModel.setInvoiceFilter(it) },
                    onSearchChanged = { viewModel.setSearchQuery(it) },
                    onPayClick = { invoice -> invoiceToPay = invoice },
                    onPrintClick = { invoice -> viewModel.openPrintDialog(invoice) },
                    onSendWhatsApp = { invoice ->
                        val intent = DueReminderNotificationHelper.createWhatsAppReminderIntent(invoice, settings)
                        context.startActivity(intent)
                    },
                    onDeleteInvoice = { invoice -> viewModel.deleteInvoice(invoice) },
                    onCreateInvoiceClick = { isCreateInvoiceDialogOpen = true }
                )

                2 -> DataUsageScreen(
                    customers = customers,
                    settings = settings,
                    onUpdateCustomerUsage = { customer -> customerToUpdateUsage = customer }
                )

                3 -> CustomersScreen(
                    customers = customers,
                    onAddCustomer = {
                        customerToAddOrEdit = null
                        isAddCustomerDialogOpen = true
                    },
                    onEditCustomer = { customer ->
                        customerToAddOrEdit = customer
                        isAddCustomerDialogOpen = true
                    },
                    onDeleteCustomer = { customer -> viewModel.deleteCustomer(customer) }
                )

                4 -> PrinterSettingsScreen(
                    settings = settings,
                    thermalPrinterManager = viewModel.thermalPrinterManager,
                    pairedDevices = printState.pairedDevices,
                    selectedDeviceAddress = printState.selectedDeviceAddress,
                    onSelectDevice = { viewModel.selectPrinterDevice(it) },
                    onRefreshDevices = { viewModel.refreshPairedDevices() },
                    onSaveSettings = { viewModel.updateSettings(it) },
                    onTestPrint = { dummyInvoice -> viewModel.openPrintDialog(dummyInvoice) }
                )
            }
        }
    }

    // --- Thermal Bluetooth Print Dialog ---
    if (printState.activeInvoice != null) {
        ThermalPrintReceiptDialog(
            printState = printState,
            invoice = printState.activeInvoice!!,
            onDismiss = { viewModel.closePrintDialog() },
            onSelectDevice = { viewModel.selectPrinterDevice(it) },
            onRefreshDevices = { viewModel.refreshPairedDevices() },
            onPrintToBluetooth = { viewModel.printCurrentReceipt() },
            onSimulatePrint = { viewModel.printSimulationSuccess() }
        )
    }

    // --- Pay Invoice Dialog ---
    if (invoiceToPay != null) {
        PayInvoiceDialog(
            invoice = invoiceToPay!!,
            onDismiss = { invoiceToPay = null },
            onConfirmPayment = { method, andPrint ->
                val inv = invoiceToPay!!
                viewModel.markAsPaid(inv.id, method)
                invoiceToPay = null
                if (andPrint) {
                    val updatedInvoice = inv.copy(
                        isPaid = true,
                        paymentMethod = method,
                        paidDateEpochMs = System.currentTimeMillis()
                    )
                    viewModel.openPrintDialog(updatedInvoice)
                }
            }
        )
    }

    // --- Add/Edit Customer Dialog ---
    if (isAddCustomerDialogOpen) {
        AddCustomerDialog(
            customerToEdit = customerToAddOrEdit,
            onDismiss = {
                isAddCustomerDialogOpen = false
                customerToAddOrEdit = null
            },
            onSave = { customer ->
                val isNew = (customerToAddOrEdit == null)
                viewModel.addOrUpdateCustomer(customer, isNew)
                isAddCustomerDialogOpen = false
                customerToAddOrEdit = null
            }
        )
    }

    // --- Update Customer Data Usage Dialog ---
    if (customerToUpdateUsage != null) {
        UpdateUsageDialog(
            customer = customerToUpdateUsage!!,
            onDismiss = { customerToUpdateUsage = null },
            onConfirmUpdate = { newUsageGb ->
                val customer = customerToUpdateUsage!!
                viewModel.updateCustomerDataUsage(customer.id, newUsageGb)
                customerToUpdateUsage = null
            }
        )
    }

    // --- Create New Invoice Dialog ---
    if (isCreateInvoiceDialogOpen) {
        CreateInvoiceDialog(
            customers = customers,
            onDismiss = { isCreateInvoiceDialogOpen = false },
            onCreateInvoice = { newInvoice ->
                viewModel.createInvoice(newInvoice)
                isCreateInvoiceDialogOpen = false
            }
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
