package io.bluetape4k.idgenerators.uuid

import io.bluetape4k.idgenerators.IdGenerator
import io.bluetape4k.logging.KLogging
import java.util.*

/**
 * [Uuid.Generator]를 [IdGenerator]<[UUID]>로 사용하는 어댑터.
 *
 * [UlidGenerator]와 동일한 구조로, 외부에서 주입받은 [Uuid.Generator]에 모든 ID 생성을 위임합니다.
 * 기본값은 현재 표준 권장 버전인 [Uuid.V7]을 사용합니다.
 *
 * ## 사용 예
 * ```kotlin
 * // 기본 v7 생성기 사용
 * val generator = UuidGenerator()
 * val id: UUID = generator.nextUUID()
 * val str: String = generator.nextIdAsString()
 *
 * // 특정 버전 지정
 * val v4Generator = UuidGenerator(Uuid.V4)
 * val ids: Sequence<UUID> = v4Generator.nextIds(10)
 * ```
 *
 * @param generator 사용할 [Uuid.Generator]. 기본값은 [Uuid.V7]
 */
class UuidGenerator(
    private val generator: Uuid.Generator = Uuid.V7,
) : IdGenerator<UUID> by generator {
    companion object : KLogging()

    /**
     * UUID 값 객체를 직접 생성합니다.
     *
     * 내부 [generator]의 [Uuid.Generator.nextUUID]를 호출합니다.
     *
     * ```kotlin
     * val id: UUID = UuidGenerator().nextUUID()
     * ```
     *
     * @return 새로 생성된 [UUID] 값 객체
     */
    fun nextUUID(): UUID = generator.nextUUID()
}
