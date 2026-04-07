package io.bluetape4k.idgenerators.uuid

import com.fasterxml.uuid.Generators
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.idgenerators.IdGenerator
import java.util.*

/**
 * 시간 기반 UUID 생성 전략 묶음을 제공합니다.
 *
 * ## 동작/계약
 * - 각 하위 객체는 JUG `Generators`를 지연 초기화해 스레드 안전하게 재사용합니다.
 * - `nextIdAsString()`은 생성된 UUID를 Base62 문자열로 인코딩합니다.
 *
 * ```kotlin
 * val id = TimebasedUuid.Default.nextId()
 * val text = TimebasedUuid.Default.nextIdAsString()
 * // id.version() > 0
 * // text.isNotBlank() == true
 * ```
 */
@Deprecated("Uuid.V1, Uuid.V6, Uuid.V7 을 사용하세요", ReplaceWith("Uuid"), DeprecationLevel.WARNING)
object TimebasedUuid {
    /**
     * 기본 time-based(UUID v1 계열) 생성기를 제공합니다.
     *
     * ## 동작/계약
     * - MAC/시간 정보 기반 JUG 생성기를 사용합니다.
     * - 호출마다 새 UUID를 생성하며 상태는 내부 생성기에 캡슐화됩니다.
     *
     * ```kotlin
     * val id = TimebasedUuid.Default.nextId()
     * // id.version() >= 1
     * ```
     */
    @Deprecated("Uuid.V1 을 사용하세요", ReplaceWith("Uuid.V1"), DeprecationLevel.WARNING)
    object Default: IdGenerator<UUID> {
        private val generator by lazy { Generators.timeBasedGenerator() }

        override fun nextId(): UUID = generator.generate()

        override fun nextIdAsString(): String = nextId().encodeBase62()
    }

    /**
     * 재정렬된 time-based UUID 생성기를 제공합니다.
     *
     * ## 동작/계약
     * - `timeBasedReorderedGenerator()`를 사용해 시간 순 정렬 친화적인 UUID를 생성합니다.
     * - 문자열 결과는 Base62 인코딩입니다.
     *
     * ```kotlin
     * val id = TimebasedUuid.Reordered.nextIdAsString()
     * // id.isNotBlank() == true
     * ```
     */
    @Deprecated("Uuid.V6 을 사용하세요", ReplaceWith("Uuid.V6"), DeprecationLevel.WARNING)
    object Reordered: IdGenerator<UUID> {
        private val generator by lazy { Generators.timeBasedReorderedGenerator() }

        override fun nextId(): UUID = generator.generate()

        override fun nextIdAsString(): String = nextId().encodeBase62()
    }

    /**
     * epoch 기반 time UUID 생성기를 제공합니다.
     *
     * ## 동작/계약
     * - `timeBasedEpochGenerator()`를 사용해 시간 축 정렬 성질을 갖는 UUID를 생성합니다.
     * - 생성 실패 예외는 JUG 구현에서 그대로 전파됩니다.
     *
     * ```kotlin
     * val id = TimebasedUuid.Epoch.nextId()
     * // id.version() > 0
     * ```
     */
    @Deprecated("Uuid.V7 을 사용하세요", ReplaceWith("Uuid.V7"), DeprecationLevel.WARNING)
    object Epoch: IdGenerator<UUID> {
        private val generator by lazy { Generators.timeBasedEpochGenerator() }

        override fun nextId(): UUID = generator.generate()

        override fun nextIdAsString(): String = nextId().encodeBase62()
    }
}
