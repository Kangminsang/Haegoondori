package com.kangminsang.hagoondori.core.model

import kotlinx.datetime.LocalDate

/**
 * 일반 일정 (스펙 4.12절).
 *
 * [title] 제약(7.5절 폰트 결정에 따름):
 * - 10자 초과 시 경고, **12자 초과는 입력 자체가 차단**되므로 여기서도 하드 제약으로 둔다.
 * - `|`와 개행 문자는 페이로드 구분자와 충돌하므로 항상 금지한다.
 * - 10자 초과~12자 이하의 "경고" 판정과 사용자向 메시지는
 *   [com.kangminsang.hagoondori.core.validation.TitleValidator]가 담당한다(입력 단계에서 먼저 걸러짐).
 *
 * 시각(몇 시)은 저장하지 않는다 — 필요하면 [memo]에 적는다.
 */
data class Event(
    val id: String,
    val title: String,
    val startDate: LocalDate,
    /** 종료일. 없으면 하루짜리 일정 */
    val endDate: LocalDate? = null,
    val isImportant: Boolean = false,
    /** 부가 메모. 장치에는 표시되지 않는다 */
    val memo: String? = null,
) {
    init {
        require(id.isNotBlank()) { "id는 비어 있을 수 없다" }
        require(title.isNotBlank()) { "title은 비어 있을 수 없다" }
        require(title.length <= MAX_TITLE_LENGTH) {
            "title은 ${MAX_TITLE_LENGTH}자를 초과할 수 없다: \"$title\" (${title.length}자)"
        }
        require('|' !in title) { "title에 '|' 문자를 넣을 수 없다 (페이로드 구분자와 충돌)" }
        require('\n' !in title && '\r' !in title) { "title에 개행 문자를 넣을 수 없다" }
        endDate?.let { require(it >= startDate) { "endDate는 startDate보다 앞설 수 없다" } }
    }

    companion object {
        /** 10자 초과 시 경고 임계값 (TitleValidator가 사용) */
        const val WARN_TITLE_LENGTH = 10

        /** 12자 초과 입력은 차단된다 */
        const val MAX_TITLE_LENGTH = 12
    }
}
