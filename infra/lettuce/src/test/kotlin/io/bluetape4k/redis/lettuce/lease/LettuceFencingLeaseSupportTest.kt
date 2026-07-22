package io.bluetape4k.redis.lettuce.lease

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.assertions.shouldNotContain
import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.RedisConnectionException
import io.lettuce.core.RedisException
import io.lettuce.core.cluster.SlotHash
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

class LettuceFencingLeaseSupportTest {

    @Test
    fun `derived keys use the exact ordering domain and one wire slot`() {
        val config = LettuceFencingLeaseConfig("orders", "rebuild", 7)
        val expected = FencingLeaseKeys(
            lease = "fence:{orders:rebuild}:7:lease",
            counter = "fence:{orders:rebuild}:7:counter",
        )

        deriveFencingLeaseKeys(config, StringCodec.UTF8) shouldBeEqualTo expected
        deriveFencingLeaseKeys(config, SameSlotWireCodec) shouldBeEqualTo expected
        SlotHash.getSlot(SameSlotWireCodec.encodeKey(expected.lease)) shouldBeEqualTo
            SlotHash.getSlot(SameSlotWireCodec.encodeKey(expected.counter))
    }

    @Test
    fun `derived keys reject a codec that routes wire bytes to different slots`() {
        val config = LettuceFencingLeaseConfig("orders", "rebuild", 7)

        SlotHash.getSlot(SplitSlotWireCodec.encodeKey("lease")) shouldNotBeEqualTo
            SlotHash.getSlot(SplitSlotWireCodec.encodeKey("counter"))
        val error = assertFailsWith<IllegalArgumentException> {
            deriveFencingLeaseKeys(config, SplitSlotWireCodec)
        }

        error.message shouldBeEqualTo "Derived fencing lease keys must share one Redis Cluster slot."
    }

    @Test
    fun `canonical decimals preserve exact long values and ordering`() {
        listOf("0", "1", "9", "10", Long.MAX_VALUE.toString()).forEach { value ->
            requireCanonicalFencingDecimal(value) shouldBeEqualTo value
            parseCanonicalFencingLong(value) shouldBeEqualTo value.toLong()
        }

        compareCanonicalFencingDecimals("9", "10") shouldBeEqualTo -1
        compareCanonicalFencingDecimals("10", "9") shouldBeEqualTo 1
        compareCanonicalFencingDecimals(Long.MAX_VALUE.toString(), Long.MAX_VALUE.toString()) shouldBeEqualTo 0
    }

    @Test
    fun `canonical decimals reject ambiguous and out of range text without echoing it`() {
        val invalidValues = listOf(
            "", "+1", "-1", "00", "01", " 1", "1 ", "1.0", "1a",
            "9223372036854775808", "1".repeat(20),
        )

        invalidValues.forEach { value ->
            val error = assertFailsWith<IllegalArgumentException> { requireCanonicalFencingDecimal(value) }
            if (value.isNotEmpty()) {
                error.message shouldNotContain value
            }
        }
    }

    @Test
    fun `lease duration accepts only positive whole milliseconds`() {
        Duration.ofMillis(1).requireFencingLeaseMillis() shouldBeEqualTo 1L
        Duration.ofMillis(Long.MAX_VALUE).requireFencingLeaseMillis() shouldBeEqualTo Long.MAX_VALUE

        listOf(Duration.ZERO, Duration.ofMillis(-1), Duration.ofNanos(1), Duration.ofNanos(1_000_001)).forEach { value ->
            val error = assertFailsWith<IllegalArgumentException> { value.requireFencingLeaseMillis() }
            error.message shouldBeEqualTo "leaseTime must fit positive whole milliseconds."
            error.cause shouldBeEqualTo null
        }

        val overflow = assertFailsWith<IllegalArgumentException> {
            Duration.ofSeconds(Long.MAX_VALUE).requireFencingLeaseMillis()
        }
        overflow.message shouldBeEqualTo "leaseTime must fit positive whole milliseconds."
        overflow.cause shouldBeEqualTo null
    }

