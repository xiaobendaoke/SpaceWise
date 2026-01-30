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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var pendingLongPressFolderId by remember { mutableStateOf<String?>(null) }  // 区域长按菜单
    var pendingLongPressItemId by remember { mutableStateOf<String?>(null) }  // 物品长按菜单
    var editingFolder by remember { mutableStateOf<Folder?>(null) }  // 编辑中的区域
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var pendingMoveItem by remember { mutableStateOf<Item?>(null) }  // 待移动的物品
    
    val sheetState = rememberModalBottomSheetState()
    
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
                        textDecoration = if (index != breadcrumbs.lastIndex) {
                            androidx.compose.ui.text.style.TextDecoration.Underline
                        } else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(
                            enabled = index != breadcrumbs.lastIndex
                        ) {
                            when {
                                crumb.isLocation -> onBack()
                                else -> onNavigateToFolder(crumb.id)
                            }
                        }
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
                    onLongClick = { pendingLongPressFolderId = folder.id }
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
                            onLongClick = { pendingLongPressItemId = item.id }
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
                Icon(Icons.Filled.CreateNewFolder, contentDescription = "新建区域")
            }
            
            // 新建物品（仅在区域内显示）
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

    // 新建区域底部弹窗
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
                    text = "新建区域",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // 区域名称输入框（移到顶部）
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("区域名称") },
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
                
                // 已选择自定义图片提示
                if (newFolderCoverPath != null) {
                    Text(
                        text = "✓ 已选择自定义图片",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                
                // 可折叠的预设图标选择区域
                var iconSectionExpanded by remember { mutableStateOf(false) }
                
                // 折叠标题行
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { iconSectionExpanded = !iconSectionExpanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择预设图标",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (iconSectionExpanded) 
                            Icons.Filled.KeyboardArrowDown 
                        else 
                            Icons.Filled.KeyboardArrowRight,
                        contentDescription = if (iconSectionExpanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 展开时显示所有图标分类
                if (iconSectionExpanded) {
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
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                androidx.compose.material3.Button(
                    onClick = {
                        val name = newFolderName.trim()
                        if (name.isBlank()) {
                            Toast.makeText(context, "请输入区域名称", Toast.LENGTH_SHORT).show()
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

    // 区域长按菜单
    pendingLongPressFolderId?.let { folderId ->
        val folder = folders.firstOrNull { it.id == folderId }
        AlertDialog(
            onDismissRequest = { pendingLongPressFolderId = null },
            title = { Text(folder?.name ?: "操作") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            editingFolder = folder
                            pendingLongPressFolderId = null
                            showNewFolderSheet = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("编辑区域")
                    }
                    OutlinedButton(
                        onClick = {
                            pendingDeleteFolderId = folderId
                            pendingLongPressFolderId = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("删除区域")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingLongPressFolderId = null },
                    shape = RoundedCornerShape(100.dp)
                ) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 物品长按菜单
    pendingLongPressItemId?.let { itemId ->
        val item = items.firstOrNull { it.id == itemId }
        AlertDialog(
            onDismissRequest = { pendingLongPressItemId = null },
            title = { Text(item?.name ?: "操作") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                           selectedItem = item
                            pendingLongPressItemId = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("编辑物品")
                    }
                    OutlinedButton(
                        onClick = {
                            pendingMoveItem = item
                            pendingLongPressItemId = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.DriveFileMove, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("移动物品")
                    }
                    OutlinedButton(
                        onClick = {
                            pendingDeleteItemId = itemId
                            pendingLongPressItemId = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("删除物品")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingLongPressItemId = null },
                    shape = RoundedCornerShape(100.dp)
                ) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 删除区域确认对话框
    pendingDeleteFolderId?.let { deleteFolderId ->
        val folderName = folders.firstOrNull { it.id == deleteFolderId }?.name ?: "该区域"
        AlertDialog(
            onDismissRequest = { pendingDeleteFolderId = null },
            title = { Text("删除区域") },
            text = { Text("确定删除 \"$folderName\" 吗？区域内的所有子区域和物品都将被删除。") },
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

    // 移动物品对话框
    pendingMoveItem?.let { item ->
        if (folderId != null) {
            ItemMoveDialog(
                viewModel = viewModel,
                currentLocationId = locationId,
                currentFolderId = folderId,
                item = item,
                onDismiss = { pendingMoveItem = null }
            )
        }
    }
}

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    text = "${folder.subFolderCount} 个区域 · ${folder.itemCount} 个物品",
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

/**
 * 移动物品对话框
 * 支持三步导航：选择场所 → 选择区域（可层层进入） → 确认移动
 */
@Composable
fun ItemMoveDialog(
    viewModel: SpaceViewModel,
    currentLocationId: String,
    currentFolderId: String,
    item: Item,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // 当前导航状态
    var selectedLocationId by remember { mutableStateOf<String?>(null) }
    var currentParentId by remember { mutableStateOf<String?>(null) }
    var navigationStack by remember { mutableStateOf(listOf<Pair<String?, String>>()) } // (parentId, name)
    
    // 获取所有场所
    val locations by viewModel.locations.collectAsState()
    
    // 获取当前场所下的文件夹
    val folders by if (selectedLocationId != null) {
        viewModel.observeFolders(selectedLocationId!!, currentParentId).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 返回按钮
                if (selectedLocationId != null) {
                    IconButton(
                        onClick = {
                            if (navigationStack.isNotEmpty()) {
                                // 返回上一级区域
                                val newStack = navigationStack.dropLast(1)
                                navigationStack = newStack
                                currentParentId = newStack.lastOrNull()?.first
                            } else {
                                // 返回场所选择
                                selectedLocationId = null
                                currentParentId = null
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
                
                Text(
                    text = when {
                        selectedLocationId == null -> "选择目标场所"
                        navigationStack.isEmpty() -> "选择目标区域"
                        else -> navigationStack.last().second
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedLocationId == null) {
                    // 显示场所列表
                    Text(
                        text = "移动 \"${item.name}\" 到：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(locations, key = { it.id }) { location ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedLocationId = location.id
                                        currentParentId = null
                                        navigationStack = emptyList()
                                    },
                                color = if (location.id == currentLocationId) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = location.icon ?: "📍",
                                        fontSize = 24.sp
                                    )
                                    Text(
                                        text = location.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (location.id == currentLocationId) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "当前",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // 显示区域列表
                    if (folders.isEmpty()) {
                        Text(
                            text = if (navigationStack.isEmpty()) "该场所下没有区域" else "该区域下没有子区域",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(folders, key = { it.id }) { folder ->
                                val isCurrentFolder = folder.id == currentFolderId
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !isCurrentFolder) {
                                            // 进入该区域
                                            navigationStack = navigationStack + (currentParentId to folder.name)
                                            currentParentId = folder.id
                                        },
                                    color = if (isCurrentFolder) {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = folder.icon ?: "📁",
                                            fontSize = 20.sp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = folder.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (isCurrentFolder) {
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            if (folder.subFolderCount > 0) {
                                                Text(
                                                    text = "${folder.subFolderCount} 个子区域",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        if (isCurrentFolder) {
                                            Text(
                                                text = "当前位置",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            // 只有选择了区域后才显示确认按钮
            if (selectedLocationId != null && currentParentId != null && currentParentId != currentFolderId) {
                androidx.compose.material3.Button(
                    onClick = {
                        // 执行移动
                        viewModel.updateItemFull(
                            itemId = item.id,
                            name = item.name,
                            note = item.note,
                            expiryDateEpochMs = item.expiryDateEpochMs,
                            currentQuantity = item.currentQuantity,
                            minQuantity = item.minQuantity,
                            imagePath = item.imagePath,
                            tagIds = emptyList(),
                            folderId = currentParentId!!
                        )
                        Toast.makeText(context, "已移动到 ${navigationStack.lastOrNull()?.second ?: "目标区域"}", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("移动到此")
                }
            }
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
