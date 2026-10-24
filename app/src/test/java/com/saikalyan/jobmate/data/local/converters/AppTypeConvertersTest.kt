package com.saikalyan.jobmate.data.local.converters

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class AppTypeConvertersTest {
    private val converters = AppTypeConverters()

    @Test
    fun stringListsRoundTripEmptyUnicodeAndDelimiterCharacters() {
        val values = listOf("", "C++: Kotlin", "Android\nCompose", "東京", "emoji 😀")
        assertEquals(values, converters.storageToStringList(converters.stringListToStorage(values)))
        assertEquals(emptyList<String>(), converters.storageToStringList(converters.stringListToStorage(emptyList())))
        assertEquals(null, converters.storageToStringList(null))
    }

    @Test
    fun instantsRoundTripAtDatabaseMillisecondPrecision() {
        val value = Instant.parse("2026-01-02T03:04:05.678Z")
        assertEquals(value, converters.epochMillisToInstant(converters.instantToEpochMillis(value)))
        assertEquals(null, converters.epochMillisToInstant(null))
    }
}