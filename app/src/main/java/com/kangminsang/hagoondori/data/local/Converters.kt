package com.kangminsang.hagoondori.data.local

import androidx.room.TypeConverter
import com.kangminsang.hagoondori.core.model.DutyType
import com.kangminsang.hagoondori.core.model.OverflowBehavior
import com.kangminsang.hagoondori.core.model.PassType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * core 모델은 kotlinx.datetime 타입과 enum을 쓰지만 Room 컬럼은 원시 타입만 다룬다.
 * 날짜는 epoch day(Int), 시각(하루 중)은 "HH:MM" 문자열, 시각(시점, Instant)은
 * epoch milliseconds(Long), enum은 이름(String)으로 왕복 변환한다.
 *
 * 파생값을 저장하지 않는다는 core의 원칙(4.1절 원칙 1)은 여기서도 유지된다 - 이
 * 변환기들은 오직 "같은 값을 다른 타입으로 표현"할 뿐, 새로운 값을 계산하지 않는다.
 */
class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Int? = date?.toEpochDays()

    @TypeConverter
    fun toLocalDate(value: Int?): LocalDate? = value?.let(LocalDate::fromEpochDays)

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? =
        time?.let { "%02d:%02d".format(it.hour, it.minute) }

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let {
        val (hour, minute) = it.split(":").map(String::toInt)
        LocalTime(hour, minute)
    }

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilliseconds()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::fromEpochMilliseconds)

    @TypeConverter
    fun fromOverflowBehavior(value: OverflowBehavior): String = value.name

    @TypeConverter
    fun toOverflowBehavior(value: String): OverflowBehavior = OverflowBehavior.valueOf(value)

    @TypeConverter
    fun fromPassType(value: PassType): String = value.name

    @TypeConverter
    fun toPassType(value: String): PassType = PassType.valueOf(value)

    @TypeConverter
    fun fromDutyType(value: DutyType): String = value.name

    @TypeConverter
    fun toDutyType(value: String): DutyType = DutyType.valueOf(value)
}
