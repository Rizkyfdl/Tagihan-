package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Customer

data class PlanPreset(
    val name: String,
    val speedMbps: Int,
    val fee: Double,
    val quotaGb: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerDialog(
    customerToEdit: Customer? = null,
    onDismiss: () -> Unit,
    onSave: (Customer) -> Unit
) {
    val planPresets = listOf(
        PlanPreset("Fiber Basic 20 Mbps", 20, 165000.0, 300),
        PlanPreset("Fiber Family 30 Mbps", 30, 210000.0, 400),
        PlanPreset("Fiber Gamer 50 Mbps", 50, 275000.0, 600),
        PlanPreset("Fiber Bisnis 100 Mbps", 100, 450000.0, 0)
    )

    var name by remember { mutableStateOf(customerToEdit?.name ?: "") }
    var phone by remember { mutableStateOf(customerToEdit?.phone ?: "") }
    var address by remember { mutableStateOf(customerToEdit?.address ?: "") }
    var customerCode by remember {
        mutableStateOf(customerToEdit?.customerCode ?: "NET-${(100..999).random()}")
    }

    var selectedPlan by remember {
        mutableStateOf(
            planPresets.find { it.name == customerToEdit?.planName } ?: planPresets.first()
        )
    }
    var customPlanName by remember { mutableStateOf(customerToEdit?.planName ?: selectedPlan.name) }
    var monthlyFeeStr by remember {
        mutableStateOf(customerToEdit?.monthlyFee?.toInt()?.toString() ?: selectedPlan.fee.toInt().toString())
    }
    var quotaLimitStr by remember {
        mutableStateOf(customerToEdit?.quotaLimitGb?.toString() ?: selectedPlan.quotaGb.toString())
    }
    var dueDayStr by remember {
        mutableStateOf(customerToEdit?.dueDayOfMonth?.toString() ?: "15")
    }

    var expandedPlanDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (customerToEdit == null) Icons.Default.PersonAdd else Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (customerToEdit == null) "Tambah Pelanggan Baru" else "Edit Pelanggan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = customerCode,
                    onValueChange = { customerCode = it },
                    label = { Text("ID Pelanggan (Kode)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_customer_code"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_customer_name"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Nomor WhatsApp / HP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_customer_phone"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat / Lokasi Pemasangan") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_customer_address"),
                    maxLines = 2
                )

                // Plan Preset Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedPlanDropdown,
                    onExpandedChange = { expandedPlanDropdown = !expandedPlanDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = customPlanName,
                        onValueChange = { customPlanName = it },
                        label = { Text("Paket Internet") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPlanDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("input_customer_plan")
                    )

                    ExposedDropdownMenu(
                        expanded = expandedPlanDropdown,
                        onDismissRequest = { expandedPlanDropdown = false }
                    ) {
                        planPresets.forEach { preset ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(preset.name, fontWeight = FontWeight.Bold)
                                        val quotaText = if (preset.quotaGb > 0) "${preset.quotaGb} GB" else "Unlimited"
                                        Text(
                                            "Rp ${preset.fee.toInt()} / bln • $quotaText",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedPlan = preset
                                    customPlanName = preset.name
                                    monthlyFeeStr = preset.fee.toInt().toString()
                                    quotaLimitStr = preset.quotaGb.toString()
                                    expandedPlanDropdown = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = monthlyFeeStr,
                        onValueChange = { monthlyFeeStr = it },
                        label = { Text("Tarif / Bln (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_customer_fee"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = quotaLimitStr,
                        onValueChange = { quotaLimitStr = it },
                        label = { Text("Kuota FUP (GB)") },
                        supportingText = { Text("0 = Unlimited") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_customer_quota"),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = dueDayStr,
                    onValueChange = { dueDayStr = it },
                    label = { Text("Tgl Jatuh Tempo Tiap Bulan (1-28)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_customer_dueday"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fee = monthlyFeeStr.toDoubleOrNull() ?: 165000.0
                    val quota = quotaLimitStr.toIntOrNull() ?: 300
                    val dueDay = (dueDayStr.toIntOrNull() ?: 10).coerceIn(1, 28)

                    val newOrUpdated = customerToEdit?.copy(
                        name = name.ifBlank { "Pelanggan Baru" },
                        customerCode = customerCode.ifBlank { "NET-100" },
                        phone = phone,
                        address = address,
                        planName = customPlanName,
                        planSpeedMbps = selectedPlan.speedMbps,
                        monthlyFee = fee,
                        quotaLimitGb = quota,
                        dueDayOfMonth = dueDay
                    ) ?: Customer(
                        customerCode = customerCode.ifBlank { "NET-100" },
                        name = name.ifBlank { "Pelanggan Baru" },
                        phone = phone,
                        address = address,
                        planName = customPlanName,
                        planSpeedMbps = selectedPlan.speedMbps,
                        monthlyFee = fee,
                        quotaLimitGb = quota,
                        currentUsageGb = 0.0,
                        dueDayOfMonth = dueDay
                    )

                    onSave(newOrUpdated)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("btn_save_customer")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
