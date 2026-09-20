package com.kangminsang.hagoondori.core.holiday

/**
 * 공공데이터포털이 오류 때 돌려주는 본문(JSON 또는 XML)에서 사유를 뽑아 사용자에게 보여줄
 * 안내문으로 바꾼다. 인증 오류는 HTTP 401/403과 함께 `returnReasonCode`, `errMsg`,
 * `returnAuthMsg`가 오는데, 상태 코드만 보면 "403 Forbidden"이라 원인을 알 수 없다.
 */
object HolidayApiErrorDescriber {

    fun describe(httpStatus: Int, body: String?): String {
        val text = body.orEmpty()
        val code = field(text, "returnReasonCode")
        val errMsg = field(text, "errMsg")
        val authMsg = field(text, "returnAuthMsg")

        val hint = when (code) {
            "30" -> "포털이 이 키를 모릅니다. 활용 신청 직후에는 반영까지 1~2시간(길면 하루) 걸릴 수 있고, 그 뒤에도 같으면 키가 잘못 복사됐는지 확인하세요"
            "20" -> "이 서비스(특일 정보)의 활용 신청이 승인되지 않았거나 키가 전달되지 않았습니다"
            "31" -> "인증키 사용 기간이 만료됐습니다"
            "22" -> "하루 호출 한도를 넘었습니다. 내일 다시 시도하세요"
            "10", "11" -> "요청 형식이 잘못됐습니다"
            "12" -> "서비스가 종료됐거나 주소가 바뀌었습니다"
            else -> null
        }
        val detail = listOfNotNull(authMsg, errMsg).joinToString(" / ").ifBlank { null }

        return buildString {
            append("HTTP $httpStatus")
            if (detail != null) append(" - $detail")
            if (code != null) append(" (코드 $code)")
            if (hint != null) append(". $hint")
        }
    }

    /** JSON(`"key": "value"`)과 XML(`<key>value</key>`) 어느 쪽이든 값을 찾는다. */
    private fun field(text: String, key: String): String? {
        Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(text)?.let { return it.groupValues[1].ifBlank { null } }
        Regex("<$key>([^<]*)</$key>").find(text)?.let { return it.groupValues[1].trim().ifBlank { null } }
        return null
    }
}
