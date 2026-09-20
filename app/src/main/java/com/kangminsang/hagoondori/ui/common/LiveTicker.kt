package com.kangminsang.hagoondori.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.datetime.Instant

/**
 * 화면이 보이는 동안 [intervalMillis]마다 갱신되는 현재 시각. 프레임 시계를 기다리므로 앱이 화면에서
 * 사라지면 갱신도 멈춘다. e-ink 장치는 이렇게 자주 그릴 수 없지만 휴대폰은 가능하다 - 흐르는 시간을
 * 눈으로 느끼게 하려는 용도다. 읽는 쪽 컴포저블만 다시 그려지도록 State로 돌려준다.
 */
@Composable
fun rememberLiveNow(intervalMillis: Long = 50L): State<Instant> = produceState(AppClock.now()) {
    var last = 0L
    while (true) {
        androidx.compose.runtime.withFrameMillis { frameTime ->
            if (frameTime - last >= intervalMillis) {
                last = frameTime
                value = AppClock.now()
            }
        }
    }
}

/**
 * 숫자마다 같은 폭의 칸에 그려서, 값이 바뀌어도(1처럼 좁은 글자가 나와도) 문자열 전체가 좌우로 흔들리지
 * 않게 한다. 비트맵 폰트는 숫자 폭이 제각각이라 그냥 그리면 빠르게 바뀌는 자리에서 글자가 떨린다.
 */
@Composable
fun FixedWidthDigits(
    text: String,
    style: TextStyle,
    digitWidth: TextUnit,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
) {
    val slot = with(LocalDensity.current) { digitWidth.toDp() }
    Row(modifier = modifier) {
        text.forEach { ch ->
            if (ch.isDigit()) {
                Box(Modifier.width(slot), contentAlignment = Alignment.Center) {
                    Text(ch.toString(), style = style, color = color)
                }
            } else {
                Text(ch.toString(), style = style, color = color)
            }
        }
    }
}
