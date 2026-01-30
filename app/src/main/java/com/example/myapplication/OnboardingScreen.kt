/**
 * 引导页/欢迎页。
 *
 * 职责：
 * - 为新用户提供功能介绍和初始化选项（如导入演示数据）。
 *
 * 上层用途：
 * - 在应用首次启动或用户手动进入时通过 `MainActivity` 导航展示。
 */
package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.LightBackground
import com.example.myapplication.ui.theme.TextPrimary
import com.example.myapplication.ui.theme.TextSecondary

private data class OnboardingStep(
    val title: String,
    val body: String,
)

@Composable
fun OnboardingScreen(
    viewModel: SpaceViewModel,
    onFinish: () -> Unit,
) {
    val steps = remember {
        listOf(
            OnboardingStep(
                title = "欢迎使用井井",
                body = "用「场所」组织你的家/办公室，用「区域」层层嵌套管理柜子、抽屉，轻松找到每一件物品。"
            ),
            OnboardingStep(
                title = "从模板快速开始",
                body = "在首页点「新建场所」，可选择模板（如我的家、办公室），自动创建常用区域结构。"
            ),
            OnboardingStep(
                title = "创建区域并添加物品",
                body = "进入场所后：点击「新建区域」创建柜子、抽屉等；区域可嵌套多层；点击区域进入，长按物品可编辑或删除。"
            ),
            OnboardingStep(
                title = "更容易找到",
                body = "物品支持拍照、OCR 识别、设置过期日期、库存等。使用「搜索」可按关键字/标签/路径快速定位。"
            ),
            OnboardingStep(
                title = "提醒、清单与备份",
                body = "设置里可开启到期提醒；清单支持旅行/搬家/补货；还能导出/导入备份。"
            )
        )
    }

    var index by remember { mutableIntStateOf(0) }
    val step = steps[index]
    val isLast = index == steps.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部进度
        Text(
            text = "${index + 1} / ${steps.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp)
        )

        // 中间内容区（垂直居中）
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = step.body,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 28.sp,
                    fontSize = 17.sp
                ),
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        // 底部操作区
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // 最后一步显示演示数据选项
            if (isLast) {
               Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "🎁 演示数据",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "创建「演示-」开头的场所和清单，帮你快速体验功能。",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        
                        Button(
                            onClick = {
                                viewModel.completeOnboarding(addDemoData = true) { onFinish() }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(100.dp),
                        ) {
                            Text("添加演示数据并开始")
                        }
                    }
                }
            }

            // 导航按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (index > 0) {
                    OutlinedButton(
                        onClick = { index -= 1 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("上一步")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            viewModel.completeOnboarding(addDemoData = false) { onFinish() }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("跳过")
                    }
                }

                Button(
                    onClick = {
                        if (isLast) {
                            viewModel.completeOnboarding(addDemoData = false) { onFinish() }
                        } else {
                            index += 1
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(if (isLast) "直接开始" else "下一步")
                }
            }
        }
    }
}
