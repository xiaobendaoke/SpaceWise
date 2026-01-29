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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.LightBackground
import com.example.myapplication.ui.theme.TextPrimary
import com.example.myapplication.ui.theme.TextSecondary

private data class OnboardingStep(
    val title: String,
    val body: String,
    val illustration: OnboardingIllustration,
)

private enum class OnboardingIllustration {
    Overview,
    Template,
    DragSpot,
    Search,
    RemindBackup,
}

@Composable
fun OnboardingScreen(
    viewModel: SpaceViewModel,
    onFinish: () -> Unit,
) {
    val steps = remember {
        listOf(
            OnboardingStep(
                title = "欢迎使用井井",
                body = "用「场所」组织你的家/办公室，用「区域」层层嵌套管理柜子、抽屉，轻松找到每一件物品。",
                illustration = OnboardingIllustration.Overview
            ),
            OnboardingStep(
                title = "从模板快速开始",
                body = "在首页点「新建场所」，可选择模板（如我的家、办公室），自动创建常用区域结构。",
                illustration = OnboardingIllustration.Template
            ),
            OnboardingStep(
                title = "创建区域并添加物品",
                body = "进入场所后：点击「新建区域」创建柜子、抽屉等；区域可嵌套多层；点击区域进入，长按物品可编辑或删除。",
                illustration = OnboardingIllustration.DragSpot
            ),
            OnboardingStep(
                title = "更容易找到",
                body = "物品支持拍照、OCR 识别、设置过期日期、库存等。使用「搜索」可按关键字/标签/路径快速定位。",
                illustration = OnboardingIllustration.Search
            ),
            OnboardingStep(
                title = "提醒、清单与备份",
                body = "设置里可开启到期提醒；清单支持旅行/搬家/补货；还能导出/导入备份。",
                illustration = OnboardingIllustration.RemindBackup
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
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("快速上手", fontWeight = FontWeight.Bold, fontSize = 26.sp, color = TextPrimary)
            Text("第 ${index + 1} / ${steps.size} 步", color = TextSecondary)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OnboardingIllustrationCard(
                        type = step.illustration,
                        height = 180.dp
                    )
                    Text(step.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                    Text(step.body, color = TextSecondary, fontSize = 14.sp)
                    Text("动效示意图（占位，可后续替换为真实录屏/GIF）", color = TextSecondary, fontSize = 12.sp)
                }
            }
            if (isLast) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("要不要先加一些演示例子？", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            "会创建几个「演示-」开头的场所和清单，不会删除你已有的数据。",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Button(
                            onClick = {
                                viewModel.completeOnboarding(addDemoData = true) { onFinish() }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("添加演示数据并开始使用")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.completeOnboarding(addDemoData = false) { onFinish() }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("我先自己试试")
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    enabled = index > 0,
                    onClick = { if (index > 0) index -= 1 },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("上一步") }
                Button(
                    enabled = !isLast,
                    onClick = { if (index < steps.lastIndex) index += 1 },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("下一步") }
            }
            OutlinedButton(
                onClick = {
                    viewModel.completeOnboarding(addDemoData = false) { onFinish() }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) { Text("跳过教程") }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun OnboardingIllustrationCard(
    type: OnboardingIllustration,
    height: Dp,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF7F4EE),
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            when (type) {
                OnboardingIllustration.Overview -> IllustrationOverview()
                OnboardingIllustration.Template -> IllustrationTemplate()
                OnboardingIllustration.DragSpot -> IllustrationDragSpot()
                OnboardingIllustration.Search -> IllustrationSearch()
                OnboardingIllustration.RemindBackup -> IllustrationRemindBackup()
            }
        }
    }
}

@Composable
private fun IllustrationOverview() {
    val t = rememberInfiniteTransition(label = "overview")
    val pulse by t.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Space cards
        drawRoundRect(
            color = Color(0xFFEDE6DB),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.02f, h * 0.10f),
            size = androidx.compose.ui.geometry.Size(w * 0.46f, h * 0.34f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
        )
        drawRoundRect(
            color = Color(0xFFEDE6DB),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.52f, h * 0.10f),
            size = androidx.compose.ui.geometry.Size(w * 0.46f, h * 0.34f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
        )
        // Map area
        drawRoundRect(
            color = Color(0xFFFFFFFF),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.02f, h * 0.52f),
            size = androidx.compose.ui.geometry.Size(w * 0.96f, h * 0.40f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
        )

        // Spot pulse
        val spot = androidx.compose.ui.geometry.Offset(w * 0.30f, h * 0.70f)
        drawCircle(color = Color(0xFFB59F88).copy(alpha = 0.35f * pulse), radius = 34f * pulse, center = spot)
        drawCircle(color = Color(0xFFB59F88), radius = 12f, center = spot)

        // Item chips
        val chipY = h * 0.84f
        drawRoundRect(
            color = Color(0xFFE7DED1),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.42f, chipY),
            size = androidx.compose.ui.geometry.Size(w * 0.22f, h * 0.08f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f),
            alpha = pulse
        )
        drawRoundRect(
            color = Color(0xFFE7DED1),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.66f, chipY),
            size = androidx.compose.ui.geometry.Size(w * 0.26f, h * 0.08f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f),
            alpha = 0.6f + 0.4f * (1f - pulse)
        )
    }
}

