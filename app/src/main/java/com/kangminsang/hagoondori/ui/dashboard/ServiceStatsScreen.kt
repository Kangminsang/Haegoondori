package com.kangminsang.hagoondori.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kangminsang.hagoondori.core.calc.LiveDurationFormatter
import com.kangminsang.hagoondori.core.calc.PastAndUpcoming
import com.kangminsang.hagoondori.core.calc.ServiceStats
import com.kangminsang.hagoondori.core.model.DutyType
import com.kangminsang.hagoondori.ui.common.Card
import java.util.Locale

/**
 * 군생활 통계 페이지. 대시보드에서 "전역까지" 남은 날을 누르면 열린다. 값은 대시보드와 같은
 * [DashboardViewModel]에서 오며(같은 원본을 같은 방식으로 계산하므로 따로 두지 않는다) 여기서는 글자로만 바꾼다.
 */
@Composable
fun ServiceStatsScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ChevronLeft, contentDescription = "뒤로") }
                Text("군생활 통계", style = MaterialTheme.typography.titleLarge)
            }
        },
    ) { padding ->
        val stats = uiState.serviceStats
        if (uiState.isLoading || stats == null) {
            Row(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalArrangement = Arrangement.Center,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 64.dp))
                } else {
                    Text(
                        "설정에서 복무 정보를 먼저 입력해 주세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 64.dp),
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item { HeadlineCard(stats, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            item { PassedCard(stats, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            item { RemainingCard(uiState, stats, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            item {
                Text(
                    "식사는 부대에서 먹는 하루 3끼로 셉니다. 휴가·외박한 날은 빼고, " +
                        "평일 외출은 저녁 1끼, 휴일 외출은 점심·저녁 2끼를 뺍니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private val n: (Long) -> String = LiveDurationFormatter::groupThousands

/** 눈에 띄는 숫자 네 개와 진행 정도에 맞춘 한 줄 문구. */
@Composable
private fun HeadlineCard(stats: ServiceStats, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                funFact(stats),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeadlineStat("실제 부대 생활 남은 날", "${n(stats.remaining.onBaseDays.toLong())}일", Modifier.weight(1f))
                StatDivider()
                HeadlineStat("남은 주말", "${n(stats.remaining.weekendDays.toLong())}일", Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeadlineStat("남은 식사", "${n(stats.remaining.meals.toLong())}끼", Modifier.weight(1f))
                StatDivider()
                HeadlineStat("지금까지 출타한 날", "${n(stats.passed.outingDays.toLong())}일", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PassedCard(stats: ServiceStats, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            StatSectionTitle("지나간 군생활")
            PlanRow("지난 날", "${n(stats.passed.days.toLong())}일 / 전체 ${n(stats.totalDays.toLong())}일")
            PlanRow("부대에서 보낸 날", "${n(stats.passed.onBaseDays.toLong())}일")
            PlanRow("먹은 식사", "${n(stats.passed.meals.toLong())}끼")
            PlanRow("출타한 날", "${stats.passed.outingDays}일 (${"%.1f".format(Locale.US, stats.outingRatioPercent)}%)")
            PlanRow("지난 주말 · 공휴일", "${stats.passed.weekendDays}일 · ${stats.passed.publicHolidays}일")
            PlanRow("휴가로 보낸 날", "${stats.leaveDays.past}일")
            PlanRow("외박 · 외출", "${stats.overnightCount.past}회 · ${stats.passCount.past}회")
            if (stats.combatRestDays.past > 0) PlanRow("전투휴무", "${stats.combatRestDays.past}일")
            dutySummary(stats.dutyCounts) { it.past }?.let { PlanRow("근무", it) }
        }
    }
}

@Composable
private fun RemainingCard(uiState: DashboardUiState, stats: ServiceStats, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            StatSectionTitle("남은 군생활")
            PlanRow("남은 날", "${n(stats.remaining.days.toLong())}일")
            PlanRow("주 · 개월로", remainingSpanText(stats))
            PlanRow("남은 식사", "${n(stats.remaining.meals.toLong())}끼")
            PlanRow("남은 밤", "${n(stats.remainingNights.toLong())}번")
            PlanRow("남은 주말 · 공휴일", "${stats.remaining.weekendDays}일 · ${stats.remaining.publicHolidays}일")
            PlanRow("쉬는 날(주말+공휴일)", "${stats.remaining.restDays}일")
            PlanRow("출타 예정", "${stats.remaining.outingDays}일 (휴가 ${stats.leaveDays.upcoming}일 포함)")
            PlanRow("남은 휴가(계획 전)", "${uiState.leaveSummaries.sumOf { it.remaining }}일")
            uiState.remainingOvernightCount?.let { PlanRow("전역까지 남은 외박", "${it}회") }
            if (stats.passCount.upcoming > 0) PlanRow("예정된 외출", "${stats.passCount.upcoming}회")
            if (stats.combatRestDays.upcoming > 0) PlanRow("예정된 전투휴무", "${stats.combatRestDays.upcoming}일")
            dutySummary(stats.dutyCounts) { it.upcoming }?.let { PlanRow("예정된 근무", it) }
            stats.nextPromotion?.let { promotion ->
                PlanRow(
                    "${promotion.rank.label} 진급까지 부대 생활",
                    "${promotion.onBaseDays}일 (전체 ${promotion.days}일)",
                )
            }
        }
    }
}

/** 숫자 하나. 대시보드의 보낸/남은 시간과 같은 글자 크기·배치(라벨 위, 굵은 값 아래). */
@Composable
private fun HeadlineStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    }
}

/** 카드 소제목. 휴가 관리 카드의 "부여 내역" 같은 카드 안 소제목과 같은 모양. */
@Composable
private fun StatSectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
}

/** 근무 종류별 횟수를 `당직 3회 · 비번 3회`처럼. 한 번도 없으면 null(줄을 숨긴다). */
private fun dutySummary(counts: Map<DutyType, PastAndUpcoming>, pick: (PastAndUpcoming) -> Int): String? =
    DutyType.entries
        .mapNotNull { type -> pick(counts[type] ?: PastAndUpcoming(0, 0)).takeIf { it > 0 }?.let { "${dutyLabel(type)} ${it}회" } }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ")

private fun dutyLabel(type: DutyType): String = when (type) {
    DutyType.DUTY -> "당직"
    DutyType.OFF_DUTY -> "비번"
    DutyType.MESS -> "츄라이"
}

/** 남은 기간을 `12주 3일 · 2개월 26일`로. */
private fun remainingSpanText(stats: ServiceStats): String {
    fun part(whole: Int, unit: String, extra: Int) = if (extra > 0) "$whole$unit ${extra}일" else "$whole$unit"
    return "${part(stats.remainingWeeks, "주", stats.remainingWeeksExtraDays)} · " +
        part(stats.remainingMonths, "개월", stats.remainingMonthsExtraDays)
}

/** 진행 정도에 맞춘 한 줄 문구. */
private fun funFact(stats: ServiceStats): String {
    val percent = stats.progressPercent.toInt()
    return when {
        stats.remaining.days == 0 -> "전역을 축하합니다!"
        stats.passed.days == 0 -> "이제 시작이에요. 남은 날을 하나씩 지워 가요"
        percent >= 50 -> "군생활의 $percent%를 지났어요. 이제 내리막이에요"
        else -> "군생활의 $percent%를 지났어요"
    }
}
