package org.coepi.api.v4

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

object Intervals {
    const val INTERVAL_LENGTH: Long = 6 * 60 * 60

    val MIN_REPORT_DATE: Instant =
            LocalDateTime.of(2019, 11, 1, 0, 0)
                    .toInstant(ZoneOffset.UTC)
}

fun Instant.toInterval(): Long = epochSecond / Intervals.INTERVAL_LENGTH

fun generateIntervalForTimestamp(timestamp: Long): Long =
    timestamp / Intervals.INTERVAL_LENGTH