package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.javatimes.period.ITimePeriodCollection
import io.bluetape4k.javatimes.period.ITimePeriodContainer
import io.bluetape4k.javatimes.period.ITimePeriodMapper

/**
 * 시간 간격(gap)을 계산하는 클래스
 *
 * ```kotlin
 * val calculator = TimeGapCalculator<ITimePeriod>()
 * val container = TimePeriodCollection()
 * container.add(TimeRange(start1, end1))
 * container.add(TimeRange(start2, end2))
 * val gaps = calculator.gaps(container) // 기간들 사이의 빈 간격
 * ```
 *
 * @param T 시간 기간 타입
 * @param mapper 시간 기간 매퍼 (optional)
 */
open class TimeGapCalculator<T: ITimePeriod>(val mapper: ITimePeriodMapper? = null) {

    /**
     * 제외할 기간들 사이의 간격(gap)을 계산합니다.
     *
     * ```kotlin
     * val calculator = TimeGapCalculator<ITimePeriod>()
     * val container = TimePeriodCollection()
     * container.add(TimeRange(start1, end1))
     * container.add(TimeRange(start3, end3)) // start3 > end1 이면 gap 발생
     * val gaps = calculator.gaps(container)
     * ```
     *
     * @param excludePeriods 제외할 기간들의 컨테이너
     * @param limits 계산할 범위를 제한하는 기간 (optional)
     * @return 계산된 간격들의 컬렉션
     */
    fun gaps(excludePeriods: ITimePeriodContainer, limits: ITimePeriod? = null): ITimePeriodCollection =
        TimeLine<T>(excludePeriods, limits, mapper).calculateGaps()

}
