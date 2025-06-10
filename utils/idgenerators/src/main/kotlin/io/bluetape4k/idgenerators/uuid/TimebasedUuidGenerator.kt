package io.bluetape4k.idgenerators.uuid

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.NoArgGenerator
import io.bluetape4k.codec.Url62
import io.bluetape4k.idgenerators.IdGenerator
import java.util.*

/**
 * Timebased UUID를 생성하는 Generator입니다.
 */
class TimebasedUuidGenerator: IdGenerator<UUID> {

    private val generator: NoArgGenerator by lazy {
        Generators.timeBasedEpochGenerator()
    }

    override fun nextId(): UUID = generator.generate()

    override fun nextIdAsString(): String = Url62.encode(nextId())

    /**
     * 새로운 Time based UUID를 생성합니다.
     * @return UUID instance
     */
    fun nextUUID(): UUID = generator.generate()

    /**
     * 지정한 `size` 만큼 Time based UUID를 생성합니다.
     *
     * ```kotlin
     * val generator = TimebasedUuidGenerator()
     * val uuids: Sequence<UUID> = generator.nextUUIDs(10)
     * ```
     *
     * @param size 원하는 크기
     * @return UUID Collection
     */
    fun nextUUIDs(size: Int): Sequence<UUID> =
        generateSequence { nextUUID() }.take(size)

    /**
     * 새로운 Time based UUID를 생성하고, 이를 [Base62]로 인코딩해서 반환합니다.
     * @return UUID 값을 [Base62]로 인코딩한 문자열
     */
    fun nextBase62String(): String = Url62.encode(nextUUID())

    /**
     * 지정한 size 만큼 Time based UUID를 생성하고, 이를 [Base62]로 인코딩해서 반환합니다.
     * @return UUID 값을 [Base62]로 인코딩한 문자열의 컬렉션
     */
    fun nextBase62Strings(size: Int): Sequence<String> =
        generateSequence { nextBase62String() }.take(size)
}
