package io.bluetape4k.idgenerators.ulid

import io.bluetape4k.idgenerators.ulid.internal.ULIDFactory
import io.bluetape4k.idgenerators.ulid.internal.ULIDMonotonic
import io.bluetape4k.idgenerators.ulid.internal.ULIDMonotonic.Companion.DefaultMonotonic
import io.bluetape4k.idgenerators.ulid.internal.ULIDStatefulMonotonic
import io.bluetape4k.idgenerators.ulid.internal.currentTimeMillis
import kotlin.random.Random

/**
 * ULID (Universally Unique Lexicographically Sortable Identifier) 인터페이스.
 *
 * 타임스탬프 기반으로 정렬 가능한 고유 식별자를 표현합니다.
 * UUID와 달리 문자열 표현이 사전순 정렬을 보장합니다.
 */
interface ULID : Comparable<ULID> {
    val mostSignificantBits: Long
    val leastSignificantBits: Long

    val timestamp: Long

    fun toByteArray(): ByteArray

    fun increment(): ULID

    /**
     * ULID 생성 팩토리 인터페이스.
     */
    interface Factory {
        fun randomULID(timestamp: Long = currentTimeMillis()): String

        fun nextULID(timestamp: Long = currentTimeMillis()): ULID

        fun fromByteArray(data: ByteArray): ULID

        fun parseULID(ulidString: String): ULID
    }

    /**
     * 단조 증가(Monotonic) ULID 생성 인터페이스.
     *
     * 동일한 타임스탬프에서 이전 ULID보다 항상 큰 값을 생성합니다.
     */
    interface Monotonic {
        fun nextULID(
            previous: ULID,
            timestamp: Long = currentTimeMillis(),
        ): ULID

        fun nextULIDStrict(
            previous: ULID,
            timestamp: Long = currentTimeMillis(),
        ): ULID?

        companion object : Monotonic by DefaultMonotonic
    }

    /**
     * 상태 기반 단조 증가 ULID 생성 인터페이스.
     *
     * 이전에 생성된 ULID를 내부적으로 추적하여 단조 증가를 보장합니다.
     */
    interface StatefulMonotonic : Factory {
        fun nextULIDStrict(timestamp: Long = currentTimeMillis()): ULID?
    }

    companion object : Factory by ULIDFactory.Default {
        fun factory(random: Random = Random): Factory = ULIDFactory(random)

        fun monotonic(factory: Factory = ULID): Monotonic = ULIDMonotonic(factory)

        fun statefulMonotonic(factory: Factory = ULID): StatefulMonotonic =
            ULIDStatefulMonotonic(factory = factory, monotonic = monotonic(factory))
    }
}
