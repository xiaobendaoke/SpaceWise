/**
 * 文件夹浏览器页面。
 *
 * 职责：
 * - 展示指定文件夹内的子文件夹和物品。
 * - 提供面包屑导航。
 * - 处理文件夹和物品的创建、删除逻辑。
 *
 * 上层用途：
 * - 用户点击场所后进入的核心浏览页面，支持无限层级嵌套。
 */
package com.example.myapplication

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 常用图标分类
 */
object FolderIcons {
    val livingRoom = listOf("🛋️", "📺", "🖼️", "💡", "🪴", "🏮", "🕰️", "🎮")
    val bedroom = listOf("🛏️", "🪟", "👗", "👔", "👕", "👖", "🧥", "🧣")
    val kitchen = listOf("🍳", "🥄", "🍶", "🫖", "🍽️", "🧊", "🥡", "🧂")
    val bathroom = listOf("🚿", "🛁", "🧴", "🪥", "🧻", "🧼", "🪒", "🪞")
    val study = listOf("📁", "📚", "📖", "💻", "🖨️", "✂️", "📐", "📝")
    val storage = listOf("🧰", "📦", "🗄️", "🗑️", "🎒", "👜", "🔧", "🔨")
    val kids = listOf("🧸", "🎨", "🎭", "🎪", "🧩", "🪁", "🎈", "🎁")
    val misc = listOf("🚗", "🚲", "⚽", "🎸", "💊", "🔑", "📱", "⌚")
    
