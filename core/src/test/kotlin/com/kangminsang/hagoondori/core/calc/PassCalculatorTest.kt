package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.PassRecord
import com.kangminsang.hagoondori.core.model.PassType
import com.kangminsang.hagoondori.core.model.UserProfile
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PassCalculatorTest {

    private val profile = UserProfile(
        enlistmentDate = LocalDate(2025, 3, 3),
        dischargeDate = LocalDate(2026, 9, 2),
        promotionDate = null,
        firstOvernightDate = null,
        wakeUpTime = LocalTime(5, 45),
        dinnerTime = LocalTime(17, 30),
        autoAddOffDuty = true,
    )

    @Test
    fun `weekday and holiday allowances are independent`() {
        assertEquals(2, PassCalculator.allowance(profile, PassType.WEEKDAY))
        assertEquals(1, PassCalculator.allowance(profile, PassType.HOLIDAY))
    }

    @Test
    fun `usedInMonth counts only matching year, month and type`() {
        val records = listOf(
            PassRecord("p1", LocalDate(2026, 9, 1), PassType.WEEKDAY),
            PassRecord("p2", LocalDate(2026, 9, 15), PassType.WEEKDAY),
            PassRecord("p3", LocalDate(2026, 9, 6), PassType.HOLIDAY), // 일요일
            PassRecord("p4", LocalDate(2026, 10, 2), PassType.WEEKDAY), // 다음 달
            PassRecord("p5", LocalDate(2025, 9, 1), PassType.WEEKDAY), // 작년 같은 달
        )
        assertEquals(2, PassCalculator.usedInMonth(records, 2026, 9, PassType.WEEKDAY))
        assertEquals(1, PassCalculator.usedInMonth(records, 2026, 9, PassType.HOLIDAY))
    }

    @Test
    fun `remainingInMonth reflects allowance minus used`() {
        val records = listOf(
            PassRecord("p1", LocalDate(2026, 9, 1), PassType.WEEKDAY),
        )
        assertEquals(1, PassCalculator.remainingInMonth(profile, records, 2026, 9, PassType.WEEKDAY))
        assertEquals(1, PassCalculator.remainingInMonth(profile, records, 2026, 9, PassType.HOLIDAY))
    }

    @Test
    fun `no carryover - fully using previous month does not affect this month`() {
        val records = listOf(
            PassRecord("p1", LocalDate(2026, 8, 3), PassType.WEEKDAY),
            PassRecord("p2", LocalDate(2026, 8, 4), PassType.WEEKDAY),
        )
        // 8월에 평일 외출을 2회(한도) 다 썼지만, 9월 잔여에는 영향이 없다
        assertEquals(2, PassCalculator.remainingInMonth(profile, records, 2026, 9, PassType.WEEKDAY))
        assertEquals(0, PassCalculator.remainingInMonth(profile, records, 2026, 8, PassType.WEEKDAY))
    }

    @Test
    fun `classifyType matches HolidayJudge - weekend and holiday both classify as HOLIDAY`() {
        val holidays = listOf(Holiday(LocalDate(2026, 9, 25), "추석"))
        assertEquals(PassType.WEEKDAY, PassCalculator.classifyType(LocalDate(2026, 9, 4), holidays)) // 금
        assertEquals(PassType.HOLIDAY, PassCalculator.classifyType(LocalDate(2026, 9, 5), holidays)) // 토
        assertEquals(PassType.HOLIDAY, PassCalculator.classifyType(LocalDate(2026, 9, 25), holidays)) // 공휴일
    }

    @Test
    fun `classifyType is a point-in-time decision - changing the holiday list later does not retroactively change it`() {
        // 이 테스트는 계약을 문서화한다: classifyType 호출 시점의 holidays로만 판정하며,
        // 이후 holidays가 바뀌어도 이미 저장된 PassRecord.type은 그대로 유지된다
        // (그 유지 자체는 PassRecord가 값을 필드로 갖고 있다는 사실 자체로 보장된다).
        val beforeUpdate = emptyList<Holiday>()
        val classifiedBefore = PassCalculator.classifyType(LocalDate(2026, 9, 25), beforeUpdate)
        assertEquals(PassType.WEEKDAY, classifiedBefore) // 아직 공휴일로 등록되기 전

        val afterUpdate = listOf(Holiday(LocalDate(2026, 9, 25), "추석"))
        val record = PassRecord("p1", LocalDate(2026, 9, 25), classifiedBefore)
        // 나중에 공휴일 데이터가 갱신돼도 이미 만든 record.type은 바뀌지 않는다
        assertEquals(PassType.WEEKDAY, record.type)
        // (참고용) 새로 판정하면 이제는 HOLIDAY가 나온다는 것만 확인
        assertEquals(PassType.HOLIDAY, PassCalculator.classifyType(LocalDate(2026, 9, 25), afterUpdate))
    }
}
