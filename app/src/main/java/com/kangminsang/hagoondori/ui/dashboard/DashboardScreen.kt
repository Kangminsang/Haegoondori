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
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.kangminsang.hagoondori.core.calc.DDayCalculator
import com.kangminsang.hagoondori.core.calc.DayCountFormatter
import com.kangminsang.hagoondori.core.calc.DayCountUnit
import com.kangminsang.hagoondori.core.calc.PastAndUpcoming
import com.kangminsang.hagoondori.core.calc.ServiceStats
import com.kangminsang.hagoondori.core.model.DutyType
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

    // "숫자를 누르면 단위가 바뀐다"는 안내는 한 번 눌러 보기 전까지만 보여준다(앱을 다시 켜도 유지).
    val context = LocalContext.current
    val hintPrefs = remember(context) { context.getSharedPreferences(UI_PREFS, Context.MODE_PRIVATE) }
    var showUnitHint by remember { mutableStateOf(!hintPrefs.getBoolean(KEY_UNIT_HINT_SEEN, false)) }
    val onUnitToggled: () -> Unit = {
        if (showUnitHint) {
            showUnitHint = false
            hintPrefs.edit().putBoolean(KEY_UNIT_HINT_SEEN, true).apply()
        }
    }

    CompositionLocalProvider(LocalUnitToggled provides onUnitToggled) {
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
                        pendingChanges = uiState.pendingChanges,
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

                item {
                    ServiceProgressCard(uiState, showUnitHint, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                item { NextOutingCard(uiState, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                item { LeaveSummaryCard(uiState, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                item { CombatRestSummaryCard(uiState, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                item { ServiceStatsCard(uiState, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            }
        }
    }
}

private const val UI_PREFS = "dashboard_ui"
private const val KEY_UNIT_HINT_SEEN = "unit_toggle_hint_seen"

/** 단위 전환 숫자를 누를 때 알릴 곳. 안내 문구를 숨기는 데 쓴다. */
private val LocalUnitToggled = staticCompositionLocalOf<() -> Unit> { {} }

/** 숫자마다 따로 기억하는 표시 단위. 화면을 돌리거나 다른 탭에 다녀와도 유지된다. */
@Composable
private fun rememberDayCountUnit(): MutableState<DayCountUnit> = rememberSaveable { mutableStateOf(DayCountUnit.DAY) }

/** 누를 때마다 [unit]을 일 → 시간 → 분 → 일로 바꾼다. 눌리는 영역이 보이도록 살짝 둥근 물결 효과를 준다. */
@Composable
private fun Modifier.toggleUnitOnClick(unit: MutableState<DayCountUnit>): Modifier {
    val onToggled = LocalUnitToggled.current
    return clip(RoundedCornerShape(4.dp)).clickable(onClickLabel = "시간 단위 바꾸기") {
        unit.value = unit.value.next()
        onToggled()
    }
}

/**
 * [target]까지의 D-day를 [unit]으로. 시간·분일 때는 그 날짜 0시까지 실제로 남은 시간이라 계속 줄어들므로,
 * 그때만 1초마다 다시 그린다.
 */
@Composable
private fun dDayLabel(today: LocalDate, target: LocalDate, unit: DayCountUnit): String {
    val dDay = DDayCalculator.dDay(today, target)
    if (unit == DayCountUnit.DAY) return DayCountFormatter.formatDDay(dDay, 0L, unit)
    val now by rememberLiveNow(intervalMillis = 1_000L)
    return DayCountFormatter.formatDDay(dDay, DDayCalculator.millisUntil(target, now, AppClock.timeZone), unit)
}

/** 단위가 길어질수록(시간·분) 글자를 줄여 한 줄에 들어가게 한다. */
private fun TextStyle.shrinkFor(unit: DayCountUnit, daySize: Int, hourSize: Int, minuteSize: Int): TextStyle {
    val size = when (unit) {
        DayCountUnit.DAY -> daySize
        DayCountUnit.HOUR -> hourSize
        DayCountUnit.MINUTE -> minuteSize
    }
    return copy(fontSize = size.sp, lineHeight = (size + 4).sp)
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
private fun ServiceProgressCard(uiState: DashboardUiState, showUnitHint: Boolean, modifier: Modifier = Modifier) {
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
                    val dischargeUnit = rememberDayCountUnit()
                    Text(
                        dDayLabel(today, profile.dischargeDate, dischargeUnit.value),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            .shrinkFor(dischargeUnit.value, daySize = 52, hourSize = 40, minuteSize = 32),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.toggleUnitOnClick(dischargeUnit),
                    )
                    if (showUnitHint) {
                        Text(
                            "남은 날짜를 누르면 시간·분으로 바뀌어요",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
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
                        val promotionUnit = rememberDayCountUnit()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.toggleUnitOnClick(promotionUnit),
                        ) {
                            Text(
                                dDayLabel(today, nextPromotion.date, promotionUnit.value),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    .shrinkFor(promotionUnit.value, daySize = 22, hourSize = 15, minuteSize = 13),
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
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
                // 종류마다 따로 단위를 기억한다(목록 순서가 바뀌어도 섞이지 않게 종류 id를 키로 쓴다).
                val leaveUnit = rememberSaveable(summary.type.id) { mutableStateOf(DayCountUnit.DAY) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(summary.type.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${DayCountFormatter.formatAmount(summary.remaining, leaveUnit.value)} 남음",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.toggleUnitOnClick(leaveUnit),
                    )
                }
                if (summary.planned > 0) {
                    val planned = DayCountFormatter.formatAmount(summary.planned, leaveUnit.value)
                    val after = DayCountFormatter.formatAmount(summary.remainingAfterPlanned, leaveUnit.value)
                    Text(
                        "예정 $planned (모두 쓰면 $after 남음)",
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
            val unit = rememberDayCountUnit()
            fun amount(days: Int) = DayCountFormatter.formatAmount(days, unit.value)
            Text(
                "${amount(summary.remaining)} (전환 ${amount(summary.convertedFromLeave)} + 부여 ${amount(summary.directGranted)})",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp).toggleUnitOnClick(unit),
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
                // 출타 중이 되면 기준 날짜가 바뀌므로 단위 기억도 따로 둔다.
                val dDayUnit = rememberSaveable(ongoing) { mutableStateOf(DayCountUnit.DAY) }
                Text(
                    dDayLabel(today, target, dDayUnit.value),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        .shrinkFor(dDayUnit.value, daySize = 40, hourSize = 34, minuteSize = 30),
                    maxLines = 1,
                    modifier = Modifier.toggleUnitOnClick(dDayUnit),
                )
                Text(
                    if (outing.start == outing.end) shortDate(outing.start) else "${shortDate(outing.start)} ~ ${shortDate(outing.end)}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (outing.totalDays > 1) {
                    val lengthUnit = rememberDayCountUnit()
                    val total = if (lengthUnit.value == DayCountUnit.DAY) {
                        "${outing.totalDays}일간"
                    } else {
                        DayCountFormatter.formatAmount(outing.totalDays, lengthUnit.value)
                    }
                    val composition = if (outing.parts.size > 1) {
                        val parts = outing.parts.joinToString(" ") {
                            "${it.label} ${DayCountFormatter.groupThousands(DayCountFormatter.convertDays(it.days.toLong(), lengthUnit.value))}"
                        }
                        "$total($parts)"
                    } else {
                        total
                    }
                    Text(
                        composition,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp).toggleUnitOnClick(lengthUnit),
                    )
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
            val passUnit = rememberDayCountUnit()
            val passText = when {
                pass == null -> "예정 없음"
                pass == today -> "${shortDate(pass)}  오늘"
                else -> "${shortDate(pass)}  ${dDayLabel(today, pass, passUnit.value)}"
            }
            PlanRow(
                label = "다음 외출",
                value = passText,
                valueModifier = if (pass != null && pass > today) Modifier.toggleUnitOnClick(passUnit) else Modifier,
            )
        }
    }
}

@Composable
private fun PlanRow(label: String, value: String, valueModifier: Modifier = Modifier) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = valueModifier)
    }
}

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

/**
 * 군생활 통계. 접힌 상태에서는 눈에 띄는 숫자 네 개만 보여주고, "더 보기"를 누르면 지나간/남은 군생활로
 * 나눈 나머지 통계를 펼친다. 값은 모두 [ServiceStats]에서 오며 여기서는 글자로만 바꾼다.
 */
@Composable
private fun ServiceStatsCard(uiState: DashboardUiState, modifier: Modifier = Modifier) {
    val stats = uiState.serviceStats ?: return
    var expanded by rememberSaveable { mutableStateOf(false) }
    val n = DayCountFormatter::groupThousands

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("군생활 통계", style = MaterialTheme.typography.titleMedium)
            Text(
                funFact(stats),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 2.dp),
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
                HeadlineStat("남은 식사", "${n(stats.remainingMeals.toLong())}끼", Modifier.weight(1f))
                StatDivider()
                HeadlineStat(
                    "지금까지 출타한 날",
                    "${n(stats.passed.outingDays.toLong())}일",
                    Modifier.weight(1f),
                )
            }

            if (expanded) {
                StatSectionTitle("지나간 군생활")
                StatRow("지난 날", "${n(stats.passed.days.toLong())}일 / 전체 ${n(stats.totalDays.toLong())}일")
                StatRow("부대에서 보낸 날", "${n(stats.passed.onBaseDays.toLong())}일")
                StatRow("출타한 날", "${stats.passed.outingDays}일 (${"%.1f".format(Locale.US, stats.outingRatioPercent)}%)")
                StatRow("지난 주말 · 공휴일", "${stats.passed.weekendDays}일 · ${stats.passed.publicHolidays}일")
                StatRow("휴가로 보낸 날", "${stats.leaveDays.past}일")
                StatRow("외박 · 외출", "${stats.overnightCount.past}회 · ${stats.passCount.past}회")
                if (stats.combatRestDays.past > 0) StatRow("전투휴무", "${stats.combatRestDays.past}일")
                dutySummary(stats.dutyCounts) { it.past }?.let { StatRow("근무", it) }

                StatSectionTitle("남은 군생활")
                StatRow("남은 날", "${n(stats.remaining.days.toLong())}일")
                StatRow("주 · 개월로", remainingSpanText(stats))
                StatRow("남은 밤", "${n(stats.remainingNights.toLong())}번")
                StatRow("남은 주말 · 공휴일", "${stats.remaining.weekendDays}일 · ${stats.remaining.publicHolidays}일")
                StatRow("쉬는 날(주말+공휴일)", "${stats.remaining.restDays}일")
                StatRow("출타 예정", "${stats.remaining.outingDays}일 (휴가 ${stats.leaveDays.upcoming}일 포함)")
                StatRow("남은 휴가(계획 전)", "${uiState.leaveSummaries.sumOf { it.remaining }}일")
                uiState.remainingOvernightCount?.let { StatRow("전역까지 남은 외박", "${it}회") }
                if (stats.passCount.upcoming > 0) StatRow("예정된 외출", "${stats.passCount.upcoming}회")
                if (stats.combatRestDays.upcoming > 0) StatRow("예정된 전투휴무", "${stats.combatRestDays.upcoming}일")
                dutySummary(stats.dutyCounts) { it.upcoming }?.let { StatRow("예정된 근무", it) }
                stats.nextPromotion?.let { promotion ->
                    StatRow(
                        "${promotion.rank.label} 진급까지 부대 생활",
                        "${promotion.onBaseDays}일 (전체 ${promotion.days}일)",
                    )
                }
            }

            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
            ) { Text(if (expanded) "접기" else "더 보기") }
        }
    }
}

@Composable
private fun HeadlineStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun StatSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
    }
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
