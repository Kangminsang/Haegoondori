package com.kangminsang.hagoondori.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import com.kangminsang.hagoondori.ui.common.EInkRing
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import com.kangminsang.hagoondori.ui.common.Card
import androidx.compose.material3.CircularProgressIndicator
import com.kangminsang.hagoondori.ui.common.EInkGauge
import com.kangminsang.hagoondori.ui.common.GaugeMarker
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kangminsang.hagoondori.core.calc.DDayCalculator
import com.kangminsang.hagoondori.core.calc.OutingPeriodCalculator
import com.kangminsang.hagoondori.ui.common.SyncStatusBanner
import com.kangminsang.hagoondori.util.AppClock
import java.util.Locale
import com.kangminsang.hagoondori.core.model.UserProfile
import com.kangminsang.hagoondori.ui.common.rememberLiveNow
import com.kangminsang.hagoondori.ui.common.FixedWidthDigits
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * ① 대시보드 (스펙 5.1/5.2절, F2) - 앱을 여는 주된 이유.
 * 전역/진급 D-day + 진행률, 휴가·전투휴무 요약,
 * 다음 출타(외박+휴가)·외출, 동기화 상태 배너를 한 화면에 모은다.
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
            item { NextOutingCard(uiState, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            item { LeaveSummaryCard(uiState, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            item { CombatRestSummaryCard(uiState, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
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
    val profile = uiState.profile ?: return
    val today = uiState.today ?: return

    // 휴대폰은 화면 갱신 제약이 없으므로, 처음 나타날 때 0에서 현재 값까지 차오르게 한다.
    val target = (uiState.serviceProgressPercent / 100.0).toFloat()
    var shown by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(target) { shown = target }
    val progress by animateFloatAsState(shown, animationSpec = tween(durationMillis = 900), label = "serviceProgress")

    // 진급 링: 직전 진급(또는 입대)부터 다음 진급까지 중 지금까지 온 만큼.
    val promotionTarget = uiState.nextPromotion?.let { (it.progressPercent / 100.0).toFloat() } ?: 0f
    var promotionShown by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(promotionTarget) { promotionShown = promotionTarget }
    val promotionProgress by animateFloatAsState(promotionShown, animationSpec = tween(durationMillis = 900), label = "promotionProgress")

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("전역까지", style = MaterialTheme.typography.titleMedium)
                    val dDayText = uiState.dischargeDDay?.let { dDayText(it) } ?: "-"
                    Text(
                        dDayText,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 52.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        "${profile.enlistmentDate.dotted()} → ${profile.dischargeDate.dotted()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                // 남은 진급이 없으면 링 자리를 비운다(전역 진행률은 아래 막대와 실시간 퍼센트가 이미 보여준다).
                uiState.nextPromotion?.let { nextPromotion ->
                    EInkRing(
                        progress = promotionProgress,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(104.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                dDayText(nextPromotion.dDay),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text("${nextPromotion.label} 진급", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            EInkGauge(
                progress = progress,
                markers = uiState.promotionMarkers.map { GaugeMarker((it.progressPercent / 100.0).toFloat(), it.label, it.passed) },
                modifier = Modifier.padding(top = 16.dp),
            )
            LiveProgressPercent(profile, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveTimeStat(
                    label = "보낸 시간",
                    modifier = Modifier.weight(1f),
                ) { now -> DDayCalculator.elapsedMillis(profile.enlistmentDate, now, AppClock.timeZone) }
                StatDivider()
                LiveTimeStat(
                    label = "남은 시간",
                    modifier = Modifier.weight(1f),
                ) { now -> DDayCalculator.remainingMillis(profile.dischargeDate, now, AppClock.timeZone) }
            }
        }
    }
}

/**
 * 복무 진행률을 소수점 아래 8자리까지 실시간으로 보여준다. 하루에 0.16%가량 오르는 값이지만, 아래
 * 자릿수는 초당 여러 번 바뀌므로 숫자가 쉬지 않고 차오르는 것처럼 보인다.
 */
@Composable
private fun LiveProgressPercent(profile: UserProfile, modifier: Modifier = Modifier) {
    val now by rememberLiveNow()
    val percent = DDayCalculator.liveServiceProgress(profile.enlistmentDate, profile.dischargeDate, now, AppClock.timeZone)
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text("복무 진행률", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        FixedWidthDigits(
            text = "%.8f".format(Locale.US, percent),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            digitWidth = 11.sp,
        )
        Text("%", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    }
}

/**
 * 밀리초 값을 `224일 22:53:26`처럼 초 단위로 흐르게 보여준다. 보낸 시간은 올라가고 남은 시간은 줄어들며,
 * 둘이 동시에 움직이는 것 자체가 시간이 흐르고 있다는 느낌을 준다.
 */
@Composable
private fun LiveTimeStat(
    label: String,
    modifier: Modifier = Modifier,
    millisAt: (Instant) -> Long,
) {
    val now by rememberLiveNow(intervalMillis = 200L)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        FixedWidthDigits(
            text = formatDuration(millisAt(now)),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            digitWidth = 11.sp,
        )
    }
}

/** 밀리초를 `224일 22:53:26`처럼 표시한다. */
private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val days = totalSeconds / 86_400
    val hours = totalSeconds % 86_400 / 3_600
    val minutes = totalSeconds % 3_600 / 60
    val seconds = totalSeconds % 60
    return "%d일 %02d:%02d:%02d".format(days, hours, minutes, seconds)
}

@Composable
private fun StatDivider() {
    Box(Modifier.width(1.5.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
}

private fun LocalDate.dotted(): String = toString().replace('-', '.')

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
                if (summary.planned > 0) {
                    Text(
                        "예정 ${summary.planned}일 (모두 쓰면 ${summary.remainingAfterPlanned}일 남음)",
                        style = MaterialTheme.typography.bodySmall,
                    )
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
private fun NextOutingCard(uiState: DashboardUiState, modifier: Modifier = Modifier) {
    val today = uiState.today ?: return
    val now by rememberLiveNow(intervalMillis = 250L)
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("다음 출타", style = MaterialTheme.typography.titleMedium)

            val outing = uiState.nextOuting
            if (outing == null) {
                Text(
                    "예정된 외박·휴가 없음",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                // 시작일 오전 8시(정문 통과 가능 시각)부터 출타 중으로 본다.
                val departure = OutingPeriodCalculator.departureAt(outing, AppClock.timeZone)
                val ongoing = now >= departure
                val target = if (ongoing) outing.end else outing.start
                val names = outing.parts.joinToString("·") { it.label }
                Text(
                    if (ongoing) "$names 종료까지" else "${names}까지",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    dDayText(DDayCalculator.dDay(today, target)),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    if (outing.start == outing.end) shortDate(outing.start) else "${shortDate(outing.start)} ~ ${shortDate(outing.end)}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (outing.totalDays > 1) {
                    val composition = if (outing.parts.size > 1) {
                        "${outing.totalDays}일간(${outing.parts.joinToString(" ") { "${it.label} ${it.days}" }})"
                    } else {
                        "${outing.totalDays}일간"
                    }
                    Text(composition, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                }
                // 출타 전에는 출발까지 남은 시간이 줄어들고, 나간 뒤에는 나가 있은 시간이 올라간다.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (ongoing) "출타 후 경과" else "출타까지 (${OutingPeriodCalculator.GATE_OPEN_TIME.hour}시 출발)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    FixedWidthDigits(
                        text = formatDuration(
                            if (ongoing) (now - departure).inWholeMilliseconds else (departure - now).inWholeMilliseconds,
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        digitWidth = 11.sp,
                    )
                }
            }

            val pass = uiState.nextPass
            val passText = when {
                pass == null -> "예정 없음"
                pass == today -> "${shortDate(pass)}  오늘"
                else -> "${shortDate(pass)}  ${dDayText(DDayCalculator.dDay(today, pass))}"
            }
            PlanRow(label = "다음 외출", value = passText)
        }
    }
}

@Composable
private fun PlanRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun dDayText(d: Int): String = if (d >= 0) "D-$d" else "D+${-d}"

/** 장치 일정 행과 같은 `M/D(요일)` 표기. */
private fun shortDate(date: LocalDate): String {
    val dow = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }
    return "${date.monthNumber}/${date.dayOfMonth}($dow)"
}
