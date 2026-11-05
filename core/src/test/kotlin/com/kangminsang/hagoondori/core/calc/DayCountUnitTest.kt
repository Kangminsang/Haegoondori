package com.kangminsang.hagoondori.core.calc

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DayCountUnitTest {

    @Test
    fun `next cycles day to hour to minute and back to day`() {
        assertEquals(DayCountUnit.HOUR, DayCountUnit.DAY.next())
        assertEquals(DayCountUnit.MINUTE, DayCountUnit.HOUR.next())
        assertEquals(DayCountUnit.DAY, DayCountUnit.MINUTE.next())
    }

    @Test
    fun `formatAmount converts whole days with thousands separators`() {
        assertEquals("13일", DayCountFormatter.formatAmount(13, DayCountUnit.DAY))
        assertEquals("312시간", DayCountFormatter.formatAmount(13, DayCountUnit.HOUR))
        assertEquals("18,720분", DayCountFormatter.formatAmount(13, DayCountUnit.MINUTE))
        assertEquals("5,376시간", DayCountFormatter.formatAmount(224, DayCountUnit.HOUR))
        assertEquals("0분", DayCountFormatter.formatAmount(0, DayCountUnit.MINUTE))
    }

    @Test
    fun `groupThousands handles boundaries and negatives`() {
        assertEquals("0", DayCountFormatter.groupThousands(0))
        assertEquals("999", DayCountFormatter.groupThousands(999))
        assertEquals("1,000", DayCountFormatter.groupThousands(1_000))
        assertEquals("1,234,567", DayCountFormatter.groupThousands(1_234_567))
        assertEquals("-12,345", DayCountFormatter.groupThousands(-12_345))
    }

    @Test
    fun `formatDDay in days keeps the familiar D notation`() {
        assertEquals("D-224", DayCountFormatter.formatDDay(224, 0, DayCountUnit.DAY))
        assertEquals("D-0", DayCountFormatter.formatDDay(0, 0, DayCountUnit.DAY))
        assertEquals("D+3", DayCountFormatter.formatDDay(-3, 0, DayCountUnit.DAY))
    }

    @Test
    fun `formatDDay in hours uses real time to the target midnight, not days times 24`() {
        val zone = TimeZone.of("Asia/Seoul")
        val target = LocalDate(2026, 11, 15)
        // 11/5 18:30 → 11/15 0시: 9일 5시간 30분 = 221.5시간 → 내림 221시간
        val now = LocalDateTime(2026, 11, 5, 18, 30).toInstant(zone)
        val millis = DDayCalculator.millisUntil(target, now, zone)
        val dDay = DDayCalculator.dDay(LocalDate(2026, 11, 5), target)
        assertEquals("D-10", DayCountFormatter.formatDDay(dDay, millis, DayCountUnit.DAY))
        assertEquals("221시간", DayCountFormatter.formatDDay(dDay, millis, DayCountUnit.HOUR))
        assertEquals("13,290분", DayCountFormatter.formatDDay(dDay, millis, DayCountUnit.MINUTE))
    }

    @Test
    fun `formatDDay on the day itself shows zero and past targets read as elapsed`() {
        assertEquals("0시간", DayCountFormatter.formatDDay(0, -5 * 3_600_000L, DayCountUnit.HOUR))
        assertEquals("72시간 지남", DayCountFormatter.formatDDay(-3, -72 * 3_600_000L, DayCountUnit.HOUR))
        assertEquals("4,320분 지남", DayCountFormatter.formatDDay(-3, -72 * 3_600_000L, DayCountUnit.MINUTE))
    }

    @Test
    fun `millisUntil is signed while remainingMillis clamps at zero`() {
        val zone = TimeZone.of("Asia/Seoul")
        val now = LocalDateTime(2026, 11, 5, 12, 0).toInstant(zone)
        assertEquals(-12 * 3_600_000L, DDayCalculator.millisUntil(LocalDate(2026, 11, 5), now, zone))
        assertEquals(0L, DDayCalculator.remainingMillis(LocalDate(2026, 11, 5), now, zone))
        assertEquals(12 * 3_600_000L, DDayCalculator.millisUntil(LocalDate(2026, 11, 6), now, zone))
    }
}
