/**
 * 标签选择与管理组件。
 *
 * 职责：
 * - 提供标签的级联选择界面。
 * - 提供标签的增删改管理入口。
 *
 * 上层用途：
 * - 被 `ItemDialogs` 等组件引用，用于在创建或编辑物品时分配标签。
 */
package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.TextPrimary
import com.example.myapplication.ui.theme.TextSecondary
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TagPickerSection(
    allTags: List<Tag>,
    selectedTagIds: Set<String>,
    onChange: (Set<String>) -> Unit,
    onCreateTag: (String, String?) -> Unit,
    onDeleteTag: (String) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    var newTagParentId by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Tag?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "标签", fontWeight = FontWeight.Bold, color = TextPrimary)

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("搜索标签") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val filtered = if (query.isBlank()) {
                allTags
            } else {
                val q = query.trim()
                allTags.filter { it.name.contains(q, ignoreCase = true) }
            }

            // Quick add if query doesn't exactly match any existing tag
            val exactMatch = filtered.any { it.name.equals(query.trim(), ignoreCase = true) }
            if (query.isNotBlank() && !exactMatch) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            onCreateTag(query.trim(), null)
                            query = ""
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "新建标签 \"${query.trim()}\"",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (filtered.isEmpty()) {
                Text(text = "暂无标签", color = TextSecondary, fontSize = 12.sp)
            } else {
                filtered.forEach { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .padding(vertical = 2.dp)
                            .clickable {
                                onChange(
                                    if (selectedTagIds.contains(tag.id)) selectedTagIds - tag.id else selectedTagIds + tag.id
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedTagIds.contains(tag.id),
                            onCheckedChange = {
                                onChange(
                                    if (selectedTagIds.contains(tag.id)) selectedTagIds - tag.id else selectedTagIds + tag.id
                                )
                            }
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        val prefix = tag.parentId?.let { "↳ " } ?: ""
                        Text(
                            text = prefix + tag.name,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { pendingDelete = tag }) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "删除标签")
                        }
                    }
                }
            }
        }

        if (!adding) {
            OutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
                Text("新建标签")
            }
        } else {
            OutlinedTextField(
                value = newTagName,
                onValueChange = { newTagName = it },
                label = { Text("标签名称") },
                modifier = Modifier.fillMaxWidth()
            )
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                val label = allTags.firstOrNull { it.id == newTagParentId }?.name ?: "不设置父标签"
                OutlinedTextField(
                    readOnly = true,
                    value = label,
                    onValueChange = {},
                    label = { Text("父标签") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("不设置父标签") },
                        onClick = {
                            newTagParentId = null
                            expanded = false
                        }
                    )
                    allTags.forEach { tag ->
                        DropdownMenuItem(
                            text = { Text(tag.name) },
                            onClick = {
                                newTagParentId = tag.id
                                expanded = false
                            }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        adding = false
                        newTagName = ""
                        newTagParentId = null
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("取消") }
                Button(
                    onClick = {
                        val n = newTagName.trim()
                        if (n.isNotBlank()) onCreateTag(n, newTagParentId)
                        adding = false
                        newTagName = ""
                        newTagParentId = null
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("创建") }
            }
        }
    }

    val target = pendingDelete
    if (target != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除标签") },
            text = { Text("确定删除“${target.name}”？已绑定的物品会移除该标签。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTag(target.id)
                        onChange(selectedTagIds - target.id)
                        pendingDelete = null
                    }
                ) { Text("删除") }
            },
            dismissButton = { OutlinedButton(onClick = { pendingDelete = null }) { Text("取消") } }
        )
    }
}
