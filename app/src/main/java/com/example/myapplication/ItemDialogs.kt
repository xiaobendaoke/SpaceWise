/**
 * 物品相关弹窗组件。
 *
 * 职责：
 * - 提供添加、编辑、详情展示等物品相关的对话框。
 * - 集成图片选择、日期选择和标签选择逻辑。
 *
 * 上层用途：
 * - 被 `SpaceDetailScreen` 等页面调用，用于处理物品的交互操作。
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.myapplication.ui.theme.TextPrimary
import com.example.myapplication.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemUpsertDialog(
    viewModel: SpaceViewModel,
    spaceId: String,
    spots: List<Spot>,
    allTags: List<Tag>,
    initialSpotId: String?,
    initialItem: Item?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEdit = initialItem != null

    var selectedSpotId by remember { mutableStateOf(initialSpotId ?: spots.firstOrNull()?.id) }
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var note by remember { mutableStateOf(initialItem?.note ?: "") }
    var expiry by remember { mutableStateOf(SpaceViewModel.formatEpochMsToDate(initialItem?.expiryDateEpochMs)) }
    var currentQty by remember { mutableStateOf((initialItem?.currentQuantity ?: 1).toString()) }
    var minQty by remember { mutableStateOf((initialItem?.minQuantity ?: 0).toString()) }
    var imagePath by remember { mutableStateOf(initialItem?.imagePath) }
    var selectedTagIds by remember { mutableStateOf(initialItem?.tags?.map { it.id }?.toSet() ?: emptySet()) }

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
        title = { Text(if (isEdit) "编辑物品" else "添加物品") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isEdit && spots.size > 1) {
                    SimpleSpotPicker(
                        spots = spots,
                        selectedSpotId = selectedSpotId,
                        onSelectSpot = { selectedSpotId = it }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("物品名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = expiry,
                    onValueChange = { expiry = it },
                    label = { Text("过期日期（YYYY-MM-DD）") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = currentQty,
                        onValueChange = { currentQty = it },
                        label = { Text("当前数量") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minQty,
                        onValueChange = { minQty = it },
                        label = { Text("最低数量") },
                        modifier = Modifier.weight(1f)
                    )
                }

                TagPickerSection(
                    allTags = allTags,
                    selectedTagIds = selectedTagIds,
                    onChange = { selectedTagIds = it },
                    onCreateTag = { tagName, parentId -> viewModel.addTag(tagName, parentId) },
                    onDeleteTag = { tagId -> viewModel.deleteTag(tagId) },
                )

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
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF0EFEA)
                    ) {
                        Image(
                            bitmap = preview.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = {
                            val uri = viewModel.createTempCameraUri()
                            pendingCameraUri = uri
                            takePictureLauncher.launch(uri)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("拍照")
                    }
                    FilledTonalButton(
                        onClick = { pickGalleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
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
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (ocrBusy) "识别中..." else "OCR 识别填充")
                }

                if (isEdit) {
                    OutlinedButton(
                        onClick = {
                            val item = initialItem ?: return@OutlinedButton
                            viewModel.removeItem(spaceId, spots.first().id, item.id)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("删除物品") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalSpotId = selectedSpotId
                    val finalName = name.trim()
                    if (finalSpotId == null || finalName.isBlank() || imagePath == null) {
                        Toast.makeText(context, "请填写物品名称并上传图片", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val expiryEpoch = SpaceViewModel.parseDateToEpochMs(expiry)
                    val current = currentQty.toIntOrNull() ?: 0
                    val min = minQty.toIntOrNull() ?: 0

                    if (!isEdit) {
                        viewModel.addItemToSpot(
                            spaceId = spaceId,
                            spotId = finalSpotId,
                            itemName = finalName,
                            note = note,
                            imagePath = imagePath,
                            expiryDateEpochMs = expiryEpoch,
                            currentQuantity = current,
                            minQuantity = min,
                            tagIds = selectedTagIds.toList(),
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
                            tagIds = selectedTagIds.toList(),
                            spotId = finalSpotId
                        )
                    }
                    onDismiss()
                }
            ) { Text("保存") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("取消") } }
    )

    fullscreenImagePath?.let { path ->
        FullScreenImageDialog(imagePath = path, onDismiss = { fullscreenImagePath = null })
    }
}

@Composable
fun BatchAddDialog(
    viewModel: SpaceViewModel,
    spaceId: String,
    spotId: String,
    allTags: List<Tag>,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var selectedTagIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量添加") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("每行一个物品名称", color = TextSecondary, fontSize = 12.sp)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    label = { Text("物品列表") }
                )
                TagPickerSection(
                    allTags = allTags,
                    selectedTagIds = selectedTagIds,
                    onChange = { selectedTagIds = it },
                    onCreateTag = { tagName, parentId -> viewModel.addTag(tagName, parentId) },
                    onDeleteTag = { tagId -> viewModel.deleteTag(tagId) },
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
                    viewModel.addItemsBatch(spaceId, spotId, names, selectedTagIds.toList())
                    onDismiss()
                }
            ) { Text("添加") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun SpotItemsDialog(
    viewModel: SpaceViewModel,
    spaceId: String,
    spot: Spot,
    allTags: List<Tag>,
    highlightItemId: String? = null,
    onDeleteSpot: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var showBatch by remember { mutableStateOf(false) }
    var editingItemId by remember { mutableStateOf<String?>(null) }
    var fullscreenImagePath by remember { mutableStateOf<String?>(null) }
    val editingItem = editingItemId?.let { id -> spot.items.firstOrNull { it.id == id } }
    val context = LocalContext.current
    val thumbSizePx = with(LocalDensity.current) { 48.dp.roundToPx() }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(spot.id, highlightItemId) {
        val targetId = highlightItemId ?: return@LaunchedEffect
        val idx = spot.items.indexOfFirst { it.id == targetId }
        if (idx >= 0) listState.scrollToItem(idx)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${spot.name} 的物品") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (spot.items.isEmpty()) {
                    Text("暂无物品", color = TextSecondary)
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 400.dp)
                    ) {
                        itemsIndexed(spot.items, key = { _, item -> item.id }) { _, item ->
                            val thumb = remember(item.imagePath) {
                                item.imagePath?.let { loadThumbnailFromInternalPath(context, it, thumbSizePx) }
                            }
                            val highlighted = item.id == highlightItemId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (highlighted) androidx.compose.ui.graphics.Color(0xFFEDE6DB) else androidx.compose.ui.graphics.Color.Transparent)
                                    .border(
                                        width = if (highlighted) 1.dp else 0.dp,
                                        color = if (highlighted) androidx.compose.ui.graphics.Color(0xFFB59F88) else androidx.compose.ui.graphics.Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .padding(8.dp)
                                    .combinedClickable(
                                        onClick = { editingItemId = item.id },
                                        onLongClick = { editingItemId = item.id }
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clickable { fullscreenImagePath = item.imagePath },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF0EFEA)
                                    ) {
                                        if (thumb != null) {
                                            Image(
                                                bitmap = thumb.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxWidth(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.PhotoLibrary,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .fillMaxWidth()
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.size(10.dp))
                                    Column {
                                        Text(item.name, color = TextPrimary, fontWeight = FontWeight.Medium)
                                        val subtitle = buildString {
                                            if (!item.note.isNullOrBlank()) append(item.note)
                                            if (item.expiryDateEpochMs != null) {
                                                if (isNotEmpty()) append(" · ")
                                                append("到期 ")
                                                append(SpaceViewModel.formatEpochMsToDate(item.expiryDateEpochMs))
                                            }
                                            if (item.minQuantity > 0) {
                                                if (isNotEmpty()) append(" · ")
                                                append("库存 ${item.currentQuantity}/${item.minQuantity}")
                                            }
                                        }
                                        if (subtitle.isNotBlank()) {
                                            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
                                        }
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = { showAdd = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("添加")
                    }
                    FilledTonalButton(
                        onClick = { showBatch = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("批量") }
                }

                OutlinedButton(onClick = onDeleteSpot, modifier = Modifier.fillMaxWidth()) {
                    Text("删除空间")
                }
            }
        },
        confirmButton = { OutlinedButton(onClick = onDismiss) { Text("关闭") } }
    )

    fullscreenImagePath?.let { path ->
        FullScreenImageDialog(imagePath = path, onDismiss = { fullscreenImagePath = null })
    }

    if (showAdd) {
        ItemUpsertDialog(
            viewModel = viewModel,
            spaceId = spaceId,
            spots = listOf(spot),
            allTags = allTags,
            initialSpotId = spot.id,
            initialItem = null,
            onDismiss = { showAdd = false }
        )
    }
    if (showBatch) {
        BatchAddDialog(
            viewModel = viewModel,
            spaceId = spaceId,
            spotId = spot.id,
            allTags = allTags,
            onDismiss = { showBatch = false }
        )
    }
    if (editingItem != null) {
        ItemUpsertDialog(
            viewModel = viewModel,
            spaceId = spaceId,
            spots = listOf(spot),
            allTags = allTags,
            initialSpotId = spot.id,
            initialItem = editingItem,
            onDismiss = { editingItemId = null }
        )
    }
}

@Composable
fun SimpleSpotPicker(
    spots: List<Spot>,
    selectedSpotId: String?,
    onSelectSpot: (String) -> Unit,
) {
    // Minimal picker (inline list). The full exposed dropdown is already in MainActivity for spaces.
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("所在地点", color = TextSecondary, fontSize = 12.sp)
        spots.forEach { spot ->
            val selected = spot.id == selectedSpotId
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectSpot(spot.id) },
                shape = RoundedCornerShape(10.dp),
                color = if (selected) androidx.compose.ui.graphics.Color(0xFFEAE1D6) else androidx.compose.ui.graphics.Color.White
            ) {
                Text(
                    text = spot.name,
                    modifier = Modifier.padding(10.dp),
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun FullScreenImageDialog(
    imagePath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // Load with a large enough max dimension for full screen clarity
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
                        .clickable(enabled = false) { } // Prevent click-through closing
                )
            }
        }
    }
}
