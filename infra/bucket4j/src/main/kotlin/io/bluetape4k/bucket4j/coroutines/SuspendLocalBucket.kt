package io.bluetape4k.bucket4j.coroutines

import io.bluetape4k.bucket4j.bucketConfiguration
import io.bluetape4k.bucket4j.internal.Slf4jBucketListener
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.BucketExceptions
import io.github.bucket4j.BucketListener
import io.github.bucket4j.ConfigurationBuilder
import io.github.bucket4j.LimitChecker
import io.github.bucket4j.MathType
import io.github.bucket4j.TimeMeter
import io.github.bucket4j.local.LockFreeBucket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.time.Duration
import kotlin.time.Duration.Companion.nanoseconds


/**
 * Coroutines Context에서 사용하는 [Bucket4j](https://github.com/bucket4j/bucket4j)'s [LockFreeBucket] 입니다.
 * [BlockingBucket](https://bucket4j.com/8.2.0/toc.html#blocking-bucket)과 의미론적으로 동등한 인터페이스를 구현합니다.
 * Bucket4j의 블로킹 동작은 CPU 블로킹을 하지만, [SuspendLocalBucket]은 대신 `delay`를  하여 코루틴 컨텍스트에서 안전하게 사용할 수 있습니다.
 *
 * ```kotlin
 *  val bucket = SuspendLocalBucket {
 *      addBandwidth {
 *          BandwidthBuilder.builder()
 *              .capacity(5)                                     // 5개의 토큰을 보유
 *              .refillIntervally(1, 1.seconds.toJavaDuration()) // 1초에 1개의 토큰을 보충
 *              .build()
 *         }
 *     }
 * }
 *
 * // 초과 소비 시도 -> false
 * bucket.tryConsume(5L + 1L, 10.milliseconds.toJavaDuration()) // false
 * ```
 *
 *
 * @param bucketConfiguration [BucketConfiguration] 정보 (참고: [bucketConfiguration] 메소드)
 * @param mathType 계산 단위
 * @param timeMeter 시간 측정 단위를 나타내는 [TimeMeter]
 */
