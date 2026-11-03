package com.kangminsang.hagoondori.core.calc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LeaveTypeValidatorTest {

    private val others = listOf("정기휴가", "포상휴가", "위로휴가")

    @Test
    fun `valid name with cap or fixed days and no duplicate has no errors`() {
        assertTrue(LeaveTypeValidator.validate("청원휴가", cap = 10, fixedDays = null, otherNames = others).isValid)
        assertTrue(LeaveTypeValidator.validate("청원휴가", cap = null, fixedDays = 5, otherNames = others).isValid)
        assertTrue(LeaveTypeValidator.validate("청원휴가", cap = null, fixedDays = null, otherNames = others).isValid)
    }

    @Test
    fun `blank name is EMPTY_NAME only`() {
        assertEquals(
            listOf(LeaveTypeError.EMPTY_NAME),
            LeaveTypeValidator.validate("   ", cap = null, fixedDays = null, otherNames = others).errors,
        )
        assertEquals(
            listOf(LeaveTypeError.EMPTY_NAME),
            LeaveTypeValidator.validate("", cap = null, fixedDays = null, otherNames = others).errors,
        )
    }

    @Test
    fun `name duplicating another type after trimming is DUPLICATE_NAME`() {
        assertEquals(
            listOf(LeaveTypeError.DUPLICATE_NAME),
            LeaveTypeValidator.validate("  포상휴가 ", cap = null, fixedDays = null, otherNames = others).errors,
        )
    }

    @Test
    fun `other names are trimmed before comparing`() {
        assertEquals(
            listOf(LeaveTypeError.DUPLICATE_NAME),
            LeaveTypeValidator.validate("청원휴가", cap = null, fixedDays = null, otherNames = listOf(" 청원휴가 ")).errors,
        )
    }

    @Test
    fun `name that the device cannot render is UNRENDERABLE_NAME`() {
        assertEquals(
            listOf(LeaveTypeError.UNRENDERABLE_NAME),
            LeaveTypeValidator.validate("휴가🎖", cap = null, fixedDays = null, otherNames = others).errors,
        )
    }

    @Test
    fun `pipe and line break in name are rejected`() {
        assertEquals(
            listOf(LeaveTypeError.NAME_CONTAINS_PIPE),
            LeaveTypeValidator.validate("청|원", cap = null, fixedDays = null, otherNames = others).errors,
        )
        assertEquals(
            listOf(LeaveTypeError.NAME_HAS_LINE_BREAK),
            LeaveTypeValidator.validate("청\n원", cap = null, fixedDays = null, otherNames = others).errors,
        )
    }

    @Test
    fun `zero or negative cap is INVALID_CAP`() {
        assertEquals(
            listOf(LeaveTypeError.INVALID_CAP),
            LeaveTypeValidator.validate("청원휴가", cap = 0, fixedDays = null, otherNames = others).errors,
        )
        assertEquals(
            listOf(LeaveTypeError.INVALID_CAP),
            LeaveTypeValidator.validate("청원휴가", cap = -3, fixedDays = null, otherNames = others).errors,
        )
    }

    @Test
    fun `zero or negative fixed days is INVALID_FIXED_DAYS`() {
        assertEquals(
            listOf(LeaveTypeError.INVALID_FIXED_DAYS),
            LeaveTypeValidator.validate("청원휴가", cap = null, fixedDays = 0, otherNames = others).errors,
        )
        assertEquals(
            listOf(LeaveTypeError.INVALID_FIXED_DAYS),
            LeaveTypeValidator.validate("청원휴가", cap = null, fixedDays = -1, otherNames = others).errors,
        )
    }

    @Test
    fun `cap and fixed days together is CAP_AND_FIXED_DAYS_TOGETHER`() {
        assertEquals(
            listOf(LeaveTypeError.CAP_AND_FIXED_DAYS_TOGETHER),
            LeaveTypeValidator.validate("청원휴가", cap = 17, fixedDays = 27, otherNames = others).errors,
        )
    }

    @Test
    fun `several problems at once are all reported`() {
        assertEquals(
            listOf(
                LeaveTypeError.DUPLICATE_NAME,
                LeaveTypeError.INVALID_CAP,
                LeaveTypeError.INVALID_FIXED_DAYS,
                LeaveTypeError.CAP_AND_FIXED_DAYS_TOGETHER,
            ),
            LeaveTypeValidator.validate("정기휴가", cap = 0, fixedDays = 0, otherNames = others).errors,
        )
    }
}
