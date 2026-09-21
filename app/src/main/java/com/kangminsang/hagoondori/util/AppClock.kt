package com.kangminsang.hagoondori.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * 이 앱은 한국 해군 복무자 전용이므로 시간대를 항상 Asia/Seoul로 고정한다
 * ([com.kangminsang.hagoondori.core.payload.PayloadEncoder]가 생성 시각을
 * 인코딩할 때 쓰는 오프셋과 동일한 기준이다 - "오늘"의 정의가 앱 전체에서 하나여야 한다).
 */
object AppClock {
    val timeZone: TimeZone = TimeZone.of("Asia/Seoul")

    fun now(): Instant = Clock.System.now()

    fun today(): LocalDate = now().toLocalDateTime(timeZone).date

    /**
     * 오늘 날짜를 내보내고, 날짜가 바뀔 때마다(자정) 새 날짜를 다시 내보낸다. D-day처럼 날짜에서 파생되는
     * 화면 상태를 앱을 켜 둔 채로도 자정에 맞춰 다시 계산하려는 용도다. 다음 자정까지 통째로 기다리지 않고
     * 최대 [maxWaitMillis]마다 시각을 다시 확인한다 - 절전 등으로 타이머가 늦게 깨어나거나 기기 시각이
     * 바뀌어도 늦어도 그 안에 바로잡힌다.
     */
    fun todayFlow(maxWaitMillis: Long = 60_000L): Flow<LocalDate> = flow {
        while (true) {
            val nowInstant = now()
            val date = nowInstant.toLocalDateTime(timeZone).date
            emit(date)
            val nextMidnight = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
            delay((nextMidnight - nowInstant).inWholeMilliseconds.coerceIn(1L, maxWaitMillis) + 50L)
        }
    }.distinctUntilChanged()
}
