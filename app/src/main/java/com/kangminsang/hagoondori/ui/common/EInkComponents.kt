package com.kangminsang.hagoondori.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button as MaterialButton

/** e-ink 화면의 기본 상자: 그림자 없이 잉크색 테두리로만 구분되는 각진 카드. */
@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(2.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        content = content,
    )
}

/** 알약형 기본 버튼 대신 카드와 같은 각진 잉크 버튼. */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(2.dp),
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        content = content,
    )
}

/** 게이지 위의 한 지점. [fraction]은 0~1, [passed]가 참이면 이미 지난 지점이다. */
data class GaugeMarker(val fraction: Float, val label: String, val passed: Boolean)

/**
 * 장치 전역 게이지(설계서 5장)와 같은 모양: 2dp 잉크 외곽선 안쪽에 여백을 두고 채우는 막대.
 * 기본 Material 진행 표시줄의 둥근 트랙 대신 각진 상자를 쓴다.
 *
 * [markers]는 막대 위아래로 삐져나오는 세로선과 그 아래 라벨로 그린다(진급 지점). 앞으로 올 지점은
 * 적색, 이미 지난 지점은 잉크색이다.
 */
@Composable
fun EInkGauge(progress: Float, modifier: Modifier = Modifier, markers: List<GaugeMarker> = emptyList()) {
    val ink = MaterialTheme.colorScheme.onSurface
    val red = MaterialTheme.colorScheme.error
    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(if (markers.isNotEmpty()) 26.dp else 14.dp)) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(14.dp)
                    .border(2.dp, ink)
                    .padding(2.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .background(ink),
                )
            }
            val markerWidth = 3.dp
            markers.forEach { marker ->
                Box(
                    Modifier
                        .offset(x = (maxWidth * marker.fraction.coerceIn(0f, 1f) - markerWidth / 2).coerceIn(0.dp, maxWidth - markerWidth))
                        .width(markerWidth)
                        .fillMaxHeight()
                        .background(if (marker.passed) ink else red),
                )
            }
        }
        if (markers.isNotEmpty()) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                val labelWidth = 26.dp
                markers.forEach { marker ->
                    Text(
                        marker.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (marker.passed) ink else red,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.offset(
                            x = (maxWidth * marker.fraction.coerceIn(0f, 1f) - labelWidth / 2).coerceIn(0.dp, maxWidth - labelWidth),
                        ),
                    )
                }
            }
        }
    }
}

/**
 * 원형 진행 링. 각진 카드와 어울리도록 끝을 둥글리지 않은(butt) 굵은 선으로 그리고, 가운데에 [content]를 둔다.
 * 시계 방향, 12시 방향에서 시작한다.
 */
@Composable
fun EInkRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 12.dp,
    color: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable BoxScope.() -> Unit,
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            drawArc(
                color, -90f, 360f * progress.coerceIn(0f, 1f),
                useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke),
            )
        }
        content()
    }
}
