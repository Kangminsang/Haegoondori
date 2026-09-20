package com.kangminsang.hagoondori.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kangminsang.hagoondori.core.calc.DDayCalculator
import com.kangminsang.hagoondori.ui.common.SyncStatusBanner
import com.kangminsang.hagoondori.util.AppClock

/**
 * ① 대시보드 (스펙 5.1/5.2절, F2) - 앱을 여는 주된 이유.
 * 전역/진급 D-day + 진행률, 휴가·전투휴무 요약, 다음 외박 D-day,
 * 이번 달 외출 잔여, 동기화 상태 배너를 한 화면에 모은다.
 */
@Composable
fun DashboardScreen(
    onNavigateToSync: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier) { padding ->
        if (uiState.isLoading) {
            Row(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator(modifier = Modifier.padding(top = 64.dp)) }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                SyncStatusBanner(
                    syncState = uiState.syncState,
                    now = AppClock.now(),
                    onSyncNow = onNavigateToSync,
                )
            }

            if (uiState.profile == null) {
                item {
                    EmptyProfileNotice(modifier = Modifier.padding(16.dp))
                }
                return@LazyColumn
            }

            item { ServiceProgressCard(uiState, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            item { LeaveSummaryCard(uiState, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            item { CombatRestSummaryCard(uiState, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            item { OvernightSummaryCard(uiState, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            item { PassSummaryCard(uiState, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
        }
    }
}

@Composable
private fun EmptyProfileNotice(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("복무 정보가 아직 없습니다", style = MaterialTheme.typography.titleMedium)
            Text(
                "설정에서 입대일·전역일 등 복무 기준 정보를 먼저 입력해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ServiceProgressCard(uiState: DashboardUiState, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("전역 D-day", style = MaterialTheme.typography.titleMedium)
            val dDayText = uiState.dischargeDDay?.let { if (it >= 0) "D-$it" else "D+${-it}" } ?: "-"
            Text(dDayText, style = MaterialTheme.typography.titleLarge)
            LinearProgressIndicator(
                progress = { (uiState.serviceProgressPercent / 100.0).toFloat() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Text(
                "복무 진행률 ${"%.1f".format(uiState.serviceProgressPercent)}%",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            uiState.promotionDDay?.let { promotionDDay ->
                val promotionText = if (promotionDDay >= 0) "D-$promotionDDay" else "D+${-promotionDDay}"
                Text(
                    "다음 진급까지 $promotionText",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun LeaveSummaryCard(uiState: DashboardUiState, modifier: Modifier = Modifier) {
    if (uiState.leaveSummaries.isEmpty()) return
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("휴가 잔여", style = MaterialTheme.typography.titleMedium)
            uiState.leaveSummaries.forEach { summary ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(summary.type.name, style = MaterialTheme.typography.bodyMedium)
                    Text("${summary.remaining}일 남음", style = MaterialTheme.typography.bodyMedium)
                }
                // 포상 상한 경고(F9, 3.2.3절): 받기 전에 미리 알려야 의미가 있다.
                val capCapacity = summary.remainingCapCapacity
                if (capCapacity != null && capCapacity <= 3) {
                    Text(
                        "포상 상한까지 ${capCapacity}일 남았습니다. 이후 받는 포상은 전투휴무로 전환됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun CombatRestSummaryCard(uiState: DashboardUiState, modifier: Modifier = Modifier) {
    val summary = uiState.combatRestSummary
    if (summary.totalGranted == 0) return
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("전투휴무 잔여", style = MaterialTheme.typography.titleMedium)
            Text(
                "${summary.remaining}일 (전환 ${summary.convertedFromLeave}일 + 부여 ${summary.directGranted}일)",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun OvernightSummaryCard(uiState: DashboardUiState, modifier: Modifier = Modifier) {
    val schedule = uiState.overnightSchedule ?: return
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("다음 외박", style = MaterialTheme.typography.titleMedium)
            val today = uiState.today
            val dDay = today?.let { DDayCalculator.dDay(it, schedule.nextScheduledDate) }
            Text(
                "${schedule.nextScheduledDate} (${schedule.nextSlotIndex + 1}차) ${dDay?.let { d -> if (d >= 0) "D-$d" else "D+${-d}" } ?: ""}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun PassSummaryCard(uiState: DashboardUiState, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("이번 달 외출 잔여", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("평일 ${uiState.weekdayPassRemaining}회", style = MaterialTheme.typography.bodyMedium)
                Text("휴일 ${uiState.holidayPassRemaining}회", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