    @Test
    fun `token epoch mismatch is rejected`() {
        val config = LettuceFencingLeaseConfig("orders", "rebuild", 7)

        requireFencingTokenEpoch(config, FencingToken(7, 1)) shouldBeEqualTo FencingToken(7, 1)
        val error = assertFailsWith<IllegalArgumentException> {
            requireFencingTokenEpoch(config, FencingToken(8, 1))
        }

        error.message shouldBeEqualTo "Fencing token epoch must match the configured ordering domain."
        error.message shouldNotContain "7"
        error.message shouldNotContain "8"
    }

    @Test
    fun `completion wrappers unwrap at most eight levels and stop on identity cycles`() {
        val backend = RedisConnectionException("connection sentinel")
        var nested: Throwable = backend
        repeat(8) { index ->
            nested = if (index % 2 == 0) CompletionException(nested) else ExecutionException(nested)
        }

        nested.unwrapFencingCompletionCause() shouldBeSameInstanceAs backend

        val tooDeep = CompletionException(nested)
        tooDeep.unwrapFencingCompletionCause() shouldNotBeEqualTo backend

        val cycle = CyclicCompletionException()
        cycle.unwrapFencingCompletionCause() shouldBeSameInstanceAs cycle
    }

    @Test
    fun `completion wrapper cancellation is always rethrown`() {
        val cancellation = CancellationException("cancel sentinel")
        val wrapped = CompletionException(ExecutionException(cancellation))

        assertFailsWith<CancellationException> {
            wrapped.unwrapFencingCompletionCause()
        } shouldBeSameInstanceAs cancellation
    }

    @Test
    fun `backend classifier maps only allowlisted Redis failures`() {
        classifyFencingBackendFailure(
            FencingLeaseOperation.ACQUIRE,
            RedisConnectionException("connection sentinel"),
        ) shouldBeEqualTo FencingLeaseBackendFailure(FencingBackendFailureKind.CONNECTION)
        classifyFencingBackendFailure(
            FencingLeaseOperation.INSPECT,
            RedisCommandTimeoutException("timeout sentinel"),
        ) shouldBeEqualTo FencingLeaseBackendFailure(FencingBackendFailureKind.TIMEOUT)
        classifyFencingBackendFailure(
            FencingLeaseOperation.RENEW,
            TimeoutException("timeout sentinel"),
        ) shouldBeEqualTo FencingLeaseBackendFailure(FencingBackendFailureKind.TIMEOUT)
        classifyFencingBackendFailure(
            FencingLeaseOperation.RELEASE,
            RedisException("command sentinel"),
        ) shouldBeEqualTo FencingLeaseBackendFailure(FencingBackendFailureKind.COMMAND)
    }

    @Test
    fun `backend classifier rethrows cancellation validation protocol and unknown failures`() {
        val failures = listOf<Throwable>(
            CancellationException("cancel sentinel"),
            IllegalArgumentException("validation sentinel"),
            FencingLeaseProtocolException(),
            IllegalStateException("unknown sentinel"),
        )

        failures.forEach { failure ->
            val thrown = assertFailsWith<Throwable> {
                classifyFencingBackendFailure(FencingLeaseOperation.BOOTSTRAP, CompletionException(failure))
            }
            thrown shouldBeSameInstanceAs failure
        }
    }

    @Test
    fun `backend log contains only allowlisted correlation fields`() {
        val logger = LoggerFactory.getLogger(FencingLeaseSupportLogger::class.java) as Logger
        val previousLevel = logger.level
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        logger.level = Level.WARN
        val config = LettuceFencingLeaseConfig("secretNamespace", "secretResource", 7)
        val secretKey = "fence:{secretNamespace:secretResource}:7:lease"
        val secretOwner = "secretOwner"
        val secretToken = "secretToken"
        val secretReply = "secretRawReply"
        val secretMessage = "$secretKey $secretOwner $secretToken $secretReply"

        try {
            classifyFencingBackendFailure(
                FencingLeaseOperation.ACQUIRE,
                RedisConnectionException(secretMessage),
                config.domainFingerprint(),
            )

            val message = appender.list.single().formattedMessage
            message shouldContain "operation=ACQUIRE"
            message shouldContain "kind=CONNECTION"
            message shouldContain "exception=io.lettuce.core.RedisConnectionException"
            message shouldContain "domain=${config.domainFingerprint()}"
            message shouldNotContain "secretNamespace"
            message shouldNotContain "secretResource"
            message shouldNotContain secretKey
            message shouldNotContain secretOwner
            message shouldNotContain secretToken
            message shouldNotContain secretReply
        } finally {
            logger.detachAppender(appender)
            logger.level = previousLevel
            appender.stop()
        }
    }

