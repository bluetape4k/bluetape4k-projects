package io.bluetape4k.idgenerators.uuid

import com.fasterxml.uuid.Generators
import io.bluetape4k.codec.Url62
import io.bluetape4k.idgenerators.IdGenerator
import java.util.*

/**
 * 랜덤 UUID 생성기의 구현입니다.
 *
 * JUG에서 제공하는 모든 구현과 마찬가지로 이 생성기는 완전히 스레드 안전합니다.
 */
class RandomUuidGenerator(
    random: Random = Random(System.currentTimeMillis()),
): IdGenerator<UUID> {

    // VERSION 4
    private val generator = Generators.randomBasedGenerator(random)

    override fun nextId(): UUID = generator.generate()

    override fun nextIdAsString(): String = Url62.encode(nextId())
}
