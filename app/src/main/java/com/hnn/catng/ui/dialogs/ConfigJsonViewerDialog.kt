package com.hnn.catng.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hnn.catng.model.ConfigItem

@Composable
fun ConfigJsonViewerDialog(
    config: ConfigItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val content = config.rawJson ?: config.rawUri ?: "{}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = config.name,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = content,
                    onValueChange = {},
                    readOnly = true,
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("CatNG Config", content)
                    clipboard.setPrimaryClip(clip)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
