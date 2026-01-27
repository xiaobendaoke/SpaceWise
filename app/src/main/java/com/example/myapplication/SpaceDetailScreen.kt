package com.example.myapplication

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.myapplication.ui.theme.LightBackground
import com.example.myapplication.ui.theme.TextPrimary
import com.example.myapplication.ui.theme.TextSecondary
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
            .background(LightBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
                Column {
                    Text(
                        text = resolvedSpace.name,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${resolvedSpace.spots.size} 个空间",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    newSpotName = ""
                    showAddSpot = true
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.size(6.dp))
                Text("添加空间")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFFFDFBF6))
                .onSizeChanged { mapSize = it }
                .padding(12.dp)
        ) {
            val coverMaxPx = with(LocalDensity.current) { 1200.dp.roundToPx() }
            val coverBitmap = remember(resolvedSpace.coverImagePath) {
                resolvedSpace.coverImagePath?.let { loadBitmapFromInternalPath(context, it, coverMaxPx) }
            }
            if (coverBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = coverBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            val boxWidthPx = constraints.maxWidth.toFloat()
            val boxHeightPx = constraints.maxHeight.toFloat()
            val markerSize = with(LocalDensity.current) { 44.dp.toPx() }
            val labelOffset = with(LocalDensity.current) { 54.dp.toPx() }

            resolvedSpace.spots.forEach { spot ->
                key(spot.id) {
                    val latestPosition by rememberUpdatedState(spot.position)
                    val draggingThis = isDraggingSpot && draggingSpotId == spot.id

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(spot.position.x.roundToInt(), spot.position.y.roundToInt()) }
                            .size(48.dp)
                            .clip(CircleShape)
                            .graphicsLayer(
                                scaleX = if (draggingThis) 1.1f else 1f,
                                scaleY = if (draggingThis) 1.1f else 1f,
                            )
                            .background(if (draggingThis) Color(0xFFD8C8B6) else Color(0xFFE4D5C5))
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
                                                viewModel.updateSpotPosition(
                                                    spaceId = resolvedSpace.id,
                                                    spotId = spot.id,
                                                    newPosition = currentSpotPosition
                                                )
                                            }
                                        }

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
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = spot.name,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = spot.name,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.offset {
                            IntOffset(spot.position.x.roundToInt(), (spot.position.y + labelOffset).roundToInt())
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "空间",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val blockScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = available
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(blockScrollConnection)
        ) {
            resolvedSpace.spots.forEach { spot ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeSpotId = spot.id },
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 6.dp,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = spot.name,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${spot.items.size} 个物品",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(180f),
                            tint = TextSecondary
                        )
                    }
                }
            }
        }
    }

    if (showAddSpot) {
        AlertDialog(
            onDismissRequest = { showAddSpot = false },
            title = { Text("添加空间") },
            text = {
                OutlinedTextField(
                    value = newSpotName,
                    onValueChange = { newSpotName = it },
                    label = { Text("空间名称") }
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
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddSpot = false }) { Text("取消") }
            }
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
