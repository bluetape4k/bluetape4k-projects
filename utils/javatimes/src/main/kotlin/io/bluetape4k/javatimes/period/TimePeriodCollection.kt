package io.bluetape4k.javatimes.period

import io.bluetape4k.logging.KLogging
import java.time.ZonedDateTime

/**
 * [ITimePeriodCollection]의 기본 구현체
 */
open class TimePeriodCollection: TimePeriodContainer(), ITimePeriodCollection {

    companion object: KLogging() {
        val EMPTY: TimePeriodCollection = TimePeriodCollection()

        operator fun invoke(element: ITimePeriod, vararg elements: ITimePeriod): TimePeriodCollection {
            return TimePeriodCollection().apply {
                add(element)
                addAll(elements)
            }
        }

        fun invoke(elements: Collection<ITimePeriod>): TimePeriodCollection {
            return TimePeriodCollection().apply {
                addAll(elements)
            }
        }

        fun ofAll(elements: Collection<ITimePeriod>): TimePeriodCollection {
            return TimePeriodCollection().apply {
                addAll(elements)
            }
        }
    }

    override fun hasInsidePeriods(that: ITimePeriod): Boolean =
        periods.any { it.hasInsideWith(that) }

    override fun hasOverlapPeriods(that: ITimePeriod): Boolean =
        periods.any { it.overlapWith(that) }

    override fun hasIntersectionPeriods(moment: ZonedDateTime): Boolean =
        periods.any { it.hasInsideWith(moment) }

    override fun hasIntersectionPeriods(that: ITimePeriod): Boolean =
        periods.any { it.intersectWith(that) }

    override fun insidePeriods(target: ITimePeriod): List<ITimePeriod> =
        periods.select { it.hasInsideWith(target) }

    override fun overlapPeriods(target: ITimePeriod): List<ITimePeriod> =
        periods.select { it.overlapWith(target) }

    override fun intersectionPeriod(moment: ZonedDateTime): List<ITimePeriod> =
        periods.select { it.hasInsideWith(moment) }

    override fun intersectionPeriod(target: ITimePeriod): List<ITimePeriod> =
        periods.select { it.intersectWith(target) }

    override fun relationPeriods(target: ITimePeriod, vararg relations: PeriodRelation): List<ITimePeriod> =
        periods.select { relations.contains(it.relationWith(target)) }
}
