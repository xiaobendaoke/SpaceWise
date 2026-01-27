/**
 * 空间模板选择器。
 *
 * 职责：
 * - 展示预设的空间模板（如厨房、书房等）供用户快速选择。
 *
 * 上层用途：
 * - 被 `SpacesScreen` 的创建流程使用，简化用户初始空间的搭建。
 */
package com.example.myapplication

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatePicker(
    selectedTemplateId: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        val label = com.example.myapplication.templates.Templates.all.firstOrNull { it.id == selectedTemplateId }?.name
            ?: "不使用模板"
        OutlinedTextField(
            readOnly = true,
            value = label,
            onValueChange = {},
            label = { Text("模板") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("不使用模板") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            com.example.myapplication.templates.Templates.all.forEach { template ->
                DropdownMenuItem(
                    text = { Text(template.name) },
                    onClick = {
                        onSelect(template.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
