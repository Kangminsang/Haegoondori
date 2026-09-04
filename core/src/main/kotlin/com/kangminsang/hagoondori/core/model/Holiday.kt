package com.kangminsang.hagoondori.core.model

import kotlinx.datetime.LocalDate

/**
 * 공휴일 정보 (스펙 4.14절).
 *
 * 확보 우선순위: (1) 한국천문연구원 특일 정보 API(공공데이터포털) (2) 앱 내장 데이터
 * (3) 사용자 수동 입력. 어느 경로로 얻었든 반드시 로컬에 캐시한다(원칙 B — 서버 없음,
 * 오프라인 동작). 이 캐시/네트워크 관심사는 app 레이어의 몫이며, core는 순수 데이터만 다룬다.
 */
data class Holiday(
    val date: LocalDate,
    val name: String,
    val isSubstitute: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "name은 비어 있을 수 없다" }
    }
}
