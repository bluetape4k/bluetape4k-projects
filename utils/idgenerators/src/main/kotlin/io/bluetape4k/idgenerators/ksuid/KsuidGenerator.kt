package io.bluetape4k.idgenerators.ksuid

import io.bluetape4k.idgenerators.IdGenerator
import io.bluetape4k.logging.KLogging

/**
 * [Ksuid.Generator]를 [IdGenerator]로 사용하는 어댑터 클래스.
 *
 * 기본 전략은 초(seconds) 기반의 [Ksuid.Seconds]입니다.
 * [UuidGenerator], [UlidGenerator]와 동일한 어댑터 패턴입니다.
 *
 * ## 사용 예
 * ```kotlin
 * // 기본 (초 기반)
 * val gen = KsuidGenerator()
 * val id: String = gen.nextId()
 *
 * // 밀리초 기반으로 교체
 * val genMillis = KsuidGenerator(Ksuid.Millis)
 * val id2: String = genMillis.nextId()
 * ```
 *
 * @param generator 사용할 KSUID 생성 전략. 기본값은 [Ksuid.Seconds]
 */
class KsuidGenerator(
    private val generator: Ksuid.Generator = Ksuid.Seconds,
): IdGenerator<String> by generator {
    companion object: KLogging()

    /**
     * KSUID를 생성합니다. [IdGenerator.nextId]와 동일합니다.
     *
     * ```kotlin
     * val gen = KsuidGenerator()
     * val id: String = gen.generate()
     * // id.length == 27
     * ```
     */
    fun generate(): String = generator.generate()
}
