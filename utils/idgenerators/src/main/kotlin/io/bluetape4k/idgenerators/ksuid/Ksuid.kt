package io.bluetape4k.idgenerators.ksuid

import io.bluetape4k.codec.encodeHexString
import io.bluetape4k.idgenerators.IdGenerator
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * KSUID (K-Sortable Unique IDentifier) 생성 전략을 통합 제공하는 진입점.
 *
 * 초(seconds) 기반 [Seconds]와 밀리초(milliseconds) 기반 [Millis] 두 가지 전략을 제공합니다.
 * 기본 전략은 초 기반의 [Seconds]입니다.
 *
 * ## 사용 예
 * ```kotlin
 * // 초 기반 KSUID (기본)
 * val id: String = Ksuid.Seconds.generate()
 *
 * // 밀리초 기반 KSUID
 * val id2: String = Ksuid.Millis.generate()
 *
 * // IdGenerator 인터페이스로 사용
 * val gen: Ksuid.Generator = Ksuid.Seconds
 * val id3: String = gen.nextId()
 * ```
 */
object Ksuid {
    /**
     * KSUID 생성기 공통 인터페이스.
     *
     * [IdGenerator]를 확장하여 KSUID 전용 편의 메서드를 추가합니다.
     */
    interface Generator : IdGenerator<String> {
        /**
         * 현재 시각으로 KSUID를 생성합니다. [nextId]와 동일합니다.
         *
         * ```kotlin
         * val gen: Ksuid.Generator = Ksuid.Seconds
         * val id: String = gen.generate()
         * // id.length == 27
         * ```
         */
        fun generate(): String = nextId()

        /**
         * 지정한 [Instant]로 KSUID를 생성합니다.
         *
         * ```kotlin
         * val gen: Ksuid.Generator = Ksuid.Seconds
         * val id: String = gen.generate(Instant.now())
         * // id.length == 27
         * ```
         */
        fun generate(instant: Instant): String

        /**
         * 지정한 [Date]로 KSUID를 생성합니다.
         *
         * ```kotlin
         * val gen: Ksuid.Generator = Ksuid.Seconds
         * val id: String = gen.generate(Date())
         * // id.length == 27
         * ```
         */
        fun generate(date: Date): String

        /**
         * 지정한 [LocalDateTime]으로 KSUID를 생성합니다. (UTC 기준)
         *
         * ```kotlin
         * val gen: Ksuid.Generator = Ksuid.Seconds
         * val id: String = gen.generate(LocalDateTime.now())
         * // id.length == 27
         * ```
         */
        fun generate(dt: LocalDateTime): String

        /**
         * KSUID를 파싱하여 시간, 타임스탬프, 페이로드 정보를 문자열로 반환합니다.
         *
         * ```kotlin
         * val gen: Ksuid.Generator = Ksuid.Seconds
         * val id: String = gen.generate()
         * val info: String = gen.prettyString(id)
         * // info.contains("Time") == true
         * ```
         */
        fun prettyString(ksuid: String): String
    }

    /**
     * 초(seconds) 기반 KSUID 생성기.
     *
     * 20 bytes = 4 bytes(timestamp) + 16 bytes(random payload).
     * Base62 인코딩으로 27자 문자열을 생성합니다.
     *
     * ```kotlin
     * val id: String = Ksuid.Seconds.generate()
     * id.length == 27
     * ```
     */
    object Seconds : Generator, KLogging() {
        private const val EPOCH_SECONDS = 1_400_000_000L

        const val TIMESTAMP_LEN = 4
        const val PAYLOAD_LEN = 16
        const val MAX_ENCODED_LEN = 27
        const val TOTAL_BYTES = TIMESTAMP_LEN + PAYLOAD_LEN

        private val random: SecureRandom = SecureRandom()

        override fun nextId(): String = generate()

        override fun nextIdAsString(): String = generate()

        override fun generate(): String = generate(generateTimestamp())

        override fun generate(instant: Instant): String = generate(generateTimestamp(instant.epochSecond))

