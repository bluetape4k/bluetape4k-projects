package io.bluetape4k.idgenerators.ksuid

import io.bluetape4k.idgenerators.IdGenerator
import java.time.Instant
import java.time.LocalDateTime
import java.util.Date

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

    override fun nextId(): String = Ksuid.Millis.nextId()

    override fun nextIdAsString(): String = Ksuid.Millis.nextIdAsString()

    fun generate(): String = Ksuid.Millis.generate()

    fun generate(instant: Instant): String = Ksuid.Millis.generate(instant)

    fun generate(date: Date): String = Ksuid.Millis.generate(date)

    fun generate(dt: LocalDateTime): String = Ksuid.Millis.generate(dt)

    fun prettyString(ksuid: String): String = Ksuid.Millis.prettyString(ksuid)
}