    @Test
    fun `domain fingerprint is stable bounded and distinguishes ordering domains`() {
        val first = LettuceFencingLeaseConfig("orders", "rebuild", 7).domainFingerprint()
        val same = LettuceFencingLeaseConfig("orders", "rebuild", 7).domainFingerprint()
        val other = LettuceFencingLeaseConfig("orders", "rebuild", 8).domainFingerprint()

        first shouldBeEqualTo same
        first shouldNotBeEqualTo other
        first.length shouldBeEqualTo 24
        first.all { character -> character in "0123456789abcdef" }.shouldBeTrue()
    }

    @Test
    fun `fixed reply decoders map every documented result`() {
        val token = FencingToken(7, 42)
        val malformed = FencingLeaseIntegrityFailure(FencingIntegrityFailureKind.MALFORMED_LEASE)
        val invalidCounter = FencingLeaseIntegrityFailure(FencingIntegrityFailureKind.INVALID_COUNTER)
        val counterBehind = FencingLeaseIntegrityFailure(FencingIntegrityFailureKind.COUNTER_BEHIND_LEASE)

        decodeFencingBootstrap(frame("INITIALIZED")) shouldBeEqualTo FencingBootstrapResult.Initialized
        decodeFencingBootstrap(frame("ALREADY_INITIALIZED")) shouldBeEqualTo
            FencingBootstrapResult.AlreadyInitialized
        decodeFencingBootstrap(integrityFrame("MALFORMED_LEASE")) shouldBeEqualTo
            FencingBootstrapResult.IntegrityFailure(malformed)

        decodeFencingAcquire(frame("ACQUIRED", "7", "42")) shouldBeEqualTo FencingAcquireResult.Acquired(token)
        decodeFencingAcquire(frame("ALREADY_OWNED", "7", "42", "10")) shouldBeEqualTo
            FencingAcquireResult.AlreadyOwned(token, 10)
        decodeFencingAcquire(frame("CONTENDED", ttl = "9")) shouldBeEqualTo FencingAcquireResult.Contended(9)
        decodeFencingAcquire(frame("COUNTER_UNAVAILABLE")) shouldBeEqualTo FencingAcquireResult.CounterUnavailable
        decodeFencingAcquire(frame("SEQUENCE_EXHAUSTED")) shouldBeEqualTo FencingAcquireResult.SequenceExhausted
        decodeFencingAcquire(integrityFrame("INVALID_COUNTER")) shouldBeEqualTo
            FencingAcquireResult.IntegrityFailure(invalidCounter)

        decodeFencingInspect(frame("OWNED", "7", "42", "8")) shouldBeEqualTo FencingInspectResult.Owned(token, 8)
        decodeFencingInspect(frame("LOST")) shouldBeEqualTo FencingInspectResult.Lost
        decodeFencingInspect(frame("CONTENDED", ttl = "7")) shouldBeEqualTo FencingInspectResult.Contended(7)
        decodeFencingInspect(integrityFrame("COUNTER_BEHIND_LEASE")) shouldBeEqualTo
            FencingInspectResult.IntegrityFailure(counterBehind)

        decodeFencingRenew(frame("RENEWED")) shouldBeEqualTo FencingRenewResult.Renewed
        decodeFencingRenew(frame("LOST")) shouldBeEqualTo FencingRenewResult.Lost
        decodeFencingRenew(frame("OWNERSHIP_MISMATCH")) shouldBeEqualTo FencingRenewResult.OwnershipMismatch
        decodeFencingRenew(integrityFrame("MALFORMED_LEASE")) shouldBeEqualTo
            FencingRenewResult.IntegrityFailure(malformed)

        decodeFencingRelease(frame("RELEASED")) shouldBeEqualTo FencingReleaseResult.Released
        decodeFencingRelease(frame("LOST")) shouldBeEqualTo FencingReleaseResult.Lost
        decodeFencingRelease(frame("OWNERSHIP_MISMATCH")) shouldBeEqualTo FencingReleaseResult.OwnershipMismatch
        decodeFencingRelease(integrityFrame("INVALID_COUNTER")) shouldBeEqualTo
            FencingReleaseResult.IntegrityFailure(invalidCounter)
    }

