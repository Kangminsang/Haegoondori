package com.kangminsang.hagoondori.ui.theme

import androidx.compose.ui.graphics.Color

// 장치(e-ink 탁상 캘린더)는 흑/적/백 3색 패널이다. 앱도 같은 패밀리 룩을 갖도록
// 팔레트를 이 세 색(과 종이 위의 옅은 회색 한 단계)으로만 제한한다. 색만으로 구분하던
// 것은 모양(도형)으로 바꿨다 - 장치에서 흑백 도형과 글자로 구분하는 것과 같은 방식이다.
// core 데이터 모델에는 색상 개념이 전혀 없다(6.4절 규칙 3).

/** 종이. e-ink 패널의 흰 바탕처럼 살짝 따뜻한 흰색. */
val Paper = Color(0xFFF1F0EA)

/** 잉크. 순수 검정보다 약간 눌린 검정(패널의 검정이 완전한 검정이 아닌 것과 비슷하다). */
val Ink = Color(0xFF1B1B1B)

/** 패널의 빨강. 공휴일·경고·강조 한 가지 용도로만 쓴다. */
val InkRed = Color(0xFFB3312C)

/** 비활성/배경 격자용. 장치에서 디더링된 회색에 해당하는 유일한 중간 톤. */
val InkGray = Color(0xFF8A8880)
val PaperShade = Color(0xFFE4E1D8)
