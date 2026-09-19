package com.kangminsang.hagoondori.ui.duty

import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.PassRecord
import com.kangminsang.hagoondori.core.model.UserProfile

/** ④ 근무 일괄 입력(스펙 5.1/5.2절, F6) 화면 상태. */
data class DutyBulkInputUiState(
    val profile: UserProfile? = null,
    val holidays: List<Holiday> = emptyList(),
    val existingAssignments: List<DutyAssignment> = emptyList(),
    val existingPasses: List<PassRecord> = emptyList(),
)
