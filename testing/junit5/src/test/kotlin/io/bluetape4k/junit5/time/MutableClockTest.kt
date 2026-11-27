package io.bluetape4k.junit5.time

import io.bluetape4k.assertions.shouldBeEqualTo
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Test

class MutableClockTest {
    @Test
    fun `initial instant and zone are observable`() {
        val initial = Instant.parse("2026-11-27T00:00:00Z")
        val clock = MutableClock(initial, ZoneId.of("Asia/Seoul"))

        clock.instant() shouldBeEqualTo initial
        clock.zone shouldBeEqualTo ZoneId.of("Asia/Seoul")
    }

    @Test
    fun `duration and seconds advance share one timeline`() {
        val initial = Instant.parse("2026-11-27T00:00:00Z")
        val clock = MutableClock(initial)

        clock.advance(Duration.ofMinutes(2)) shouldBeEqualTo initial.plusSeconds(120)
        clock.advanceSeconds(30) shouldBeEqualTo initial.plusSeconds(150)
        clock.instant() shouldBeEqualTo initial.plusSeconds(150)
    }

    @Test
    fun `zone view shares timeline and supports negative duration`() {
        val initial = Instant.parse("2026-11-27T00:00:00Z")
        val clock = MutableClock(initial)
        val view = clock.withZone(ZoneOffset.ofHours(9))

        view.zone shouldBeEqualTo ZoneOffset.ofHours(9)
        clock.advanceSeconds(60)
        view.instant() shouldBeEqualTo initial.plusSeconds(60)
        view.advance(Duration.ofSeconds(-10)) shouldBeEqualTo initial.plusSeconds(50)
        clock.instant() shouldBeEqualTo initial.plusSeconds(50)
    }

    @Test
    fun `concurrent advances are atomic`() {
        val initial = Instant.parse("2026-11-27T00:00:00Z")
        val clock = MutableClock(initial)
        val executor = Executors.newFixedThreadPool(8)

        try {
            val futures = List(256) { executor.submit { clock.advanceSeconds(1) } }
            futures.forEach { it.get(5, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        clock.instant() shouldBeEqualTo initial.plusSeconds(256)
    }
}
