package io.bluetape4k.assertions

import io.bluetape4k.assertions.assertFailsWith
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Date

class DateTimesTest {

    // ── Instant ───────────────────────────────────────────────────────────────

    @Nested
    inner class InstantAssertions {

        private val now = Instant.parse("2026-05-07T10:00:00Z")
        private val before = Instant.parse("2026-05-07T09:00:00Z")
        private val after = Instant.parse("2026-05-07T11:00:00Z")

        @Test
        fun `shouldBeAfter passes when receiver is after expected`() {
            after shouldBeAfter now
        }

        @Test
        fun `shouldBeAfter fails when receiver is equal to expected`() {
            assertFailsWith<AssertionFailedError> {
                now shouldBeAfter now
            }
        }

        @Test
        fun `shouldBeAfter fails when receiver is before expected`() {
            assertFailsWith<AssertionFailedError> {
                before shouldBeAfter now
            }
        }

        @Test
        fun `shouldBeBefore passes when receiver is before expected`() {
            before shouldBeBefore now
        }

        @Test
        fun `shouldBeBefore fails when receiver is equal to expected`() {
            assertFailsWith<AssertionFailedError> {
                now shouldBeBefore now
            }
        }

        @Test
        fun `shouldBeBefore fails when receiver is after expected`() {
            assertFailsWith<AssertionFailedError> {
                after shouldBeBefore now
            }
        }

        @Test
        fun `shouldBeOnOrAfter passes when receiver is after expected`() {
            after shouldBeOnOrAfter now
        }

        @Test
        fun `shouldBeOnOrAfter passes when receiver is equal to expected`() {
            now shouldBeOnOrAfter now
        }

        @Test
        fun `shouldBeOnOrAfter fails when receiver is before expected`() {
            assertFailsWith<AssertionFailedError> {
                before shouldBeOnOrAfter now
            }
        }

        @Test
        fun `shouldBeOnOrBefore passes when receiver is before expected`() {
            before shouldBeOnOrBefore now
        }

        @Test
        fun `shouldBeOnOrBefore passes when receiver is equal to expected`() {
            now shouldBeOnOrBefore now
        }

        @Test
        fun `shouldBeOnOrBefore fails when receiver is after expected`() {
            assertFailsWith<AssertionFailedError> {
                after shouldBeOnOrBefore now
            }
        }

        @Test
        fun `shouldBeAfter supports chaining`() {
            after
                .shouldBeAfter(now)
                .shouldBeAfter(before)
        }
    }

    // ── ZonedDateTime ─────────────────────────────────────────────────────────

    @Nested
    inner class ZonedDateTimeAssertions {

        private val now = ZonedDateTime.of(2026, 5, 7, 10, 0, 0, 0, ZoneOffset.UTC)
        private val before = ZonedDateTime.of(2026, 5, 7, 9, 0, 0, 0, ZoneOffset.UTC)
        private val after = ZonedDateTime.of(2026, 5, 7, 11, 0, 0, 0, ZoneOffset.UTC)

        @Test
        fun `shouldBeAfter passes when receiver is after expected`() {
            after shouldBeAfter now
        }

        @Test
        fun `shouldBeAfter fails when receiver is equal to expected`() {
            assertFailsWith<AssertionFailedError> {
                now shouldBeAfter now
            }
        }

        @Test
        fun `shouldBeAfter fails when receiver is before expected`() {
            assertFailsWith<AssertionFailedError> {
                before shouldBeAfter now
            }
        }

        @Test
        fun `shouldBeBefore passes when receiver is before expected`() {
            before shouldBeBefore now
        }

        @Test
        fun `shouldBeBefore fails when receiver is equal to expected`() {
            assertFailsWith<AssertionFailedError> {
                now shouldBeBefore now
            }
        }

        @Test
        fun `shouldBeBefore fails when receiver is after expected`() {
            assertFailsWith<AssertionFailedError> {
                after shouldBeBefore now
            }
        }

        @Test
        fun `shouldBeOnOrAfter passes when receiver is after expected`() {
            after shouldBeOnOrAfter now
        }

        @Test
        fun `shouldBeOnOrAfter passes when receiver is equal to expected`() {
            now shouldBeOnOrAfter now
        }

        @Test
        fun `shouldBeOnOrAfter fails when receiver is before expected`() {
            assertFailsWith<AssertionFailedError> {
                before shouldBeOnOrAfter now
            }
        }

        @Test
        fun `shouldBeOnOrBefore passes when receiver is before expected`() {
            before shouldBeOnOrBefore now
        }

        @Test
        fun `shouldBeOnOrBefore passes when receiver is equal to expected`() {
            now shouldBeOnOrBefore now
        }

        @Test
        fun `shouldBeOnOrBefore fails when receiver is after expected`() {
            assertFailsWith<AssertionFailedError> {
                after shouldBeOnOrBefore now
            }
        }

        @Test
        fun `shouldBeAfter supports chaining`() {
            after
                .shouldBeAfter(now)
                .shouldBeAfter(before)
        }
    }

