package io.bluetape4k.javatimes.period

import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging

abstract class AbstractPeriodTest {

    companion object: KLogging()

    val now = nowZonedDateTime()

    val testDate = zonedDateTimeOf(2000, 10, 2, 13, 45, 53, 673)
    val testDiffDate = zonedDateTimeOf(2002, 9, 3, 7, 14, 22, 234)
    val testNow = nowZonedDateTime()
}
