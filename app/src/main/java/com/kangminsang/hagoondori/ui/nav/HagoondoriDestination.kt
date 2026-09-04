package com.kangminsang.hagoondori.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** 하단 네비게이션의 5개 탭(스펙 5.1절 ①~⑤). */
enum class HagoondoriDestination(val route: String, val label: String, val icon: ImageVector) {
    Dashboard(route = "dashboard", label = "대시보드", icon = Icons.Filled.Home),
    Calendar(route = "calendar", label = "달력", icon = Icons.Filled.CalendarMonth),
    Leave(route = "leave", label = "출타관리", icon = Icons.Filled.BeachAccess),
    Duty(route = "duty", label = "근무입력", icon = Icons.Filled.Assignment),
    Settings(route = "settings", label = "설정", icon = Icons.Filled.Settings),
}

/** 하단 탭에 속하지 않는 서브 라우트(설정 화면에서 진입). */
object HagoondoriSubRoute {
    /** 장치 미리보기(F16) - 800x480 렌더링 결과를 확인한다. */
    const val DEVICE_PREVIEW = "preview"
}