class SuspendLocalBucket private constructor(
    bucketConfiguration: BucketConfiguration,
    mathType: MathType,
    timeMeter: TimeMeter,
    listener: BucketListener,
): LockFreeBucket(bucketConfiguration, mathType, timeMeter, listener) {

    companion object: KLoggingChannel() {
        @JvmStatic
        val DEFAULT_TIME_METER: TimeMeter = TimeMeter.SYSTEM_MILLISECONDS

        @JvmStatic
        val DEFAULT_MATH_TYPE: MathType = MathType.INTEGER_64_BITS

        @JvmStatic
        val DEFAULT_MAX_WAIT_TIME: Duration = Duration.ofSeconds(3)

        /**
         * 기존 [BucketConfiguration]을 사용해 [SuspendLocalBucket]을 생성합니다.
         *
         * @param config 버킷 대역폭/리필 정책을 포함한 구성
         * @param mathType 토큰 계산에 사용할 수학 정책
         * @param timeMeter 현재 시간을 읽을 [TimeMeter]
         * @param listener 토큰 소비/대기/취소 이벤트를 수집할 [BucketListener]
         * @return 생성한 [SuspendLocalBucket]
         */
        @JvmStatic
        operator fun invoke(
            config: BucketConfiguration,
            mathType: MathType = DEFAULT_MATH_TYPE,
            timeMeter: TimeMeter = DEFAULT_TIME_METER,
            listener: BucketListener = Slf4jBucketListener(log),
        ): SuspendLocalBucket {
            return SuspendLocalBucket(config, mathType, timeMeter, listener)
        }

        /**
         * DSL 기반 설정 블록으로 [SuspendLocalBucket]을 생성합니다.
         *
         * @param mathType 토큰 계산에 사용할 수학 정책
         * @param timeMeter 현재 시간을 읽을 [TimeMeter]
         * @param listener 토큰 소비/대기/취소 이벤트를 수집할 [BucketListener]
         * @param configurer [ConfigurationBuilder] 초기화 블록
         * @return 생성한 [SuspendLocalBucket]
         */
        @JvmStatic
        operator fun invoke(
            mathType: MathType = DEFAULT_MATH_TYPE,
            timeMeter: TimeMeter = DEFAULT_TIME_METER,
            listener: BucketListener = Slf4jBucketListener(log),
            configurer: ConfigurationBuilder.() -> Unit,
        ): SuspendLocalBucket {
            return invoke(bucketConfiguration(configurer), mathType, timeMeter, listener)
        }
    }

    /**
     * 코루틴 컨텍스트에서 사용하는 tryConsume
     *
     * ```kotlin
     * val bucket = SuspendLocalBucket { addBandwidth { Bandwidth.simple(10, Duration.ofSeconds(1)) } }
     * val consumed = bucket.tryConsume(1L, Duration.ofMillis(100))
     * // consumed == true (토큰 여유가 있는 경우)
     * ```
     *
     * @param tokensToConsume 소비할 토큰 수
     * @param maxWaitTime 최대 대기 시간
     * @return 최대 대기 시간까지 요청한 토큰을 받을 수 없다면 false를 반환한다
     */
    suspend fun tryConsume(tokensToConsume: Long = 1L, maxWaitTime: Duration = DEFAULT_MAX_WAIT_TIME): Boolean {
        LimitChecker.checkTokensToConsume(tokensToConsume)
        val maxWaitTimeNanos = maxWaitTime.toNanosChecked("maxWaitTime")
        LimitChecker.checkMaxWaitTime(maxWaitTimeNanos)

        val nanosToDelay: Long = reserveAndCalculateTimeToSleepImpl(tokensToConsume, maxWaitTimeNanos)

        if (nanosToDelay == INFINITY_DURATION) {
            log.debug { "rejected. nanosToDelay is INFINITY_DURATION" }
            listener.onRejected(tokensToConsume)
            return false
        }

        listener.onConsumed(tokensToConsume)
        suspendIfNeeded(nanosToDelay)

        return true
    }

    /**
     * 코루틴 컨텍스트에서 사용하는 consume 함수.
     * 토큰이 채워질 때까지 대기합니다. 예약 오버플로우 시 예외가 발생합니다.
     *
     * ```kotlin
     * val bucket = SuspendLocalBucket { addBandwidth { Bandwidth.simple(10, Duration.ofSeconds(1)) } }
     * bucket.consume(1L) // 토큰 1개 소비, 부족 시 채워질 때까지 delay
     * ```
     *
     * @param tokensToConsume 소비할 Token 수
     */
    suspend fun consume(tokensToConsume: Long = 1L) {
        LimitChecker.checkTokensToConsume(tokensToConsume)

        val nanosToDelay: Long = reserveAndCalculateTimeToSleepImpl(tokensToConsume, INFINITY_DURATION)

        if (nanosToDelay == INFINITY_DURATION) {
            log.warn { "reservation overflow" }
            throw BucketExceptions.reservationOverflow()
        }

        listener.onConsumed(tokensToConsume)
        suspendIfNeeded(nanosToDelay)
    }

    /**
     * 예약된 대기 시간이 있다면 코루틴을 일시 중단한다.
     * BucketListener 이벤트를 일관되게 기록하기 위해 delayed/parked/interrupted를 함께 처리한다.
     */
    private suspend fun suspendIfNeeded(nanosToDelay: Long) {
        if (nanosToDelay <= 0L) return

        listener.onDelayed(nanosToDelay)
        log.trace { "nanos to delay=$nanosToDelay" }
        try {
            delay(nanosToDelay.nanoseconds)
            listener.onParked(nanosToDelay)
        } catch (e: CancellationException) {
            listener.onInterrupted(InterruptedException("Coroutine cancelled while waiting tokens.").apply { initCause(e) })
            throw e
        }
    }

    private fun Duration.toNanosChecked(name: String): Long {
        return runCatching { toNanos() }
            .getOrElse {
                throw IllegalArgumentException("$name is too large to convert to nanos. value=$this", it)
            }
    }
}
