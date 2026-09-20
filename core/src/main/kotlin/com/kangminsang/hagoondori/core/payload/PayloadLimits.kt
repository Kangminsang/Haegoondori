package com.kangminsang.hagoondori.core.payload

/** 장치 RAM 한계에서 실측으로 정한 페이로드 크기 상한 (calendar.txt 전송 명세 v5, 8절). */
object PayloadLimits {
    /** 하드 상한(`Z` 줄 포함). 넘으면 장치가 조용히 갱신에 실패할 수 있어 앱이 쓰지 않는다. */
    const val MAX_BYTES = 16_000
}
