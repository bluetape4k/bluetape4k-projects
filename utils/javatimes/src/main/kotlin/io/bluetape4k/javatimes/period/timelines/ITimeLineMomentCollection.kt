package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.ValueObject
import io.bluetape4k.javatimes.period.ITimePeriod
import java.time.ZonedDateTime

/**
 * [ITimeLineMoment]의 컬렉션 인터페이스
 *
 * ```kotlin
 * val collection: ITimeLineMomentCollection = TimeLineMomentCollection()
 * val period = TimeRange(ZonedDateTime.now(), ZonedDateTime.now().plusDays(1))
 * collection.add(period)
 * collection.find(ZonedDateTime.now()) // 해당 시각의 Moment 반환
 * collection.minOrNull() // 가장 이른 Moment
 * collection.maxOrNull() // 가장 늦은 Moment
 * ```
 */
interface ITimeLineMomentCollection: ValueObject, MutableList<ITimeLineMoment> {

    /**
     * 가장 이른 시각의 [ITimeLineMoment]를 반환합니다.
     *
     * ```kotlin
     * collection.minOrNull()?.moment // 가장 이른 시각
     * ```
     */
    fun minOrNull(): ITimeLineMoment?

    /**
     * 가장 늦은 시각의 [ITimeLineMoment]를 반환합니다.
     *
     * ```kotlin
     * collection.maxOrNull()?.moment // 가장 늦은 시각
     * ```
     */
    fun maxOrNull(): ITimeLineMoment?

    /**
     * 기간을 추가합니다.
     *
     * ```kotlin
     * collection.add(TimeRange(start, end))
     * ```
     */
    fun add(period: ITimePeriod)

    /**
     * 여러 기간을 추가합니다.
     *
     * ```kotlin
     * collection.addAll(listOf(TimeRange(start1, end1), TimeRange(start2, end2)))
     * ```
     */
    fun addAll(periods: Collection<ITimePeriod>)

    /**
     * 기간을 제거합니다.
     *
     * ```kotlin
     * collection.remove(period)
     * ```
     */
    fun remove(period: ITimePeriod)

    /**
     * 특정 시각의 [ITimeLineMoment]를 찾아 반환합니다.
     *
     * ```kotlin
     * val moment = collection.find(ZonedDateTime.now())
     * ```
     */
    fun find(moment: ZonedDateTime): ITimeLineMoment?

    /**
     * 특정 시각의 [ITimeLineMoment]가 있는지 여부를 반환합니다.
     *
     * ```kotlin
     * collection.contains(ZonedDateTime.now()) // true or false
     * ```
     */
    fun contains(moment: ZonedDateTime): Boolean

}
