package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.AppSettings
import com.example.data.model.BillInvoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.UUID

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val isPaired: Boolean = true
)

sealed class PrintResult {
    data object Success : PrintResult()
    data class Error(val message: String) : PrintResult()
}

class ThermalPrinterManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    companion object {
        // Standard Serial Port Profile (SPP) UUID for Bluetooth Thermal Printers
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val PAPER_WIDTH_CHARS = 32 // 58mm Thermal Printer standard
    }

    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(): List<BluetoothDeviceInfo> {
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!hasBluetoothPermissions()) return emptyList()

        return try {
            val paired = adapter.bondedDevices ?: emptySet()
            paired.map { device ->
                BluetoothDeviceInfo(
                    name = device.name ?: "Printer Bluetooth (${device.address})",
                    address = device.address,
                    isPaired = true
                )
            }
        } catch (e: SecurityException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Generates ESC/POS command byte array for 58mm/80mm thermal receipt printers.
     */
    fun buildEscPosReceipt(invoice: BillInvoice, settings: AppSettings): ByteArray {
        val stream = ByteArrayOutputStream()
        val charset = Charset.forName("CP437") // Default ESC/POS code page

        // Commands
        val escInit = byteArrayOf(0x1B, 0x40) // Initialize printer
        val alignCenter = byteArrayOf(0x1B, 0x61, 0x01)
        val alignLeft = byteArrayOf(0x1B, 0x61, 0x00)
        val alignRight = byteArrayOf(0x1B, 0x61, 0x02)
        val boldOn = byteArrayOf(0x1B, 0x45, 0x01)
        val boldOff = byteArrayOf(0x1B, 0x45, 0x00)
        val doubleHeight = byteArrayOf(0x1D, 0x21, 0x01)
        val doubleBoth = byteArrayOf(0x1D, 0x21, 0x11)
        val textNormal = byteArrayOf(0x1D, 0x21, 0x00)
        val lineFeed = byteArrayOf(0x0A)
        val feedAndCut = byteArrayOf(0x1D, 0x56, 0x41, 0x10) // Cut command

        fun write(bytes: ByteArray) = stream.write(bytes)
        fun writeText(text: String) = stream.write(text.toByteArray(charset))
        fun writeLine(text: String = "") {
            if (text.isNotEmpty()) writeText(text)
            write(lineFeed)
        }

        fun writeRow(left: String, right: String, totalWidth: Int = PAPER_WIDTH_CHARS) {
            val spaceNeeded = totalWidth - left.length - right.length
            val spaces = if (spaceNeeded > 0) " ".repeat(spaceNeeded) else " "
            writeLine(left + spaces + right)
        }

        fun divider(char: Char = '-') {
            writeLine(char.toString().repeat(PAPER_WIDTH_CHARS))
        }

        // --- Start ESC/POS Sequence ---
        write(escInit)

        // 1. Header (Center, Bold, Large)
        write(alignCenter)
        write(boldOn)
        write(doubleBoth)
        writeLine(settings.providerName)
        write(textNormal)
        write(boldOff)

        if (settings.providerTagline.isNotEmpty()) {
            writeLine(settings.providerTagline)
        }
        if (settings.providerAddress.isNotEmpty()) {
            writeLine(settings.providerAddress)
        }
        if (settings.providerPhone.isNotEmpty()) {
            writeLine("Telp/WA: ${settings.providerPhone}")
        }
        write(lineFeed)

        // 2. Receipt Title
        divider('=')
        write(boldOn)
        write(doubleHeight)
        writeLine("BUKTI PEMBAYARAN INTERNET")
        write(textNormal)
        write(boldOff)
        divider('=')

        // 3. Invoice & Customer Meta
        write(alignLeft)
        writeRow("No. Struk", invoice.invoiceNumber)
        writeRow("Periode", invoice.periodMonthYear)
        writeRow("Tgl Bayar", invoice.formattedPaidDate ?: BillInvoice.formatDate(System.currentTimeMillis()))
        writeRow("ID Pelanggan", invoice.customerCode)
        writeRow("Nama", invoice.customerName)
        if (invoice.customerPhone.isNotEmpty()) {
            writeRow("Kontak", invoice.customerPhone)
        }
        writeRow("Paket", invoice.planName)

        divider('-')

        // 4. Financial Breakdown
        writeRow("Biaya Langganan", invoice.formattedAmount)
        writeRow("Biaya Layanan/Adm", invoice.formattedAdminFee)
        divider('-')

        // Total (Bold, Right)
        write(boldOn)
        write(doubleHeight)
        writeRow("TOTAL BAYAR", invoice.formattedTotal)
        write(textNormal)
        write(boldOff)
        divider('=')

        // Payment status
        write(alignCenter)
        write(boldOn)
        val statusText = if (invoice.isPaid) {
            "STATUS: [ L U N A S ]"
        } else {
            "STATUS: [ BELUM DIBAYAR ]"
        }
        writeLine(statusText)
        invoice.paymentMethod?.let {
            writeLine("Metode: $it")
        }
        write(boldOff)
        divider('-')

        // 5. Footer & Notes
        write(alignCenter)
        writeLine(settings.receiptFooter)
        write(lineFeed)
        writeLine("Simpan struk ini sebagai bukti resmi.")
        writeLine("Layanan Gangguan Hub: ${settings.providerPhone}")
        write(lineFeed)
        write(lineFeed)
        write(lineFeed)

        // Cut paper if supported
        write(feedAndCut)

        return stream.toByteArray()
    }

    /**
     * Generates a realistic plain text preview matching the 32-column thermal paper format.
     */
    fun getReceiptPreviewText(invoice: BillInvoice, settings: AppSettings): String {
        val sb = StringBuilder()
        fun padRow(left: String, right: String): String {
            val spaceCount = (PAPER_WIDTH_CHARS - left.length - right.length).coerceAtLeast(1)
            return left + " ".repeat(spaceCount) + right
        }

        fun center(text: String): String {
            val spaces = ((PAPER_WIDTH_CHARS - text.length) / 2).coerceAtLeast(0)
            return " ".repeat(spaces) + text
        }

        val divider1 = "=".repeat(PAPER_WIDTH_CHARS)
        val divider2 = "-".repeat(PAPER_WIDTH_CHARS)

        sb.appendLine(center(settings.providerName))
        if (settings.providerTagline.isNotEmpty()) sb.appendLine(center(settings.providerTagline))
        if (settings.providerAddress.isNotEmpty()) sb.appendLine(center(settings.providerAddress))
        if (settings.providerPhone.isNotEmpty()) sb.appendLine(center("Telp: ${settings.providerPhone}"))
        sb.appendLine(divider1)
        sb.appendLine(center("BUKTI PEMBAYARAN INTERNET"))
        sb.appendLine(divider1)

        sb.appendLine(padRow("No. Struk", invoice.invoiceNumber))
        sb.appendLine(padRow("Periode", invoice.periodMonthYear))
        sb.appendLine(padRow("Tgl Bayar", invoice.formattedPaidDate ?: BillInvoice.formatDate(System.currentTimeMillis())))
        sb.appendLine(padRow("ID Pelanggan", invoice.customerCode))
        sb.appendLine(padRow("Nama", invoice.customerName))
        sb.appendLine(padRow("Paket", invoice.planName))
        sb.appendLine(divider2)

        sb.appendLine(padRow("Biaya Langganan", invoice.formattedAmount))
        sb.appendLine(padRow("Biaya Layanan/Adm", invoice.formattedAdminFee))
        sb.appendLine(divider2)
        sb.appendLine(padRow("TOTAL BAYAR", invoice.formattedTotal))
        sb.appendLine(divider1)

        val statusText = if (invoice.isPaid) "[ L U N A S ]" else "[ BELUM LUNAS ]"
        sb.appendLine(center(statusText))
        invoice.paymentMethod?.let {
            sb.appendLine(center("Metode: $it"))
        }
        sb.appendLine(divider2)
        sb.appendLine(center("Simpan struk ini sbg bukti sah"))
        sb.appendLine(center("CS: ${settings.providerPhone}"))

        return sb.toString()
    }

    /**
     * Prints receipt directly over Bluetooth RFCOMM socket to the selected device address.
     */
    @SuppressLint("MissingPermission")
    suspend fun printReceipt(
        deviceAddress: String,
        invoice: BillInvoice,
        settings: AppSettings
    ): PrintResult = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter
            ?: return@withContext PrintResult.Error("Perangkat tidak mendukung Bluetooth.")

        if (!adapter.isEnabled) {
            return@withContext PrintResult.Error("Bluetooth dinonaktifkan. Silakan aktifkan Bluetooth terlebih dahulu.")
        }

        if (!hasBluetoothPermissions()) {
            return@withContext PrintResult.Error("Izin Bluetooth belum diberikan pada aplikasi.")
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)

            // Cancel discovery before connecting for optimal bandwidth/speed
            try {
                adapter.cancelDiscovery()
            } catch (_: Exception) {}

            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()

            outputStream = socket.outputStream
            val payload = buildEscPosReceipt(invoice, settings)
            outputStream.write(payload)
            outputStream.flush()

            // Wait brief moment for transmission to finish
            Thread.sleep(500)

            PrintResult.Success
        } catch (e: Exception) {
            PrintResult.Error("Gagal mencetak: ${e.localizedMessage ?: "Tidak dapat terhubung ke printer thermal Bluetooth"}")
        } finally {
            try {
                outputStream?.close()
            } catch (_: Exception) {}
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }
}
