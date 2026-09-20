package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "invoices")
data class BillInvoice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String, // e.g. "INV-202609-001"
    val customerId: Long,
    val customerName: String,
    val customerCode: String,
    val customerPhone: String,
    val periodMonthYear: String, // e.g. "September 2026"
    val planName: String,
    val dataUsedGb: Double,
    val quotaLimitGb: Int,
    val amount: Double,
    val taxAdminFee: Double = 2500.0,
    val totalAmount: Double = amount + taxAdminFee,
    val dueDateEpochMs: Long,
    val isPaid: Boolean = false,
    val paidDateEpochMs: Long? = null,
    val paymentMethod: String? = null, // "Tunai", "Transfer Bank", "QRIS"
    val notes: String = ""
) {
    val formattedTotal: String
        get() = formatRupiah(totalAmount)

    val formattedAmount: String
        get() = formatRupiah(amount)

    val formattedAdminFee: String
        get() = formatRupiah(taxAdminFee)

    val formattedDueDate: String
        get() = formatDate(dueDateEpochMs)

    val formattedPaidDate: String?
        get() = paidDateEpochMs?.let { formatDate(it) }

    fun isOverdue(currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        return !isPaid && currentTimeMs > dueDateEpochMs
    }

    fun daysUntilDue(currentTimeMs: Long = System.currentTimeMillis()): Int {
        val diffMs = dueDateEpochMs - currentTimeMs
        return (diffMs / (1000 * 60 * 60 * 24)).toInt()
    }

    companion object {
        fun formatRupiah(value: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            format.maximumFractionDigits = 0
            return format.format(value)
        }

        fun formatDate(epochMs: Long): String {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            return sdf.format(Date(epochMs))
        }

        fun formatDateTime(epochMs: Long): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
            return sdf.format(Date(epochMs))
        }
    }
}
