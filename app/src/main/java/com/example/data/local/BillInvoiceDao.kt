package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BillInvoice
import kotlinx.coroutines.flow.Flow

@Dao
interface BillInvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY dueDateEpochMs ASC, id DESC")
    fun getAllInvoices(): Flow<List<BillInvoice>>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: Long): BillInvoice?

    @Query("SELECT * FROM invoices WHERE isPaid = 0 ORDER BY dueDateEpochMs ASC")
    fun getUnpaidInvoices(): Flow<List<BillInvoice>>

    @Query("SELECT * FROM invoices WHERE isPaid = 0")
    suspend fun getUnpaidInvoicesSync(): List<BillInvoice>

    @Query("SELECT * FROM invoices WHERE customerId = :customerId ORDER BY id DESC")
    fun getInvoicesForCustomer(customerId: Long): Flow<List<BillInvoice>>

    @Query("SELECT COUNT(*) FROM invoices")
    suspend fun getInvoiceCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: BillInvoice): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<BillInvoice>)

    @Update
    suspend fun updateInvoice(invoice: BillInvoice)

    @Query("UPDATE invoices SET isPaid = 1, paidDateEpochMs = :paidDateMs, paymentMethod = :paymentMethod WHERE id = :invoiceId")
    suspend fun markAsPaid(invoiceId: Long, paidDateMs: Long, paymentMethod: String)

    @Delete
    suspend fun deleteInvoice(invoice: BillInvoice)
}
