package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerCode: String, // e.g. "NET-001"
    val name: String,
    val phone: String,
    val address: String,
    val planName: String, // e.g. "Fiber Home 20 Mbps"
    val planSpeedMbps: Int, // e.g. 20
    val monthlyFee: Double, // e.g. 165000.0
    val quotaLimitGb: Int, // 0 = Unlimited, or e.g. 300 GB
    val currentUsageGb: Double, // e.g. 145.8
    val dueDayOfMonth: Int, // e.g. 10 (Jatuh tempo setiap tgl 10)
    val ipAddress: String = "",
    val isActive: Boolean = true
) {
    val usagePercentage: Float
        get() = if (quotaLimitGb > 0) {
            ((currentUsageGb / quotaLimitGb) * 100f).coerceIn(0.0, 100.0).toFloat()
        } else {
            0f
        }

    val isFupWarning: Boolean
        get() = quotaLimitGb > 0 && usagePercentage >= 85f
}
