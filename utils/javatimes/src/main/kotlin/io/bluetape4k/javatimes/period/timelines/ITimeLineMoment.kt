package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.ValueObject
import io.bluetape4k.javatimes.period.ITimePeriodCollection
import java.time.ZonedDateTime

/**
 * 타임라인의 특정 시각에 대한 정보를 나타내는 인터페이스
 *
 * ```kotlin
 * val moment: ITimeLineMoment = TimeLineMoment(ZonedDateTime.now())
 * moment.startCount // 이 시각에서 시작하는 기간 수
 * moment.endCount   // 이 시각에서 종료하는 기간 수
 * ```
 */
interface ITimeLineMoment: ValueObject, Comparable<ITimeLineMoment> {

    val moment: ZonedDateTime

    val periods: ITimePeriodCollection

    val startCount: Long

    val endCount: Long

}