    // ── OffsetDateTime ────────────────────────────────────────────────────────

    @Nested
    inner class OffsetDateTimeAssertions {

        private val now = OffsetDateTime.of(2026, 5, 7, 10, 0, 0, 0, ZoneOffset.UTC)
        private val before = OffsetDateTime.of(2026, 5, 7, 9, 0, 0, 0, ZoneOffset.UTC)
        private val after = OffsetDateTime.of(2026, 5, 7, 11, 0, 0, 0, ZoneOffset.UTC)

        @Test
        fun `shouldBeAfter passes when receiver is after expected`() {
            after shouldBeAfter now
        }

        @Test
        fun `shouldBeAfter fails when receiver is equal to expected`() {
            assertFailsWith<AssertionFailedError> {
                now shouldBeAfter now
            }
        }

        @Test
        fun `shouldBeAfter fails when receiver is before expected`() {
            assertFailsWith<AssertionFailedError> {
                before shouldBeAfter now
            }
        }

        @Test
        fun `shouldBeBefore passes when receiver is before expected`() {
            before shouldBeBefore now
        }

        @Test
        fun `shouldBeBefore fails when receiver is equal to expected`() {
            assertFailsWith<AssertionFailedError> {
                now shouldBeBefore now
            }
        }

        @Test
        fun `shouldBeBefore fails when receiver is after expected`() {
            assertFailsWith<AssertionFailedError> {
                after shouldBeBefore now
            }
        }

        @Test
        fun `shouldBeOnOrAfter passes when receiver is after expected`() {
            after shouldBeOnOrAfter now
        }

        @Test
        fun `shouldBeOnOrAfter passes when receiver is equal to expected`() {
            now shouldBeOnOrAfter now
        }

        @Test
        fun `shouldBeOnOrAfter fails when receiver is before expected`() {
            assertFailsWith<AssertionFailedError> {
                before shouldBeOnOrAfter now
            }
        }

        @Test
        fun `shouldBeOnOrBefore passes when receiver is before expected`() {
            before shouldBeOnOrBefore now
        }

        @Test
        fun `shouldBeOnOrBefore passes when receiver is equal to expected`() {
            now shouldBeOnOrBefore now
        }

        @Test
        fun `shouldBeOnOrBefore fails when receiver is after expected`() {
            assertFailsWith<AssertionFailedError> {
                after shouldBeOnOrBefore now
            }
        }

        @Test
        fun `shouldBeAfter supports chaining`() {
            after
                .shouldBeAfter(now)
                .shouldBeAfter(before)
        }
    }

    // ── LocalDateTime ─────────────────────────────────────────────────────────

