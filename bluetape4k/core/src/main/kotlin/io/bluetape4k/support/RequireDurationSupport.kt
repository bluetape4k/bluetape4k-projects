package io.bluetape4k.support

import java.time.Duration as JavaDuration
import kotlin.time.Duration as KotlinDuration


// ── java.time.Duration ──────────────────────────────────────────────────────

/** Java Duration가 0보다 큰지 검증한다. */
fun JavaDuration.requirePositive(parameterName: String): JavaDuration = apply {
    requirePositive { "$parameterName[$this] must be positive" }
}

fun JavaDuration.requirePositive(message: () -> String): JavaDuration = apply {
    require(this > JavaDuration.ZERO, message)
}

/** Java Duration가 0보다 크거나 같은지 검증한다. */
fun JavaDuration.requireZeroOrPositive(parameterName: String): JavaDuration = apply {
    requireZeroOrPositive { "$parameterName[$this] must be zero or positive." }
}

fun JavaDuration.requireZeroOrPositive(message: () -> String): JavaDuration = apply {
    require(!this.isNegative, message)
}

/** Java Duration가 0보다 작은지 검증한다. */
fun JavaDuration.requireNegative(parameterName: String): JavaDuration = apply {
    requireNegative { "$parameterName[$this] must be negative." }
}

fun JavaDuration.requireNegative(message: () -> String): JavaDuration = apply {
    require(this.isNegative, message)
}

/** Java Duration가 양수가 아닌지 검증한다. */
fun JavaDuration.requireNotPositive(parameterName: String): JavaDuration = apply {
    requireNotPositive { "$parameterName[$this] must be not positive." }
}

fun JavaDuration.requireNotPositive(message: () -> String): JavaDuration = apply {
    require(!this.isPositive, message)
}

// ── kotlin.time.Duration ────────────────────────────────────────────────────

/** Kotlin Duration가 0보다 큰지 검증한다. */
fun KotlinDuration.requirePositive(parameterName: String): KotlinDuration = apply {
    requirePositive { "$parameterName[$this] must be positive" }
}

fun KotlinDuration.requirePositive(message: () -> String): KotlinDuration = apply {
    require(this.isPositive(), message)
}

/** Kotlin Duration가 0보다 크거나 같은지 검증한다. */
fun KotlinDuration.requireZeroOrPositive(parameterName: String): KotlinDuration = apply {
    requireZeroOrPositive { "$parameterName[$this] must be zero or positive." }
}

fun KotlinDuration.requireZeroOrPositive(message: () -> String): KotlinDuration = apply {
    require(!this.isNegative(), message)
}

/** Kotlin Duration가 0보다 작은지 검증한다. */
fun KotlinDuration.requireNegative(parameterName: String): KotlinDuration = apply {
    requireNegative { "$parameterName[$this] must be negative." }
}

fun KotlinDuration.requireNegative(message: () -> String): KotlinDuration = apply {
    require(this.isNegative(), message)
}

/** Kotlin Duration가 양수가 아닌지 검증한다. */
fun KotlinDuration.requireNotPositive(parameterName: String): KotlinDuration = apply {
    requireNotPositive { "$parameterName[$this] must be not positive." }
}

fun KotlinDuration.requireNotPositive(message: () -> String): KotlinDuration = apply {
    require(!this.isPositive(), message)
}
