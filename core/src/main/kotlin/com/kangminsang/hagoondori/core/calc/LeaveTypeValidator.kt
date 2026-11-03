package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.validation.HangulRenderableValidator

/**
 * 휴가 종류 입력 오류 하나. 한 입력에서 여러 오류가 동시에 나올 수 있으므로 UI는 목록 전체를 보여 준다.
 * [message]는 입력란 아래에 그대로 띄울 문구다.
 */
enum class LeaveTypeError(val message: String) {
    /** 이름이 비어 있거나 공백뿐이다. */
    EMPTY_NAME("이름을 입력하세요"),

    /** 다른 휴가 종류와 같은 이름이다(앞뒤 공백을 뗀 뒤 비교). */
    DUPLICATE_NAME("같은 이름의 휴가 종류가 이미 있습니다"),

    /** 장치 폰트가 그릴 수 없는 문자가 있다(KS X 1001 완성형 한글과 ASCII만 가능). */
    UNRENDERABLE_NAME("장치 화면에 표시할 수 없는 문자가 있습니다"),

    /** `|`는 장치 전송 형식의 구분자라 이름에 쓸 수 없다. */
    NAME_CONTAINS_PIPE("이름에 '|' 문자를 쓸 수 없습니다 (장치 전송 형식과 충돌)"),

    /** 이름에 줄바꿈을 넣을 수 없다. */
    NAME_HAS_LINE_BREAK("이름에 줄바꿈을 넣을 수 없습니다"),

    /** 상한 일수는 지정할 때 1 이상이어야 한다. */
    INVALID_CAP("상한 일수는 1 이상이어야 합니다"),

    /** 고정 일수는 지정할 때 1 이상이어야 한다. */
    INVALID_FIXED_DAYS("고정 일수는 1 이상이어야 합니다"),

    /** 상한과 고정 일수는 동시에 지정할 수 없다(고정이면 상한이 필요 없다). */
    CAP_AND_FIXED_DAYS_TOGETHER("상한과 고정 일수는 함께 지정할 수 없습니다"),
}

/** [LeaveTypeValidator.validate]의 결과. 오류가 없으면 [isValid]가 참이다. */
data class LeaveTypeValidation(val errors: List<LeaveTypeError>) {
    val isValid: Boolean get() = errors.isEmpty()
}

/**
 * 휴가 종류 추가·수정 입력 검증. UI가 저장 버튼을 누르기 전에 호출한다.
 *
 * 이름은 장치 라벨(앞 두 글자)과 전송 형식에 그대로 들어가므로, 장치가 그릴 수 있는 문자와
 * 전송 형식의 구분자 규칙을 함께 본다. 수정할 때는 자기 자신의 이름을 [otherNames]에서 빼고 넘긴다.
 */
object LeaveTypeValidator {

    /**
     * @param name 입력한 이름(앞뒤 공백은 검사 전에 뗀다)
     * @param cap 상한 일수. 상한이 없으면 null
     * @param fixedDays 고정 총량 일수. 부여 기록을 합산하는 종류면 null
     * @param otherNames 같은 휴가 목록의 다른 종류 이름들(수정 중인 종류는 제외)
     */
    fun validate(
        name: String,
        cap: Int?,
        fixedDays: Int?,
        otherNames: Collection<String>,
    ): LeaveTypeValidation {
        val errors = mutableListOf<LeaveTypeError>()
        val trimmed = name.trim()

        when {
            trimmed.isEmpty() -> errors += LeaveTypeError.EMPTY_NAME
            else -> {
                if ('|' in trimmed) errors += LeaveTypeError.NAME_CONTAINS_PIPE
                if ('\n' in trimmed || '\r' in trimmed) {
                    errors += LeaveTypeError.NAME_HAS_LINE_BREAK
                } else if (!HangulRenderableValidator.isRenderable(trimmed)) {
                    // 줄바꿈은 위에서 따로 알리므로 여기서는 이중으로 세지 않는다.
                    errors += LeaveTypeError.UNRENDERABLE_NAME
                }
                if (otherNames.any { it.trim() == trimmed }) errors += LeaveTypeError.DUPLICATE_NAME
            }
        }

        if (cap != null && cap < 1) errors += LeaveTypeError.INVALID_CAP
        if (fixedDays != null && fixedDays < 1) errors += LeaveTypeError.INVALID_FIXED_DAYS
        if (cap != null && fixedDays != null) errors += LeaveTypeError.CAP_AND_FIXED_DAYS_TOGETHER

        return LeaveTypeValidation(errors)
    }
}
