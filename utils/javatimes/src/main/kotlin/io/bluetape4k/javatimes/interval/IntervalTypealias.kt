package io.bluetape4k.javatimes.interval

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZonedDateTime

typealias InstantInterval = TemporalInterval<Instant>

typealias LocalDateInterval = TemporalInterval<LocalDate>
typealias LocalTimeInterval = TemporalInterval<LocalTime>
typealias LocalDateTimeInterval = TemporalInterval<LocalDateTime>

typealias OffsetTimeInterval = TemporalInterval<OffsetTime>
typealias OffsetDateTimeInterval = TemporalInterval<OffsetDateTime>

typealias ZonedDateTimeInterval = TemporalInterval<ZonedDateTime>
