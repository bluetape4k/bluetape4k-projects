package io.bluetape4k.redis.lettuce.lock

import java.time.Duration

internal fun Duration.requirePositiveMillisDuration(parameterName: String): Duration = apply {
    require(!isNegative && !isZero && toMillis() > 0L) {
        "$parameterName must be at least 1ms. $parameterName=$this"
    }
}

internal fun Duration.requireZeroOrPositiveDuration(parameterName: String): Duration = apply {
    require(!isNegative) {
        "$parameterName must be zero or positive. $parameterName=$this"
    }
}
