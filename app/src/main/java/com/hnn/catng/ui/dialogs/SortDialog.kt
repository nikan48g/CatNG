package com.hnn.catng.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hnn.catng.model.SortType

@Composable
fun SortDialog(
    currentSort: SortType,
    onDismiss: () -> Unit,
    onSortSelected: (SortType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Sort Configs",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SortOptionRow(
                    title = "Best Ping (Fastest first)",
                    icon = Icons.Default.Speed,
                    isSelected = currentSort == SortType.BEST_PING,
                    onClick = {
                        onSortSelected(SortType.BEST_PING)
                        onDismiss()
                    }
                )

                SortOptionRow(
                    title = "Name (A to Z)",
                    icon = Icons.Default.SortByAlpha,
                    isSelected = currentSort == SortType.NAME_ASC,
                    onClick = {
                        onSortSelected(SortType.NAME_ASC)
                        onDismiss()
                    }
                )

                SortOptionRow(
                    title = "Name (Z to A)",
                    icon = Icons.Default.SortByAlpha,
                    isSelected = currentSort == SortType.NAME_DESC,
                    onClick = {
                        onSortSelected(SortType.NAME_DESC)
                        onDismiss()
                    }
                )

                SortOptionRow(
                    title = "Recently Added (Newest)",
                    icon = Icons.Default.DateRange,
                    isSelected = currentSort == SortType.NEWEST,
                    onClick = {
                        onSortSelected(SortType.NEWEST)
                        onDismiss()
                    }
                )

                SortOptionRow(
                    title = "Oldest First",
                    icon = Icons.Default.DateRange,
                    isSelected = currentSort == SortType.OLDEST,
                    onClick = {
                        onSortSelected(SortType.OLDEST)
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun SortOptionRow(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
    }
}
