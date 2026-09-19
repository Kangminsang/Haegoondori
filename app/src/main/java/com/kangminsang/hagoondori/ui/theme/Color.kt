package com.kangminsang.hagoondori.ui.theme

import androidx.compose.ui.graphics.Color

// 해군 색상(감청색 계열)을 기본 톤으로 삼은 Material3 팔레트.
// 주의: 이 색상은 앱 화면(휴대폰) 전용이다 - 장치(e-ink) 쪽 렌더링은 흑/적/백
// 3색뿐이며 앱과 무관하게 장치 펌웨어의 렌더링이 결정한다.
// core 데이터 모델에는 색상 개념이 전혀 없다(6.4절 규칙 3).

val NavyBlue40 = Color(0xFF33547E)
val NavyBlue80 = Color(0xFFAEC6E8)
val NavyBlue20 = Color(0xFF15294A)
val NavyBlue90 = Color(0xFFD8E3F4)

val SteelGray40 = Color(0xFF556070)
val SteelGray80 = Color(0xFFBDC8DA)

val Amber40 = Color(0xFF7A5900)
val Amber80 = Color(0xFFF0C15C)

val Error40 = Color(0xFFBA1A1A)
val Error80 = Color(0xFFFFB4AB)

val Surface99 = Color(0xFFFDFBFF)
val Surface10 = Color(0xFF1A1C1E)

// 3.3.3절: 전투휴무는 달력에서 휴가와 시각적으로 구분되어야 한다(영외 이동 불가).
val CombatRestTint = Color(0xFF6B4A00)
val LeaveTint = NavyBlue40
val OvernightTint = Color(0xFF2E6E4E)
val PassTint = Color(0xFF7A4A9E)
