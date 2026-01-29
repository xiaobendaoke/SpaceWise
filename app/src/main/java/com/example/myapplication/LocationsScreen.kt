/**
 * 场所列表页面（首页）。
 *
 * 职责：
 * - 展示所有已创建的场所卡片。
 * - 处理场所的创建和删除逻辑。
 *
 * 上层用途：
 * - 应用启动后的默认展示页面（首页）。
 */
package com.example.myapplication

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(
    viewModel: SpaceViewModel,
    onLocationClick: (String) -> Unit,
    onVaultClick: () -> Unit = {}
) {
    val locations by viewModel.locations.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    var pendingDeleteLocationId by remember { mutableStateOf<String?>(null) }
    var pendingLongPressLocationId by remember { mutableStateOf<String?>(null) }  // 长按菜单
    var editingLocation by remember { mutableStateOf<Location?>(null) }  // 编辑中的场所
    var newLocationName by remember { mutableStateOf("") }
    var newLocationIcon by remember { mutableStateOf("🏠") }
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok ->
        val name = newLocationName.trim()
        val uri = pendingCameraUri
        if (ok && uri != null && name.isNotBlank()) {
            scope.launch(Dispatchers.IO) {
                val coverPath = viewModel.persistCapturedPhoto(uri)
                launch(Dispatchers.Main) {
                    viewModel.addLocation(name, newLocationIcon, coverPath)
                    newLocationName = ""
                    newLocationIcon = "🏠"
                    showSheet = false
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val name = newLocationName.trim()
        if (uri != null && name.isNotBlank()) {
            scope.launch(Dispatchers.IO) {
                val coverPath = viewModel.persistGalleryUri(uri)
                launch(Dispatchers.Main) {
                    if (coverPath != null) {
                        viewModel.addLocation(name, newLocationIcon, coverPath)
                        newLocationName = ""
                        newLocationIcon = "🏠"
                        showSheet = false
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
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "我的场所",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 10.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${locations.size} 个场所",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 保险箱入口
                item(key = "vault") {
                    VaultEntryCard(onClick = onVaultClick)
                }
                
                items(locations, key = { it.id }) { location ->
                    LocationCard(
                        location = location,
                        onClick = { onLocationClick(location.id) },
                        onLongClick = { pendingLongPressLocationId = location.id }
                    )
                }
            }
            
            // 右下角新建场所按钮
            androidx.compose.material3.FloatingActionButton(
                onClick = {
                    editingLocation = null // Clear editing state
                    newLocationName = ""
                    newLocationIcon = "🏠"
                    showSheet = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加场所")
            }
        }
    }

    // 长按操作菜单
    pendingLongPressLocationId?.let { locationId ->
        val location = locations.firstOrNull { it.id == locationId }
        AlertDialog(
            onDismissRequest = { pendingLongPressLocationId = null },
            title = { Text(location?.name ?: "操作") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            editingLocation = location
                            pendingLongPressLocationId = null
                            showSheet = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("编辑场所")
                    }
                    OutlinedButton(
                        onClick = {
                            pendingDeleteLocationId = locationId
                            pendingLongPressLocationId = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("删除场所")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingLongPressLocationId = null },
                    shape = RoundedCornerShape(100.dp)
                ) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 删除确认对话框
    pendingDeleteLocationId?.let { locationId ->
        val locationName = locations.firstOrNull { it.id == locationId }?.name ?: "该场所"
        AlertDialog(
            onDismissRequest = { pendingDeleteLocationId = null },
            title = { Text("删除场所") },
            text = { Text("确定删除 \"$locationName\" 吗？此操作无法撤销，场所内的所有区域和物品都将被删除。") },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.removeLocation(locationId)
                        pendingDeleteLocationId = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(100.dp)
                ) { Text("删除") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingDeleteLocationId = null },
                    shape = RoundedCornerShape(100.dp)
                ) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 新建/编辑场所底部弹窗
    if (showSheet) {
        // 初始化编辑状态
        LaunchedEffect(editingLocation) {
            val location = editingLocation
            if (location != null) {
                newLocationName = location.name
                newLocationIcon = location.icon ?: "🏠"
            }
        }
        
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = if (editingLocation != null) "编辑场所" else "新建场所",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // 图标选择
                Text(
                    text = "选择图标",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val icons = listOf("🏠", "🏢", "🏪", "🏥", "🏫", "🏭", "🏡", "🏘️")
                    icons.forEach { icon ->
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { newLocationIcon = icon },
                            color = if (newLocationIcon == icon) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(text = icon, fontSize = 24.sp)
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = newLocationName,
                    onValueChange = { newLocationName = it },
                    label = { Text("场所名称") },
                    placeholder = { Text("例如：我的家、办公室") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Text(
                    text = "选择封面照片（可选）",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = {
                            val name = newLocationName.trim()
                            if (name.isBlank()) {
                                Toast.makeText(context, "请先输入场所名称", Toast.LENGTH_SHORT).show()
                            } else {
                                val uri = viewModel.createTempCameraUri()
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("拍照")
                    }
                    FilledTonalButton(
                        onClick = {
                            val name = newLocationName.trim()
                            if (name.isBlank()) {
                                Toast.makeText(context, "请先输入场所名称", Toast.LENGTH_SHORT).show()
                            } else {
                                galleryLauncher.launch("image/*")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("相册")
                    }
                }
                androidx.compose.material3.Button(
                    onClick = {
                        val name = newLocationName.trim()
                        if (name.isBlank()) {
                            Toast.makeText(context, "请先输入场所名称", Toast.LENGTH_SHORT).show()
                        } else {
                            val location = editingLocation
                            if (location != null) {
                                // 编辑模式
                                viewModel.updateLocation(
                                    locationId = location.id,
                                    name = name,
                                    icon = newLocationIcon
                                )
                            } else {
                                // 新建模式
                                viewModel.addLocation(name, newLocationIcon, null)
                            }
                            newLocationName = ""
                            newLocationIcon = "🏠"
                            editingLocation = null
                            showSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("直接创建")
                }
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(56.dp)
                .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun LocationCard(
    location: Location,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val coverMaxPx = with(LocalDensity.current) { 900.dp.roundToPx() }
    var coverBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(location.coverImagePath) {
        coverBitmap = kotlinx.coroutines.withContext(Dispatchers.IO) {
            location.coverImagePath?.let { loadBitmapFromInternalPath(context, it, coverMaxPx) }
        }
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color(0x408D7B68),
                spotColor = Color(0x408D7B68)
            )
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = coverBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 显示图标
                    Text(
                        text = location.icon ?: "🏠",
                        fontSize = 48.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (location.icon != null) {
                        Text(text = location.icon, fontSize = 18.sp)
                    }
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                Text(
                    text = "${location.folderCount} 个区域 · ${location.itemCount} 个物品",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 保险箱入口卡片
 */
@Composable
fun VaultEntryCard(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color(0x40685B68),
                spotColor = Color(0x40685B68)
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔐",
                    fontSize = 48.sp,
                    textAlign = TextAlign.Center
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "保险箱",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "安全存储重要物品",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}
