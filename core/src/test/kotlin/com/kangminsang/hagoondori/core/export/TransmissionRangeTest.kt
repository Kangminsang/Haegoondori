package com.kangminsang.hagoondori.core.export

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TransmissionRangeTest {

    @Test
    fun `시작은 동기화 날짜가 속한 달의 1일`() {
        assertEquals(LocalDate(2026, 9, 1), TransmissionRange.start(LocalDate(2026, 9, 19)))
        assertEquals(LocalDate(2026, 9, 1), TransmissionRange.start(LocalDate(2026, 9, 1)))
        assertEquals(LocalDate(2026, 1, 1), TransmissionRange.start(LocalDate(2026, 1, 31)))
    }

    @Test
    fun `끝은 180일 뒤가 속한 달의 말일 - 명세 예시`() =
        assertEquals(LocalDate(2027, 3, 31), TransmissionRange.end(LocalDate(2026, 9, 19)))

    @Test
    fun `끝이 2월이면 평년 28일 윤년 29일`() {
        assertEquals(LocalDate(2027, 2, 28), TransmissionRange.end(LocalDate(2026, 8, 20)))
        assertEquals(LocalDate(2028, 2, 29), TransmissionRange.end(LocalDate(2027, 8, 20)))
    }

    @Test
    fun `끝이 12월이면 연말`() =
        assertEquals(LocalDate(2026, 12, 31), TransmissionRange.end(LocalDate(2026, 6, 20)))
}
