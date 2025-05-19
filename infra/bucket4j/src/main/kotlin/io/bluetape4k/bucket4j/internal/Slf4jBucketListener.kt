package io.bluetape4k.bucket4j.internal

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.github.bucket4j.BucketListener
import kotlinx.atomicfu.atomic

/**
 * [BucketListener] 구현체를 SLF4J Logger로 래핑하는 [BucketListener] 구현체
 *
 * Token 소비, 거부, 지연, 대기, 인터럽트 이벤트를 SLF4J Logger로 출력합니다.
 *
 * ```
 * val bucket = Bucket4j.builder()
 *    .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofSeconds(1))))
 *    .addListener(Slf4jBucketListener())
 *    .build()
 * ```
 *
 * @property log KLogger 인스턴스
 */
class Slf4jBucketListener(val log: org.slf4j.Logger = this.log): BucketListener {

    companion object: KLoggingChannel()

    private val consumedCounter = atomic(0L)
    private val rejectedCounter = atomic(0L)
    private val delayedNanosCounter = atomic(0L)
    private val parkedNanosCounter = atomic(0L)
    private val interruptedCounter = atomic(0L)

    val consumed by consumedCounter
    val rejected by rejectedCounter
    val delayedNanos by delayedNanosCounter
    val parkedNanos by parkedNanosCounter
    val interrupted by interruptedCounter

    override fun onConsumed(tokens: Long) {
        consumedCounter.addAndGet(tokens)
        log.debug { "Bucket on consumed($tokens). all consumed=$consumed" }
    }

    override fun onRejected(tokens: Long) {
        rejectedCounter.addAndGet(tokens)
        log.debug { "Bucket on rejected($tokens). all rejected=$rejected" }
    }

    override fun onDelayed(nanos: Long) {
        delayedNanosCounter.addAndGet(nanos)
        log.debug { "Bucket on delayed($nanos). all delayed nanos=$delayedNanos" }
    }

    override fun onParked(nanos: Long) {
        parkedNanosCounter.addAndGet(nanos)
        log.debug { "Bucket on parked($nanos). all parked nanos=$parkedNanos" }
    }

    override fun onInterrupted(e: InterruptedException?) {
        interruptedCounter.incrementAndGet()
        log.debug(e) { "Bucket on interrupted. all interrupted=$interrupted" }
    }
}
