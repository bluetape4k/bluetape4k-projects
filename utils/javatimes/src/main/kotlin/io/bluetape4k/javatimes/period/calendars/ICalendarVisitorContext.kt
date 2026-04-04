package io.bluetape4k.javatimes.period.calendars

import io.bluetape4k.ValueObject

/**
 * 달력 방문자 컨텍스트 인터페이스
 *
 * 달력 탐색 시 상태 정보를 저장하는 컨텍스트
 *
 * ```kotlin
 * val context: ICalendarVisitorContext = CalendarPeriodCollectorContext(CollectKind.Day)
 * // CalendarVisitor의 탐색 시 상태 유지에 사용
 * ```
 */
interface ICalendarVisitorContext: ValueObject
