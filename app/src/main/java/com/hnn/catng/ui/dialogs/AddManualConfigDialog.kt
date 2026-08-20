package com.hnn.catng.ui.dialogs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import com.hnn.catng.parser.EmojiCleaner

@Composable
fun AddManualConfigDialog(
    initialProtocol: String = "VLESS",
    onDismiss: () -> Unit,
    onConfirm: (
        protocol: String,
        remarks: String,
        server: String,
        port: Int,
        userId: String,
        network: String,
        path: String,
        sni: String,
        security: String
    ) -> Unit
) {
    val protocols = listOf("VLESS", "VMess", "Trojan", "Shadowsocks", "Socks5")
    var selectedProtocol by remember { mutableStateOf(initialProtocol) }

    var remarks by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf("443") }
    var userId by remember { mutableStateOf("") }
    var network by remember { mutableStateOf("ws") }
    var path by remember { mutableStateOf("/") }
    var sni by remember { mutableStateOf("") }
    var security by remember { mutableStateOf("tls") }

    val networks = listOf("ws", "grpc", "tcp")
    val securities = listOf("tls", "reality", "none")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add $selectedProtocol Config Manual",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // انتخاب پروتکل
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    protocols.forEach { proto ->
                        FilterChip(
                            selected = selectedProtocol.equals(proto, ignoreCase = true),
                            onClick = {
                                selectedProtocol = proto
                                if (proto == "Socks5" && portText == "443") portText = "1080"
                                if (proto == "Shadowsocks" && portText == "443") portText = "8388"
                            },
                            label = { Text(proto) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = EmojiCleaner.clean(it) },
                    label = { Text("Remarks / Name") },
                    placeholder = { Text("My Server") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = server,
                        onValueChange = { server = it.trim() },
                        label = { Text("Server Address") },
                        placeholder = { Text("example.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(2f)
                    )

                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Port") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it.trim() },
                    label = { Text(if (selectedProtocol in listOf("VLESS", "VMess")) "UUID / User ID" else "Password") },
                    placeholder = { Text("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // تنظیمات نتورک
                Text(
                    text = "Transport Network",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    networks.forEach { net ->
                        FilterChip(
                            selected = network == net,
                            onClick = { network = net },
                            label = { Text(net.uppercase()) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                if (network == "ws") {
                    OutlinedTextField(
                        value = path,
                        onValueChange = { path = it.trim() },
                        label = { Text("WS Path") },
                        placeholder = { Text("/path") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // تنظیمات امنیت
                Text(
                    text = "TLS / Security",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    securities.forEach { sec ->
                        FilterChip(
                            selected = security == sec,
                            onClick = { security = sec },
                            label = { Text(sec.uppercase()) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                if (security != "none") {
                    OutlinedTextField(
                        value = sni,
                        onValueChange = { sni = it.trim() },
                        label = { Text("SNI / Server Name") },
                        placeholder = { Text("domain.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (server.isNotBlank()) {
                        val port = portText.toIntOrNull() ?: 443
                        onConfirm(
                            selectedProtocol,
                            remarks.ifBlank { "$selectedProtocol - $server" },
                            server,
                            port,
                            userId,
                            network,
                            path,
                            sni.ifBlank { server },
                            security
                        )
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save")
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
