package io.bluetape4k.idgenerators.ksuid

import io.bluetape4k.idgenerators.IdGenerator
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

/**
 * 밀리초(milliseconds) 기반 KSUID 생성기.
 *
 * @deprecated [Ksuid.Millis]로 대체됩니다.
 *
 * ```kotlin
 * // Before
 * val id = KsuidMillis.generate()
 *
 * // After
 * val id = Ksuid.Millis.generate()
 * ```
 */
@Deprecated(
    message = "Use Ksuid.Millis instead",
    replaceWith = ReplaceWith("Ksuid.Millis", "io.bluetape4k.idgenerators.ksuid.Ksuid"),
    level = DeprecationLevel.WARNING
)
object KsuidMillis : IdGenerator<String> {
    const val TIMESTAMP_LEN = Ksuid.Millis.TIMESTAMP_LEN
    const val PAYLOAD_LEN = Ksuid.Millis.PAYLOAD_LEN
    const val MAX_ENCODED_LEN = Ksuid.Millis.MAX_ENCODED_LEN
    const val TOTAL_BYTES = Ksuid.Millis.TOTAL_BYTES

    /**
     * 밀리초 기반 KSUID를 생성합니다.
     *
     * ```kotlin
     * val id: String = KsuidMillis.nextId()
     * // id.length == 27
     * ```
     */
    override fun nextId(): String = Ksuid.Millis.nextId()

    /**
     * 밀리초 기반 KSUID를 문자열로 반환합니다.
     *
     * ```kotlin
     * val id: String = KsuidMillis.nextIdAsString()
     * // id.length == 27
     * ```
     */
    override fun nextIdAsString(): String = Ksuid.Millis.nextIdAsString()

    /**
     * 현재 시각으로 밀리초 기반 KSUID를 생성합니다.
     *
     * ```kotlin
     * val id: String = KsuidMillis.generate()
     * // id.length == 27
     * ```
     */
    fun generate(): String = Ksuid.Millis.generate()

    /**
     * 지정한 [Instant]로 밀리초 기반 KSUID를 생성합니다.
     *
     * ```kotlin
     * val id: String = KsuidMillis.generate(Instant.now())
     * // id.length == 27
     * ```
     */
    fun generate(instant: Instant): String = Ksuid.Millis.generate(instant)

    /**
     * 지정한 [Date]로 밀리초 기반 KSUID를 생성합니다.
     *
     * ```kotlin
     * val id: String = KsuidMillis.generate(Date())
     * // id.length == 27
     * ```
     */
    fun generate(date: Date): String = Ksuid.Millis.generate(date)

    /**
     * 지정한 [LocalDateTime]으로 밀리초 기반 KSUID를 생성합니다.
     *
     * ```kotlin
     * val id: String = KsuidMillis.generate(LocalDateTime.now())
     * // id.length == 27
     * ```
     */
    fun generate(dt: LocalDateTime): String = Ksuid.Millis.generate(dt)

    /**
     * KSUID를 파싱하여 시간, 타임스탬프, 페이로드 정보를 문자열로 반환합니다.
     *
     * ```kotlin
     * val id: String = KsuidMillis.generate()
     * val info: String = KsuidMillis.prettyString(id)
     * // info.contains("Time") == true
     * ```
     */
    fun prettyString(ksuid: String): String = Ksuid.Millis.prettyString(ksuid)
}
