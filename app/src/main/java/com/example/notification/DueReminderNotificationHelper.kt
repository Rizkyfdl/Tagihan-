package com.example.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.model.AppSettings
import com.example.data.model.BillInvoice
import java.net.URLEncoder

object DueReminderNotificationHelper {

    const val CHANNEL_ID = "channel_tagihan_internet_due"
    const val NOTIFICATION_ID_BASE = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Pengingat Jatuh Tempo Tagihan"
            val descriptionText = "Notifikasi otomatis ketika tagihan internet mendekati atau melewati jatuh tempo"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun sendDueNotifications(
        context: Context,
        dueInvoices: List<BillInvoice>,
        settings: AppSettings
    ): Int {
        if (!hasNotificationPermission(context)) return 0
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = NotificationManagerCompat.from(context)
        var count = 0

        // If multiple invoices, send a summary notification as well
        if (dueInvoices.size > 1) {
            val summaryTitle = "Pemberitahuan: ${dueInvoices.size} Tagihan Mendekati Jatuh Tempo"
            val summaryContent = "Terdapat tagihan internet yang perlu segera diselesaikan oleh pelanggan."

            val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(summaryTitle)
                .setContentText(summaryContent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID_BASE, summaryNotification)
            count++
        }

        // Send individual notifications for up to top 3 urgent invoices
        dueInvoices.take(3).forEachIndexed { index, invoice ->
            val days = invoice.daysUntilDue()
            val dueStatus = when {
                days < 0 -> "TERLAMBAT ${-days} HARI"
                days == 0 -> "JATUH TEMPO HARI INI"
                days == 1 -> "JATUH TEMPO BESOK"
                else -> "Jatuh tempo dalam $days hari"
            }

            val title = "Tagihan Internet: ${invoice.customerName} ($dueStatus)"
            val message = "ID: ${invoice.customerCode} | Paket: ${invoice.planName} | Total: ${invoice.formattedTotal} | Tempo: ${invoice.formattedDueDate}"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID_BASE + index + 1, notification)
            count++
        }

        return count
    }

    /**
     * Builds standard WhatsApp reminder URL with pre-filled professional reminder text.
     */
    fun createWhatsAppReminderIntent(invoice: BillInvoice, settings: AppSettings): Intent {
        val cleanPhone = formatPhoneNumber(invoice.customerPhone)
        val days = invoice.daysUntilDue()
        val dueStatus = when {
            days < 0 -> "telah melewati jatuh tempo sejak ${invoice.formattedDueDate}"
            days == 0 -> "jatuh tempo HARI INI (${invoice.formattedDueDate})"
            days == 1 -> "akan jatuh tempo BESOK (${invoice.formattedDueDate})"
            else -> "akan jatuh tempo pada ${invoice.formattedDueDate}"
        }

        val message = """
            *Pemberitahuan Tagihan Internet*
            _${settings.providerName}_
            
            Yth. Bpk/Ibu *${invoice.customerName}*,
            
            Menginformasikan bahwa tagihan internet Anda $dueStatus.
            
            *Rincian Tagihan:*
            • No. Tagihan: ${invoice.invoiceNumber}
            • ID Pelanggan: ${invoice.customerCode}
            • Paket: ${invoice.planName}
            • Periode: ${invoice.periodMonthYear}
            • Biaya Paket: ${invoice.formattedAmount}
            • Biaya Admin: ${invoice.formattedAdminFee}
            • *TOTAL TAGIHAN: ${invoice.formattedTotal}*
            
            Mohon melakukan pembayaran sebelum batas jatuh tempo untuk kenyamanan koneksi internet Anda.
            
            _Pusat Bantuan & Layanan: ${settings.providerPhone}_
            Terima kasih atas kerja samanya.
        """.trimIndent()

        val encodedMessage = URLEncoder.encode(message, "UTF-8")
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")

        return Intent(Intent.ACTION_VIEW, uri)
    }

    private fun formatPhoneNumber(phone: String): String {
        var clean = phone.replace(Regex("[^0-9]"), "")
        if (clean.startsWith("0")) {
            clean = "62" + clean.substring(1)
        }
        return clean
    }
}
