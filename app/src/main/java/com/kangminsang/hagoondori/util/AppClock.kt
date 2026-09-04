package com.kangminsang.hagoondori.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
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
}