    val all = livingRoom + bedroom + kitchen + bathroom + study + storage + kids + misc
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun FolderBrowserScreen(
    viewModel: SpaceViewModel,
    locationId: String,
    folderId: String?,          // null 表示场所根目录
    onBack: () -> Unit,
    onNavigateToFolder: (String) -> Unit,
    onOpenItem: (Item) -> Unit
) {
    val folders by viewModel.observeFolders(locationId, folderId).collectAsState(initial = emptyList())
    val items by if (folderId != null) {
        viewModel.observeItemsInFolder(folderId).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<Item>()) }
    }
    val breadcrumbs by viewModel.getBreadcrumbs(locationId, folderId).collectAsState(initial = emptyList())
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showNewFolderSheet by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var newFolderIcon by remember { mutableStateOf("📁") }
    var newFolderCoverPath by remember { mutableStateOf<String?>(null) }
    var pendingDeleteFolderId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteItemId by remember { mutableStateOf<String?>(null) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var showIconPicker by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // 拍照和相册选择器
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok ->
        val uri = pendingCameraUri
        if (ok && uri != null) {
            scope.launch(Dispatchers.IO) {
                val path = viewModel.persistCapturedPhoto(uri)
                launch(Dispatchers.Main) {
                    if (path != null) {
                        newFolderCoverPath = path
                        newFolderIcon = "" // 清空 emoji，使用图片
                    } else {
                        Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                    }
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
                    if (path != null) {
                        newFolderCoverPath = path
                        newFolderIcon = "" // 清空 emoji，使用图片
                    } else {
                        Toast.makeText(context, "无法读取图片", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            
            // 面包屑导航
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                breadcrumbs.forEachIndexed { index, crumb ->
                    if (index > 0) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = crumb.name,
                        style = if (index == breadcrumbs.lastIndex) {
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = if (index == breadcrumbs.lastIndex) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 内容区
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 文件夹标题
            if (folders.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "文件夹",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${folders.size} 个",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 文件夹列表
            items(folders, key = { "folder_${it.id}" }) { folder ->
                FolderCard(
                    folder = folder,
                    onClick = { onNavigateToFolder(folder.id) },
                    onLongClick = { pendingDeleteFolderId = folder.id }
                )
            }
            
            // 物品标题
            if (folderId != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "物品",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${items.size} 个",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // 物品列表
                if (items.isEmpty()) {
                    item {
                        Text(
                            text = "暂无物品，点击右下角按钮添加",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(items, key = { "item_${it.id}" }) { item ->
                        ItemCard(
                            item = item,
                            onClick = { selectedItem = item },
                            onLongClick = { pendingDeleteItemId = item.id }
                        )
                    }
                }
            } else {
                // 在根目录提示用户进入文件夹添加物品
                if (folders.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📂",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "还没有文件夹",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "点击右下角按钮创建文件夹",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // 底部留白
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // 悬浮按钮
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // 新建文件夹
            FloatingActionButton(
                onClick = { 
                    newFolderName = ""
                    newFolderIcon = "📁"
                    newFolderCoverPath = null
                    showNewFolderSheet = true 
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Filled.CreateNewFolder, contentDescription = "新建文件夹")
            }
            
            // 新建物品（仅在文件夹内显示）
            if (folderId != null) {
                FloatingActionButton(
                    onClick = { showAddItemDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "添加物品")
                }
            }
        }
    }

    // 新建文件夹底部弹窗
    if (showNewFolderSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNewFolderSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "新建文件夹",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // 文件夹名称输入框（移到顶部）
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("文件夹名称") },
                    placeholder = { Text("例如：客厅、书架、抽屉") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                
                // 图标选择区域
                Text(
                    text = "选择图标",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 自定义图片预览 + 拍照/相册按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 当前选中的图片预览
                    val thumbSizePx = with(LocalDensity.current) { 56.dp.roundToPx() }
                    val coverBitmap = remember(newFolderCoverPath) {
                        newFolderCoverPath?.let { loadBitmapFromInternalPath(context, it, thumbSizePx) }
                    }
                    if (coverBitmap != null) {
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { newFolderCoverPath = null },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Image(
                                bitmap = coverBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    
                    FilledTonalButton(
                        onClick = {
                            val uri = viewModel.createTempCameraUri()
                            pendingCameraUri = uri
                            takePictureLauncher.launch(uri)
                        },
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("拍照")
                    }
                    FilledTonalButton(
                        onClick = { pickGalleryLauncher.launch("image/*") },
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("相册")
                    }
                }
                
                // 分类图标选择
                IconCategoryInline("常用", listOf("📁", "🛋️", "🛏️", "🍳", "🚿", "📚", "📦", "🧰"), newFolderIcon, newFolderCoverPath) { 
                    newFolderIcon = it
                    newFolderCoverPath = null
                }
                IconCategoryInline("客厅", FolderIcons.livingRoom, newFolderIcon, newFolderCoverPath) { 
                    newFolderIcon = it
                    newFolderCoverPath = null
                }
                IconCategoryInline("卧室", FolderIcons.bedroom, newFolderIcon, newFolderCoverPath) {
                    newFolderIcon = it
                    newFolderCoverPath = null
                }
                IconCategoryInline("厨房", FolderIcons.kitchen, newFolderIcon, newFolderCoverPath) {
                    newFolderIcon = it
                    newFolderCoverPath = null
                }
                IconCategoryInline("浴室", FolderIcons.bathroom, newFolderIcon, newFolderCoverPath) {
                    newFolderIcon = it
                    newFolderCoverPath = null
                }
                IconCategoryInline("书房", FolderIcons.study, newFolderIcon, newFolderCoverPath) {
                    newFolderIcon = it
                    newFolderCoverPath = null
                }
                IconCategoryInline("收纳", FolderIcons.storage, newFolderIcon, newFolderCoverPath) {
                    newFolderIcon = it
                    newFolderCoverPath = null
                }
                IconCategoryInline("儿童", FolderIcons.kids, newFolderIcon, newFolderCoverPath) {
                    newFolderIcon = it
                    newFolderCoverPath = null
                }
                IconCategoryInline("其他", FolderIcons.misc, newFolderIcon, newFolderCoverPath) {
                    newFolderIcon = it
                    newFolderCoverPath = null
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                androidx.compose.material3.Button(
                    onClick = {
                        val name = newFolderName.trim()
                        if (name.isBlank()) {
                            Toast.makeText(context, "请输入文件夹名称", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addFolder(
                                locationId = locationId, 
                                parentId = folderId, 
                                name = name, 
                                icon = if (newFolderCoverPath != null) null else newFolderIcon,
                                coverImagePath = newFolderCoverPath
                            )
                            newFolderName = ""
                            showNewFolderSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("创建")
                }
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }


    // 添加物品对话框（完整版）
    if (showAddItemDialog && folderId != null) {
        ItemUpsertDialog(
            viewModel = viewModel,
            folderId = folderId,
            initialItem = null,
            onDismiss = { showAddItemDialog = false }
        )
    }

    // 删除文件夹确认对话框
    pendingDeleteFolderId?.let { deleteFolderId ->
        val folderName = folders.firstOrNull { it.id == deleteFolderId }?.name ?: "该文件夹"
        AlertDialog(
            onDismissRequest = { pendingDeleteFolderId = null },
            title = { Text("删除文件夹") },
            text = { Text("确定删除 \"$folderName\" 吗？文件夹内的所有子文件夹和物品都将被删除。") },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.removeFolder(deleteFolderId)
                        pendingDeleteFolderId = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(100.dp)
                ) { Text("删除") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingDeleteFolderId = null },
                    shape = RoundedCornerShape(100.dp)
                ) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 删除物品确认对话框
    pendingDeleteItemId?.let { deleteItemId ->
        val itemName = items.firstOrNull { it.id == deleteItemId }?.name ?: "该物品"
        AlertDialog(
            onDismissRequest = { pendingDeleteItemId = null },
            title = { Text("删除物品") },
            text = { Text("确定删除 \"$itemName\" 吗？") },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.removeItem(deleteItemId)
                        pendingDeleteItemId = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(100.dp)
                ) { Text("删除") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingDeleteItemId = null },
                    shape = RoundedCornerShape(100.dp)
                ) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 物品详情/编辑对话框
    selectedItem?.let { item ->
        if (folderId != null) {
            ItemUpsertDialog(
                viewModel = viewModel,
                folderId = folderId,
                initialItem = item,
                onDismiss = { selectedItem = null }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconCategoryInline(
    title: String,
    icons: List<String>,
    selectedIcon: String,
    selectedCoverPath: String?,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icons.forEach { icon ->
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onSelect(icon) },
                    color = if (icon == selectedIcon && selectedCoverPath == null)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(text = icon, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(
    folder: Folder,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false,
                ambientColor = Color(0x208D7B68),
                spotColor = Color(0x208D7B68)
            )
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 图标
            Surface(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    val thumbSizePx = with(LocalDensity.current) { 48.dp.roundToPx() }
                    val coverBitmap = remember(folder.coverImagePath) {
                        folder.coverImagePath?.let { loadBitmapFromInternalPath(context, it, thumbSizePx) }
                    }
                    if (coverBitmap != null) {
                        Image(
                            bitmap = coverBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = folder.icon ?: "📁",
                            fontSize = 24.sp
                        )
                    }
                }
            }
            
            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${folder.subFolderCount} 个文件夹 · ${folder.itemCount} 个物品",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemCard(
    item: Item,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false,
                ambientColor = Color(0x208D7B68),
                spotColor = Color(0x208D7B68)
            )
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 图标/缩略图
            Surface(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    val thumbSizePx = with(LocalDensity.current) { 48.dp.roundToPx() }
                    val thumbBitmap = remember(item.imagePath) {
                        item.imagePath?.let { loadThumbnailFromInternalPath(context, it, thumbSizePx) }
                    }
                    if (thumbBitmap != null) {
                        Image(
                            bitmap = thumbBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val subtitle = buildString {
                    if (!item.note.isNullOrBlank()) append(item.note)
                    if (item.expiryDateEpochMs != null) {
                        if (isNotEmpty()) append(" · ")
                        append("到期 ")
                        append(SpaceViewModel.formatEpochMsToDate(item.expiryDateEpochMs))
                    }
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // 数量
            if (item.currentQuantity > 1) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "×${item.currentQuantity}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
