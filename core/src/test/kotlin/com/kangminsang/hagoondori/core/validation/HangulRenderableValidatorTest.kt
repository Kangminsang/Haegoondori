package com.kangminsang.hagoondori.core.validation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HangulRenderableValidatorTest {

    @Test
    fun `generated table has exactly 2350 characters - self-check against the KS X1001 standard`() {
        assertEquals(2350, HangulRenderableValidator.allowedHangulCount)
    }

    @Test
    fun `ascii printable characters are renderable`() {
        assertTrue(HangulRenderableValidator.isRenderable('A'))
        assertTrue(HangulRenderableValidator.isRenderable('0'))
        assertTrue(HangulRenderableValidator.isRenderable(' '))
        assertTrue(HangulRenderableValidator.isRenderable("Duty 2026-09-04"))
    }

    @Test
    fun `common hangul syllables are renderable`() {
        assertTrue(HangulRenderableValidator.isRenderable("가"))
        assertTrue(HangulRenderableValidator.isRenderable("군"))
        assertTrue(HangulRenderableValidator.isRenderable("면회 진급 심사"))
    }

    @Test
    fun `a syllable outside the 2350-character wansung set is not renderable`() {
        // "뷁"은 KS X1001 완성형 2350자에 포함되지 않는 대표적인 예시(조합형에는 존재).
        assertFalse(HangulRenderableValidator.isRenderable('뷁'))
    }

    @Test
    fun `hanja and emoji are not renderable`() {
        assertFalse(HangulRenderableValidator.isRenderable('軍')) // 한자
        assertFalse(HangulRenderableValidator.isRenderable("😀".first())) // 이모지(서로게이트 페어 중 한쪽 char)
    }

    @Test
    fun `findUnrenderable reports index and character of each offending character`() {
        val result = HangulRenderableValidator.findUnrenderable("휴가뷁")
        assertEquals(listOf(2 to '뷁'), result)
    }

    @Test
    fun `findUnrenderable returns empty list when everything is renderable`() {
        assertTrue(HangulRenderableValidator.findUnrenderable("정상 제목 123").isEmpty())
    }
}
