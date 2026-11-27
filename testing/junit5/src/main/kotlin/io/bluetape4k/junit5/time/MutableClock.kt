package io.bluetape4k.junit5.time

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicReference

/**
 * 테스트에서 wall-clock timeline을 원자적으로 조정하는 [Clock] 구현입니다.
 *
 * [withZone]으로 만든 view는 동일한 timeline을 공유합니다. 이 클래스는
 * monotonic elapsed-time이나 scheduler tick을 모델링하지 않습니다.
 */
open class MutableClock private constructor(
    private val current: AtomicReference<Instant>,
    private val zone: ZoneId,
) : Clock() {
    constructor(initial: Instant, zone: ZoneId = ZoneOffset.UTC) : this(AtomicReference(initial), zone)

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): MutableClock =
        if (zone == this.zone) this else MutableClock(current, zone)

    override fun instant(): Instant = current.get()

    fun advance(duration: Duration): Instant = current.updateAndGet { it.plus(duration) }

    fun advanceSeconds(seconds: Long): Instant = advance(Duration.ofSeconds(seconds))
}
