package com.kangminsang.hagoondori.data.util

import java.util.UUID

/** 새 레코드에 부여할 고유 id. core 모델의 id는 전부 String이므로 UUID 문자열을 쓴다. */
object IdGenerator {
    fun newId(): String = UUID.randomUUID().toString()
}
