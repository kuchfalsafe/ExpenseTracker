package com.kishan.expensetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.util.*

@Composable
fun MonthYearPicker(
    onDismissRequest: () -> Unit,
    onDateSelected: (Int, Int) -> Unit, // month (0-11), year
    initialMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    initialYear: Int = Calendar.getInstance().get(Calendar.YEAR)
) {
    var selectedMonth by remember { mutableStateOf(initialMonth) }
    var selectedYear by remember { mutableStateOf(initialYear) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select Month & Year",
                    style = MaterialTheme.typography.titleLarge
                )

                // Month Selection
                Text(
                    text = "Month",
                    style = MaterialTheme.typography.titleMedium
                )
                val months = listOf(
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    months.chunked(3).forEach { monthGroup ->
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            monthGroup.forEachIndexed { index, month ->
                                val monthIndex = months.indexOf(month)
                                FilterChip(
                                    selected = selectedMonth == monthIndex,
                                    onClick = { selectedMonth = monthIndex },
                                    label = { Text(month.take(3)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Year Selection
                Text(
                    text = "Year",
                    style = MaterialTheme.typography.titleMedium
                )
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val years = (currentYear - 5..currentYear + 1).toList().reversed()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    years.chunked(3).forEach { yearGroup ->
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            yearGroup.forEach { year ->
                                FilterChip(
                                    selected = selectedYear == year,
                                    onClick = { selectedYear = year },
                                    label = { Text(year.toString()) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onDateSelected(selectedMonth, selectedYear)
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

