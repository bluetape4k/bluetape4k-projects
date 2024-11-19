package io.bluetape4k.javatimes.period.calendars

/**
 * [CalendarPeriodCollector]에서 사용할 컨텍스트입니다.
 *
 * @property scope 수집 범위 정보 (day, month, year, ...)
 */
data class CalendarPeriodCollectorContext(
    val scope: CollectKind,
): ICalendarVisitorContext
