package com.kangminsang.hagoondori.core.holiday

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HolidayApiErrorDescriberTest {

    @Test
    fun `json unregistered key body is explained`() {
        val body = """{"OpenAPI_ServiceResponse":{"cmmMsgHeader":{"errMsg":"SERVICE_KEY_IS_NOT_REGISTERED_ERROR","returnAuthMsg":"등록되지 않은 서비스키","returnReasonCode":"30"}}}"""
        val text = HolidayApiErrorDescriber.describe(403, body)
        assertTrue(text.startsWith("HTTP 403 - 등록되지 않은 서비스키 / SERVICE_KEY_IS_NOT_REGISTERED_ERROR (코드 30)"), text)
        assertTrue("1~2시간" in text, text)
    }

    @Test
    fun `xml body is explained`() {
        val body = "<OpenAPI_ServiceResponse><cmmMsgHeader><errMsg>SERVICE_KEY_IS_NULL</errMsg>" +
            "<returnAuthMsg>서비스 접근거부</returnAuthMsg><returnReasonCode>20</returnReasonCode></cmmMsgHeader></OpenAPI_ServiceResponse>"
        val text = HolidayApiErrorDescriber.describe(401, body)
        assertTrue("서비스 접근거부" in text && "코드 20" in text, text)
    }

    @Test
    fun `unknown or empty body falls back to the status alone`() {
        assertEquals("HTTP 500", HolidayApiErrorDescriber.describe(500, null))
        assertEquals("HTTP 502", HolidayApiErrorDescriber.describe(502, "<html>Bad Gateway</html>"))
    }
}
