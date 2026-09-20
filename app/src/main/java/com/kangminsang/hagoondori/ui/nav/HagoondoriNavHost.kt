package com.kangminsang.hagoondori.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kangminsang.hagoondori.ui.calendar.CalendarScreen
import com.kangminsang.hagoondori.ui.dashboard.DashboardScreen
import com.kangminsang.hagoondori.ui.leave.LeaveManagementScreen
import com.kangminsang.hagoondori.ui.settings.SettingsScreen

/**
 * 앱의 화면 전환 전체를 담당한다(스펙 5.1절). 하단 탭 4개로 구성된다.
 */
@Composable
fun HagoondoriNavHost() {
    val navController = rememberNavController()
    // 대시보드의 동기화 배너에서 설정으로 넘어올 때 "장치 연동" 카드까지 스크롤하라는 요청.
    var scrollToDeviceSection by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = { HagoondoriBottomBar(navController) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HagoondoriDestination.Dashboard.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(HagoondoriDestination.Dashboard.route) {
                DashboardScreen(
                    onNavigateToSync = {
                        scrollToDeviceSection = true
                        navController.navigate(HagoondoriDestination.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(HagoondoriDestination.Calendar.route) { CalendarScreen() }
            composable(HagoondoriDestination.Leave.route) { LeaveManagementScreen() }
            composable(HagoondoriDestination.Settings.route) {
                SettingsScreen(
                    scrollToDeviceSection = scrollToDeviceSection,
                    onScrollHandled = { scrollToDeviceSection = false },
                )
            }
        }
    }
}

@Composable
private fun HagoondoriBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Column {
    HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        HagoondoriDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    // 탭을 반복 클릭해도 백스택이 계속 쌓이지 않도록 표준 패턴을 따른다:
                    // 시작 목적지까지 popUp하되 상태는 저장하고, 최상단 중복은 만들지 않는다.
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
    }
}
