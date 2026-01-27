/**
 * 清单功能页面。
 *
 * 职责：
 * - 展示清单列表（`ListsScreen`）。
 * - 展现具体清单的条目详情（`ListDetailScreen`）。
 * - 处理清单的创建、删除及条目勾选状态。
 *
 * 上层用途：
 * - 作为应用的主要功能模块之一，由 `MainActivity` 进行导航。
 */
package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    viewModel: SpaceViewModel,
    onOpenList: (String) -> Unit,
) {
    val lists by viewModel.lists.collectAsState()
    var newName by remember { mutableStateOf("") }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "清单",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${lists.size} 个清单",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ActionButton(
                        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                        label = "生成补货",
                        onClick = { viewModel.generateRestockList() }
                    )
                    ActionButton(
                        icon = Icons.Filled.Add,
                        label = "新建清单",
                        onClick = {
                            newName = ""
                            showSheet = true
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (lists.isEmpty()) {
            item {
                Text(
                    text = "暂无清单",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        } else {
            items(lists, key = { it.id }) { list ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(24.dp),
                            clip = false,
                            ambientColor = Color(0x408D7B68),
                            spotColor = Color(0x408D7B68)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onOpenList(list.id) },
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = list.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    if (showSheet) {
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
                    text = "新建清单",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("清单名称") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                androidx.compose.material3.Button(
                    onClick = {
                        val name = newName.trim()
                        if (name.isNotBlank()) {
                            viewModel.createList(name)
                            newName = ""
                            showSheet = false
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
}


@Composable
fun ListDetailScreen(
    viewModel: SpaceViewModel,
    listId: String,
    onBack: () -> Unit,
) {
    val lists by viewModel.lists.collectAsState()
    val list = lists.firstOrNull { it.id == listId }
    val items by viewModel.observeListItems(listId).collectAsState(initial = emptyList())
    var newItem by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val canAdd = newItem.trim().isNotBlank() && listId.isNotBlank()

    fun addNow() {
        val name = newItem.trim()
        if (listId.isBlank()) {
            android.widget.Toast.makeText(context, "清单 ID 异常，无法添加", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (name.isBlank()) {
            android.widget.Toast.makeText(context, "请输入条目名称", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            try {
                viewModel.addListItemSuspend(listId, name)
                newItem = ""
                focusManager.clearFocus()
            } catch (e: Throwable) {
                android.widget.Toast.makeText(context, "添加失败：${e.message ?: "未知错误"}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = list?.name ?: "清单",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    shape = RoundedCornerShape(100.dp)
                ) { Text("删除") }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = newItem,
                    onValueChange = { newItem = it },
                    label = { Text("新增条目") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addNow() }),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )
                androidx.compose.material3.Button(
                    onClick = { addNow() },
                    enabled = canAdd,
                    modifier = Modifier.heightIn(min = 56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("添加") }
            }
        }

        if (items.isEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("暂无条目", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(items, key = { it.id }) { item ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(16.dp),
                            clip = false,
                            ambientColor = Color(0x208D7B68),
                            spotColor = Color(0x208D7B68)
                        ),
                    shape = RoundedCornerShape(16.dp),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = item.checked,
                                onCheckedChange = { viewModel.toggleListItemChecked(item) }
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Column {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val q = item.quantityNeeded
                                if (q != null) Text("建议补 $q", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                            }
                        }
                        IconButton(onClick = { viewModel.deleteListItem(item.id) }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除清单") },
            text = { Text("确定删除该清单吗？") },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.deleteList(listId)
                        confirmDelete = false
                        onBack()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                         containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(100.dp)
                ) { Text("删除") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { confirmDelete = false },
                    shape = RoundedCornerShape(100.dp)
                ) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
