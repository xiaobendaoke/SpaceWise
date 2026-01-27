/**
 * 全局搜索页面。
 *
 * 职责：
 * - 提供关键词搜索界面，支持搜索物品、空间、点位和标签。
 * - 展示搜索结果并支持跳转。
 *
 * 上层用途：
 * - 作为应用的核心导航入口之一，在底部导航栏中访问。
 */
package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme

@Composable
fun SearchScreen(
    viewModel: SpaceViewModel,
    onOpenResult: (ItemSearchResult) -> Unit,
) {
    val queryResults by viewModel.searchResults.collectAsState()
    var query by remember { mutableStateOf("") }
    LaunchedEffect(query) { viewModel.setSearchQuery(query) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .statusBarsPadding()
    ) {
        Text(
            "搜索",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("搜索物品/标签/位置/备注") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (query.isBlank()) {
            Text("输入关键字开始搜索", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        if (queryResults.isEmpty()) {
            Text("没有找到结果", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            queryResults.forEach { r ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenResult(r) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            r.itemName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            r.path,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!r.note.isNullOrBlank()) {
                            Text(
                                r.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top=4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
