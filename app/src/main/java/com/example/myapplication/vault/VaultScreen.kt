/**
 * 保险箱主界面
 */
package com.example.myapplication.vault

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.myapplication.storage.InternalImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 保险箱主入口
 */
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 认证状态
    var isAuthenticated by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showSetupDialog by remember { mutableStateOf(false) }
    
    // 检查是否首次使用
    val isInitialized = viewModel.isVaultInitialized()
    
    // 如果未认证，显示认证界面
    if (!isAuthenticated) {
        if (!isInitialized) {
            // 首次使用，显示设置密码对话框
            VaultSetupScreen(
                onSetPassword = { password ->
                    viewModel.setVaultPassword(password)
                    isAuthenticated = true
                },
                onBack = onBack
            )
        } else {
            // 已初始化，需要认证
            VaultAuthScreen(
                viewModel = viewModel,
                onAuthenticated = { isAuthenticated = true },
                onBack = onBack
            )
        }
    } else {
        // 已认证，显示保险箱内容
        VaultContentScreen(
            viewModel = viewModel,
            onBack = onBack
        )
    }
}

/**
 * 首次设置密码界面
 */
@Composable
fun VaultSetupScreen(
    onSetPassword: (String) -> Unit,
    onBack: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "设置保险箱密码",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "保险箱用于存储重要物品信息，请设置一个安全的密码",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("密码") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; error = null },
                label = { Text("确认密码") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("取消")
                }
                
                androidx.compose.material3.Button(
                    onClick = {
                        when {
                            password.length < 4 -> error = "密码至少4位"
                            password != confirmPassword -> error = "两次密码不一致"
                            else -> onSetPassword(password)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("确认")
                }
            }
        }
    }
}

/**
 * 认证界面
 */
@Composable
fun VaultAuthScreen(
    viewModel: VaultViewModel,
    onAuthenticated: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showBiometric by remember { mutableStateOf(true) }
    
    // 尝试生物识别
    if (showBiometric && viewModel.isBiometricAvailable()) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                viewModel.getAuthManager().authenticateWithBiometric(
                    activity = activity,
                    onSuccess = { onAuthenticated() },
                    onError = { msg -> error = msg; showBiometric = false },
                    onFallbackToPassword = { showBiometric = false }
                )
            } else {
                showBiometric = false
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // 返回按钮
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "解锁保险箱",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("输入密码") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            androidx.compose.material3.Button(
                onClick = {
                    if (viewModel.verifyPassword(password)) {
                        onAuthenticated()
                    } else {
                        error = "密码错误"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("解锁")
            }
            
            if (viewModel.isBiometricAvailable()) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            viewModel.getAuthManager().authenticateWithBiometric(
                                activity = activity,
                                onSuccess = { onAuthenticated() },
                                onError = { msg -> error = msg },
                                onFallbackToPassword = { }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("使用生物识别")
                }
            }
        }
    }
}

/**
 * 保险箱内容界面
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VaultContentScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val items by viewModel.items.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<VaultItem?>(null) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var pendingLongPressItem by remember { mutableStateOf<VaultItem?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                
                Text(
                    text = "🔐 保险箱",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // 搜索栏
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("搜索物品...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "清除")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 物品列表
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🔐",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "保险箱是空的" else "没有找到匹配的物品",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "点击右下角按钮添加物品",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        VaultItemCard(
                            item = item,
                            onClick = { editingItem = item },
                            onLongClick = { pendingLongPressItem = item }
                        )
                    }
                }
            }
        }
        
        // FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加物品")
        }
    }
    
    // 添加/编辑物品对话框
    if (showAddDialog || editingItem != null) {
        VaultItemDialog(
            viewModel = viewModel,
            initialItem = editingItem,
            onDismiss = {
                showAddDialog = false
                editingItem = null
            }
        )
    }
    
    // 长按菜单
    pendingLongPressItem?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingLongPressItem = null },
            title = { Text(item.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            editingItem = item
                            pendingLongPressItem = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("编辑")
                    }
                    OutlinedButton(
                        onClick = {
                            pendingDeleteId = item.id
                            pendingLongPressItem = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("删除")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingLongPressItem = null },
                    shape = RoundedCornerShape(100.dp)
                ) { Text("取消") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
    
    // 删除确认
    pendingDeleteId?.let { itemId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("确认删除") },
            text = { Text("删除后无法恢复，确定要删除吗？") },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.deleteItem(itemId)
                        pendingDeleteId = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(100.dp)
                ) { Text("删除") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingDeleteId = null },
                    shape = RoundedCornerShape(100.dp)
                ) { Text("取消") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

/**
 * 保险箱物品卡片
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VaultItemCard(
    item: VaultItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column {
            // 图片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (item.imagePath != null) {
                    val bitmap = remember(item.imagePath) {
                        try {
                            val file = InternalImageStore.resolveFile(context, item.imagePath)
                            android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        } catch (_: Exception) { null }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("🔒", fontSize = 32.sp)
                    }
                } else {
                    Text("🔒", fontSize = 32.sp)
                }
            }
            
            // 信息
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.note != null) {
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 添加/编辑物品对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultItemDialog(
    viewModel: VaultViewModel,
    initialItem: VaultItem?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var note by remember { mutableStateOf(initialItem?.note ?: "") }
    var imagePath by remember { mutableStateOf(initialItem?.imagePath) }
    var quantity by remember { mutableStateOf(initialItem?.currentQuantity?.toString() ?: "1") }
    
    // 拍照
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok ->
        val uri = pendingCameraUri
        if (ok && uri != null) {
            scope.launch(Dispatchers.IO) {
                val path = viewModel.persistCapturedPhoto(uri)
                if (path != null) {
                    imagePath = path
                }
            }
        }
    }
    
    // 相册选择
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                val path = viewModel.saveVaultImage(it)
                if (path != null) {
                    imagePath = path
                }
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialItem == null) "添加物品" else "编辑物品") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 图片预览
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (imagePath != null) {
                        val bitmap = remember(imagePath) {
                            try {
                                val file = InternalImageStore.resolveFile(context, imagePath!!)
                                android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                            } catch (_: Exception) { null }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Text("点击下方按钮添加图片", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                // 图片按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val uri = viewModel.createTempCameraUri()
                            pendingCameraUri = uri
                            takePictureLauncher.launch(uri)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("拍照", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { pickImageLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("相册", fontSize = 12.sp)
                    }
                }
                
                // 名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("物品名称") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                // 备注
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )
                
                // 数量
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                    label = { Text("数量") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "请输入物品名称", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    val qty = quantity.toIntOrNull() ?: 1
                    
                    if (initialItem == null) {
                        viewModel.addItem(
                            name = name,
                            note = note.ifBlank { null },
                            imagePath = imagePath,
                            expiryDateEpochMs = null,
                            currentQuantity = qty,
                            minQuantity = 0
                        )
                    } else {
                        viewModel.updateItem(
                            itemId = initialItem.id,
                            name = name,
                            note = note.ifBlank { null },
                            imagePath = imagePath,
                            expiryDateEpochMs = initialItem.expiryDateEpochMs,
                            currentQuantity = qty,
                            minQuantity = initialItem.minQuantity
                        )
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(if (initialItem == null) "添加" else "保存")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(100.dp)
            ) { Text("取消") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