    @Test
    fun `fixed reply decoders reject unknown statuses malformed fields and invalid shapes`() {
        val invalidCalls = listOf<() -> Unit>(
            { decodeFencingAcquire(emptyList()) },
            { decodeFencingAcquire(listOf("ACQUIRED", "7", "42")) },
            { decodeFencingAcquire(frame("UNKNOWN")) },
            { decodeFencingAcquire(frame("ACQUIRED", "07", "42")) },
            { decodeFencingAcquire(frame("ACQUIRED", "7", "0")) },
            { decodeFencingAcquire(frame("ACQUIRED", "7", "42", "0")) },
            { decodeFencingAcquire(frame("ALREADY_OWNED", "7", "42", "-1")) },
            { decodeFencingAcquire(frame("CONTENDED", "1", "0", "10")) },
            { decodeFencingAcquire(frame("MALFORMED_LEASE")) },
            { decodeFencingAcquire(integrityFrame("UNKNOWN")) },
            { decodeFencingAcquire(frame("INTEGRITY_FAILURE", "INVALID_COUNTER", "1")) },
            { decodeFencingAcquire(frame("INTEGRITY_FAILURE", "INVALID_COUNTER", ttl = "0")) },
            { decodeFencingInspect(frame("LOST", ttl = "0")) },
            { decodeFencingRenew(frame("RENEWED", "1", "0")) },
            { decodeFencingRelease(integrityFrame("MALFORMED_LEASE").dropLast(1)) },
        )

        invalidCalls.forEach { call ->
            val error = assertFailsWith<FencingLeaseProtocolException> { call() }
            error.message shouldBeEqualTo "Malformed fencing lease response."
        }
    }

    private fun frame(
        status: String,
        value1: String = "0",
        value2: String = "0",
        ttl: String = "-1",
    ): List<String> = listOf(status, value1, value2, ttl)

    private fun integrityFrame(kind: String): List<String> = frame("INTEGRITY_FAILURE", kind)

    private object SameSlotWireCodec: RedisCodec<String, String> {
        override fun decodeKey(bytes: ByteBuffer): String = decode(bytes)
        override fun decodeValue(bytes: ByteBuffer): String = decode(bytes)
        override fun encodeKey(key: String): ByteBuffer = encode("wire:{same}:$key")
        override fun encodeValue(value: String): ByteBuffer = encode(value)
    }

    private object SplitSlotWireCodec: RedisCodec<String, String> {
        override fun decodeKey(bytes: ByteBuffer): String = decode(bytes)
        override fun decodeValue(bytes: ByteBuffer): String = decode(bytes)
        override fun encodeKey(key: String): ByteBuffer =
            encode(if (key.endsWith(":lease") || key == "lease") "wire:{one}" else "wire:{two}")
        override fun encodeValue(value: String): ByteBuffer = encode(value)
    }

    private class CyclicCompletionException: CompletionException(null as Throwable?) {
        override val cause: Throwable get() = this
    }

    private companion object {
        fun encode(value: String): ByteBuffer = ByteBuffer.wrap(value.toByteArray(StandardCharsets.UTF_8))

        fun decode(bytes: ByteBuffer): String = bytes.duplicate().let { copy ->
            ByteArray(copy.remaining()).also(copy::get).toString(StandardCharsets.UTF_8)
        }
    }
}
