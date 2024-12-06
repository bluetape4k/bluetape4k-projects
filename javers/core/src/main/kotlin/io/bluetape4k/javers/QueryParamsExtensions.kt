package io.bluetape4k.javers

import org.javers.repository.api.QueryParams
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

fun QueryParams.isDateInRange(date: LocalDateTime): Boolean {
    if (from().getOrNull()?.isAfter(date) == true) {
        return false
    }
    if (to().getOrNull()?.isBefore(date) == true) {
        return false
    }
    return true
}