        override fun generate(date: Date): String = generate(generateTimestamp(date.time / 1000L))

        override fun generate(dt: LocalDateTime): String =
            generate(generateTimestamp(dt.toInstant(ZoneOffset.UTC).epochSecond))

        private fun generate(timestamp: ByteArray): String {
            val buffer = ByteBuffer.allocate(TOTAL_BYTES)
            buffer.put(timestamp)
            buffer.put(generatePayload())
            val uid = BytesBase62.encode(buffer.array())
            log.trace { "generated uid=$uid" }
            return uid.substring(0, MAX_ENCODED_LEN)
        }

        private fun generateTimestamp(epochSeconds: Long = System.currentTimeMillis() / 1000): ByteArray =
            ByteBuffer.allocate(TIMESTAMP_LEN).putInt((epochSeconds - EPOCH_SECONDS).toInt()).array()

        private fun generatePayload(): ByteArray = ByteArray(PAYLOAD_LEN).apply { random.nextBytes(this) }

        override fun prettyString(ksuid: String): String {
            val bytes = BytesBase62.decode(ksuid, expectedBytes = TOTAL_BYTES)
            require(bytes.size >= TOTAL_BYTES) { "Invalid ksuid length. size=${bytes.size}" }
            val timestamp = extractTimestamp(bytes)
            val utcTimeString = Instant.ofEpochSecond(timestamp).atZone(ZoneOffset.UTC)
            return """
                |Time = $utcTimeString
                |Timestamp = $timestamp
                |Payload = ${extractPayload(bytes)}
                """.trimMargin()
        }

        private fun extractTimestamp(decodedKsuid: ByteArray): Long =
            ByteBuffer.wrap(decodedKsuid.copyOf(TIMESTAMP_LEN)).int.toLong() + EPOCH_SECONDS

        private fun extractPayload(decodedKsuid: ByteArray): String =
            decodedKsuid.copyOfRange(TIMESTAMP_LEN, TIMESTAMP_LEN + PAYLOAD_LEN).encodeHexString()
    }

    /**
     * 밀리초(milliseconds) 기반 KSUID 생성기.
     *
     * 20 bytes = 8 bytes(timestamp) + 12 bytes(random payload).
     * Base62 인코딩으로 27자 문자열을 생성합니다.
     *
     * ```kotlin
     * val id: String = Ksuid.Millis.generate()
     * id.length == 27
     * ```
     */
    object Millis : Generator, KLogging() {
        private const val EPOCH_MILLIS = 1_400_000_000_000L

        const val TIMESTAMP_LEN = 8
        const val PAYLOAD_LEN = 12
        const val MAX_ENCODED_LEN = 27
        const val TOTAL_BYTES = TIMESTAMP_LEN + PAYLOAD_LEN

        private val random: SecureRandom = SecureRandom()

        override fun nextId(): String = generate()

        override fun nextIdAsString(): String = generate()

        override fun generate(): String = generate(generateTimestamp())

        override fun generate(instant: Instant): String = generate(generateTimestamp(instant.toEpochMilli()))

        override fun generate(date: Date): String = generate(generateTimestamp(date.time))

        override fun generate(dt: LocalDateTime): String =
            generate(generateTimestamp(dt.toInstant(ZoneOffset.UTC).toEpochMilli()))

        private fun generate(timestamp: ByteArray): String {
            val buffer = ByteBuffer.allocate(TOTAL_BYTES).put(timestamp).put(generatePayload())
            val uid = BytesBase62.encode(buffer.array())
            log.trace { "generated uid=$uid" }
            return uid.substring(0, MAX_ENCODED_LEN)
        }

        private fun generateTimestamp(epochMillis: Long = System.currentTimeMillis()): ByteArray =
            ByteBuffer.allocate(TIMESTAMP_LEN).putLong(epochMillis - EPOCH_MILLIS).array()

        private fun generatePayload(): ByteArray = ByteArray(PAYLOAD_LEN).apply { random.nextBytes(this) }

