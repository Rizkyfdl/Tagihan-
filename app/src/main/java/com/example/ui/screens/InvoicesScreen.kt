package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BillInvoice
import com.example.ui.InvoiceFilter
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    invoices: List<BillInvoice>,
    currentFilter: InvoiceFilter,
    searchQuery: String,
    onFilterChanged: (InvoiceFilter) -> Unit,
    onSearchChanged: (String) -> Unit,
    onPayClick: (BillInvoice) -> Unit,
    onPrintClick: (BillInvoice) -> Unit,
    onSendWhatsApp: (BillInvoice) -> Unit,
    onDeleteInvoice: (BillInvoice) -> Unit,
    onCreateInvoiceClick: () -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateInvoiceClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_create_invoice")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Buat Tagihan Baru")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("invoices_screen")
        ) {
            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChanged,
                placeholder = { Text("Cari nama, kode pelanggan, no tagihan...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_invoices_field")
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentFilter == InvoiceFilter.ALL,
                    onClick = { onFilterChanged(InvoiceFilter.ALL) },
                    label = { Text("Semua") }
                )
                FilterChip(
                    selected = currentFilter == InvoiceFilter.UNPAID,
                    onClick = { onFilterChanged(InvoiceFilter.UNPAID) },
                    label = { Text("Belum Bayar") }
                )
                FilterChip(
                    selected = currentFilter == InvoiceFilter.DUE_SOON,
                    onClick = { onFilterChanged(InvoiceFilter.DUE_SOON) },
                    label = { Text("Jatuh Tempo") }
                )
                FilterChip(
                    selected = currentFilter == InvoiceFilter.PAID,
                    onClick = { onFilterChanged(InvoiceFilter.PAID) },
                    label = { Text("Lunas") }
                )
            }

            // Invoice List
            if (invoices.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tidak Ada Tagihan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tidak ada tagihan yang cocok dengan filter saat ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(invoices, key = { it.id }) { invoice ->
                        InvoiceItemCard(
                            invoice = invoice,
                            onPayClick = { onPayClick(invoice) },
                            onPrintClick = { onPrintClick(invoice) },
                            onWhatsAppClick = { onSendWhatsApp(invoice) },
                            onDeleteClick = { onDeleteInvoice(invoice) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceItemCard(
    invoice: BillInvoice,
    onPayClick: () -> Unit,
    onPrintClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val now = System.currentTimeMillis()
    val isOverdue = invoice.isOverdue(now)
    val daysUntil = invoice.daysUntilDue(now)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("invoice_card_${invoice.id}"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Customer Name + Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invoice.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${invoice.invoiceNumber} • ${invoice.customerCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (invoice.isPaid) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusSuccess.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LUNAS",
                                color = StatusSuccess,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    val (badgeColor, badgeLabel) = when {
                        isOverdue -> StatusDanger to "LEWAT TEMPO"
                        daysUntil == 0 -> StatusWarning to "TEMPO HARI INI"
                        daysUntil <= 3 -> StatusWarning to "H-$daysUntil TEMPO"
                        else -> MaterialTheme.colorScheme.primary to "BELUM BAYAR"
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badgeLabel,
                            color = badgeColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Paket: ${invoice.planName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Periode: ${invoice.periodMonthYear}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val quotaText = if (invoice.quotaLimitGb > 0) "${invoice.quotaLimitGb} GB" else "Unlimited"
                    Text(
                        text = "Pemakaian Data: ${invoice.dataUsedGb} GB / $quotaText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (invoice.isPaid && invoice.formattedPaidDate != null) {
                        Text(
                            text = "Dibayar: ${invoice.formattedPaidDate} (${invoice.paymentMethod ?: "Tunai"})",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusSuccess
                        )
                    } else {
                        Text(
                            text = "Jatuh Tempo: ${invoice.formattedDueDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverdue) StatusDanger else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Tagihan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = invoice.formattedTotal,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "(Termasuk adm ${invoice.formattedAdminFee})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!invoice.isPaid) {
                    Button(
                        onClick = onPayClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_pay_${invoice.id}")
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bayar", fontSize = 13.sp)
                    }
                }

                // Bluetooth Thermal Receipt Button
                FilledTonalButton(
                    onClick = onPrintClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_print_${invoice.id}")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cetak Struk", fontSize = 13.sp)
                }

                // WhatsApp Reminder / Receipt button
                OutlinedButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier.testTag("btn_wa_${invoice.id}")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (invoice.isPaid) "Kirim WA" else "Ingatkan", fontSize = 13.sp)
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
