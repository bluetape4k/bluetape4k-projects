package io.bluetape4k.javatimes.interval

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZonedDateTime

internal typealias InstantInterval = TemporalInterval<Instant>

internal typealias LocalDateInterval = TemporalInterval<LocalDate>
internal typealias LocalTimeInterval = TemporalInterval<LocalTime>
internal typealias LocalDateTimeInterval = TemporalInterval<LocalDateTime>

internal typealias OffsetTimeInterval = TemporalInterval<OffsetTime>
internal typealias OffsetDateTimeInterval = TemporalInterval<OffsetDateTime>

internal typealias ZonedDateTimeInterval = TemporalInterval<ZonedDateTime>
