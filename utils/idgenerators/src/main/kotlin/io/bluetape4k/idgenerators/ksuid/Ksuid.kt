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
 * KSUID (K-Sortable Unique IDentifier)는 생성 시간에 따라 정렬 가능한 전역적으로 고유한 식별자입니다.
 *
 * KSUID는 일반적으로 UUID와 비슷하지만, 생성 시간을 포함하여 생성 시간에 따라 "대략적으로" 정렬할 수 있습니다. KSUID의 나머지 부분은 무작위로 생성된 바이트입니다.
 * KSUID의 길이는 27이며, Base62로 인코딩되어 URL Safe합니다.
 *
 * 참고: [ksuid](https://github.com/ksuid/ksuid) 를 참고하여 Kotlin으로 제작
 *
 *
 * 요약:
 *
 * * **대충 생성시간으로 정렬될 수 있다**
 * * 문자 27개로 구성되어 있다
 * * 20 byte 배열로 정렬이 가능한다
 * * 문자열 포맷은 Base 62 인코딩(0-9A-Za-z) 방식이다
 * * 문자열 포맷은 URL에 안전하고, 하이픈(`-`)은 없다
 *
 * KSUIDs의 발전 단계는 [A brief history of the UUID](https://segment.com/blog/a-brief-history-of-the-uuid/)를 참고하세요
 */
object Ksuid: IdGenerator<String>, KLogging() {

    private const val EPOCH_SECONDS = 1_400_000_000L

    private const val TIMESTAMP_LEN = 4
    private const val PAYLOAD_LEN = 16
    private const val MAX_ENCODED_LEN = 27

    private val random: SecureRandom = SecureRandom()

    override fun nextId(): String = generate()

    override fun nextIdAsString(): String = generate()

    fun generate(): String {
        return generate(generateTimestamp())
    }

    fun generate(instant: Instant): String {
        return generate(generateTimestamp(instant.epochSecond))
    }

    fun generate(date: Date): String {
        return generate(generateTimestamp(date.time / 1000L))
    }

    fun generate(dt: LocalDateTime): String {
        return generate(generateTimestamp(dt.toInstant(ZoneOffset.UTC).epochSecond))
    }

    /**
     * [timestamp]와 무작위 페이로드를 이용하여 KSUID를 생성합니다.
     *
     * @param timestamp KSUID의 타임스탬프
     */
    private fun generate(timestamp: ByteArray): String {
        val buffer = ByteBuffer.allocate(MAX_ENCODED_LEN)
        buffer.put(timestamp)
        buffer.put(generatePayload())

//        val array = ByteArray(MAX_ENCODED_LEN)
//        timestamp.copyInto(array)
//        generatePayload().copyInto(array, timestamp.size)

        val uid = BytesBase62.encode(buffer.array())
        log.trace { "generated uid=$uid" }

        return uid.substring(0, MAX_ENCODED_LEN)
    }

    private fun generateTimestamp(epochSeconds: Long = System.currentTimeMillis() / 1000): ByteArray {
        return ByteBuffer.allocate(TIMESTAMP_LEN)
            .putInt((epochSeconds - EPOCH_SECONDS).toInt())
            .array()
    }

    private fun generatePayload(): ByteArray {
        return ByteArray(PAYLOAD_LEN).apply {
            random.nextBytes(this)
        }
    }

    /**
     * Ksuid 를 파싱하여, 시간, 타임스탬프, 페이로드를 포함한 정보를 문자열로 반환합니다.
     *
     * ```
     * val ksuid = Ksuid.generate()
     * val prettyString = Ksuid.prettyString(ksuid)
     * ```
     *
     * @param ksuid 파싱할 KSUID 문자열
     */
    fun prettyString(ksuid: String): String {
        val bytes = BytesBase62.decode(ksuid)
        val timestamp = extractTimestamp(bytes)
        val utcTimeString = Instant.ofEpochSecond(timestamp).atZone(ZoneOffset.UTC)

        return """
            |Time = $utcTimeString
            |Timestamp = $timestamp
            |Payload = ${extractPayload(bytes)}
            """.trimMargin()
    }

    private fun extractTimestamp(decodedKsuid: ByteArray): Long {
        val timestamp = decodedKsuid.copyOf(TIMESTAMP_LEN)
        return ByteBuffer.wrap(timestamp).int.toLong() + EPOCH_SECONDS
    }

    private fun extractPayload(decodedKsuid: ByteArray): String {
        val payload = decodedKsuid.copyOfRange(TIMESTAMP_LEN, decodedKsuid.size - TIMESTAMP_LEN)
        return payload.encodeHexString()
    }
}
