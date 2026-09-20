package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
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
import com.example.data.model.BillInvoice
import com.example.data.model.Customer
import com.example.data.repository.BillingRepository
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceDialog(
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onCreateInvoice: (BillInvoice) -> Unit
) {
    if (customers.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Belum Ada Pelanggan") },
            text = { Text("Silakan tambahkan data pelanggan terlebih dahulu sebelum membuat tagihan.") },
            confirmButton = {
                Button(onClick = onDismiss) { Text("Mengerti") }
            }
        )
        return
    }

    var selectedCustomer by remember { mutableStateOf(customers.first()) }
    var expandedCustomerDropdown by remember { mutableStateOf(false) }

    var periodMonthYear by remember { mutableStateOf(BillingRepository.getCurrentPeriod()) }
    var amountStr by remember { mutableStateOf(selectedCustomer.monthlyFee.toInt().toString()) }
    var adminFeeStr by remember { mutableStateOf("2500") }
    var daysUntilDueStr by remember { mutableStateOf("7") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Buat Tagihan Baru",
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
                // Customer Selector
                ExposedDropdownMenuBox(
                    expanded = expandedCustomerDropdown,
                    onExpandedChange = { expandedCustomerDropdown = !expandedCustomerDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "${selectedCustomer.name} (${selectedCustomer.customerCode})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pilih Pelanggan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCustomerDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("select_customer_for_invoice")
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCustomerDropdown,
                        onDismissRequest = { expandedCustomerDropdown = false }
                    ) {
                        customers.forEach { customer ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(customer.name, fontWeight = FontWeight.Bold)
                                        Text("${customer.customerCode} • ${customer.planName}", style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                onClick = {
                                    selectedCustomer = customer
                                    amountStr = customer.monthlyFee.toInt().toString()
                                    expandedCustomerDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = periodMonthYear,
                    onValueChange = { periodMonthYear = it },
                    label = { Text("Periode Tagihan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Biaya Paket (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = adminFeeStr,
                        onValueChange = { adminFeeStr = it },
                        label = { Text("Biaya Admin (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = daysUntilDueStr,
                    onValueChange = { daysUntilDueStr = it },
                    label = { Text("Jatuh Tempo (Berapa hari dari sekarang)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: selectedCustomer.monthlyFee
                    val adminFee = adminFeeStr.toDoubleOrNull() ?: 2500.0
                    val days = (daysUntilDueStr.toIntOrNull() ?: 7).coerceAtLeast(1)

                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, days)
                    val dueMs = cal.timeInMillis

                    val invoice = BillInvoice(
                        invoiceNumber = BillingRepository.generateInvoiceNumber(),
                        customerId = selectedCustomer.id,
                        customerName = selectedCustomer.name,
                        customerCode = selectedCustomer.customerCode,
                        customerPhone = selectedCustomer.phone,
                        periodMonthYear = periodMonthYear.ifBlank { BillingRepository.getCurrentPeriod() },
                        planName = selectedCustomer.planName,
                        dataUsedGb = selectedCustomer.currentUsageGb,
                        quotaLimitGb = selectedCustomer.quotaLimitGb,
                        amount = amount,
                        taxAdminFee = adminFee,
                        totalAmount = amount + adminFee,
                        dueDateEpochMs = dueMs,
                        isPaid = false
                    )
                    onCreateInvoice(invoice)
                },
                modifier = Modifier.testTag("btn_save_invoice")
            ) {
                Text("Buat Tagihan")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
