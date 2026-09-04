package com.kangminsang.hagoondori.core.validation

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * 장치가 렌더링할 수 있는 문자 집합 판정 (스펙 7.5절).
 *
 * 장치 폰트는 **KS X 1001 완성형 한글 2350자 + ASCII**만 담고 있다. 이 문자 집합을
 * 별도 데이터 파일로 들고 다니는 대신, JDK가 이 표준을 정확히 구현하는 EUC-KR
 * 코덱을 내장하고 있다는 점을 이용해 런타임에 정확한 테이블을 만든다.
 *
 * KS X 1001 완성형 한글 영역은 EUC-KR에서 lead byte `0xB0`~`0xC8`(25개),
 * trail byte `0xA1`~`0xFE`(94개) 범위를 차지하며, 25 × 94 = **2350자**로
 * 스펙 수치와 정확히 일치한다([allowedHangulCount]를 테스트에서 검증한다).
 *
 * 이 집합 밖의 문자(희귀 한자, 조합형 전용 음절, 이모지 등)가 입력되면 장치는
 * 대체 문자로 표시한다 — 앱은 그 전에 경고해야 한다.
 */
object HangulRenderableValidator {

    private const val LEAD_BYTE_START = 0xB0
    private const val LEAD_BYTE_END = 0xC8
    private const val TRAIL_BYTE_START = 0xA1
    private const val TRAIL_BYTE_END = 0xFE

    /** ASCII 출력 가능 문자 범위 (공백~물결표). */
    private val asciiPrintableRange = 0x20..0x7E

    private val allowedHangul: Set<Char> by lazy { buildAllowedHangulTable() }

    private fun buildAllowedHangulTable(): Set<Char> {
        val decoder = Charset.forName("EUC-KR").newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)

        val result = LinkedHashSet<Char>()
        for (lead in LEAD_BYTE_START..LEAD_BYTE_END) {
            for (trail in TRAIL_BYTE_START..TRAIL_BYTE_END) {
                val bytes = byteArrayOf(lead.toByte(), trail.toByte())
                // decode(ByteBuffer)는 한 번 호출로 reset→decode→flush를 수행하므로
                // 디코더를 매번 새로 만들 필요가 없다.
                val decoded = runCatching { decoder.decode(ByteBuffer.wrap(bytes)) }.getOrNull()
                if (decoded != null && decoded.length == 1) {
                    result.add(decoded[0])
                }
            }
        }
        return result
    }

    /** ASCII 출력 가능 문자이거나, KS X1001 완성형 한글 2350자 중 하나인지. */
    fun isRenderable(char: Char): Boolean =
        char.code in asciiPrintableRange || char in allowedHangul

    fun isRenderable(text: String): Boolean = text.all(::isRenderable)

    /** 렌더링 불가능한 문자들의 (인덱스, 문자) 목록. 전부 렌더링 가능하면 빈 리스트. */
    fun findUnrenderable(text: String): List<Pair<Int, Char>> =
        text.withIndex()
            .filterNot { (_, c) -> isRenderable(c) }
            .map { (index, c) -> index to c }

    /** 생성된 완성형 한글 테이블의 크기. 정확히 2350이어야 한다(테스트에서 자체 검증). */
    val allowedHangulCount: Int get() = allowedHangul.size
}
