package io.bluetape4k.bucket4j.coroutines

import io.bluetape4k.bucket4j.bucketConfiguration
import io.bluetape4k.bucket4j.internal.Slf4jBucketListener
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.BucketExceptions
import io.github.bucket4j.ConfigurationBuilder
import io.github.bucket4j.LimitChecker
import io.github.bucket4j.MathType
import io.github.bucket4j.TimeMeter
import io.github.bucket4j.local.LockFreeBucket
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Coroutines Context에서 사용하는 [Bucket4j](https://github.com/bucket4j/bucket4j)'s [LockFreeBucket] 입니다.
 * [BlockingBucket](https://bucket4j.com/8.2.0/toc.html#blocking-bucket)과 의미론적으로 동등한 인터페이스를 구현합니다.
 * Bucket4j의 블로킹 동작은 CPU 블로킹을 하지만, [CoLocalBucket]은 대신 `delay`를  하여 코루틴 컨텍스트에서 안전하게 사용할 수 있습니다.
 *
 * ```
 *  val bucket = CoLocalBucket {
 *      addBandwidth {
 *          BandwidthBuilder.builder()
 *              .capacity(5)                                     // 5개의 토큰을 보유
 *              .refillIntervally(1, 1.seconds.toJavaDuration()) // 1초에 1개의 토큰을 보충
 *              .build()
 *         }
 *     }
 * }
 *
 * // 5개를 보유하고 있다
 * bucket.coTryConsume(5L + 1L, 10.milliseconds).shouldBeFalse()
 * ```
 *
 *
 * @param bucketConfiguration [BucketConfiguration] 정보 (참고: [bucketConfiguration] 메소드)
 * @param mathType 계산 단위
 * @param timeMeter 시간 측정 단위를 나타내는 [TimeMeter]
 */
@Deprecated(
    message = "Use `io.bluetape4k.bucket4j.coroutines.SuspendedLocalBucket` instead",
    replaceWith = ReplaceWith("io.bluetape4k.bucket4j.coroutines.SuspendedLocalBucket")
)
class CoLocalBucket private constructor(
    bucketConfiguration: BucketConfiguration,
    mathType: MathType,
    timeMeter: TimeMeter,
): LockFreeBucket(bucketConfiguration, mathType, timeMeter, Slf4jBucketListener(log)) {

    companion object: KLoggingChannel() {
        @JvmStatic
        val DEFAULT_TIME_METER: TimeMeter = TimeMeter.SYSTEM_MILLISECONDS

        @JvmStatic
        val DEFAULT_MATH_TYPE: MathType = MathType.INTEGER_64_BITS

        @JvmStatic
        val DEFAULT_MAX_WAIT_TIME: Duration = 3.seconds

        @JvmStatic
        operator fun invoke(
            config: BucketConfiguration,
            mathType: MathType = DEFAULT_MATH_TYPE,
            timeMeter: TimeMeter = DEFAULT_TIME_METER,
        ): CoLocalBucket {
            return CoLocalBucket(config, mathType, timeMeter)
        }

        @JvmStatic
        operator fun invoke(
            mathType: MathType = DEFAULT_MATH_TYPE,
            timeMeter: TimeMeter = DEFAULT_TIME_METER,
            configurer: ConfigurationBuilder.() -> Unit,
        ): CoLocalBucket {
            return invoke(bucketConfiguration(configurer), mathType, timeMeter)
        }
    }

    /**
     * 코루틴 컨텍스트에서 사용하는 tryConsume
     *
     * @param tokensToConsume 소비할 토큰 수
     * @param maxWaitTime 최대 대기 시간
     * @return 최대 대기 시간까지 요청한 토큰을 받을 수 없다면 false를 반환한다
     */
    suspend fun coTryConsume(tokensToConsume: Long = 1L, maxWaitTime: Duration = DEFAULT_MAX_WAIT_TIME): Boolean {
        LimitChecker.checkTokensToConsume(tokensToConsume)
        val maxWaitTimeNanos: Long = maxWaitTime.inWholeNanoseconds
        LimitChecker.checkMaxWaitTime(maxWaitTimeNanos)

        val nanosToDelay: Long = reserveAndCalculateTimeToSleepImpl(tokensToConsume, maxWaitTimeNanos)
        log.trace { "nanosToDelay=$nanosToDelay" }

        if (nanosToDelay == INFINITY_DURATION) {
            log.debug { "rejected. nanosToDelay is INFINITY_DURATION" }
            listener.onRejected(tokensToConsume)
            return false
        }

        listener.onConsumed(tokensToConsume)
        if (nanosToDelay > 0L) {
            delay(nanosToDelay.nanoseconds)
            listener.onParked(nanosToDelay)
        }

        return true
    }

    /**
     * 코루틴 컨텍스트에서 사용하는 consume 함수
     *
     * @param tokensToConsume 소비할 Token 수
     */
    suspend fun coConsume(tokensToConsume: Long = 1L) {
        LimitChecker.checkTokensToConsume(tokensToConsume)

        val nanosToDelay: Long = reserveAndCalculateTimeToSleepImpl(tokensToConsume, INFINITY_DURATION)
        log.trace { "nanos to delay=$nanosToDelay" }

        if (nanosToDelay == INFINITY_DURATION) {
            log.warn { "reservation overflow" }
            throw BucketExceptions.reservationOverflow()
        }

        listener.onConsumed(tokensToConsume)
        if (nanosToDelay > 0L) {
            delay(nanosToDelay.nanoseconds)
        }
    }
}
