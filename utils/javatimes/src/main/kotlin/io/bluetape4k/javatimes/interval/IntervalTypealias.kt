package io.bluetape4k.javatimes.interval

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZonedDateTime

/** [Instant] 기반의 [TemporalInterval] */
internal typealias InstantInterval = TemporalInterval<Instant>

/** [LocalDate] 기반의 [TemporalInterval] */
internal typealias LocalDateInterval = TemporalInterval<LocalDate>

/** [LocalTime] 기반의 [TemporalInterval] */
internal typealias LocalTimeInterval = TemporalInterval<LocalTime>

/** [LocalDateTime] 기반의 [TemporalInterval] */
internal typealias LocalDateTimeInterval = TemporalInterval<LocalDateTime>

/** [OffsetTime] 기반의 [TemporalInterval] */
internal typealias OffsetTimeInterval = TemporalInterval<OffsetTime>

/** [OffsetDateTime] 기반의 [TemporalInterval] */
internal typealias OffsetDateTimeInterval = TemporalInterval<OffsetDateTime>

/** [ZonedDateTime] 기반의 [TemporalInterval] */
internal typealias ZonedDateTimeInterval = TemporalInterval<ZonedDateTime>
