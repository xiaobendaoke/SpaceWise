/**
 * 空间详情/平面图查看页面。
 *
 * 职责：
 * - 展示空间的平面视图分布。
 * - 支持点位的交互（查看、移动、编辑）。
 * - 展示点位内的物品清单。
 *
 * 上层用途：
 * - 用户从空间列表进入后的核心交互场景。
 */
package com.example.myapplication

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun SpaceDetailScreen(
    viewModel: SpaceViewModel,
    spaceId: String,
    initialSpotId: String? = null,
    highlightItemId: String? = null,
    onBack: () -> Unit
) {
    val space by viewModel.observeSpace(spaceId).collectAsState(initial = null)
    val tags by viewModel.tags.collectAsState()
    val resolvedSpace = space ?: return

    var showAddSpot by remember { mutableStateOf(false) }
    var newSpotName by remember { mutableStateOf("") }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }
    var activeSpotId by remember { mutableStateOf<String?>(null) }
    var currentSpotPosition by remember { mutableStateOf(Offset.Zero) }
    var isDraggingSpot by remember { mutableStateOf(false) }
    var draggingSpotId by remember { mutableStateOf<String?>(null) }
    var lastDragEndTime by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current

    LaunchedEffect(initialSpotId, resolvedSpace.id) {
        if (!initialSpotId.isNullOrBlank()) {
            activeSpotId = initialSpotId
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column {
                    Text(
                        text = resolvedSpace.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${resolvedSpace.spots.size} 个位置",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    newSpotName = ""
                    showAddSpot = true
                },
                shape = RoundedCornerShape(100.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("添加位置")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp) // Slightly taller map
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(24.dp),
                    clip = false,
                    ambientColor = Color(0x308D7B68),
                    spotColor = Color(0x308D7B68)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface) // Paper-like background
                .onSizeChanged { mapSize = it }
        ) {
            val coverMaxPx = with(LocalDensity.current) { 1200.dp.roundToPx() }
            // 异步加载封面图片
            var coverBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
            LaunchedEffect(resolvedSpace.coverImagePath) {
                coverBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    resolvedSpace.coverImagePath?.let { loadBitmapFromInternalPath(context, it, coverMaxPx) }
                }
            }
            
            // Map Background Image
            val bitmap = coverBitmap
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.9f // Slightly faded for "paper map" feel
                )
            } else {
                 Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                 ) {
                     Text("暂无平面图", color = MaterialTheme.colorScheme.onSurfaceVariant)
                 }
            }

            val boxWidthPx = constraints.maxWidth.toFloat()
            val boxHeightPx = constraints.maxHeight.toFloat()
            val markerSize = with(LocalDensity.current) { 56.dp.toPx() } // Larger touch target
            val labelOffset = with(LocalDensity.current) { 60.dp.toPx() }

            resolvedSpace.spots.forEach { spot ->
                key(spot.id) {
                    val latestPosition by rememberUpdatedState(spot.position)
                    val draggingThis = isDraggingSpot && draggingSpotId == spot.id
                    
                    // "Sticky Note" Marker
                    Box(
                        modifier = Modifier
                            .offset {
                                val pos = if (draggingThis) currentSpotPosition else spot.position
                                IntOffset(pos.x.roundToInt(), pos.y.roundToInt())
                            }
                            .size(56.dp)
                            // Removed shadow, background, and border to match "no box" request
                            .graphicsLayer(
                                scaleX = if (draggingThis) 1.2f else 1f,
                                scaleY = if (draggingThis) 1.2f else 1f,
                                translationY = if (draggingThis) -30f else 0f // Lift up slightly when dragging
                            )
                            .pointerInput(resolvedSpace.id, spot.id) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val longPress = awaitLongPressOrCancellation(down.id)
                                    if (longPress != null) {
                                        currentSpotPosition = latestPosition
                                        activeSpotId = null
                                        isDraggingSpot = true
                                        draggingSpotId = spot.id

                                        drag(down.id) { change ->
                                            val delta = change.positionChange()
                                            if (delta != Offset.Zero) {
                                                change.consume()
                                                currentSpotPosition = Offset(
                                                    (currentSpotPosition.x + delta.x).coerceIn(0f, boxWidthPx - markerSize),
                                                    (currentSpotPosition.y + delta.y).coerceIn(0f, boxHeightPx - markerSize)
                                                )
                                                // 拖拽过程中不写入数据库，仅更新本地状态
                                            }
                                        }

                                        // 拖拽结束后才保存到数据库，减少 I/O 操作
                                        viewModel.updateSpotPosition(
                                            spaceId = resolvedSpace.id,
                                            spotId = spot.id,
                                            newPosition = currentSpotPosition
                                        )
                                        isDraggingSpot = false
                                        draggingSpotId = null
                                        lastDragEndTime = System.currentTimeMillis()
                                    } else {
                                        val now = System.currentTimeMillis()
                                        if (!isDraggingSpot && now - lastDragEndTime > 200) {
                                            activeSpotId = spot.id
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Sticker Content 
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                             // Maybe just an icon? Or letter?
                             // Icon is clearer.
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = spot.name,
                                tint = if (draggingThis) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary, // Color change on drag
                                modifier = Modifier.size(48.dp) // Larger icon since no box
                            )
                        }
                    }
                    
                    // Label outside the sticky note
                    Box(
                        modifier = Modifier
                            .offset { 
                                val pos = if (draggingThis) currentSpotPosition else spot.position
                                IntOffset(
                                    (pos.x + markerSize/2 - 50 ).roundToInt(), // Centered approx 
                                    (pos.y + labelOffset).roundToInt()
                                ) 
                             }
                             .background(MaterialTheme.colorScheme.surface.copy(alpha=0.8f), RoundedCornerShape(4.dp))
                             .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                         Text(
                            text = spot.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "位置列表",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp), // Increased spacing
                modifier = Modifier.fillMaxWidth()
            ) {
                resolvedSpace.spots.forEach { spot ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 2.dp,
                                shape = RoundedCornerShape(20.dp),
                                clip = false,
                                ambientColor = Color(0x208D7B68),
                                spotColor = Color(0x208D7B68)
                            )
                            .clickable { activeSpotId = spot.id },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = spot.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${spot.items.size} 个物品",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(180f),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSpot) {
        AlertDialog(
            onDismissRequest = { showAddSpot = false },
            title = { Text("添加位置", style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = newSpotName,
                    onValueChange = { newSpotName = it },
                    label = { Text("位置名称") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newSpotName.trim()
                        if (name.isNotBlank()) {
                            val defaultPosition = Offset(
                                (mapSize.width / 2f).coerceAtLeast(0f),
                                (mapSize.height / 2f).coerceAtLeast(0f)
                            )
                            viewModel.addSpot(spaceId, name, defaultPosition)
                            newSpotName = ""
                            showAddSpot = false
                        }
                    },
                    shape = RoundedCornerShape(100.dp)
                ) { Text("确定") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddSpot = false },
                    shape = RoundedCornerShape(100.dp)
                ) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    val activeSpot = activeSpotId?.let { id -> resolvedSpace.spots.firstOrNull { it.id == id } }
    if (activeSpot != null) {
        SpotItemsDialog(
            viewModel = viewModel,
            spaceId = resolvedSpace.id,
            spot = activeSpot,
            allTags = tags,
            highlightItemId = highlightItemId,
            onDismiss = { activeSpotId = null },
            onDeleteSpot = {
                viewModel.removeSpot(resolvedSpace.id, activeSpot.id)
                activeSpotId = null
            }
        )
    }
}
