package com.saikalyan.jobmate.data.local.converters

import androidx.room.TypeConverter
import java.time.Instant

class AppTypeConverters {
    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    /** Length-prefix encoding avoids delimiter collisions in skill names. */
    @TypeConverter
    fun stringListToStorage(value: List<String>?): String? =
        value?.joinToString(separator = "") { "${it.length}:$it" }

    @TypeConverter
    fun storageToStringList(value: String?): List<String>? {
        if (value == null) return null
        if (value.isEmpty()) return emptyList()

        val result = mutableListOf<String>()
        var cursor = 0
        while (cursor < value.length) {
            val separator = value.indexOf(':', cursor)
            require(separator > cursor) { "Invalid encoded string list" }
            val length = value.substring(cursor, separator).toIntOrNull()
                ?: throw IllegalArgumentException("Invalid encoded string list length")
            require(length >= 0) { "Invalid encoded string list length" }
            val start = separator + 1
            val end = start + length
            require(end <= value.length) { "Truncated encoded string list" }
            result += value.substring(start, end)
            cursor = end
        }
        return result
    }
}