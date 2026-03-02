package io.bluetape4k.idgenerators.uuid

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.TimeBasedReorderedGenerator
import io.bluetape4k.codec.Url62
import io.bluetape4k.idgenerators.IdGenerator
import java.util.*

/**
 * 재정렬된 시간 기반 UUID를 생성하는 생성기입니다.
 *
 * ## 동작/계약
 * - 내부적으로 JUG `timeBasedReorderedGenerator()`를 지연 초기화해 재사용합니다.
 * - 문자열 변환은 URL-safe Base62 인코딩([Url62])을 사용합니다.
 * - `size` 인자는 별도 검증 없이 `take(size)`를 사용하므로 `size <= 0`이면 빈 시퀀스를 반환합니다.
 *
 * ```kotlin
 * val generator = TimebasedUuidGenerator()
 * val ids = generator.nextUUIDs(2).toList()
 * // ids.size == 2
 * ```
 */
class TimebasedUuidGenerator: IdGenerator<UUID> {

    private val generator: TimeBasedReorderedGenerator by lazy {
        Generators.timeBasedReorderedGenerator()
    }

    override fun nextId(): UUID = generator.generate()

    override fun nextIdAsString(): String = Url62.encode(nextId())

    /**
     * 새로운 Time based UUID를 생성합니다.
     *
     * ## 동작/계약
     * - 생성기 상태를 공유하지만 호출마다 새 UUID를 반환합니다.
     *
     * ```kotlin
     * val id = TimebasedUuidGenerator().nextUUID()
     * // id.version() > 0
     * ```
     */
    fun nextUUID(): UUID = generator.generate()

    /**
     * 지정한 개수만큼 시간 기반 UUID 시퀀스를 생성합니다.
     *
     * ## 동작/계약
     * - 지연 시퀀스로 생성되며 소비 시점에 UUID가 생성됩니다.
     * - `size <= 0`이면 빈 시퀀스를 반환합니다.
     *
     * ```kotlin
     * val generator = TimebasedUuidGenerator()
     * val uuids: Sequence<UUID> = generator.nextUUIDs(10)
     * ```
     *
     * @param size 생성할 UUID 개수
     */
    fun nextUUIDs(size: Int): Sequence<UUID> =
        generateSequence { nextUUID() }.take(size)

    /**
     * 시간 기반 UUID를 생성하고 Base62 문자열로 반환합니다.
     *
     * ## 동작/계약
     * - [nextUUID] 결과를 즉시 Base62로 인코딩합니다.
     *
     * ```kotlin
     * val value = TimebasedUuidGenerator().nextBase62String()
     * // value.isNotBlank() == true
     * ```
     */
    fun nextBase62String(): String = Url62.encode(nextUUID())

    /**
     * 지정한 개수만큼 Base62 문자열 UUID 시퀀스를 생성합니다.
     *
     * ## 동작/계약
     * - [nextBase62String]을 반복 호출하는 지연 시퀀스를 반환합니다.
     * - `size <= 0`이면 빈 시퀀스를 반환합니다.
     *
     * ```kotlin
     * val values = TimebasedUuidGenerator().nextBase62Strings(2).toList()
     * // values.size == 2
     * ```
     */
    fun nextBase62Strings(size: Int): Sequence<String> =
        generateSequence { nextBase62String() }.take(size)
}
