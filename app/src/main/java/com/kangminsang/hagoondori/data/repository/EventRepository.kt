package com.kangminsang.hagoondori.data.repository

import com.kangminsang.hagoondori.core.model.Event
import com.kangminsang.hagoondori.data.local.dao.EventDao
import com.kangminsang.hagoondori.data.mapper.toCore
import com.kangminsang.hagoondori.data.mapper.toEntity
import com.kangminsang.hagoondori.data.util.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** 일반 일정 데이터 접근(스펙 4.12절, F4). */
@Singleton
class EventRepository @Inject constructor(
    private val dao: EventDao,
    private val syncStateRepository: SyncStateRepository,
) {
    fun observeAll(): Flow<List<Event>> =
        dao.observeAll().map { list -> list.map { it.toCore() } }

    /**
     * 새 일정을 만든다. [title]에 대한 검증(길이/문자)은 이 함수를 호출하기 전에
     * [com.kangminsang.hagoondori.core.validation.TitleValidator]로 이미 끝났어야
     * 한다 - [Event]의 생성자 자체도 하드 제약을 한 번 더 강제한다.
     */
    suspend fun addEvent(
        title: String,
        startDate: LocalDate,
        endDate: LocalDate?,
        isImportant: Boolean,
        memo: String?,
    ): Event {
        val event = Event(IdGenerator.newId(), title, startDate, endDate, isImportant, memo)
        dao.upsert(event.toEntity())
        syncStateRepository.markChanged("일정 추가: $title")
        return event
    }

    suspend fun update(event: Event) {
        dao.upsert(event.toEntity())
        syncStateRepository.markChanged("일정 수정: ${event.title}")
    }

    suspend fun delete(event: Event) {
        dao.delete(event.toEntity())
        syncStateRepository.markChanged("일정 삭제: ${event.title}")
    }
}
