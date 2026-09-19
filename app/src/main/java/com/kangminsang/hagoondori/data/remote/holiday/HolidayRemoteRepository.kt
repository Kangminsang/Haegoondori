package com.kangminsang.hagoondori.data.remote.holiday

import com.kangminsang.hagoondori.core.holiday.BuiltInHolidaySeed
import com.kangminsang.hagoondori.core.holiday.HolidayApiErrorDescriber
import com.kangminsang.hagoondori.core.holiday.HolidayApiPayloadParser
import com.kangminsang.hagoondori.data.repository.HolidayRepository
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/** [HolidayRemoteRepository.refreshYear] 결과 - 설정 화면이 사용자에게 보여줄 안내에 쓰인다. */
sealed class HolidayFetchResult {
    data class Success(val year: Int, val count: Int) : HolidayFetchResult()
    data class UsedBuiltInFallback(val year: Int, val reason: String) : HolidayFetchResult()
}

/**
 * 공휴일 확보 전략 전체(스펙 4.14절 우선순위)를 조율한다:
 * 1) 공공데이터포털 API(정확, 대체공휴일 포함)
 * 2) 앱 내장 데이터(오프라인 대비, 고정 양력 공휴일만)
 * 3) 사용자 수동 입력(최후 수단 - 이 클래스가 아니라 [HolidayRepository.addManual]로 처리)
 *
 * API 키가 없거나(사용자가 설정에서 입력한 키가 없음), 호출이 실패하거나,
 * 파싱 결과가 비어 있으면 자동으로 2단계(내장 데이터)로 폴백한다 - 앱이 키 없이도
 * 완전히 동작해야 한다는 요구사항을 여기서 만족시킨다.
 */
@Singleton
class HolidayRemoteRepository @Inject constructor(
    private val api: HolidayApiService,
    private val holidayRepository: HolidayRepository,
    private val apiKeyStore: HolidayApiKeyStore,
) {
    suspend fun refreshYear(year: Int): HolidayFetchResult {
        val serviceKey = apiKeyStore.get()
        if (serviceKey.isBlank()) {
            return fallBackToBuiltIn(year, "공공데이터포털 API 키가 설정되지 않았습니다. 설정에서 키를 입력해 주세요")
        }

        return try {
            val raw = api.getRestDeInfo(serviceKey = serviceKey, solYear = year)
            val holidays = HolidayApiPayloadParser.parse(raw)
            if (holidays.isEmpty()) {
                // API 호출 자체는 성공했지만 파싱 결과가 비었다 - 실제로 0건일 수도 있고,
                // 응답 스키마가 예상과 달라 파서가 아무것도 못 찾았을 수도 있다. 어느
                // 쪽이든 사용자가 공휴일 없이 방치되지 않도록 내장 데이터로 보완한다.
                fallBackToBuiltIn(year, "API 응답에서 공휴일을 찾지 못해 내장 데이터로 대체했습니다")
            } else {
                holidayRepository.upsertAll(holidays)
                HolidayFetchResult.Success(year, holidays.size)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            // 상태 코드만으로는 원인을 알 수 없어(403 = 키 미등록/미승인 등) 본문의 사유를 풀어 보여준다.
            val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            fallBackToBuiltIn(year, HolidayApiErrorDescriber.describe(e.code(), body))
        } catch (e: Exception) {
            fallBackToBuiltIn(year, e.message ?: "네트워크 오류로 내장 데이터로 대체했습니다")
        }
    }

    /** 설정 화면의 "공휴일 갱신" 버튼이 부르는 기본 진입점 - 올해와 내년을 함께 갱신한다. */
    suspend fun refreshCurrentAndNextYear(): List<HolidayFetchResult> {
        val year = AppClock.today().year
        return listOf(refreshYear(year), refreshYear(year + 1))
    }

    private suspend fun fallBackToBuiltIn(year: Int, reason: String): HolidayFetchResult.UsedBuiltInFallback {
        holidayRepository.upsertAll(BuiltInHolidaySeed.generate(year, year))
        return HolidayFetchResult.UsedBuiltInFallback(year, reason)
    }
}
