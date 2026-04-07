package io.bluetape4k.idgenerators.uuid

import com.fasterxml.uuid.Generators
import io.bluetape4k.codec.Url62
import io.bluetape4k.idgenerators.IdGenerator
import java.security.SecureRandom
import java.util.*

/**
 * 이름 기반 UUID를 생성하는 생성기입니다.
 *
 * ## 동작/계약
 * - 내부적으로 랜덤 UUID 문자열을 이름 값으로 사용해 name-based UUID를 만듭니다.
 * - 결과적으로 호출마다 값이 달라질 수 있으며, 동일 입력 재현형 UUID 생성기는 아닙니다.
 * - 문자열은 [Url62] Base62 인코딩으로 반환됩니다.
 *
 * ```kotlin
 * val generator = NamebasedUuidGenerator()
 * val id = generator.nextId()
 * val text = generator.nextIdAsString()
 * // id.version() > 0
 * // text.isNotBlank() == true
 * ```
 */
@Deprecated(
    "Uuid.V5 또는 Uuid.namebased(name) 을 사용하세요",
    ReplaceWith("Uuid.V5"),
    DeprecationLevel.WARNING
)
class NamebasedUuidGenerator: IdGenerator<UUID> {
    private val namebasedUuid = Generators.nameBasedGenerator()
    private val randomUuid =
        Generators.randomBasedGenerator(SecureRandom())

    /**
     * 다음 UUID를 생성합니다.
     *
     * ## 동작/계약
     * - 내부 name-based 생성기에 랜덤 문자열을 공급해 UUID를 생성합니다.
     *
     * ```kotlin
     * val id = NamebasedUuidGenerator().nextId()
     * // id.version() > 0
     * ```
     */
    override fun nextId(): UUID = nextIdInternal()

    /**
     * 다음 UUID를 Base62로 인코딩하여 문자열로 반환합니다.
     *
     * ## 동작/계약
     * - [nextIdInternal] 결과를 Base62로 변환합니다.
     *
     * ```kotlin
     * val text = NamebasedUuidGenerator().nextIdAsString()
     * // text.isNotBlank() == true
     * ```
     */
    override fun nextIdAsString(): String = Url62.encode(nextIdInternal())

    private fun nextIdInternal(): UUID = namebasedUuid.generate(randomUuid.generate().toString())
}
