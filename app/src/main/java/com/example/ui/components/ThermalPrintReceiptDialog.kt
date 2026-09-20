package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.bluetooth.BluetoothDeviceInfo
import com.example.data.model.BillInvoice
import com.example.ui.PrintUiState
import com.example.ui.theme.PaperReceiptBackground
import com.example.ui.theme.PaperReceiptBorder
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalPrintReceiptDialog(
    printState: PrintUiState,
    invoice: BillInvoice,
    onDismiss: () -> Unit,
    onSelectDevice: (String) -> Unit,
    onRefreshDevices: () -> Unit,
    onPrintToBluetooth: () -> Unit,
    onSimulatePrint: () -> Unit
) {
    val context = LocalContext.current
    var expandedDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!printState.isPrinting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cetak Kuitansi Thermal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    enabled = !printState.isPrinting
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Device selection
                Text(
                    text = "Pilih Printer Thermal Bluetooth (58mm / 80mm):",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown },
                        modifier = Modifier.weight(1f)
                    ) {
                        val selectedDevice = printState.pairedDevices.firstOrNull {
                            it.address == printState.selectedDeviceAddress
                        }
                        val displayText = selectedDevice?.let { "${it.name} (${it.address})" }
                            ?: if (printState.pairedDevices.isEmpty()) "Tidak ada printer dipasangkan" else "Pilih Printer"

                        OutlinedTextField(
                            value = displayText,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("select_printer_dropdown"),
                            leadingIcon = {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            singleLine = true
                        )

                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            if (printState.pairedDevices.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Belum ada printer Bluetooth dipasangkan di HP ini") },
                                    onClick = { expandedDropdown = false }
                                )
                            } else {
                                printState.pairedDevices.forEach { device: BluetoothDeviceInfo ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(device.name, fontWeight = FontWeight.SemiBold)
                                                Text(device.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            }
                                        },
                                        onClick = {
                                            onSelectDevice(device.address)
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onRefreshDevices,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .testTag("btn_refresh_printers")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Segarkan Printer")
                    }
                }

                // Status Message Box
                if (printState.errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusDanger)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = printState.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                if (printState.printSuccess) {
                    Surface(
                        color = StatusSuccess.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kuitansi berhasil dikirim ke printer thermal!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = StatusSuccess
                            )
                        }
                    }
                }

                // Paper Receipt Simulation Preview
                Text(
                    text = "Pratinjau Struk Kertas Thermal (32 Karakter):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PaperReceiptBackground, RoundedCornerShape(8.dp))
                        .border(1.dp, PaperReceiptBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        Text(
                            text = printState.receiptPreviewText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = Color(0xFF1E293B),
                            modifier = Modifier.testTag("thermal_receipt_preview_text")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Direct Bluetooth print button
                    Button(
                        onClick = onPrintToBluetooth,
                        enabled = !printState.isPrinting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_print_bluetooth"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (printState.isPrinting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mencetak...")
                        } else {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cetak BT")
                        }
                    }

                    // Simulation print button
                    OutlinedButton(
                        onClick = onSimulatePrint,
                        enabled = !printState.isPrinting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_simulate_print")
                    ) {
                        Text("Simulasi Cetak")
                    }
                }

                // Share text button
                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, printState.receiptPreviewText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Kuitansi"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_share_receipt")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bagikan Teks Kuitansi")
                }
            }
        },
        dismissButton = {}
    )
}
