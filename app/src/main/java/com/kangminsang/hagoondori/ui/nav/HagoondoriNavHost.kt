package com.kangminsang.hagoondori.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kangminsang.hagoondori.preview.DevicePreviewScreen
import com.kangminsang.hagoondori.ui.calendar.CalendarScreen
import com.kangminsang.hagoondori.ui.dashboard.DashboardScreen
import com.kangminsang.hagoondori.ui.duty.DutyBulkInputScreen
import com.kangminsang.hagoondori.ui.leave.LeaveManagementScreen
import com.kangminsang.hagoondori.ui.settings.SettingsScreen

/**
 * 앱의 화면 전환 전체를 담당한다(스펙 5.1절). 하단 탭 5개 + 설정에서 진입하는
 * 장치 미리보기(F16) 서브 라우트로 구성된다.
 */
@Composable
fun HagoondoriNavHost() {
    val navController = rememberNavController()

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
            composable(HagoondoriDestination.Duty.route) { DutyBulkInputScreen() }
            composable(HagoondoriDestination.Settings.route) {
                SettingsScreen(
                    onOpenDevicePreview = { navController.navigate(HagoondoriSubRoute.DEVICE_PREVIEW) },
                )
            }
            composable(HagoondoriSubRoute.DEVICE_PREVIEW) { DevicePreviewScreen() }
        }
    }
}

@Composable
private fun HagoondoriBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
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
            )
        }
    }
}
