package com.kangminsang.hagoondori.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kangminsang.hagoondori.R

// 장치(e-ink)와 같은 갈무리(OFL 1.1, 라이선스: assets/licenses/GALMURI-LICENSE.txt) 비트맵 계열.
// 장치 설계서 v2(3장)의 본문/소형은 갈무리11, 제목은 갈무리14다. 갈무리11만 굵은체 파일이
// 있고 갈무리14는 굵은체가 없어(장치도 겹쳐 찍어 굵게 만든다) 제목은 시스템 합성 굵기를 쓴다.
val Galmuri11 = FontFamily(
    Font(R.font.galmuri11, FontWeight.Normal),
    Font(R.font.galmuri11, FontWeight.Medium),
    Font(R.font.galmuri11_bold, FontWeight.Bold),
    Font(R.font.galmuri11_bold, FontWeight.Black),
)
val Galmuri14 = FontFamily(Font(R.font.galmuri14, FontWeight.Normal))

val HagoondoriTypography = Typography(
    titleLarge = TextStyle(fontFamily = Galmuri14, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontFamily = Galmuri14, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = Galmuri11, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Galmuri11, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Galmuri11, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Galmuri11, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Galmuri11, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Galmuri11, fontSize = 11.sp, lineHeight = 16.sp),
    headlineSmall = TextStyle(fontFamily = Galmuri14, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineMedium = TextStyle(fontFamily = Galmuri14, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
)
