package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.LightBackground
import com.example.myapplication.ui.theme.TextPrimary
import com.example.myapplication.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SpaceViewModel,
    onOpenOnboarding: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()

    var daysText by remember { mutableStateOf(settings.daysBeforeExpiry.toString()) }
    LaunchedEffect(settings.daysBeforeExpiry) { daysText = settings.daysBeforeExpiry.toString() }

    val initialGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(initialGranted) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { ok -> granted = ok }

    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var confirmImport by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                runCatching { com.example.myapplication.backup.BackupManager.exportToZip(context, uri) }
                launch(Dispatchers.Main) { Toast.makeText(context, "已导出", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            confirmImport = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(20.dp)
            .statusBarsPadding()
    ) {
        Text("设置", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
        Spacer(modifier = Modifier.height(12.dp))

        Text("教程与示例", fontWeight = FontWeight.Bold, color = TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onOpenOnboarding,
                modifier = Modifier.weight(1f)
            ) { Text("打开教程") }
            OutlinedButton(
                onClick = {
                    viewModel.addDemoData()
                    Toast.makeText(context, "已添加演示数据", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f)
            ) { Text("添加示例") }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text("到期提醒", color = TextPrimary, fontWeight = FontWeight.Medium)
                Text("每天检查临近过期物品", color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = settings.remindersEnabled,
                onCheckedChange = { viewModel.setRemindersEnabled(it) }
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = daysText,
            onValueChange = { daysText = it },
            label = { Text("提前提醒天数（0-30）") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
            onClick = { viewModel.setDaysBeforeExpiry(daysText.toIntOrNull() ?: settings.daysBeforeExpiry) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("保存提醒设置") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("通知权限（Android 13+）", fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(
            text = if (granted) "已授予" else "未授予（将无法弹出系统通知）",
            color = TextSecondary,
            fontSize = 12.sp
        )
        OutlinedButton(
            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("申请通知权限") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("备份与恢复", fontWeight = FontWeight.Bold, color = TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { exportLauncher.launch("myapplication_backup.zip") },
                modifier = Modifier.weight(1f)
            ) { Text("导出") }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/zip")) },
                modifier = Modifier.weight(1f)
            ) { Text("导入") }
        }
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("导入备份") },
            text = { Text("导入会覆盖当前所有数据，确定继续吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingImportUri
                        confirmImport = false
                        if (uri != null) {
                            scope.launch(Dispatchers.IO) {
                                runCatching { com.example.myapplication.backup.BackupManager.importFromZip(context, uri) }
                                launch(Dispatchers.Main) { Toast.makeText(context, "已导入", Toast.LENGTH_SHORT).show() }
                            }
                        }
                    }
                ) { Text("确定") }
            },
            dismissButton = { OutlinedButton(onClick = { confirmImport = false }) { Text("取消") } }
        )
    }
}
