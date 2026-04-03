package io.bluetape4k.protobuf

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class TimestampSupportTest {
    companion object : KLogging()

    @Test
    fun `PROTO_TIMESTAMP_MIN 상수는 최소값을 가진다`() {
        PROTO_TIMESTAMP_MIN.shouldNotBeNull()
        PROTO_TIMESTAMP_MIN.seconds shouldBeEqualTo -62135596800L
    }

    @Test
    fun `PROTO_TIMESTAMP_MAX 상수는 최대값을 가진다`() {
        PROTO_TIMESTAMP_MAX.shouldNotBeNull()
        PROTO_TIMESTAMP_MAX.seconds shouldBeEqualTo 253402300799L
    }

    @Test
    fun `PROTO_TIMESTAMP_EPOCH 상수는 Unix epoch을 나타낸다`() {
        PROTO_TIMESTAMP_EPOCH.shouldNotBeNull()
        PROTO_TIMESTAMP_EPOCH.seconds shouldBeEqualTo 0L
        PROTO_TIMESTAMP_EPOCH.nanos shouldBeEqualTo 0
    }

    @Test
    fun `protoTimestampOfSeconds - epoch 초로 Timestamp를 생성한다`() {
        val ts = protoTimestampOfSeconds(42L)
        ts.seconds shouldBeEqualTo 42L
        ts.nanos shouldBeEqualTo 0
    }

    @Test
    fun `protoTimestampOfMillis - epoch 밀리초로 Timestamp를 생성한다`() {
        val ts = protoTimestampOfMillis(2000L)
        ts.seconds shouldBeEqualTo 2L
    }

    @Test
    fun `protoTimestampOfMicros - epoch 마이크로초로 Timestamp를 생성한다`() {
        val ts = protoTimestampOfMicros(3_000_000L)
        ts.seconds shouldBeEqualTo 3L
    }

    @Test
    fun `protoTimestampOfNanos - epoch 나노초로 Timestamp를 생성한다`() {
        val ts = protoTimestampOfNanos(4_000_000_000L)
        ts.seconds shouldBeEqualTo 4L
    }

    @Test
    fun `protoTimestampOf(Instant) - Instant를 Timestamp로 변환한다`() {
        val instant = Instant.ofEpochSecond(100L, 999)
        val ts = protoTimestampOf(instant)
        ts.seconds shouldBeEqualTo 100L
        ts.nanos shouldBeEqualTo 999
    }

    @Test
    fun `protoTimestampOf(Date) - Date를 Timestamp로 변환한다`() {
        val date = Date(5000L)
        val ts = protoTimestampOf(date)
        ts.toMillis() shouldBeEqualTo 5000L
    }

    @Test
    fun `protoTimestampOf(String) - RFC3339 문자열을 Timestamp로 파싱한다`() {
        val ts = protoTimestampOf("1970-01-01T00:00:01Z")
        ts.seconds shouldBeEqualTo 1L
    }

    @Test
    fun `toInstant - Timestamp를 Instant로 변환한다`() {
        val ts = protoTimestampOfSeconds(7L)
        val instant = ts.toInstant()
        instant.epochSecond shouldBeEqualTo 7L
    }

    @Test
    fun `toInstant 왕복 변환 - Instant → Timestamp → Instant`() {
        val original = Instant.ofEpochSecond(123L, 456)
        val ts = protoTimestampOf(original)
        val restored = ts.toInstant()
        restored shouldBeEqualTo original
    }

    @Test
    fun `asString - Timestamp를 RFC3339 문자열로 직렬화한다`() {
        val ts = protoTimestampOfSeconds(0L)
        ts.asString() shouldBeEqualTo "1970-01-01T00:00:00Z"
    }

    @Test
    fun `protoTimestampOfUnchecked - RFC3339 문자열을 파싱한다`() {
        val ts = protoTimestampOfUnchecked("1970-01-01T00:00:01Z")
        ts.seconds shouldBeEqualTo 1L
    }

    @Test
    fun `isValid - 유효 범위 Timestamp는 true를 반환한다`() {
        protoTimestampOfSeconds(0L).isValid.shouldBeTrue()
    }

    @Test
    fun `compareTo - 이전 Timestamp는 음수를 반환한다`() {
        val earlier = protoTimestampOfSeconds(1L)
        val later = protoTimestampOfSeconds(2L)
        (earlier.compareTo(later) < 0).shouldBeTrue()
    }

    @Test
    fun `plus 연산자 - Timestamp에 Duration을 더한다`() {
        val ts = protoTimestampOfSeconds(1L)
        val duration = protoDurationOf(protoTimestampOfSeconds(0L), protoTimestampOfSeconds(2L))
        val result = ts + duration
        result.seconds shouldBeEqualTo 3L
    }

    @Test
    fun `minus 연산자 - Timestamp에서 Duration을 뺀다`() {
        val ts = protoTimestampOfSeconds(5L)
        val duration = protoDurationOf(protoTimestampOfSeconds(0L), protoTimestampOfSeconds(3L))
        val result = ts - duration
        result.seconds shouldBeEqualTo 2L
    }

    @Test
    fun `protoDurationOf - 두 Timestamp 사이의 Duration을 계산한다`() {
        val from = protoTimestampOfSeconds(1L)
        val to = protoTimestampOfSeconds(4L)
        val duration = protoDurationOf(from, to)
        duration.seconds shouldBeEqualTo 3L
    }

    @Test
    fun `toSeconds - 밀리초 Timestamp를 초로 변환 시 소수점 이하는 버린다`() {
        val ts = protoTimestampOfMillis(1999L)
        ts.toSeconds() shouldBeEqualTo 1L
    }

    @Test
    fun `toMillis - Timestamp를 epoch 밀리초로 변환한다`() {
        val ts = protoTimestampOfSeconds(2L)
        ts.toMillis() shouldBeEqualTo 2000L
    }
}
