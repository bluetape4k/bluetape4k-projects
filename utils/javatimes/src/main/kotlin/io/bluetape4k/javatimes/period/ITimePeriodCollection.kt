package io.bluetape4k.javatimes.period

import java.time.ZonedDateTime

/**
 * 여러 기간[ITimePeriod] 들을 가지는 컬렉션
 *
 * ```kotlin
 * val collection = TimePeriodCollection()
 * val now = ZonedDateTime.now()
 * collection.add(TimeRange(now, now.plusDays(3)))
 * collection.add(TimeRange(now.plusDays(1), now.plusDays(5)))
 * val target = TimeRange(now.plusDays(1), now.plusDays(2))
 * collection.hasInsidePeriods(target) // true
 * ```
 */
interface ITimePeriodCollection: ITimePeriodContainer {

    /**
     * 지정한 기간이 컬렉션의 어떤 기간 내부에 완전히 포함되는지 여부를 반환합니다.
     *
     * ```kotlin
     * val collection = TimePeriodCollection()
     * val now = ZonedDateTime.now()
     * collection.add(TimeRange(now, now.plusDays(5)))
     * val inner = TimeRange(now.plusDays(1), now.plusDays(3))
     * collection.hasInsidePeriods(inner) // true
     * ```
     */
    fun hasInsidePeriods(that: ITimePeriod): Boolean

    /**
     * 지정한 기간과 겹치는 기간이 컬렉션에 있는지 여부를 반환합니다.
     *
     * ```kotlin
     * val collection = TimePeriodCollection()
     * val now = ZonedDateTime.now()
     * collection.add(TimeRange(now, now.plusDays(3)))
     * val overlap = TimeRange(now.plusDays(2), now.plusDays(6))
     * collection.hasOverlapPeriods(overlap) // true
     * ```
     */
    fun hasOverlapPeriods(that: ITimePeriod): Boolean

    /**
     * 특정 시각이 컬렉션의 어떤 기간과 교차하는지 여부를 반환합니다.
     *
     * ```kotlin
     * val collection = TimePeriodCollection()
     * val now = ZonedDateTime.now()
     * collection.add(TimeRange(now, now.plusDays(3)))
     * collection.hasIntersectionPeriods(now.plusDays(1)) // true
     * collection.hasIntersectionPeriods(now.plusDays(5)) // false
     * ```
     */
    fun hasIntersectionPeriods(moment: ZonedDateTime): Boolean

    /**
     * 지정한 기간과 교차하는 기간이 컬렉션에 있는지 여부를 반환합니다.
     *
     * ```kotlin
     * val collection = TimePeriodCollection()
     * val now = ZonedDateTime.now()
     * collection.add(TimeRange(now, now.plusDays(3)))
     * val target = TimeRange(now.plusDays(2), now.plusDays(6))
     * collection.hasIntersectionPeriods(target) // true
     * ```
     */
    fun hasIntersectionPeriods(that: ITimePeriod): Boolean

    /**
     * 지정한 기간 내부에 완전히 포함된 기간 목록을 반환합니다.
     *
     * ```kotlin
     * val collection = TimePeriodCollection()
     * val now = ZonedDateTime.now()
     * collection.add(TimeRange(now.plusDays(1), now.plusDays(2)))
     * collection.add(TimeRange(now.plusDays(4), now.plusDays(6)))
     * val target = TimeRange(now, now.plusDays(3))
     * collection.insidePeriods(target) // [TimeRange(now+1d, now+2d)]
     * ```
     */
    fun insidePeriods(target: ITimePeriod): List<ITimePeriod>

    /**
     * 지정한 기간과 겹치는 기간 목록을 반환합니다.
     *
     * ```kotlin
     * val collection = TimePeriodCollection()
     * val now = ZonedDateTime.now()
     * collection.add(TimeRange(now, now.plusDays(3)))
     * collection.add(TimeRange(now.plusDays(5), now.plusDays(7)))
     * val target = TimeRange(now.plusDays(2), now.plusDays(6))
     * collection.overlapPeriods(target).size // 2
     * ```
     */
    fun overlapPeriods(target: ITimePeriod): List<ITimePeriod>

    /**
     * 특정 시각을 포함하는 기간 목록을 반환합니다.
     *
     * ```kotlin
     * val collection = TimePeriodCollection()
     * val now = ZonedDateTime.now()
     * collection.add(TimeRange(now, now.plusDays(3)))
     * collection.add(TimeRange(now.plusDays(2), now.plusDays(5)))
     * collection.intersectionPeriod(now.plusDays(2)).size // 2
     * ```
     */
    fun intersectionPeriod(moment: ZonedDateTime): List<ITimePeriod>

    /**
     * 지정한 기간과 교차하는 기간 목록을 반환합니다.
     *
     * ```kotlin
     * val collection = TimePeriodCollection()
     * val now = ZonedDateTime.now()
     * collection.add(TimeRange(now, now.plusDays(3)))
     * collection.add(TimeRange(now.plusDays(4), now.plusDays(6)))
     * val target = TimeRange(now.plusDays(2), now.plusDays(5))
     * collection.intersectionPeriod(target).size // 2
     * ```
     */
    fun intersectionPeriod(target: ITimePeriod): List<ITimePeriod>

    /**
     * 지정한 기간과 주어진 관계([PeriodRelation])에 해당하는 기간 목록을 반환합니다.
     *
     * ```kotlin
     * val collection = TimePeriodCollection()
     * val now = ZonedDateTime.now()
     * collection.add(TimeRange(now, now.plusDays(3)))
     * val target = TimeRange(now.plusDays(1), now.plusDays(4))
     * collection.relationPeriods(target, PeriodRelation.Overlapping) // 겹치는 기간 목록
     * ```
     */
    fun relationPeriods(target: ITimePeriod, vararg relations: PeriodRelation): List<ITimePeriod>
}
