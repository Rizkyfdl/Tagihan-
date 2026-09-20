package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bluetooth.BluetoothDeviceInfo
import com.example.bluetooth.ThermalPrinterManager
import com.example.data.model.AppSettings
import com.example.data.model.BillInvoice
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun PrinterSettingsScreen(
    settings: AppSettings,
    thermalPrinterManager: ThermalPrinterManager,
    pairedDevices: List<BluetoothDeviceInfo>,
    selectedDeviceAddress: String?,
    onSelectDevice: (String) -> Unit,
    onRefreshDevices: () -> Unit,
    onSaveSettings: (AppSettings) -> Unit,
    onTestPrint: (BillInvoice) -> Unit
) {
    val context = LocalContext.current

    val isBtEnabled = thermalPrinterManager.isBluetoothEnabled()
    val hasBtPermissions = thermalPrinterManager.hasBluetoothPermissions()

    // Permission launcher for Bluetooth
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefreshDevices()
    }

    // Editable Provider settings states
    var providerName by remember { mutableStateOf(settings.providerName) }
    var providerTagline by remember { mutableStateOf(settings.providerTagline) }
    var providerAddress by remember { mutableStateOf(settings.providerAddress) }
    var providerPhone by remember { mutableStateOf(settings.providerPhone) }
    var receiptFooter by remember { mutableStateOf(settings.receiptFooter) }
    var autoDaysStr by remember { mutableStateOf(settings.autoReminderDaysBefore.toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("printer_settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Bluetooth Printer Hardware
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Printer Thermal Bluetooth",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Format ESC/POS 58mm / 80mm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onRefreshDevices) {
                            Icon(Icons.Default.Refresh, contentDescription = "Segarkan")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bluetooth Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isBtEnabled) StatusSuccess.copy(alpha = 0.15f) else StatusDanger.copy(alpha = 0.15f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isBtEnabled) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                                    contentDescription = null,
                                    tint = if (isBtEnabled) StatusSuccess else StatusDanger,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBtEnabled) "Bluetooth Aktif" else "Bluetooth Nonaktif",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBtEnabled) StatusSuccess else StatusDanger
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasBtPermissions) StatusSuccess.copy(alpha = 0.15f) else StatusWarning.copy(alpha = 0.15f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (hasBtPermissions) Icons.Default.CheckCircle else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (hasBtPermissions) StatusSuccess else StatusWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hasBtPermissions) "Izin Diberikan" else "Butuh Izin",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasBtPermissions) StatusSuccess else StatusWarning
                                )
                            }
                        }
                    }

                    // Request permission / Open Bluetooth settings button if needed
                    if (!hasBtPermissions) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.BLUETOOTH_CONNECT,
                                            Manifest.permission.BLUETOOTH_SCAN
                                        )
                                    )
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.BLUETOOTH,
                                            Manifest.permission.BLUETOOTH_ADMIN,
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Izinkan Akses Bluetooth")
                        }
                    } else if (!isBtEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Buka Pengaturan Bluetooth HP")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Daftar Perangkat Thermal Printer Terpasang:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (pairedDevices.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Belum ada printer thermal Bluetooth terpasang.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Pasangkan (pair) printer Bluetooth thermal Anda (misal: RPP02N, Panda POS, PT-210) melalui Pengaturan Bluetooth HP, lalu tekan tombol segarkan di atas.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        pairedDevices.forEach { device ->
                            val isSelected = device.address == selectedDeviceAddress
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onSelectDevice(device.address) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onSelectDevice(device.address) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = device.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = device.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Terpilih",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Test Print Button
                    FilledTonalButton(
                        onClick = {
                            val dummyInvoice = BillInvoice(
                                id = 999,
                                invoiceNumber = "TEST-PRINT-01",
                                customerId = 0,
                                customerName = "CONTOH PELANGGAN",
                                customerCode = "NET-TEST",
                                customerPhone = "08123456789",
                                periodMonthYear = "Bulan Ini",
                                planName = "Paket Uji Coba Printer",
                                dataUsedGb = 45.2,
                                quotaLimitGb = 300,
                                amount = 150000.0,
                                taxAdminFee = 2500.0,
                                totalAmount = 152500.0,
                                dueDateEpochMs = System.currentTimeMillis(),
                                isPaid = true,
                                paidDateEpochMs = System.currentTimeMillis(),
                                paymentMethod = "Tunai (Uji Coba Struk)"
                            )
                            onTestPrint(dummyInvoice)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_test_print_receipt")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tes Pratinjau & Cetak Struk Kuitansi")
                    }
                }
            }
        }

        // Section: Provider & ISP Profile
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Profil Penyedia & Struk Kuitansi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Data yang dicetak pada kepala struk & pesan pengingat",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = providerName,
                        onValueChange = { providerName = it },
                        label = { Text("Nama Usaha / Provider (ISP / RT-RW Net)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = providerTagline,
                        onValueChange = { providerTagline = it },
                        label = { Text("Slogan / Subtitle") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = providerAddress,
                        onValueChange = { providerAddress = it },
                        label = { Text("Alamat Kantor / Posko") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = providerPhone,
                        onValueChange = { providerPhone = it },
                        label = { Text("Nomor CS / WhatsApp Layanan") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it },
                        label = { Text("Pesan Kaki Struk (Footer)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    OutlinedTextField(
                        value = autoDaysStr,
                        onValueChange = { autoDaysStr = it },
                        label = { Text("Kirim Notifikasi Otomatis Sebelum Jatuh Tempo (Hari)") },
                        supportingText = { Text("Default: 3 hari sebelum jatuh tempo (H-3)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            val days = (autoDaysStr.toIntOrNull() ?: 3).coerceIn(1, 14)
                            val updated = settings.copy(
                                providerName = providerName.ifBlank { "NEXUS FIBER NETWORK" },
                                providerTagline = providerTagline,
                                providerAddress = providerAddress,
                                providerPhone = providerPhone,
                                receiptFooter = receiptFooter,
                                autoReminderDaysBefore = days
                            )
                            onSaveSettings(updated)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_save_settings")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan Pengaturan")
                    }
                }
            }
        }
    }
}
