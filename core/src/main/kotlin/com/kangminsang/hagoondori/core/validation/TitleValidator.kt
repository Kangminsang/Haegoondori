package com.kangminsang.hagoondori.core.validation

import com.kangminsang.hagoondori.core.model.Event

/** [TitleValidator.validate]의 결과. UI가 이 값을 보고 경고 문구를 띄우거나 입력을 막는다. */
sealed class TitleValidation {
    data object Ok : TitleValidation()
    data class Warning(val message: String) : TitleValidation()
    data class Invalid(val reason: String) : TitleValidation()
}

/**
 * 일정 제목 입력 검증 (스펙 4.12절 / 7.5절).
 *
 * [Event]의 생성자는 이미 이 규칙을 하드 제약(초과 12자, `|`, 개행)으로 강제하지만,
 * 이 객체는 그 판정을 **[Event]를 실제로 만들기 전에** UI에서 미리 보여주기 위한
 * 것이다 — 특히 10자 초과 12자 이하 구간은 Event 생성자로는 표현할 수 없는
 * "경고이지만 허용"의 소프트 판정이다.
 */
object TitleValidator {
    fun validate(title: String): TitleValidation {
        if ('|' in title) {
            return TitleValidation.Invalid("제목에 '|' 문자를 쓸 수 없습니다 (장치 전송 형식과 충돌)")
        }
        if ('\n' in title || '\r' in title) {
            return TitleValidation.Invalid("제목에 줄바꿈을 넣을 수 없습니다")
        }
        return when {
            title.length > Event.MAX_TITLE_LENGTH ->
                TitleValidation.Invalid(
                    "제목은 ${Event.MAX_TITLE_LENGTH}자를 초과할 수 없습니다 (현재 ${title.length}자)"
                )

            title.length > Event.WARN_TITLE_LENGTH ->
                TitleValidation.Warning(
                    "제목이 ${Event.WARN_TITLE_LENGTH}자를 넘으면 장치 화면에서 잘릴 수 있습니다 (현재 ${title.length}자)"
                )

            else -> TitleValidation.Ok
        }
    }
}