@Composable
private fun IllustrationTemplate() {
    val t = rememberInfiniteTransition(label = "template")
    val slide by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "slide"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Dialog
        drawRoundRect(
            color = Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.06f, h * 0.08f),
            size = androidx.compose.ui.geometry.Size(w * 0.88f, h * 0.84f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f)
        )
        // Title bar
        drawRoundRect(
            color = Color(0xFFEDE6DB),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.10f, h * 0.14f),
            size = androidx.compose.ui.geometry.Size(w * 0.52f, h * 0.08f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
        )
        // Template list
        val listX = w * 0.10f
        val listY = h * 0.28f
        val rowH = h * 0.12f
        repeat(4) { i ->
            val y = listY + i * (rowH + h * 0.03f)
            drawRoundRect(
                color = Color(0xFFF2EEE7),
                topLeft = androidx.compose.ui.geometry.Offset(listX, y),
                size = androidx.compose.ui.geometry.Size(w * 0.80f, rowH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
            )
        }
        // Moving highlight
        val idx = ((slide * 4f).toInt()).coerceIn(0, 3)
        val highlightY = listY + idx * (rowH + h * 0.03f)
        drawRoundRect(
            color = Color(0xFFB59F88).copy(alpha = 0.25f),
            topLeft = androidx.compose.ui.geometry.Offset(listX, highlightY),
            size = androidx.compose.ui.geometry.Size(w * 0.80f, rowH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
        )
        // Button area
        drawRoundRect(
            color = Color(0xFFB59F88),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.80f),
            size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.10f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f),
            alpha = 0.5f + 0.5f * (1f - (slide - 0.5f).coerceIn(0f, 0.5f) * 2f)
        )
    }
}

@Composable
private fun IllustrationDragSpot() {
    val t = rememberInfiniteTransition(label = "drag")
    val p by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "p"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val start = androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.70f)
        val end = androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.34f)
        val dot = androidx.compose.ui.geometry.Offset(
            x = start.x + (end.x - start.x) * p,
            y = start.y + (end.y - start.y) * p
        )

        // Map background
        drawRoundRect(
            color = Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.06f, h * 0.10f),
            size = androidx.compose.ui.geometry.Size(w * 0.88f, h * 0.80f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f)
        )

        // Path
        val path = Path().apply {
            moveTo(start.x, start.y)
            quadraticTo(w * 0.52f, h * 0.80f, end.x, end.y)
        }
        drawPath(
            path = path,
            color = Color(0xFFB59F88).copy(alpha = 0.35f),
            style = Stroke(width = 10f, cap = StrokeCap.Round)
        )

        // Spot dot
        drawCircle(color = Color(0xFFB59F88).copy(alpha = 0.25f), radius = 30f, center = dot)
        drawCircle(color = Color(0xFFB59F88), radius = 12f, center = dot)

        // "Finger" indicator
        drawCircle(color = Color(0xFF6B5B4C).copy(alpha = 0.20f), radius = 22f, center = dot + androidx.compose.ui.geometry.Offset(24f, 20f))
    }
}

@Composable
private fun IllustrationSearch() {
    val t = rememberInfiniteTransition(label = "search")
    val scan by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "scan"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Search bar
        drawRoundRect(
            color = Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.06f, h * 0.12f),
            size = androidx.compose.ui.geometry.Size(w * 0.88f, h * 0.18f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f)
        )
        // Magnifier circle
        drawCircle(
            color = Color(0xFFB59F88),
            radius = 18f,
            center = androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.21f),
            alpha = 0.55f
        )
        // Results
        val listX = w * 0.06f
        val listY = h * 0.36f
        val rowH = h * 0.14f
        repeat(3) { i ->
            val y = listY + i * (rowH + h * 0.05f)
            drawRoundRect(
                color = Color.White,
                topLeft = androidx.compose.ui.geometry.Offset(listX, y),
                size = androidx.compose.ui.geometry.Size(w * 0.88f, rowH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
            )
        }
        // Moving highlight
        val y = listY + (rowH + h * 0.05f) * scan
        drawRoundRect(
            color = Color(0xFFB59F88).copy(alpha = 0.18f),
            topLeft = androidx.compose.ui.geometry.Offset(listX, y),
            size = androidx.compose.ui.geometry.Size(w * 0.88f, rowH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
        )
    }
}

@Composable
private fun IllustrationRemindBackup() {
    val t = rememberInfiniteTransition(label = "remind")
    val pulse by t.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val bounce by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "bounce"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Bell-ish circle
        val bell = androidx.compose.ui.geometry.Offset(w * 0.20f, h * 0.34f)
        drawCircle(color = Color(0xFFB59F88).copy(alpha = 0.25f), radius = 44f * pulse, center = bell)
        drawCircle(color = Color(0xFFB59F88), radius = 18f, center = bell, alpha = 0.65f)

        // Checklist
        val listX = w * 0.38f
        val listY = h * 0.16f
        val rowH = h * 0.16f
        repeat(3) { i ->
            val y = listY + i * (rowH + h * 0.06f)
            drawRoundRect(
                color = Color.White,
                topLeft = androidx.compose.ui.geometry.Offset(listX, y),
                size = androidx.compose.ui.geometry.Size(w * 0.56f, rowH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
            )
            val checkAlpha = if (i == 0) 0.35f + 0.65f * pulse else 0.25f
            drawCircle(
                color = Color(0xFF6B5B4C).copy(alpha = checkAlpha),
                radius = 10f,
                center = androidx.compose.ui.geometry.Offset(listX + 26f, y + rowH / 2f)
            )
        }

        // Backup arrow-ish bouncing dot
        val dot = androidx.compose.ui.geometry.Offset(w * 0.18f + 40f, h * 0.70f - 30f * kotlin.math.sin(bounce * 3.14159f))
        drawCircle(color = Color(0xFF6B5B4C).copy(alpha = 0.25f), radius = 22f, center = dot)
        drawCircle(color = Color(0xFF6B5B4C).copy(alpha = 0.55f), radius = 10f, center = dot)
    }
}
