package com.example.data.repository

import com.example.data.local.BillInvoiceDao
import com.example.data.local.CustomerDao
import com.example.data.model.BillInvoice
import com.example.data.model.Customer
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BillingRepository(
    private val customerDao: CustomerDao,
    private val invoiceDao: BillInvoiceDao
) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allInvoices: Flow<List<BillInvoice>> = invoiceDao.getAllInvoices()
    val unpaidInvoices: Flow<List<BillInvoice>> = invoiceDao.getUnpaidInvoices()

    suspend fun getCustomerById(id: Long): Customer? = customerDao.getCustomerById(id)
    suspend fun getInvoiceById(id: Long): BillInvoice? = invoiceDao.getInvoiceById(id)

    suspend fun insertCustomer(customer: Customer): Long = customerDao.insertCustomer(customer)
    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: Customer) = customerDao.deleteCustomer(customer)

    suspend fun updateCustomerDataUsage(customerId: Long, usageGb: Double) {
        customerDao.updateUsage(customerId, usageGb)
    }

    suspend fun insertInvoice(invoice: BillInvoice): Long = invoiceDao.insertInvoice(invoice)
    suspend fun updateInvoice(invoice: BillInvoice) = invoiceDao.updateInvoice(invoice)
    suspend fun deleteInvoice(invoice: BillInvoice) = invoiceDao.deleteInvoice(invoice)

    suspend fun markInvoiceAsPaid(
        invoiceId: Long,
        paymentMethod: String,
        paidDateMs: Long = System.currentTimeMillis()
    ) {
        invoiceDao.markAsPaid(invoiceId, paidDateMs, paymentMethod)
    }

    suspend fun getUnpaidInvoicesSnapshot(): List<BillInvoice> {
        return invoiceDao.getUnpaidInvoicesSync()
    }

    suspend fun generateMonthlyInvoicesForActiveCustomers(period: String = getCurrentPeriod()): Int {
        val customers = mutableListOf<Customer>()
        // Generate new invoices for customers who don't have an invoice for this period
        // For simplicity, we can query or retrieve
        return 0
    }

    companion object {
        fun getCurrentPeriod(): String {
            val sdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
            return sdf.format(Date())
        }

        fun generateInvoiceNumber(): String {
            val sdf = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault())
            val randomDigits = (100..999).random()
            return "INV-${sdf.format(Date())}-$randomDigits"
        }
    }
}
