package com.kangminsang.hagoondori.core.holiday

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HolidayApiPayloadParserTest {

    @Test
    fun `parses multiple items as an array`() {
        val json = """
            {
              "response": {
                "header": { "resultCode": "00", "resultMsg": "NORMAL_SERVICE" },
                "body": {
                  "items": {
                    "item": [
                      { "dateKind": "01", "dateName": "신정", "isHoliday": "Y", "locdate": 20260101, "seq": 1 },
                      { "dateKind": "01", "dateName": "추석", "isHoliday": "Y", "locdate": 20260925, "seq": 2 }
                    ]
                  },
                  "numOfRows": 100, "pageNo": 1, "totalCount": 2
                }
              }
            }
        """.trimIndent()

        val holidays = HolidayApiPayloadParser.parse(Json.parseToJsonElement(json))

        assertEquals(2, holidays.size)
        assertEquals(LocalDate(2026, 1, 1), holidays[0].date)
        assertEquals("신정", holidays[0].name)
        assertEquals(false, holidays[0].isSubstitute)
        assertEquals(LocalDate(2026, 9, 25), holidays[1].date)
        assertEquals("추석", holidays[1].name)
    }

    @Test
    fun `parses a single item as an object, not an array`() {
        // 공공데이터포털류 API에서 결과가 1건이면 item이 배열이 아니라 객체 하나로 온다.
        val json = """
            {
              "response": {
                "body": {
                  "items": {
                    "item": { "dateName": "삼일절", "locdate": 20260301 }
                  }
                }
              }
            }
        """.trimIndent()

        val holidays = HolidayApiPayloadParser.parse(Json.parseToJsonElement(json))

        assertEquals(1, holidays.size)
        assertEquals(LocalDate(2026, 3, 1), holidays.single().date)
        assertEquals("삼일절", holidays.single().name)
    }

    @Test
    fun `substitute holiday is detected from the name containing a substitution marker`() {
        val json = """
            {
              "response": {
                "body": {
                  "items": {
                    "item": [
                      { "dateName": "설날", "locdate": 20260216 },
                      { "dateName": "대체공휴일", "locdate": 20260217 }
                    ]
                  }
                }
              }
            }
        """.trimIndent()

        val holidays = HolidayApiPayloadParser.parse(Json.parseToJsonElement(json))

        assertEquals(false, holidays[0].isSubstitute)
        assertEquals(true, holidays[1].isSubstitute)
    }

    @Test
    fun `empty result where items is an empty string yields an empty list, not a crash`() {
        // 공공데이터포털 XML-to-JSON 변환의 잘 알려진 특성: 결과 0건이면 items가
        // 객체가 아니라 빈 문자열로 온다.
        val json = """
            { "response": { "body": { "items": "", "totalCount": 0 } } }
        """.trimIndent()

        val holidays = HolidayApiPayloadParser.parse(Json.parseToJsonElement(json))

        assertTrue(holidays.isEmpty())
    }

    @Test
    fun `missing body or items yields an empty list`() {
        assertTrue(HolidayApiPayloadParser.parse(Json.parseToJsonElement("""{"response":{}}""")).isEmpty())
        assertTrue(HolidayApiPayloadParser.parse(Json.parseToJsonElement("""{}""")).isEmpty())
    }

    @Test
    fun `an item missing locdate or dateName is skipped rather than crashing the whole batch`() {
        val json = """
            {
              "response": {
                "body": {
                  "items": {
                    "item": [
                      { "dateName": "정상", "locdate": 20260101 },
                      { "dateName": "날짜없음" },
                      { "locdate": 20260102 }
                    ]
                  }
                }
              }
            }
        """.trimIndent()

        val holidays = HolidayApiPayloadParser.parse(Json.parseToJsonElement(json))

        assertEquals(1, holidays.size)
        assertEquals("정상", holidays.single().name)
    }

    @Test
    fun `malformed locdate that is not 8 digits is skipped`() {
        val json = """
            { "response": { "body": { "items": { "item": { "dateName": "이상함", "locdate": 12345 } } } } }
        """.trimIndent()

        assertTrue(HolidayApiPayloadParser.parse(Json.parseToJsonElement(json)).isEmpty())
    }
}
