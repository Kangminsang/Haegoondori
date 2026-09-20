package com.kangminsang.hagoondori.ui.duty

import com.kangminsang.hagoondori.core.model.DutyType

/** 화면에 보이는 근무 종류 이름. 영어 상수명(DUTY 등)을 사용자에게 그대로 보이지 않기 위한 단일 출처. */
fun dutyTypeLabel(type: DutyType): String = when (type) {
    DutyType.DUTY -> "당직"
    DutyType.OFF_DUTY -> "비번"
    DutyType.MESS -> "츄라이(식사당번)"
}

/** 달력 칸처럼 좁은 곳에 쓰는 한 글자 표기 (장치 화면의 칩 글자와 같다). */
fun dutyTypeShortLabel(type: DutyType): String = when (type) {
    DutyType.DUTY -> "당"
    DutyType.OFF_DUTY -> "비"
    DutyType.MESS -> "츄"
}