    @Nested
    inner class LocalDateTimeAssertions {

        private val now = LocalDateTime.of(2026, 5, 7, 10, 0, 0)
        private val before = LocalDateTime.of(2026, 5, 7, 9, 0, 0)
        private val after = LocalDateTime.of(2026, 5, 7, 11, 0, 0)

        @Test
        fun `shouldBeAfter passes when receiver is after expected`() {
            after shouldBeAfter now
        }

        @Test
        fun `shouldBeAfter fails when receiver is equal to expected`() {
            assertFailsWith<AssertionFailedError> {
                now shouldBeAfter now
            }
        }

        @Test
        fun `shouldBeAfter fails when receiver is before expected`() {
            assertFailsWith<AssertionFailedError> {
                before shouldBeAfter now
            }
        }

        @Test
        fun `shouldBeBefore passes when receiver is before expected`() {
            before shouldBeBefore now
        }

        @Test
        fun `shouldBeBefore fails when receiver is equal to expected`() {
            assertFailsWith<AssertionFailedError> {
                now shouldBeBefore now
            }
        }

        @Test
        fun `shouldBeBefore fails when receiver is after expected`() {
            assertFailsWith<AssertionFailedError> {
                after shouldBeBefore now
            }
        }

        @Test
        fun `shouldBeOnOrAfter passes when receiver is after expected`() {
            after shouldBeOnOrAfter now
        }

        @Test
        fun `shouldBeOnOrAfter passes when receiver is equal to expected`() {
            now shouldBeOnOrAfter now
        }

        @Test
        fun `shouldBeOnOrAfter fails when receiver is before expected`() {
            assertFailsWith<AssertionFailedError> {
                before shouldBeOnOrAfter now
            }
        }

        @Test
        fun `shouldBeOnOrBefore passes when receiver is before expected`() {
            before shouldBeOnOrBefore now
        }

        @Test
        fun `shouldBeOnOrBefore passes when receiver is equal to expected`() {
            now shouldBeOnOrBefore now
        }

        @Test
        fun `shouldBeOnOrBefore fails when receiver is after expected`() {
            assertFailsWith<AssertionFailedError> {
                after shouldBeOnOrBefore now
            }
        }

        @Test
        fun `shouldBeAfter supports chaining`() {
            after
                .shouldBeAfter(now)
                .shouldBeAfter(before)
        }
    }

    // ── LocalDate ─────────────────────────────────────────────────────────────

    @Nested
    inner class LocalDateAssertions {

        private val today = LocalDate.of(2026, 5, 7)
        private val yesterday = LocalDate.of(2026, 5, 6)
        private val tomorrow = LocalDate.of(2026, 5, 8)

        @Test
        fun `shouldBeAfter passes when receiver is after expected`() {
            tomorrow shouldBeAfter today
        }

        @Test
        fun `shouldBeAfter fails when receiver is equal to expected`() {
            assertFailsWith<AssertionFailedError> {
                today shouldBeAfter today
            }
        }

        @Test
        fun `shouldBeAfter fails when receiver is before expected`() {
            assertFailsWith<AssertionFailedError> {
                yesterday shouldBeAfter today
            }
        }

        @Test
        fun `shouldBeBefore passes when receiver is before expected`() {
            yesterday shouldBeBefore today
        }

        @Test
        fun `shouldBeBefore fails when receiver is equal to expected`() {
            assertFailsWith<AssertionFailedError> {
                today shouldBeBefore today
            }
        }

        @Test
        fun `shouldBeBefore fails when receiver is after expected`() {
            assertFailsWith<AssertionFailedError> {
                tomorrow shouldBeBefore today
            }
        }

        @Test
        fun `shouldBeOnOrAfter passes when receiver is after expected`() {
            tomorrow shouldBeOnOrAfter today
        }

        @Test
        fun `shouldBeOnOrAfter passes when receiver is equal to expected`() {
            today shouldBeOnOrAfter today
        }

        @Test
        fun `shouldBeOnOrAfter fails when receiver is before expected`() {
            assertFailsWith<AssertionFailedError> {
                yesterday shouldBeOnOrAfter today
            }
        }

        @Test
        fun `shouldBeOnOrBefore passes when receiver is before expected`() {
            yesterday shouldBeOnOrBefore today
        }

        @Test
        fun `shouldBeOnOrBefore passes when receiver is equal to expected`() {
            today shouldBeOnOrBefore today
        }

        @Test
        fun `shouldBeOnOrBefore fails when receiver is after expected`() {
            assertFailsWith<AssertionFailedError> {
                tomorrow shouldBeOnOrBefore today
            }
        }

        @Test
        fun `shouldBeAfter supports chaining`() {
            tomorrow
                .shouldBeAfter(today)
                .shouldBeAfter(yesterday)
        }
    }

    // ── LocalTime ─────────────────────────────────────────────────────────────

