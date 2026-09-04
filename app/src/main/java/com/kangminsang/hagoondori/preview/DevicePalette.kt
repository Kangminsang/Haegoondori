package com.kangminsang.hagoondori.preview

import androidx.compose.ui.graphics.Color

/**
 * 장치 화면의 흑·적·백 3색 팔레트를 낮/밤 테마로 재현한다(스펙 1.4/5.3절).
 * 실제 e-ink 패널은 흑백 버퍼와 적백 버퍼 두 장을 합성하지만, 미리보기에서는
 * 그 결과물(세 가지 색만 쓰는 화면)만 재현하면 충분하다.
 */
data class DevicePalette(val background: Color, val foreground: Color, val accent: Color) {
    companion object {
        val DAY = DevicePalette(background = Color.White, foreground = Color.Black, accent = Color(0xFFCC0000))
        val NIGHT = DevicePalette(background = Color.Black, foreground = Color.White, accent = Color(0xFFFF5A5A))
    }
}
