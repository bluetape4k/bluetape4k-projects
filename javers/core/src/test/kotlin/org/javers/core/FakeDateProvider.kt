package org.javers.core

import org.javers.common.date.DateProvider
import java.time.ZonedDateTime

class FakeDateProvider(private var dateTime: ZonedDateTime? = null): DateProvider {

    override fun now(): ZonedDateTime {
        return dateTime ?: ZonedDateTime.now()
    }

    fun set(dateTime: ZonedDateTime?) {
        this.dateTime = dateTime
    }
}
