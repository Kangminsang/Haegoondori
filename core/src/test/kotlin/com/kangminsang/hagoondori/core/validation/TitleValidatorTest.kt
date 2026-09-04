package com.kangminsang.hagoondori.core.validation

import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class TitleValidatorTest {

    @Test
    fun `10 chars or fewer is Ok`() {
        assertInstanceOf(TitleValidation.Ok::class.java, TitleValidator.validate("일이삼사오육칠팔구십")) // 10자
    }

    @Test
    fun `11 to 12 chars is a Warning`() {
        assertInstanceOf(TitleValidation.Warning::class.java, TitleValidator.validate("일이삼사오육칠팔구십일")) // 11자
        assertInstanceOf(TitleValidation.Warning::class.java, TitleValidator.validate("일이삼사오육칠팔구십일이")) // 12자
    }

    @Test
    fun `13 chars or more is Invalid`() {
        assertInstanceOf(TitleValidation.Invalid::class.java, TitleValidator.validate("일이삼사오육칠팔구십일이삼")) // 13자
    }

    @Test
    fun `pipe character is always Invalid regardless of length`() {
        assertInstanceOf(TitleValidation.Invalid::class.java, TitleValidator.validate("a|b"))
    }

    @Test
    fun `newline is always Invalid`() {
        assertInstanceOf(TitleValidation.Invalid::class.java, TitleValidator.validate("a\nb"))
        assertInstanceOf(TitleValidation.Invalid::class.java, TitleValidator.validate("a\rb"))
    }
}
