package io.bluetape4k.cassandra.data

import com.datastax.oss.driver.api.core.data.CqlDuration
import java.time.Duration

fun Duration.toCqlDuration(): CqlDuration {
    return cqlDurationOf(0, toDays().toInt(), toNanos())
}

fun cqlDurationOf(month: Int, days: Int, nanos: Long): CqlDuration {
    return CqlDuration.newInstance(month, days, nanos)
}