        override fun prettyString(ksuid: String): String {
            val bytes = BytesBase62.decode(ksuid, expectedBytes = TOTAL_BYTES)
            val timestamp = extractTimestamp(bytes)
            val utcTimeString = Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC)
            return """
                |Time = $utcTimeString
                |Timestamp = $timestamp
                |Payload = ${extractPayload(bytes)}
                """.trimMargin()
        }

        private fun extractTimestamp(decodedKsuid: ByteArray): Long =
            ByteBuffer.wrap(decodedKsuid.copyOf(TIMESTAMP_LEN)).long + EPOCH_MILLIS

        private fun extractPayload(decodedKsuid: ByteArray): String =
            decodedKsuid.copyOfRange(TIMESTAMP_LEN, decodedKsuid.size - TIMESTAMP_LEN).encodeHexString()
    }

    // ── 하위 호환 상수 (Seconds 위임) ──────────────────────────────────────

    /** @see Seconds.TIMESTAMP_LEN */
    const val TIMESTAMP_LEN = Seconds.TIMESTAMP_LEN

    /** @see Seconds.PAYLOAD_LEN */
    const val PAYLOAD_LEN = Seconds.PAYLOAD_LEN

    /** @see Seconds.MAX_ENCODED_LEN */
    const val MAX_ENCODED_LEN = Seconds.MAX_ENCODED_LEN

    /** @see Seconds.TOTAL_BYTES */
    const val TOTAL_BYTES = Seconds.TOTAL_BYTES

    // ── 하위 호환 메서드 (Seconds 위임) ─────────────────────────────────────

    /**
     * 초 기반 KSUID를 생성합니다.
     *
     * @see Seconds.nextId
     */
    @Deprecated("Use Ksuid.Seconds.nextId()", ReplaceWith("Ksuid.Seconds.nextId()"))
    fun nextId(): String = Seconds.nextId()

    /**
     * 초 기반 KSUID를 문자열로 반환합니다.
     *
     * @see Seconds.nextIdAsString
     */
    @Deprecated("Use Ksuid.Seconds.nextIdAsString()", ReplaceWith("Ksuid.Seconds.nextIdAsString()"))
    fun nextIdAsString(): String = Seconds.nextIdAsString()

    /**
     * 현재 시각으로 초 기반 KSUID를 생성합니다.
     *
     * @see Seconds.generate
     */
    @Deprecated("Use Ksuid.Seconds.generate()", ReplaceWith("Ksuid.Seconds.generate()"))
    fun generate(): String = Seconds.generate()

    /**
     * 지정한 [Instant]로 초 기반 KSUID를 생성합니다.
     *
     * @see Seconds.generate
     */
    @Deprecated("Use Ksuid.Seconds.generate(instant)", ReplaceWith("Ksuid.Seconds.generate(instant)"))
    fun generate(instant: Instant): String = Seconds.generate(instant)

    /**
     * 지정한 [Date]로 초 기반 KSUID를 생성합니다.
     *
     * @see Seconds.generate
     */
    @Deprecated("Use Ksuid.Seconds.generate(date)", ReplaceWith("Ksuid.Seconds.generate(date)"))
    fun generate(date: Date): String = Seconds.generate(date)

    /**
     * 지정한 [LocalDateTime]으로 초 기반 KSUID를 생성합니다.
     *
     * @see Seconds.generate
     */
    @Deprecated("Use Ksuid.Seconds.generate(dt)", ReplaceWith("Ksuid.Seconds.generate(dt)"))
    fun generate(dt: LocalDateTime): String = Seconds.generate(dt)

    /**
     * 초 기반 KSUID를 파싱합니다.
     *
     * @see Seconds.prettyString
     */
    @Deprecated("Use Ksuid.Seconds.prettyString(ksuid)", ReplaceWith("Ksuid.Seconds.prettyString(ksuid)"))
    fun prettyString(ksuid: String): String = Seconds.prettyString(ksuid)
}
