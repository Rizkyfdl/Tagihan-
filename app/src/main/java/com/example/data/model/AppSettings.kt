package com.example.data.model

data class AppSettings(
    val providerName: String = "NEXUS FIBER NETWORK",
    val providerTagline: String = "Layanan Internet Cepat & Stabil",
    val providerAddress: String = "Jl. Merdeka No. 88, Pusat Usaha",
    val providerPhone: String = "0812-3456-7890",
    val receiptFooter: String = "Terima kasih atas pembayaran Anda. Simpan struk ini sebagai bukti pembayaran sah.",
    val selectedPrinterAddress: String? = null,
    val selectedPrinterName: String? = null,
    val autoReminderDaysBefore: Int = 3
)
