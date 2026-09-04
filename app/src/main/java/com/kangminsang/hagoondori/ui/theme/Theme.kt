package com.kangminsang.hagoondori.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = NavyBlue40,
    onPrimary = Surface99,
    primaryContainer = NavyBlue90,
    onPrimaryContainer = NavyBlue20,
    secondary = SteelGray40,
    tertiary = Amber40,
    error = Error40,
    background = Surface99,
    surface = Surface99,
)

private val DarkColors = darkColorScheme(
    primary = NavyBlue80,
    onPrimary = NavyBlue20,
    primaryContainer = NavyBlue40,
    onPrimaryContainer = NavyBlue90,
    secondary = SteelGray80,
    tertiary = Amber80,
    error = Error80,
    background = Surface10,
    surface = Surface10,
)

@Composable
fun HagoondoriTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = HagoondoriTypography,
        content = content,
    )
}