    @Nested
    inner class LocalTimeAssertions {

        private val noon = LocalTime.of(12, 0, 0)
        private val morning = LocalTime.of(9, 0, 0)
        private val afternoon = LocalTime.of(15, 0, 0)

        @Test
        fun `shouldBeAfter passes when receiver is after expected`() {
            afternoon shouldBeAfter noon
        }

        @Test
        fun `shouldBeAfter fails when receiver is equal to expected`() {
            assertFailsWith<AssertionFailedError> {
                noon shouldBeAfter noon
            }
        }

        @Test
        fun `shouldBeAfter fails when receiver is before expected`() {
            assertFailsWith<AssertionFailedError> {
                morning shouldBeAfter noon
            }
        }

        @Test
        fun `shouldBeBefore passes when receiver is before expected`() {
            morning shouldBeBefore noon
        }

        @Test
        fun `shouldBeBefore fails when receiver is equal to expected`() {
            assertFailsWith<AssertionFailedError> {
                noon shouldBeBefore noon
            }
        }

        @Test
        fun `shouldBeBefore fails when receiver is after expected`() {
            assertFailsWith<AssertionFailedError> {
                afternoon shouldBeBefore noon
            }
        }

        @Test
        fun `shouldBeOnOrAfter passes when receiver is after expected`() {
            afternoon shouldBeOnOrAfter noon
        }

        @Test
        fun `shouldBeOnOrAfter passes when receiver is equal to expected`() {
            noon shouldBeOnOrAfter noon
        }

        @Test
        fun `shouldBeOnOrAfter fails when receiver is before expected`() {
            assertFailsWith<AssertionFailedError> {
                morning shouldBeOnOrAfter noon
            }
        }

        @Test
        fun `shouldBeOnOrBefore passes when receiver is before expected`() {
            morning shouldBeOnOrBefore noon
        }

        @Test
        fun `shouldBeOnOrBefore passes when receiver is equal to expected`() {
            noon shouldBeOnOrBefore noon
        }

        @Test
        fun `shouldBeOnOrBefore fails when receiver is after expected`() {
            assertFailsWith<AssertionFailedError> {
                afternoon shouldBeOnOrBefore noon
            }
        }

        @Test
        fun `shouldBeAfter supports chaining`() {
            afternoon
                .shouldBeAfter(noon)
                .shouldBeAfter(morning)
        }
    }

    // ── java.util.Date ────────────────────────────────────────────────────────

    @Nested
    inner class JavaUtilDateAssertions {

        private val base = Date(1_000_000L)
        private val earlier = Date(999_999L)
        private val later = Date(1_000_001L)

        // Millisecond boundary: same millisecond is equal (not after/before)
        private val sameMs = Date(1_000_000L)

        @Test
        fun `shouldBeAfter passes when receiver is after expected`() {
            later shouldBeAfter base
        }

        @Test
        fun `shouldBeAfter fails when receiver is equal (same millisecond)`() {
            assertFailsWith<AssertionFailedError> {
                sameMs shouldBeAfter base
            }
        }

        @Test
        fun `shouldBeAfter fails when receiver is before expected`() {
            assertFailsWith<AssertionFailedError> {
                earlier shouldBeAfter base
            }
        }

        @Test
        fun `shouldBeBefore passes when receiver is before expected`() {
            earlier shouldBeBefore base
        }

        @Test
        fun `shouldBeBefore fails when receiver is equal (same millisecond)`() {
            assertFailsWith<AssertionFailedError> {
                sameMs shouldBeBefore base
            }
        }

        @Test
        fun `shouldBeBefore fails when receiver is after expected`() {
            assertFailsWith<AssertionFailedError> {
                later shouldBeBefore base
            }
        }

        @Test
        fun `shouldBeOnOrAfter passes when receiver is after expected`() {
            later shouldBeOnOrAfter base
        }

        @Test
        fun `shouldBeOnOrAfter passes when receiver is equal (same millisecond)`() {
            sameMs shouldBeOnOrAfter base
        }

        @Test
        fun `shouldBeOnOrAfter fails when receiver is before expected`() {
            assertFailsWith<AssertionFailedError> {
                earlier shouldBeOnOrAfter base
            }
        }

        @Test
        fun `shouldBeOnOrBefore passes when receiver is before expected`() {
            earlier shouldBeOnOrBefore base
        }

        @Test
        fun `shouldBeOnOrBefore passes when receiver is equal (same millisecond)`() {
            sameMs shouldBeOnOrBefore base
        }

        @Test
        fun `shouldBeOnOrBefore fails when receiver is after expected`() {
            assertFailsWith<AssertionFailedError> {
                later shouldBeOnOrBefore base
            }
        }

        @Test
        fun `shouldBeAfter supports chaining`() {
            later
                .shouldBeAfter(base)
                .shouldBeAfter(earlier)
        }

        @Test
        fun `millisecond precision boundary - one ms difference is enough for after`() {
            val t1 = Date(0L)
            val t2 = Date(1L)
            t2 shouldBeAfter t1
            t1 shouldBeBefore t2
        }
    }
}
