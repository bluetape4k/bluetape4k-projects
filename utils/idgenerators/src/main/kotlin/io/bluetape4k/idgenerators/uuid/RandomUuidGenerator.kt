package io.bluetape4k.idgenerators.uuid

import com.fasterxml.uuid.Generators
import io.bluetape4k.codec.Url62
import io.bluetape4k.idgenerators.IdGenerator
import java.security.SecureRandom
import java.util.*

/**
 * 랜덤 기반(UUID v4) 식별자를 생성합니다.
 *
 * ## 동작/계약
 * - 기본 난수원은 [SecureRandom]이며, 별도로 생성한 인스턴스끼리 시간 시드를 공유하지 않습니다.
 * - `nextId()`는 UUID를, `nextIdAsString()`은 Base62 문자열을 반환합니다.
 * - 수신 객체 상태를 외부에 노출하지 않으며 생성기만 내부 재사용합니다.
 *
 * ```kotlin
 * val generator = RandomUuidGenerator()
 * val id = generator.nextId()
 * val text = generator.nextIdAsString()
 * // id.version() == 4
 * // text.isNotBlank() == true
 * ```
 */
@Deprecated(
    "Uuid.V4 또는 Uuid.random() 을 사용하세요",
    ReplaceWith("Uuid.V4"),
    DeprecationLevel.WARNING
)
class RandomUuidGenerator(
    random: Random = SecureRandom(),
): IdGenerator<UUID> {
    // VERSION 4
    private val generator = Generators.randomBasedGenerator(random)

    override fun nextId(): UUID = generator.generate()

    override fun nextIdAsString(): String = Url62.encode(nextId())
}
