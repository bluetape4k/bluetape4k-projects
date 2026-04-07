package io.bluetape4k.exposed.postgresql.tsrange

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * [TimestampRange] 값 객체 단위 테스트.
 *
 * DB 접근 없이 [contains], [overlaps], `equals`/`hashCode` 등 순수 로직을 검증한다.
 */
class TimestampRangeTest {

    companion object: KLogging()

    private val start = Instant.parse("2024-01-01T00:00:00Z")
    private val end = Instant.parse("2024-12-31T23:59:59Z")

    // ──────────────────────────────────────────────
    // contains(Instant) 테스트
    // ──────────────────────────────────────────────

    @Test
    fun `contains - 기본 경계 lowerInclusive=true, upperInclusive=false`() {
        val range = TimestampRange(start, end) // [start, end)

        range.contains(Instant.parse("2024-06-15T12:00:00Z")).shouldBeTrue()
        range.contains(start).shouldBeTrue()          // 하한 포함
        range.contains(end).shouldBeFalse()            // 상한 미포함
        range.contains(start.minusMillis(1)).shouldBeFalse()
        range.contains(end.plusMillis(1)).shouldBeFalse()
    }

    @Test
    fun `contains - 양쪽 포함 경계 lowerInclusive=true, upperInclusive=true`() {
        val range = TimestampRange(start, end, lowerInclusive = true, upperInclusive = true) // [start, end]

        range.contains(start).shouldBeTrue()
        range.contains(end).shouldBeTrue()             // 상한도 포함
        range.contains(Instant.parse("2024-06-15T12:00:00Z")).shouldBeTrue()
        range.contains(start.minusMillis(1)).shouldBeFalse()
        range.contains(end.plusMillis(1)).shouldBeFalse()
    }

    @Test
    fun `contains - 양쪽 미포함 경계 lowerInclusive=false, upperInclusive=false`() {
        val range = TimestampRange(start, end, lowerInclusive = false, upperInclusive = false) // (start, end)

        range.contains(start).shouldBeFalse()          // 하한 미포함
        range.contains(end).shouldBeFalse()            // 상한 미포함
        range.contains(start.plusMillis(1)).shouldBeTrue()
        range.contains(end.minusMillis(1)).shouldBeTrue()
    }

    @Test
    fun `contains - 하한 미포함, 상한 포함 경계`() {
        val range = TimestampRange(start, end, lowerInclusive = false, upperInclusive = true) // (start, end]

        range.contains(start).shouldBeFalse()
        range.contains(end).shouldBeTrue()
        range.contains(start.plusMillis(1)).shouldBeTrue()
        range.contains(end.plusMillis(1)).shouldBeFalse()
    }

    // ──────────────────────────────────────────────
    // overlaps(TimestampRange) 테스트
    // ──────────────────────────────────────────────

    @Test
    fun `overlaps - 겹치는 범위`() {
        val range1 = TimestampRange(
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-06-30T23:59:59Z"),
        )
        val range2 = TimestampRange(
            Instant.parse("2024-06-01T00:00:00Z"),
            Instant.parse("2024-12-31T23:59:59Z"),
        )

        range1.overlaps(range2).shouldBeTrue()
        range2.overlaps(range1).shouldBeTrue()
    }

    @Test
    fun `overlaps - 겹치지 않는 범위 (기본 경계)`() {
        // range1=[..., 06-01) 이고 range2=[06-01, ...) → 경계가 닿지만 upperInclusive=false 이므로 안 겹침
        val range1 = TimestampRange(
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-06-01T00:00:00Z"),
        )
        val range2 = TimestampRange(
            Instant.parse("2024-06-01T00:00:00Z"),
            Instant.parse("2024-12-31T23:59:59Z"),
        )

        range1.overlaps(range2).shouldBeFalse()
        range2.overlaps(range1).shouldBeFalse()
    }

    @Test
    fun `overlaps - 인접하지만 경계 포함 시 겹침`() {
        // range1=[..., 06-01] 이고 range2=[06-01, ...) → 06-01이 양쪽에 포함되므로 겹침
        val range1 = TimestampRange(
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-06-01T00:00:00Z"),
            lowerInclusive = true,
            upperInclusive = true,
        )
        val range2 = TimestampRange(
            Instant.parse("2024-06-01T00:00:00Z"),
            Instant.parse("2024-12-31T23:59:59Z"),
        )

        range1.overlaps(range2).shouldBeTrue()
        range2.overlaps(range1).shouldBeTrue()
    }

    @Test
    fun `overlaps - 완전히 분리된 범위`() {
        val range1 = TimestampRange(
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-03-01T00:00:00Z"),
        )
        val range2 = TimestampRange(
            Instant.parse("2024-06-01T00:00:00Z"),
            Instant.parse("2024-09-01T00:00:00Z"),
        )

        range1.overlaps(range2).shouldBeFalse()
        range2.overlaps(range1).shouldBeFalse()
    }

    // ──────────────────────────────────────────────
    // data class equals / hashCode 테스트
    // ──────────────────────────────────────────────

    @Test
    fun `equals - 동일 값이면 같다`() {
        val r1 = TimestampRange(start, end, lowerInclusive = true, upperInclusive = false)
        val r2 = TimestampRange(start, end, lowerInclusive = true, upperInclusive = false)

        (r1 == r2).shouldBeTrue()
        (r1.hashCode() == r2.hashCode()).shouldBeTrue()
    }

    @Test
    fun `equals - 경계 포함 여부가 다르면 다르다`() {
        val r1 = TimestampRange(start, end, lowerInclusive = true, upperInclusive = false)
        val r2 = TimestampRange(start, end, lowerInclusive = true, upperInclusive = true)

        (r1 == r2).shouldBeFalse()
    }

    @Test
    fun `equals - 시작 또는 종료가 다르면 다르다`() {
        val r1 = TimestampRange(start, end)
        val r2 = TimestampRange(start.plusSeconds(1), end)

        (r1 == r2).shouldBeFalse()
    }

    @Test
    fun `기본 경계 값 확인`() {
        val range = TimestampRange(start, end)

        range.lowerInclusive shouldBeEqualTo true
        range.upperInclusive shouldBeEqualTo false
    }
}
