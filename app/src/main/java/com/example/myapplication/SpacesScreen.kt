/**
 * 空间列表页面。
 *
 * 职责：
 * - 展示所有已创建的空间卡片。
 * - 处理空间的创建和删除逻辑。
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.LightBackground
import com.example.myapplication.ui.theme.TextPrimary
import com.example.myapplication.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpacesScreen(
    viewModel: SpaceViewModel,
    onSpaceClick: (String) -> Unit
) {
    val spaces by viewModel.spaces.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    var pendingDeleteSpaceId by remember { mutableStateOf<String?>(null) }
    var newSpaceName by remember { mutableStateOf("") }
    var selectedTemplateId by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok ->
        val name = newSpaceName.trim()
        val uri = pendingCameraUri
        if (ok && uri != null && name.isNotBlank()) {
            scope.launch(Dispatchers.IO) {
                val coverPath = viewModel.persistCapturedPhoto(uri)
                launch(Dispatchers.Main) {
                    viewModel.addSpace(name, coverPath, selectedTemplateId)
                    newSpaceName = ""
                    showSheet = false
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val name = newSpaceName.trim()
        if (uri != null && name.isNotBlank()) {
            scope.launch(Dispatchers.IO) {
                val coverPath = viewModel.persistGalleryUri(uri)
                launch(Dispatchers.Main) {
                    if (coverPath != null) {
                        viewModel.addSpace(name, coverPath, selectedTemplateId)
                        newSpaceName = ""
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
            .background(LightBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "我的空间",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${spaces.size} 个空间",
                    color = TextSecondary,
                    fontSize = 15.sp
                )
            }
            ActionButton(
                icon = Icons.Filled.Add,
                label = "添加空间",
                onClick = {
                    selectedTemplateId = null
                    newSpaceName = ""
                    showSheet = true
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(spaces, key = { it.id }) { space ->
                ModernSpaceCard(
                    space = space,
                    onClick = { onSpaceClick(space.id) },
                    onLongClick = { pendingDeleteSpaceId = space.id }
                )
            }
        }
    }

    pendingDeleteSpaceId?.let { spaceId ->
        val spaceName = spaces.firstOrNull { it.id == spaceId }?.name ?: "该空间"
        AlertDialog(
            onDismissRequest = { pendingDeleteSpaceId = null },
            title = { Text("删除空间") },
            text = { Text("确定删除 \"$spaceName\" 吗？此操作无法撤销。") },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.removeSpace(spaceId)
                        pendingDeleteSpaceId = null
                    }
                ) { Text("删除") }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingDeleteSpaceId = null }) { Text("取消") }
            }
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "新建空间",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = TextPrimary
                )
                OutlinedTextField(
                    value = newSpaceName,
                    onValueChange = { newSpaceName = it },
                    label = { Text("空间名称") },
                    modifier = Modifier.fillMaxWidth()
                )

                TemplatePicker(
                    selectedTemplateId = selectedTemplateId,
                    onSelect = { selectedTemplateId = it }
                )

                Text(
                    text = "选择封面照片",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = {
                            val name = newSpaceName.trim()
                            if (name.isBlank()) {
                                Toast.makeText(context, "请先输入空间名称", Toast.LENGTH_SHORT).show()
                            } else {
                                val uri = viewModel.createTempCameraUri()
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("拍照")
                    }
                    FilledTonalButton(
                        onClick = {
                            val name = newSpaceName.trim()
                            if (name.isBlank()) {
                                Toast.makeText(context, "请先输入空间名称", Toast.LENGTH_SHORT).show()
                            } else {
                                galleryLauncher.launch("image/*")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("相册")
                    }
                }
                OutlinedButton(
                    onClick = {
                        val name = newSpaceName.trim()
                        if (name.isBlank()) {
                            Toast.makeText(context, "请先输入空间名称", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addSpace(name, null, selectedTemplateId)
                            newSpaceName = ""
                            showSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
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
                .size(58.dp)
                .shadow(elevation = 10.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            color = Color.White
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = TextPrimary
                )
            }
        }
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 13.sp
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ModernSpaceCard(
    space: SpaceCard,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val coverMaxPx = with(LocalDensity.current) { 900.dp.roundToPx() }
    val coverBitmap = remember(space.coverImagePath) {
        space.coverImagePath?.let { loadBitmapFromInternalPath(context, it, coverMaxPx) }
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFEAE7E2)),
                contentAlignment = Alignment.Center
            ) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Text(
                text = space.name,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimary
            )
            Text(
                text = "${space.itemCount} 个物品",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}
