package com.kangminsang.hagoondori.data.remote.holiday

import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 한국천문연구원 "특일 정보" API(공공데이터포털, service id `B090041`)의
 * `getRestDeInfo` 오퍼레이션 - 대체공휴일을 포함한 실제 공휴일 목록을 반환한다
 * (스펙 4.14절 1순위: 정확하고 대체공휴일 포함).
 *
 * 응답을 특정 데이터 클래스로 강하게 매핑하지 않고 [JsonElement]로 그대로 받는다:
 * 이 API의 정확한 응답 스키마(특히 결과 0/1건일 때 형태가 바뀌는 것)를 이
 * 개발 환경에서는 실호출로 검증하지 못했기 때문에, 실제 해석은 core 모듈의
 * [com.kangminsang.hagoondori.core.holiday.HolidayApiPayloadParser](단위 테스트로
 * 검증됨)에 위임한다. 실제 키로 첫 호출을 해봤을 때 필드명이 다르면 그 파서만
 * 고치면 되고, 이 인터페이스는 그대로 둘 수 있다.
 */
interface HolidayApiService {
    @GET("getRestDeInfo")
    suspend fun getRestDeInfo(
        /**
         * 공공데이터포털에서 발급받은 서비스키(Decoding 키 권장). 사용자가 설정 화면에서
         * 입력해 [HolidayApiKeyStore]에 보관한 값이다(빌드에 넣지 않는다). Retrofit이 표준적으로 URL 인코딩하므로, 이미 퍼센트 인코딩된
         * "Encoding" 키를 그대로 쓰면 이중 인코딩될 수 있다 - Decoding 키를 쓸 것.
         */
        @Query("serviceKey") serviceKey: String,
        @Query("solYear") solYear: Int,
        @Query("numOfRows") numOfRows: Int = 100,
        @Query("_type") type: String = "json",
    ): JsonElement

    companion object {
        const val BASE_URL = "https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/"
    }
}
