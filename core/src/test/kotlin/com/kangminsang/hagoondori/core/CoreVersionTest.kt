package com.kangminsang.hagoondori.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CoreVersionTest {
    @Test
    fun `payload format version matches spec section 7_2`() {
        assertEquals(1, PAYLOAD_FORMAT_VERSION)
    }
}
