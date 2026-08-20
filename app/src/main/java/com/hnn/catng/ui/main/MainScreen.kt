package com.hnn.catng.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hnn.catng.model.ConfigItem
import com.hnn.catng.model.ConnectionStatus
import com.hnn.catng.ui.dialogs.AddManualConfigDialog
import com.hnn.catng.ui.dialogs.AddSubscriptionDialog
import com.hnn.catng.ui.dialogs.ConfigJsonViewerDialog
import com.hnn.catng.ui.dialogs.SortDialog
import com.hnn.catng.ui.dialogs.SubscriptionSelectorDialog
import com.hnn.catng.ui.theme.ConnectedGreen
import com.hnn.catng.ui.theme.ConnectingYellow
import com.hnn.catng.ui.theme.DisconnectedGrey
import com.hnn.catng.ui.theme.PingGreen
import com.hnn.catng.ui.theme.PingGrey
import com.hnn.catng.ui.theme.PingRed
import com.hnn.catng.ui.theme.PingYellow
import com.hnn.catng.ui.theme.PrimaryCyan
import com.hnn.catng.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val configs by viewModel.displayConfigs.collectAsState()
    val allConfigs by viewModel.repository.configs.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()
    val selectedSubId by viewModel.selectedSubscriptionId.collectAsState()
    val selectedConfigId by viewModel.selectedConfigId.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val vpnState by viewModel.vpnState.collectAsState()
    val isTestingPings by viewModel.isTestingPings.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showAddSubDialog by remember { mutableStateOf(false) }
    var showAddManualConfigDialog by remember { mutableStateOf(false) }
    var manualConfigProtocol by remember { mutableStateOf("VLESS") }
    var showSortDialog by remember { mutableStateOf(false) }
    var showSubSelectorDialog by remember { mutableStateOf(false) }
    var configToViewJson by remember { mutableStateOf<ConfigItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val currentGroupName = if (selectedSubId == null || selectedSubId == "ALL") {
        "All"
    } else {
        subscriptions.firstOrNull { it.id == selectedSubId }?.name ?: "import_sub"
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "CatNG",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CatNG",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            maxLines = 1
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clickable { showSubSelectorDialog = true }
                            .padding(end = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Subscriptions",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentGroupName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 100.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.testAllCurrentConfigs() },
                        enabled = !isTestingPings && configs.isNotEmpty(),
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (isTestingPings) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Speed, contentDescription = "Test Ping", modifier = Modifier.size(20.dp))
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", modifier = Modifier.size(20.dp))
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Paste From Clipboard") },
                                leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.pasteFromClipboard()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Add Subscription Manual") },
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showAddSubDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Add Config Manual") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    manualConfigProtocol = "VLESS"
                                    showAddManualConfigDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Test All Configs") },
                                leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.testAllCurrentConfigs()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Sort") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showSortDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            BottomConnectionBar(
                vpnState = vpnState,
                activeConfig = configs.firstOrNull { it.id == selectedConfigId }
                    ?: allConfigs.firstOrNull { it.id == selectedConfigId },
                onToggleConnect = { viewModel.toggleConnection(context) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (configs.isEmpty()) {
                EmptyStateView(
                    onPasteClick = { viewModel.pasteFromClipboard() },
                    onAddSubClick = { showAddSubDialog = true },
                    onAddManualClick = {
                        manualConfigProtocol = "VLESS"
                        showAddManualConfigDialog = true
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(configs, key = { it.id }) { config ->
                        ConfigCard(
                            config = config,
                            isSelected = config.id == selectedConfigId,
                            onSelect = { viewModel.selectConfig(config.id) },
                            onTestPing = { viewModel.testSinglePing(config) },
                            onViewJson = { configToViewJson = config },
                            onDelete = { viewModel.deleteConfig(config.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddSubDialog) {
        AddSubscriptionDialog(
            onDismiss = { showAddSubDialog = false },
            onConfirm = { url, name ->
                viewModel.fetchAndAddSubscription(url, name)
            }
        )
    }

    if (showAddManualConfigDialog) {
        AddManualConfigDialog(
            initialProtocol = manualConfigProtocol,
            onDismiss = { showAddManualConfigDialog = false },
            onConfirm = { proto, remarks, server, port, uid, net, path, sni, sec ->
                viewModel.addManualConfig(proto, remarks, server, port, uid, net, path, sni, sec)
            }
        )
    }

    if (showSortDialog) {
        SortDialog(
            currentSort = sortType,
            onDismiss = { showSortDialog = false },
            onSortSelected = { viewModel.setSortType(it) }
        )
    }

    if (showSubSelectorDialog) {
        SubscriptionSelectorDialog(
            subscriptions = subscriptions,
            selectedSubId = selectedSubId,
            totalAllConfigsCount = allConfigs.size,
            onDismiss = { showSubSelectorDialog = false },
            onSelectSub = { viewModel.selectSubscriptionFilter(it) },
            onDeleteSub = { viewModel.deleteSubscription(it) },
            onAddSubClick = { showAddSubDialog = true }
        )
    }

    configToViewJson?.let { config ->
        ConfigJsonViewerDialog(
            config = config,
            onDismiss = { configToViewJson = null }
        )
    }
}

@Composable
private fun ConfigCard(
    config: ConfigItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onTestPing: () -> Unit,
    onViewJson: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = if (isSelected) BorderStroke(1.5.dp, borderColor) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = config.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${config.server}:${config.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = config.protocol,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PingBadge(pingMs = config.pingMs, onTest = onTestPing)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onViewJson, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "View Config",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onTestPing, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Test Ping",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PingBadge(pingMs: Long?, onTest: () -> Unit) {
    val (color, text) = when {
        pingMs == null -> Pair(PingGrey, "Test Ping")
        pingMs < 0 -> Pair(PingRed, "Timeout")
        pingMs < 200 -> Pair(PingGreen, "$pingMs ms")
        pingMs < 450 -> Pair(PingYellow, "$pingMs ms")
        else -> Pair(PingRed, "$pingMs ms")
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.clickable(onClick = onTest)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

@Composable
private fun BottomConnectionBar(
    vpnState: com.hnn.catng.model.VpnState,
    activeConfig: ConfigItem?,
    onToggleConnect: () -> Unit
) {
    val isConnected = vpnState.status == ConnectionStatus.CONNECTED

    val statusText = when (vpnState.status) {
        ConnectionStatus.CONNECTED -> "Connected"
        ConnectionStatus.CONNECTING -> "Connecting..."
        ConnectionStatus.DISCONNECTING -> "Disconnecting..."
        ConnectionStatus.DISCONNECTED -> "Disconnected"
    }

    val statusColor by animateColorAsState(
        targetValue = when (vpnState.status) {
            ConnectionStatus.CONNECTED -> ConnectedGreen
            ConnectionStatus.CONNECTING -> ConnectingYellow
            else -> DisconnectedGrey
        },
        label = "statusColor"
    )

    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding() // جلوگیری کامل از تداخل با کلیدهای نوار ناوبری اندروید
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(statusColor, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                    }

                    Text(
                        text = activeConfig?.name ?: "No Config Selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isConnected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpeedStatItem(
                            icon = Icons.Default.ArrowDownward,
                            speedBytes = vpnState.downloadSpeedBps,
                            tint = PingGreen
                        )
                        SpeedStatItem(
                            icon = Icons.Default.ArrowUpward,
                            speedBytes = vpnState.uploadSpeedBps,
                            tint = PrimaryCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            ButtonConnect(
                status = vpnState.status,
                onClick = onToggleConnect
            )
        }
    }
}

@Composable
private fun SpeedStatItem(
    icon: ImageVector,
    speedBytes: Long,
    tint: Color
) {
    val speedText = formatSpeed(speedBytes)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = speedText,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    val kb = bytesPerSec / 1024.0
    return if (kb < 1000) {
        String.format("%.1f KB/s", kb)
    } else {
        String.format("%.2f MB/s", kb / 1024.0)
    }
}

@Composable
private fun ButtonConnect(
    status: ConnectionStatus,
    onClick: () -> Unit
) {
    val isConnected = status == ConnectionStatus.CONNECTED
    val isConnecting = status == ConnectionStatus.CONNECTING

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnecting) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val buttonColor by animateColorAsState(
        targetValue = when (status) {
            ConnectionStatus.CONNECTED -> ConnectedGreen
            ConnectionStatus.CONNECTING -> ConnectingYellow
            else -> MaterialTheme.colorScheme.primary
        },
        label = "buttonColor"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = buttonColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .scale(pulseScale)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isConnecting) {
                CircularProgressIndicator(
                    color = Color.Black,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Connecting...",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black
                )
            } else {
                Icon(
                    imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = if (isConnected) Color.Black else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "Disconnect" else "Tap To Connect",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isConnected) Color.Black else MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    onPasteClick: () -> Unit,
    onAddSubClick: () -> Unit,
    onAddManualClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Configs Available",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Add a subscription link, paste your Xray JSON configs or create one manually.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onPasteClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Paste", style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
                onClick = onAddSubClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Sub", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
