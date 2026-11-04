package com.kodomo.app.data.local

import androidx.room.TypeConverter
import com.kodomo.app.domain.model.Interest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

class Converters {
    private val json = Json { ignoreUnknownKeys = true }
    private val stringList = ListSerializer(String.serializer())

    @TypeConverter fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()
    @TypeConverter fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter fun instantToMillis(instant: Instant?): Long? = instant?.toEpochMilli()
    @TypeConverter fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter fun interestsToString(set: Set<Interest>?): String? = set?.joinToString(",") { it.name }
    @TypeConverter fun stringToInterests(value: String?): Set<Interest>? = value?.split(',')
        ?.mapNotNull { name -> Interest.entries.firstOrNull { it.name == name } }
        ?.toSet()

    @TypeConverter fun listToJson(list: List<String>?): String? = list?.let { json.encodeToString(stringList, it) }
    @TypeConverter fun jsonToList(value: String?): List<String>? = value?.let { json.decodeFromString(stringList, it) }
}
