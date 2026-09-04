package com.kangminsang.hagoondori.core.holiday

import com.kangminsang.hagoondori.core.model.Holiday
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/**
 * 한국천문연구원 "특일 정보" API(공공데이터포털, `getRestDeInfo` 오퍼레이션 - 대체공휴일을
 * 포함한 실제 공휴일 목록)의 JSON 응답을 [Holiday] 목록으로 파싱한다(스펙 4.14절).
 *
 * **이 파서를 core에 둔 이유**: 네트워크 호출/Retrofit 배선 자체는 app 계층(Android)의
 * 몫이지만, "JSON을 어떻게 해석할지"는 문자열 입력 → 모델 출력의 순수 함수다. 이
 * 개발 환경은 Android SDK가 없어 app 모듈은 빌드조차 검증할 수 없지만, 이 파서를
 * core에 두면 이 세션에서도 실제로 단위 테스트할 수 있다 - 공공데이터포털 API를
 * 이 세션에서 실호출로 검증할 방법이 없는 상황에서, 적어도 "알려진 응답 형태를
 * 정확히 해석하는지"만큼은 검증해 둔다.
 *
 * 이 API의 정확한 응답 스키마는 이 세션에서 실호출로 검증하지 못했다 - 알려진
 * 문서/사례를 근거로 방어적으로 작성했다. 실제 키로 첫 호출을 해보고 필드명이
 * 다르면 이 파일만 고치면 된다(Retrofit 쪽은 원시 [JsonElement]를 그대로 넘기도록
 * 설계해 이 파서와만 맞물리게 했다).
 *
 * 알려진 정상 응답 형태:
 * ```json
 * { "response": { "header": {...}, "body": { "items": { "item": [ {...}, {...} ] } } } }
 * ```
 * - 결과가 1건뿐이면 `item`이 배열이 아니라 객체 하나로 오는 경우가 흔하다 - 처리한다.
 * - 결과가 0건이면 `items`가 객체가 아니라 빈 문자열 `""`로 오는 경우가 흔하다
 *   (공공데이터포털 XML→JSON 변환의 잘 알려진 특성) - 정상적으로 빈 목록 처리한다.
 * - `dateName`에 "대체"가 포함되어 있으면 대체공휴일로 판정한다(예: "대체공휴일").
 */
object HolidayApiPayloadParser {

    fun parse(root: JsonElement): List<Holiday> {
        val itemsElement = root.asObjectOrNull()
            ?.get("response")?.asObjectOrNull()
            ?.get("body")?.asObjectOrNull()
            ?.get("items")
            ?: return emptyList()

        val itemObjects: List<JsonObject> = when (itemsElement) {
            is JsonObject -> when (val item = itemsElement["item"]) {
                is JsonArray -> item.mapNotNull { it as? JsonObject }
                is JsonObject -> listOf(item)
                else -> emptyList()
            }
            else -> emptyList() // 결과 0건일 때의 "" 등 예상 밖 형태
        }

        return itemObjects.mapNotNull(::parseItem)
    }

    private fun parseItem(item: JsonObject): Holiday? {
        val locdate = item["locdate"]?.asPrimitiveOrNull()?.longOrNull ?: return null
        val name = item["dateName"]?.asPrimitiveOrNull()?.contentOrNull ?: return null
        val date = parseLocdate(locdate) ?: return null
        return Holiday(date = date, name = name, isSubstitute = "대체" in name)
    }

    /** `locdate`는 `YYYYMMDD` 형태의 정수다(예: `20260925` → 2026-09-25). */
    private fun parseLocdate(locdate: Long): LocalDate? {
        val text = locdate.toString()
        if (text.length != 8) return null
        val year = text.substring(0, 4).toIntOrNull() ?: return null
        val month = text.substring(4, 6).toIntOrNull() ?: return null
        val day = text.substring(6, 8).toIntOrNull() ?: return null
        return runCatching { LocalDate(year, month, day) }.getOrNull()
    }

    private fun JsonElement.asObjectOrNull(): JsonObject? = this as? JsonObject
    private fun JsonElement.asPrimitiveOrNull(): JsonPrimitive? = this as? JsonPrimitive
}
