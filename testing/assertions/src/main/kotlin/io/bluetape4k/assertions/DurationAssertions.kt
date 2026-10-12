package io.bluetape4k.assertions

import io.bluetape4k.assertions.internal.Failures
import io.bluetape4k.assertions.internal.Messages
import java.time.Duration as JavaDuration
import kotlin.time.Duration as KotlinDuration

// ── java.time.Duration ──────────────────────────────────────────────────────

/** Java Duration가 0보다 큰지 검증한다. */
fun JavaDuration.shouldBePositive(): JavaDuration = apply {
    if (this <= JavaDuration.ZERO) {
        Failures.failComparison(Messages.expectedToBe("be positive", "> 0", this), "> 0", this)
    }
}

/** Java Duration가 0보다 크거나 같은지 검증한다. */
fun JavaDuration.shouldBeZeroOrPositive(): JavaDuration = apply {
    if (this.isNegative) {
        Failures.failComparison(Messages.expectedToBe("be zero or positive", ">= 0", this), ">= 0", this)
    }
}

/** Java Duration가 0보다 작은지 검증한다. */
fun JavaDuration.shouldBeNegative(): JavaDuration = apply {
    if (this >= JavaDuration.ZERO) {
        Failures.failComparison(Messages.expectedToBe("be negative", "< 0", this), "< 0", this)
    }
}

/** Java Duration가 양수가 아닌지 검증한다. */
fun JavaDuration.shouldNotBePositive(): JavaDuration = apply {
    if (this > JavaDuration.ZERO) {
        Failures.failComparison(Messages.expectedNotToBe("be positive", "> 0", this), "> 0", this)
    }
}

// ── kotlin.time.Duration ────────────────────────────────────────────────────

/** Kotlin Duration가 0보다 큰지 검증한다. 무한대도 양수로 취급한다. */
fun KotlinDuration.shouldBePositive(): KotlinDuration = apply {
    if (this <= KotlinDuration.ZERO) {
        Failures.failComparison(Messages.expectedToBe("be positive", "> 0", this), "> 0", this)
    }
}

/** Kotlin Duration가 0보다 크거나 같은지 검증한다. 무한대도 양수로 취급한다. */
fun KotlinDuration.shouldBeZeroOrPositive(): KotlinDuration = apply {
    if (this < KotlinDuration.ZERO) {
        Failures.failComparison(Messages.expectedToBe("be zero or positive", ">= 0", this), ">= 0", this)
    }
}

/** Kotlin Duration가 0보다 작은지 검증한다. 음의 무한대도 음수로 취급한다. */
fun KotlinDuration.shouldBeNegative(): KotlinDuration = apply {
    if (this >= KotlinDuration.ZERO) {
        Failures.failComparison(Messages.expectedToBe("be negative", "< 0", this), "< 0", this)
    }
}

/** Kotlin Duration가 양수가 아닌지 검증한다. */
fun KotlinDuration.shouldNotBePositive(): KotlinDuration = apply {
    if (this > KotlinDuration.ZERO) {
        Failures.failComparison(Messages.expectedNotToBe("be positive", "> 0", this), "> 0", this)
    }
}
