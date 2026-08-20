package com.hnn.catng.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddSubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, name: String?) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Subscription Manual",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter your subscription link. Configs will be imported and grouped automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        isError = false
                    },
                    label = { Text("Subscription URL") },
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = "URL")
                    },
                    singleLine = true,
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Please enter a valid URL")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name (Optional)") },
                    placeholder = { Text("import_sub (Auto-detect)") },
                    leadingIcon = {
                        Icon(Icons.Default.Bookmark, contentDescription = "Name")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))) {
                        onConfirm(url.trim(), name.trim().ifBlank { null })
                        onDismiss()
                    } else {
                        isError = true
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
