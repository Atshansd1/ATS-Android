package com.ats.android.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ats.android.ui.theme.*
import com.ats.android.utils.DebugLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(DebugLogger.getLogs()) }
    var filterText by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var autoScroll by remember { mutableStateOf(true) }
    var showMessage by remember { mutableStateOf<String?>(null) }
    
    val listState = rememberLazyListState()
    
    // Auto-refresh logs every 500ms
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500)
        while (true) {
            logs = if (filterText.isNotEmpty()) {
                DebugLogger.getFilteredLogs(filterText)
            } else {
                DebugLogger.getLogs()
            }
            
            // Auto-scroll to bottom if enabled
            if (autoScroll && logs.isNotEmpty()) {
                listState.animateScrollToItem(logs.size - 1)
            }
            
            kotlinx.coroutines.delay(500)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Debug Logs")
                        Text(
                            "${logs.size} entries",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Filter button
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            if (filterText.isNotEmpty()) Icons.Default.FilterAlt else Icons.Default.FilterList,
                            "Filter",
                            tint = if (filterText.isNotEmpty()) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    
                    // Auto-scroll toggle
                    IconButton(onClick = { autoScroll = !autoScroll }) {
                        Icon(
                            if (autoScroll) Icons.Default.VerticalAlignBottom else Icons.Default.SwapVert,
                            "Auto Scroll",
                            tint = if (autoScroll) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    
                    // Copy all logs
                    IconButton(onClick = {
                        copyLogsToClipboard(context, DebugLogger.getLogsAsString())
                        showMessage = "Logs copied to clipboard"
                    }) {
                        Icon(Icons.Default.ContentCopy, "Copy Logs")
                    }
                    
                    // Clear logs
                    IconButton(onClick = {
                        DebugLogger.clear()
                        logs = emptyList()
                        showMessage = "Logs cleared"
                    }) {
                        Icon(Icons.Default.Delete, "Clear Logs")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter input
            if (showFilters) {
                OutlinedTextField(
                    value = filterText,
                    onValueChange = { 
                        filterText = it
                        logs = if (it.isNotEmpty()) {
                            DebugLogger.getFilteredLogs(it)
                        } else {
                            DebugLogger.getLogs()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Filter logs (tag or message)") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (filterText.isNotEmpty()) {
                            IconButton(onClick = { filterText = "" }) {
                                Icon(Icons.Default.Clear, "Clear filter")
                            }
                        }
                    },
                    singleLine = true
                )
                
                // Quick filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterText == "ExcelReportGenerator",
                        onClick = { filterText = if (filterText == "ExcelReportGenerator") "" else "ExcelReportGenerator" },
                        label = { Text("Excel", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = filterText == "ReportsViewModel",
                        onClick = { filterText = if (filterText == "ReportsViewModel") "" else "ReportsViewModel" },
                        label = { Text("ViewModel", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = filterText.startsWith("E/"),
                        onClick = { filterText = if (filterText.startsWith("E/")) "" else "E/" },
                        label = { Text("Errors", fontSize = 12.sp) }
                    )
                }
            }
            
            // Logs list
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Article,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Text(
                            if (filterText.isNotEmpty()) "No logs match filter" else "No logs yet",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        if (filterText.isEmpty()) {
                            Text(
                                "Try Excel export to see logs",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(logs) { entry ->
                        LogEntryItem(entry) {
                            // Copy single log entry
                            val text = "${entry.timestamp} ${entry.level}/${entry.tag}: ${entry.message}"
                            copyLogsToClipboard(context, text)
                            showMessage = "Log entry copied"
                        }
                    }
                }
            }
        }
        
        // Show message snackbar
        showMessage?.let { message ->
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(2000)
                showMessage = null
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            }
        }
    }
}

@Composable
fun LogEntryItem(
    entry: DebugLogger.LogEntry,
    onCopy: () -> Unit
) {
    val backgroundColor = when (entry.level) {
        "E" -> Color(0xFFFFEBEE) // Light red
        "W" -> Color(0xFFFFF3E0) // Light orange
        "I" -> Color(0xFFE3F2FD) // Light blue
        else -> Color.Transparent
    }
    
    val levelColor = when (entry.level) {
        "E" -> Color(0xFFD32F2F)
        "W" -> Color(0xFFF57C00)
        "I" -> Color(0xFF1976D2)
        else -> Color.Gray
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = backgroundColor,
        tonalElevation = if (entry.level == "E") 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Level indicator
            Text(
                entry.level,
                modifier = Modifier
                    .background(levelColor, MaterialTheme.shapes.small)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Log content
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        entry.timestamp,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        entry.tag,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    entry.message,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            // Copy button
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    "Copy",
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
            }
        }
    }
}

private fun copyLogsToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Debug Logs", text)
    clipboard.setPrimaryClip(clip)
}
