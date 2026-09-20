package com.kangminsang.hagoondori.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** 하단 네비게이션의 4개 탭(스펙 5.1절 - 근무 입력은 달력 탭의 입력 모드로 옮겼다). */
enum class HagoondoriDestination(val route: String, val label: String, val icon: ImageVector) {
    Dashboard(route = "dashboard", label = "대시보드", icon = Icons.Filled.Home),
    Calendar(route = "calendar", label = "달력", icon = Icons.Filled.CalendarMonth),
    Leave(route = "leave", label = "출타관리", icon = Icons.Filled.BeachAccess),
    Settings(route = "settings", label = "설정", icon = Icons.Filled.Settings),
}
