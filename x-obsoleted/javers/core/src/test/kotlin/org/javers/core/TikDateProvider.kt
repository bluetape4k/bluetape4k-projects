package org.javers.core

import org.javers.common.date.DateProvider
import java.time.ZonedDateTime

class TikDateProvider(
    private var dateTime: ZonedDateTime = ZonedDateTime.now(),
): DateProvider {

    override fun now(): ZonedDateTime {
        val now = dateTime
        dateTime = dateTime.plusSeconds(1)
        return now
    }

    fun set(dateTime: ZonedDateTime) {
        this.dateTime = dateTime
    }
}
