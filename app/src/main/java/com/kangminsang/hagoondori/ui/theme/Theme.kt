package com.kangminsang.hagoondori.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// e-ink 패널은 종이처럼 밝은 바탕에 잉크를 얹는 표시장치라 다크 모드가 없다.
// 시스템 다크 테마와 무관하게 항상 이 한 가지 스킴만 쓴다.
private val EInkColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = Ink,
    onPrimaryContainer = Paper,
    secondary = Ink,
    onSecondary = Paper,
    secondaryContainer = Ink,
    onSecondaryContainer = Paper,
    tertiary = InkRed,
    onTertiary = Paper,
    tertiaryContainer = InkRed,
    onTertiaryContainer = Paper,
    error = InkRed,
    onError = Paper,
    errorContainer = Paper,
    onErrorContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperShade,
    onSurfaceVariant = Ink,
    surfaceTint = Paper,
    surfaceContainerLowest = Paper,
    surfaceContainerLow = Paper,
    surfaceContainer = Paper,
    surfaceContainerHigh = Paper,
    surfaceContainerHighest = Paper,
    outline = Ink,
    outlineVariant = InkGray,
)

// 그림자·둥근 모서리 없이 각진 상자에 선으로 구분하는 것이 e-ink 화면의 문법이다.
private val EInkShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(2.dp),
)

@Composable
fun HagoondoriTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EInkColors,
        shapes = EInkShapes,
        typography = HagoondoriTypography,
        content = content,
    )
}
