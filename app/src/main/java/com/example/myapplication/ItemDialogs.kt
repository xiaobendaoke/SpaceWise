/**
 * 物品相关弹窗组件。
 *
 * 职责：
 * - 提供添加、编辑、详情展示等物品相关的对话框。
 * - 集成图片选择、日期选择逻辑。
 *
 * 上层用途：
 * - 被 `FolderBrowserScreen` 等页面调用，用于处理物品的交互操作。
 */
package com.example.myapplication

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 添加/编辑物品的对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemUpsertDialog(
    viewModel: SpaceViewModel,
    folderId: String,
    initialItem: Item?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEdit = initialItem != null

    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var note by remember { mutableStateOf(initialItem?.note ?: "") }
    var expiry by remember { mutableStateOf(SpaceViewModel.formatEpochMsToDate(initialItem?.expiryDateEpochMs)) }
    var currentQty by remember { mutableStateOf((initialItem?.currentQuantity ?: 1).toString()) }
    var minQty by remember { mutableStateOf((initialItem?.minQuantity ?: 0).toString()) }
    var imagePath by remember { mutableStateOf(initialItem?.imagePath) }
    var showDatePicker by remember { mutableStateOf(false) }

    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var ocrBusy by remember { mutableStateOf(false) }
    var fullscreenImagePath by remember { mutableStateOf<String?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok ->
        val uri = pendingCameraUri
        if (ok && uri != null) {
            scope.launch(Dispatchers.IO) {
                val path = viewModel.persistCapturedPhoto(uri)
                launch(Dispatchers.Main) {
                    if (path != null) imagePath = path else Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val pickGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val path = viewModel.persistGalleryUri(uri)
                launch(Dispatchers.Main) {
                    if (path != null) imagePath = path else Toast.makeText(context, "无法读取图片", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEdit) "编辑物品" else "添加物品",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("物品名称") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = expiry,
                    onValueChange = {},  // 禁止手动输入
                    readOnly = true,
                    label = { Text("过期日期") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = "选择日期")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(16.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = currentQty,
                        onValueChange = { currentQty = it },
                        label = { Text("当前数量") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    OutlinedTextField(
                        value = minQty,
                        onValueChange = { minQty = it },
                        label = { Text("最低数量") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                val previewSizePx = with(LocalDensity.current) { 220.dp.roundToPx() }
                val preview = remember(imagePath) {
                    imagePath?.let { loadBitmapFromInternalPath(context, it, previewSizePx) }
                }
                if (preview != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clickable { fullscreenImagePath = imagePath },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Image(
                            bitmap = preview.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = {
                            val uri = viewModel.createTempCameraUri()
                            pendingCameraUri = uri
                            takePictureLauncher.launch(uri)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("拍照")
                    }
                    FilledTonalButton(
                        onClick = { pickGalleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("相册")
                    }
                }

                FilledTonalButton(
                    enabled = !ocrBusy && imagePath != null,
                    onClick = {
                        val p = imagePath ?: return@FilledTonalButton
                        ocrBusy = true
                        scope.launch {
                            val text = com.example.myapplication.ocr.OcrRecognizer.recognizeFromInternalPath(context, p)
                            if (!text.isNullOrBlank()) {
                                if (name.isBlank()) name = text.lines().firstOrNull().orEmpty()
                                if (note.isBlank()) note = text
                            } else {
                                Toast.makeText(context, "未识别到文字", Toast.LENGTH_SHORT).show()
                            }
                            ocrBusy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(if (ocrBusy) "识别中..." else "OCR 识别填充")
                }

                if (isEdit) {
                    OutlinedButton(
                        onClick = {
                            val item = initialItem ?: return@OutlinedButton
                            viewModel.removeItem(item.id)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(100.dp),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("删除物品") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = name.trim()
                    if (finalName.isBlank() || imagePath == null) {
                        Toast.makeText(context, "请填写物品名称并上传图片", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val expiryEpoch = SpaceViewModel.parseDateToEpochMs(expiry)
                    val current = currentQty.toIntOrNull() ?: 0
                    val min = minQty.toIntOrNull() ?: 0

                    if (!isEdit) {
                        viewModel.addItemToFolder(
                            folderId = folderId,
                            itemName = finalName,
                            note = note,
                            imagePath = imagePath,
                            expiryDateEpochMs = expiryEpoch,
                            currentQuantity = current,
                            minQuantity = min,
                            tagIds = emptyList(),
                        )
                    } else {
                        val item = initialItem ?: return@Button
                        viewModel.updateItemFull(
                            itemId = item.id,
                            name = finalName,
                            note = note,
                            expiryDateEpochMs = expiryEpoch,
                            currentQuantity = current,
                            minQuantity = min,
                            imagePath = imagePath,
                            tagIds = emptyList(),
                            folderId = folderId
                        )
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(100.dp)
            ) { Text("保存") }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(100.dp)
            ) { Text("取消") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )

    // 日历选择器对话框
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = SpaceViewModel.parseDateToEpochMs(expiry) ?: System.currentTimeMillis()
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            expiry = SpaceViewModel.formatEpochMsToDate(millis) ?: ""
                        }
                        showDatePicker = false
                    },
                    shape = RoundedCornerShape(100.dp)
                ) { Text("确定") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDatePicker = false },
                    shape = RoundedCornerShape(100.dp)
                ) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    fullscreenImagePath?.let { path ->
        FullScreenImageDialog(imagePath = path, onDismiss = { fullscreenImagePath = null })
    }
}

/**
 * 批量添加物品对话框
 */
@Composable
fun BatchAddDialog(
    viewModel: SpaceViewModel,
    folderId: String,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量添加", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("每行一个物品名称", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    label = { Text("物品列表") },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val names = text.lines().map { it.trim() }.filter { it.isNotBlank() }
                    if (names.isEmpty()) {
                        Toast.makeText(context, "没有可添加的物品", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.addItemsBatch(folderId, names, emptyList())
                    onDismiss()
                },
                shape = RoundedCornerShape(100.dp)
            ) { Text("添加") }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(100.dp)
            ) { Text("取消") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * 全屏预览图片对话框
 */
@Composable
fun FullScreenImageDialog(
    imagePath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(imagePath) { loadBitmapFromInternalPath(context, imagePath, 2000) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                        .clickable(enabled = false) { }
                )
            }
        }
    }
}
