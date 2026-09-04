package com.kangminsang.hagoondori.core.export

import com.kangminsang.hagoondori.core.model.CombatRestUsage
import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.Event
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.UserProfile
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * 특정 시점의 전체 데이터를 담은 읽기 전용 묶음 (스펙 6.2절).
 *
 * app의 내부 구조(Room 엔티티 등)를 전송 계층에 그대로 넘기지 않고, 앱 내부 구조가
 * 바뀌어도 전송 계층이 깨지지 않도록 이 스냅샷 하나로 경계를 긋는다. 색상, 픽셀
 * 좌표, 레이아웃 정보는 절대 담지 않는다(6.4절) — 그건 렌더링 단계의 일이다.
 *
 * 어떤 필드를 담을지는 7.1절 "계산 주체 분담" 원칙을 따른다: 날짜가 바뀌면 값이
 * 매일 변하는 것(D-day, 진행률, 오늘 날짜 등)은 장치가 스스로 계산하므로 여기 담지
 * 않는다. 동기화 시점에 고정되는 값(다음 외박 예정일, 공휴일 목록, 일정/근무/휴가
 * 기간 원본 등)만 담는다.
 */
data class CalendarSnapshot(
    /** 이 스냅샷을 만든 시각(=페이로드 생성 시각) */
    val generatedAt: Instant,
    val rangeStart: LocalDate,
    val rangeEnd: LocalDate,
    val profile: UserProfile,
    /**
     * 다음 외박 예정일. 차수 매칭 알고리즘이 복잡하고 결과가 날짜와 무관하므로
     * (동기화 이후 날짜가 바뀌어도 재계산할 필요가 없으므로) 앱이 계산해 넣는다(7.1절).
     * 아직 첫 외박일이 설정되지 않았다면 null.
     */
    val nextOvernightDate: LocalDate?,
    val holidays: List<Holiday>,
    val events: List<Event>,
    val leaveUsages: List<LeaveUsage>,
    val combatRestUsages: List<CombatRestUsage>,
    val dutyAssignments: List<DutyAssignment>,
) {
    init {
        require(rangeEnd >= rangeStart) { "rangeEnd는 rangeStart보다 앞설 수 없다" }
    }
}
