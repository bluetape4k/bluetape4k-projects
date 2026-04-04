package io.bluetape4k.javatimes.period

import io.bluetape4k.SortDirection
import java.time.ZonedDateTime

/**
 * 복수 개의 [ITimePeriod]를 가지는 컨테이너
 *
 * ```kotlin
 * val collection = TimePeriodCollection()
 * val range1 = TimeRange(ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
 *                        ZonedDateTime.of(2024, 3, 31, 0, 0, 0, 0, ZoneOffset.UTC))
 * val range2 = TimeRange(ZonedDateTime.of(2024, 7, 1, 0, 0, 0, 0, ZoneOffset.UTC),
 *                        ZonedDateTime.of(2024, 9, 30, 0, 0, 0, 0, ZoneOffset.UTC))
 * collection.add(range1)
 * collection.add(range2)
 * collection.sortByStart() // 시작 시각 순으로 정렬
 * ```
 */
interface ITimePeriodContainer: MutableList<ITimePeriod>, ITimePeriod {

    val periods: MutableList<ITimePeriod>

    override var start: ZonedDateTime
    override val readonly: Boolean

    override fun addAll(elements: Collection<ITimePeriod>): Boolean

    fun addAll(array: Array<out ITimePeriod>): Boolean = addAll(array.asList())

    /**
     * 컨테이너가 대상 기간을 포함하는지 여부를 반환합니다.
     *
     * ```kotlin
     * val container = TimePeriodCollection()
     * val range = TimeRange(ZonedDateTime.now(), ZonedDateTime.now().plusDays(7))
     * container.add(range)
     * container.containsPeriod(range) // true
     * ```
     */
    fun containsPeriod(target: ITimePeriod): Boolean = periods.contains(target)

    /**
     * 시작 시각 기준으로 기간들을 정렬합니다.
     *
     * ```kotlin
     * val container = TimePeriodCollection()
     * // ... 기간 추가 ...
     * container.sortByStart(SortDirection.ASC) // 오름차순 정렬
     * ```
     */
    fun sortByStart(sortDir: SortDirection = SortDirection.ASC) {
        when (sortDir) {
            SortDirection.ASC  -> periods.sortBy { it.start }
            SortDirection.DESC -> periods.sortByDescending { it.start }
        }
    }

    /**
     * 종료 시각 기준으로 기간들을 정렬합니다.
     *
     * ```kotlin
     * val container = TimePeriodCollection()
     * // ... 기간 추가 ...
     * container.sortByEnd(SortDirection.DESC) // 내림차순 정렬
     * ```
     */
    fun sortByEnd(sortDir: SortDirection = SortDirection.ASC) {
        when (sortDir) {
            SortDirection.ASC  -> periods.sortBy { it.end }
            SortDirection.DESC -> periods.sortByDescending { it.end }
        }
    }

    /**
     * 기간 길이 기준으로 기간들을 정렬합니다.
     *
     * ```kotlin
     * val container = TimePeriodCollection()
     * // ... 기간 추가 ...
     * container.sortByDuration(SortDirection.ASC) // 짧은 기간 우선
     * ```
     */
    fun sortByDuration(sortDir: SortDirection = SortDirection.ASC) {
        when (sortDir) {
            SortDirection.ASC  -> periods.sortBy { it.duration }
            SortDirection.DESC -> periods.sortByDescending { it.duration }
        }
    }

    /**
     * 두 기간의 시작 시각을 비교합니다.
     *
     * ```kotlin
     * val container = TimePeriodCollection()
     * val p1 = TimeRange(ZonedDateTime.now(), ZonedDateTime.now().plusDays(1))
     * val p2 = TimeRange(ZonedDateTime.now().plusDays(2), ZonedDateTime.now().plusDays(3))
     * container.compare(p1, p2) // 음수 (p1이 p2보다 이전)
     * ```
     */
    fun compare(p1: ITimePeriod, p2: ITimePeriod): Int =
        p1.start.compareTo(p2.start)

}
