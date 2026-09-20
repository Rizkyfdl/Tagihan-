package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BillInvoice
import com.example.data.model.Customer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [Customer::class, BillInvoice::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun billInvoiceDao(): BillInvoiceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "internet_billing_db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        prepopulateDatabase(database.customerDao(), database.billInvoiceDao())
                    }
                }
            }
        }

        private suspend fun prepopulateDatabase(customerDao: CustomerDao, invoiceDao: BillInvoiceDao) {
            val customers = listOf(
                Customer(
                    id = 1,
                    customerCode = "NET-001",
                    name = "Budi Pratama",
                    phone = "081234567891",
                    address = "Jl. Merpati No. 14, RT 02/05",
                    planName = "Fiber Home 20 Mbps",
                    planSpeedMbps = 20,
                    monthlyFee = 165000.0,
                    quotaLimitGb = 300,
                    currentUsageGb = 265.4, // 88% FUP alert
                    dueDayOfMonth = 21,
                    ipAddress = "192.168.10.11"
                ),
                Customer(
                    id = 2,
                    customerCode = "NET-002",
                    name = "Siti Rahmawati",
                    phone = "085712345678",
                    address = "Jl. Flamboyan No. 5",
                    planName = "Fiber Gamer 50 Mbps",
                    planSpeedMbps = 50,
                    monthlyFee = 275000.0,
                    quotaLimitGb = 600,
                    currentUsageGb = 340.2, // ~56%
                    dueDayOfMonth = 20,
                    ipAddress = "192.168.10.12"
                ),
                Customer(
                    id = 3,
                    customerCode = "NET-003",
                    name = "Ahmad Hidayat",
                    phone = "082198765432",
                    address = "Komplek Melati Indah B2",
                    planName = "Fiber Bisnis 100 Mbps",
                    planSpeedMbps = 100,
                    monthlyFee = 450000.0,
                    quotaLimitGb = 0, // Unlimited
                    currentUsageGb = 820.5,
                    dueDayOfMonth = 25,
                    ipAddress = "192.168.10.13"
                ),
                Customer(
                    id = 4,
                    customerCode = "NET-004",
                    name = "Dewi Lestari",
                    phone = "081377889900",
                    address = "Gg. Kancil No. 8, RW 03",
                    planName = "Fiber Family 30 Mbps",
                    planSpeedMbps = 30,
                    monthlyFee = 210000.0,
                    quotaLimitGb = 400,
                    currentUsageGb = 385.0, // 96% high alert
                    dueDayOfMonth = 18,
                    ipAddress = "192.168.10.14"
                ),
                Customer(
                    id = 5,
                    customerCode = "NET-005",
                    name = "Hendro Wijaya",
                    phone = "089855667788",
                    address = "Ruko Grand City No. 3",
                    planName = "Fiber Bisnis 50 Mbps",
                    planSpeedMbps = 50,
                    monthlyFee = 320000.0,
                    quotaLimitGb = 500,
                    currentUsageGb = 210.0,
                    dueDayOfMonth = 22,
                    ipAddress = "192.168.10.15"
                )
            )
            customerDao.insertCustomers(customers)

            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()

            // Due tomorrow (H-1 alert)
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val dueTomorrow = cal.timeInMillis

            // Due today (Hari H)
            cal.timeInMillis = now
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            val dueToday = cal.timeInMillis

            // Overdue by 2 days
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, -2)
            val overdueDays = cal.timeInMillis

            // Due in 5 days
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, 5)
            val dueIn5Days = cal.timeInMillis

            val invoices = listOf(
                BillInvoice(
                    id = 1,
                    invoiceNumber = "INV-202609-001",
                    customerId = 1,
                    customerName = "Budi Pratama",
                    customerCode = "NET-001",
                    customerPhone = "081234567891",
                    periodMonthYear = "September 2026",
                    planName = "Fiber Home 20 Mbps",
                    dataUsedGb = 265.4,
                    quotaLimitGb = 300,
                    amount = 165000.0,
                    taxAdminFee = 2500.0,
                    totalAmount = 167500.0,
                    dueDateEpochMs = dueTomorrow,
                    isPaid = false
                ),
                BillInvoice(
                    id = 2,
                    invoiceNumber = "INV-202609-002",
                    customerId = 2,
                    customerName = "Siti Rahmawati",
                    customerCode = "NET-002",
                    customerPhone = "085712345678",
                    periodMonthYear = "September 2026",
                    planName = "Fiber Gamer 50 Mbps",
                    dataUsedGb = 340.2,
                    quotaLimitGb = 600,
                    amount = 275000.0,
                    taxAdminFee = 2500.0,
                    totalAmount = 277500.0,
                    dueDateEpochMs = dueToday,
                    isPaid = false
                ),
                BillInvoice(
                    id = 3,
                    invoiceNumber = "INV-202609-003",
                    customerId = 4,
                    customerName = "Dewi Lestari",
                    customerCode = "NET-004",
                    customerPhone = "081377889900",
                    periodMonthYear = "September 2026",
                    planName = "Fiber Family 30 Mbps",
                    dataUsedGb = 385.0,
                    quotaLimitGb = 400,
                    amount = 210000.0,
                    taxAdminFee = 2500.0,
                    totalAmount = 212500.0,
                    dueDateEpochMs = overdueDays,
                    isPaid = false,
                    notes = "Terlambat 2 hari"
                ),
                BillInvoice(
                    id = 4,
                    invoiceNumber = "INV-202609-004",
                    customerId = 3,
                    customerName = "Ahmad Hidayat",
                    customerCode = "NET-003",
                    customerPhone = "082198765432",
                    periodMonthYear = "September 2026",
                    planName = "Fiber Bisnis 100 Mbps",
                    dataUsedGb = 820.5,
                    quotaLimitGb = 0,
                    amount = 450000.0,
                    taxAdminFee = 2500.0,
                    totalAmount = 452500.0,
                    dueDateEpochMs = dueIn5Days,
                    isPaid = true,
                    paidDateEpochMs = now - (1000 * 60 * 60 * 36),
                    paymentMethod = "Transfer Bank BCA",
                    notes = "Pembayaran lunas via transfer m-BCA"
                ),
                BillInvoice(
                    id = 5,
                    invoiceNumber = "INV-202609-005",
                    customerId = 5,
                    customerName = "Hendro Wijaya",
                    customerCode = "NET-005",
                    customerPhone = "089855667788",
                    periodMonthYear = "September 2026",
                    planName = "Fiber Bisnis 50 Mbps",
                    dataUsedGb = 210.0,
                    quotaLimitGb = 500,
                    amount = 320000.0,
                    taxAdminFee = 2500.0,
                    totalAmount = 322500.0,
                    dueDateEpochMs = dueIn5Days,
                    isPaid = false
                )
            )
            invoiceDao.insertInvoices(invoices)
        }
    }
}
