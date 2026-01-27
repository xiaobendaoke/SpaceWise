/**
 * Compose 主题配置。
 *
 * 职责：
 * - 配置 Material 3 主题（颜色、字体等）。
 */
package com.example.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = WarmCoffee,
    onPrimary = Color.White,
    secondary = Oatmeal,
    onSecondary = DarkEarth,
    tertiary = SageGreen,
    onTertiary = Color.White,
    background = Cream,
    onBackground = DarkEarth,
    surface = SoftWhite,
    onSurface = DarkEarth,
    surfaceVariant = Oatmeal,
    onSurfaceVariant = MutedEarth,
    primaryContainer = WarmCoffeeContainer,
    onPrimaryContainer = DarkEarth,
    secondaryContainer = SageGreenContainer,
    onSecondaryContainer = DarkEarth,
    outline = CoffeeMilk,
    outlineVariant = Oatmeal
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
