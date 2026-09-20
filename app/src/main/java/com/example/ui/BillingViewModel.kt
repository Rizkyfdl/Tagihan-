package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.BluetoothDeviceInfo
import com.example.bluetooth.PrintResult
import com.example.bluetooth.ThermalPrinterManager
import com.example.data.local.AppDatabase
import com.example.data.model.AppSettings
import com.example.data.model.BillInvoice
import com.example.data.model.Customer
import com.example.data.repository.BillingRepository
import com.example.notification.DueReminderNotificationHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class InvoiceFilter {
    ALL, UNPAID, DUE_SOON, PAID
}

data class PrintUiState(
    val isPrinting: Boolean = false,
    val printSuccess: Boolean = false,
    val errorMessage: String? = null,
    val activeInvoice: BillInvoice? = null,
    val receiptPreviewText: String = "",
    val pairedDevices: List<BluetoothDeviceInfo> = emptyList(),
    val selectedDeviceAddress: String? = null
)

class BillingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BillingRepository
    val thermalPrinterManager: ThermalPrinterManager = ThermalPrinterManager(application)

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _invoiceFilter = MutableStateFlow(InvoiceFilter.ALL)
    val invoiceFilter: StateFlow<InvoiceFilter> = _invoiceFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _printState = MutableStateFlow(PrintUiState())
    val printState: StateFlow<PrintUiState> = _printState.asStateFlow()

    private val _snackBarMessage = MutableSharedFlow<String>()
    val snackBarMessage: SharedFlow<String> = _snackBarMessage.asSharedFlow()

    val allCustomers: StateFlow<List<Customer>>
    val allInvoices: StateFlow<List<BillInvoice>>

    val filteredInvoices: StateFlow<List<BillInvoice>>

    // Quick Stats
    val totalPendingAmount: StateFlow<Double>
    val unpaidCount: StateFlow<Int>
    val overdueCount: StateFlow<Int>

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = BillingRepository(db.customerDao(), db.billInvoiceDao())

        allCustomers = repository.allCustomers.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allInvoices = repository.allInvoices.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        filteredInvoices = combine(allInvoices, _invoiceFilter, _searchQuery) { invoices, filter, query ->
            val now = System.currentTimeMillis()
            invoices.filter { invoice ->
                val matchesFilter = when (filter) {
                    InvoiceFilter.ALL -> true
                    InvoiceFilter.UNPAID -> !invoice.isPaid
                    InvoiceFilter.PAID -> invoice.isPaid
                    InvoiceFilter.DUE_SOON -> !invoice.isPaid && (invoice.isOverdue(now) || invoice.daysUntilDue(now) <= 3)
                }
                val matchesQuery = if (query.isBlank()) {
                    true
                } else {
                    invoice.customerName.contains(query, ignoreCase = true) ||
                    invoice.customerCode.contains(query, ignoreCase = true) ||
                    invoice.invoiceNumber.contains(query, ignoreCase = true)
                }
                matchesFilter && matchesQuery
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        totalPendingAmount = allInvoices.combine(_settings) { invoices, _ ->
            invoices.filter { !it.isPaid }.sumOf { it.totalAmount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        unpaidCount = allInvoices.combine(_settings) { invoices, _ ->
            invoices.count { !it.isPaid }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        overdueCount = allInvoices.combine(_settings) { invoices, _ ->
            val now = System.currentTimeMillis()
            invoices.count { !it.isPaid && (it.isOverdue(now) || it.daysUntilDue(now) <= 1) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        refreshPairedDevices()
    }

    fun setInvoiceFilter(filter: InvoiceFilter) {
        _invoiceFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        viewModelScope.launch {
            _snackBarMessage.emit("Pengaturan berhasil disimpan.")
        }
    }

    fun markAsPaid(invoiceId: Long, paymentMethod: String) {
        viewModelScope.launch {
            repository.markInvoiceAsPaid(invoiceId, paymentMethod)
            _snackBarMessage.emit("Tagihan berhasil ditandai LUNAS.")
        }
    }

    fun addOrUpdateCustomer(customer: Customer, isNew: Boolean) {
        viewModelScope.launch {
            if (isNew) {
                repository.insertCustomer(customer)
                _snackBarMessage.emit("Pelanggan baru berhasil ditambahkan.")
            } else {
                repository.updateCustomer(customer)
                _snackBarMessage.emit("Data pelanggan berhasil diperbarui.")
            }
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            _snackBarMessage.emit("Pelanggan ${customer.name} telah dihapus.")
        }
    }

    fun createInvoice(invoice: BillInvoice) {
        viewModelScope.launch {
            repository.insertInvoice(invoice)
            _snackBarMessage.emit("Tagihan baru berhasil dibuat.")
        }
    }

    fun deleteInvoice(invoice: BillInvoice) {
        viewModelScope.launch {
            repository.deleteInvoice(invoice)
            _snackBarMessage.emit("Tagihan ${invoice.invoiceNumber} berhasil dihapus.")
        }
    }

    // --- Automatic Notifications Trigger ---
    fun checkAndSendAutomatedDueNotifications(context: Context) {
        viewModelScope.launch {
            val unpaid = repository.getUnpaidInvoicesSnapshot()
            val now = System.currentTimeMillis()
            val dueInvoices = unpaid.filter { it.isOverdue(now) || it.daysUntilDue(now) <= _settings.value.autoReminderDaysBefore }

            if (dueInvoices.isEmpty()) {
                _snackBarMessage.emit("Tidak ada tagihan yang mendekati jatuh tempo saat ini.")
            } else {
                val sentCount = DueReminderNotificationHelper.sendDueNotifications(
                    context,
                    dueInvoices,
                    _settings.value
                )
                if (sentCount > 0) {
                    _snackBarMessage.emit("$sentCount notifikasi jatuh tempo berhasil dikirimkan.")
                } else {
                    _snackBarMessage.emit("Izin notifikasi diperlukan untuk menampilkan pengingat otomatis.")
                }
            }
        }
    }

    // --- Thermal Bluetooth Printer ---
    fun refreshPairedDevices() {
        val paired = thermalPrinterManager.getPairedPrinters()
        val defaultDevice = paired.firstOrNull()?.address ?: _settings.value.selectedPrinterAddress
        _printState.value = _printState.value.copy(
            pairedDevices = paired,
            selectedDeviceAddress = defaultDevice
        )
    }

    fun selectPrinterDevice(address: String) {
        _printState.value = _printState.value.copy(selectedDeviceAddress = address)
        _settings.value = _settings.value.copy(selectedPrinterAddress = address)
    }

    fun openPrintDialog(invoice: BillInvoice) {
        refreshPairedDevices()
        val preview = thermalPrinterManager.getReceiptPreviewText(invoice, _settings.value)
        _printState.value = _printState.value.copy(
            activeInvoice = invoice,
            receiptPreviewText = preview,
            isPrinting = false,
            printSuccess = false,
            errorMessage = null
        )
    }

    fun closePrintDialog() {
        _printState.value = _printState.value.copy(activeInvoice = null)
    }

    fun printCurrentReceipt() {
        val invoice = _printState.value.activeInvoice ?: return
        val targetAddress = _printState.value.selectedDeviceAddress

        if (targetAddress.isNullOrBlank()) {
            _printState.value = _printState.value.copy(
                errorMessage = "Belum ada printer Bluetooth yang dipilih. Pilih printer yang telah dipasangkan (paired) atau jalankan simulasi cetak."
            )
            return
        }

        viewModelScope.launch {
            _printState.value = _printState.value.copy(isPrinting = true, errorMessage = null, printSuccess = false)
            val result = thermalPrinterManager.printReceipt(targetAddress, invoice, _settings.value)
            when (result) {
                is PrintResult.Success -> {
                    _printState.value = _printState.value.copy(
                        isPrinting = false,
                        printSuccess = true,
                        errorMessage = null
                    )
                    _snackBarMessage.emit("Struk berhasil dikirim ke printer thermal!")
                }
                is PrintResult.Error -> {
                    _printState.value = _printState.value.copy(
                        isPrinting = false,
                        printSuccess = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun printSimulationSuccess() {
        viewModelScope.launch {
            _printState.value = _printState.value.copy(
                isPrinting = true,
                errorMessage = null,
                printSuccess = false
            )
            kotlinx.coroutines.delay(800)
            _printState.value = _printState.value.copy(
                isPrinting = false,
                printSuccess = true,
                errorMessage = null
            )
            _snackBarMessage.emit("Kuitansi siap! Cetak simulasi thermal berhasil diselesaikan.")
        }
    }
}
