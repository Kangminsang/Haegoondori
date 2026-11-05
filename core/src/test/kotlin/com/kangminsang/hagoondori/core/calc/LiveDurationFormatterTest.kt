package com.kangminsang.hagoondori.core.calc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LiveDurationFormatterTest {

    // 224일 22시간 53분 26초 + 0.9초(밀리초는 버린다)
    private val sample = ((224L * 86_400 + 22 * 3_600 + 53 * 60 + 26) * 1000) + 900

    @Test
    fun `next cycles day to hour to minute to second and back to day`() {
        assertEquals(LiveDurationUnit.HOUR, LiveDurationUnit.DAY.next())
        assertEquals(LiveDurationUnit.MINUTE, LiveDurationUnit.HOUR.next())
        assertEquals(LiveDurationUnit.SECOND, LiveDurationUnit.MINUTE.next())
        assertEquals(LiveDurationUnit.DAY, LiveDurationUnit.SECOND.next())
    }

    @Test
    fun `formats each unit with the remainder down to seconds`() {
        assertEquals("224일 22:53:26", LiveDurationFormatter.format(sample, LiveDurationUnit.DAY))
        assertEquals("5,398시간 53:26", LiveDurationFormatter.format(sample, LiveDurationUnit.HOUR))
        assertEquals("323,933분 26초", LiveDurationFormatter.format(sample, LiveDurationUnit.MINUTE))
        assertEquals("19,436,006초", LiveDurationFormatter.format(sample, LiveDurationUnit.SECOND))
    }

    @Test
    fun `day unit keeps the original format and groups large day counts`() {
        assertEquals("0일 00:00:59", LiveDurationFormatter.format(59_999, LiveDurationUnit.DAY))
        assertEquals("1,000일 01:02:03", LiveDurationFormatter.format((1_000L * 86_400 + 3_723) * 1000, LiveDurationUnit.DAY))
    }

    @Test
    fun `small values are not padded in the leading unit`() {
        assertEquals("0시간 01:05", LiveDurationFormatter.format(65_000, LiveDurationUnit.HOUR))
        assertEquals("1분 05초", LiveDurationFormatter.format(65_000, LiveDurationUnit.MINUTE))
        assertEquals("65초", LiveDurationFormatter.format(65_000, LiveDurationUnit.SECOND))
    }

    @Test
    fun `zero and negative durations clamp to zero`() {
        LiveDurationUnit.entries.forEach { unit ->
            assertEquals(LiveDurationFormatter.format(0, unit), LiveDurationFormatter.format(-5_000, unit))
        }
        assertEquals("0일 00:00:00", LiveDurationFormatter.format(0, LiveDurationUnit.DAY))
        assertEquals("0시간 00:00", LiveDurationFormatter.format(-1, LiveDurationUnit.HOUR))
        assertEquals("0분 00초", LiveDurationFormatter.format(-1, LiveDurationUnit.MINUTE))
        assertEquals("0초", LiveDurationFormatter.format(Long.MIN_VALUE, LiveDurationUnit.SECOND))
    }

    @Test
    fun `groupThousands handles boundaries and negatives`() {
        assertEquals("0", LiveDurationFormatter.groupThousands(0))
        assertEquals("999", LiveDurationFormatter.groupThousands(999))
        assertEquals("1,000", LiveDurationFormatter.groupThousands(1_000))
        assertEquals("1,234,567", LiveDurationFormatter.groupThousands(1_234_567))
        assertEquals("-12,345", LiveDurationFormatter.groupThousands(-12_345))
    }
}
