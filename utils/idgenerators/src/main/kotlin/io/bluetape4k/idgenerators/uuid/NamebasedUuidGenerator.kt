package io.bluetape4k.idgenerators.uuid

import com.fasterxml.uuid.Generators
import io.bluetape4k.codec.Url62
import io.bluetape4k.idgenerators.IdGenerator
import java.security.SecureRandom
import java.util.*

/**
 * 이름 기반 UUID 생성 방식 중 하나인 3 (MD5) 버전 또는 5 (SHA1) 버전을 사용하는 UUID 생성기의 구현입니다.
 *
 * JUG에서 제공하는 모든 구현과 마찬가지로 이 생성기는 완전히 스레드 안전합니다. 필요에 따라 다이제스터에 대한 액세스가 동기화됩니다.
 */
class NamebasedUuidGenerator: IdGenerator<UUID> {

    private val namebasedUuid = Generators.nameBasedGenerator()
    private val randomUuid =
        Generators.randomBasedGenerator(SecureRandom())

    /**
     * 다음 UUID를 생성합니다.
     *
     * @return 생성된 UUID
     */
    override fun nextId(): UUID = nextIdInternal()

    /**
     * 다음 UUID를 Base62로 인코딩하여 문자열로 반환합니다.
     *
     * @return Base62로 인코딩된 UUID 문자열
     */
    override fun nextIdAsString(): String = Url62.encode(nextIdInternal())

    private fun nextIdInternal(): UUID {
        return namebasedUuid.generate(randomUuid.generate().toString())
    }
}
