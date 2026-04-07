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
 *
 * ```kotlin
 * val ulid: ULID = ULID.nextULID()
 * val ulidStr: String = ULID.randomULID()
 * // ulidStr.length == 26
 * val parsed: ULID = ULID.parseULID(ulidStr)
 * // parsed.timestamp > 0L
 * ```
 */
interface ULID: Comparable<ULID> {
    /**
     * ULID의 상위 64비트 (타임스탬프 + 랜덤 상위 비트)
     *
     * ```kotlin
     * val ulid: ULID = ULID.nextULID()
     * val msb: Long = ulid.mostSignificantBits
     * ```
     */
    val mostSignificantBits: Long

    /**
     * ULID의 하위 64비트 (랜덤 하위 비트)
     *
     * ```kotlin
     * val ulid: ULID = ULID.nextULID()
     * val lsb: Long = ulid.leastSignificantBits
     * ```
     */
    val leastSignificantBits: Long

    /**
     * ULID 생성 시각 (밀리초 Unix timestamp)
     *
     * ```kotlin
     * val ulid: ULID = ULID.nextULID()
     * val ts: Long = ulid.timestamp
     * // ts > 0L
     * ```
     */
    val timestamp: Long

    /**
     * ULID를 16바이트 배열로 변환합니다.
     *
     * ```kotlin
     * val ulid: ULID = ULID.nextULID()
     * val bytes: ByteArray = ulid.toByteArray()
     * // bytes.size == 16
     * ```
     */
    fun toByteArray(): ByteArray

    /**
     * 현재 ULID에서 1 증가한 새 ULID를 반환합니다.
     *
     * ```kotlin
     * val ulid: ULID = ULID.nextULID()
     * val next: ULID = ulid.increment()
     * // next > ulid
     * ```
     */
    fun increment(): ULID

    /**
     * ULID 생성 팩토리 인터페이스.
     *
     * ```kotlin
     * val factory: ULID.Factory = ULID.factory()
     * val ulidStr: String = factory.randomULID()
     * val ulid: ULID = factory.nextULID()
     * ```
     */
    interface Factory {
        /**
         * 지정한 타임스탬프로 랜덤 ULID 문자열을 생성합니다.
         *
         * ```kotlin
         * val factory: ULID.Factory = ULID.factory()
         * val ulidStr: String = factory.randomULID()
         * // ulidStr.length == 26
         * ```
         */
        fun randomULID(timestamp: Long = currentTimeMillis()): String

        /**
         * 지정한 타임스탬프로 ULID 값 객체를 생성합니다.
         *
         * ```kotlin
         * val factory: ULID.Factory = ULID.factory()
         * val ulid: ULID = factory.nextULID()
         * // ulid.timestamp > 0L
         * ```
         */
        fun nextULID(timestamp: Long = currentTimeMillis()): ULID

        /**
         * 16바이트 배열에서 ULID를 복원합니다.
         *
         * ```kotlin
         * val ulid: ULID = ULID.nextULID()
         * val bytes: ByteArray = ulid.toByteArray()
         * val restored: ULID = ULID.fromByteArray(bytes)
         * // restored == ulid
         * ```
         */
        fun fromByteArray(data: ByteArray): ULID

        /**
         * 26자 Crockford Base32 문자열을 ULID로 파싱합니다.
         *
         * ```kotlin
         * val ulidStr: String = ULID.randomULID()
         * val ulid: ULID = ULID.parseULID(ulidStr)
         * // ulid.timestamp > 0L
         * ```
         */
        fun parseULID(ulidString: String): ULID
    }

    /**
     * 단조 증가(Monotonic) ULID 생성 인터페이스.
     *
     * 동일한 타임스탬프에서 이전 ULID보다 항상 큰 값을 생성합니다.
     *
     * ```kotlin
     * val monotonic: ULID.Monotonic = ULID.monotonic()
     * val prev: ULID = ULID.nextULID()
     * val next: ULID = monotonic.nextULID(prev)
     * // next > prev
     * ```
     */
    interface Monotonic {
        /**
         * 이전 ULID보다 단조 증가하는 다음 ULID를 반환합니다.
         *
         * ```kotlin
         * val monotonic: ULID.Monotonic = ULID.monotonic()
         * val prev: ULID = ULID.nextULID()
         * val next: ULID = monotonic.nextULID(prev)
         * // next > prev
         * ```
         */
        fun nextULID(
            previous: ULID,
            timestamp: Long = currentTimeMillis(),
        ): ULID

        /**
         * 단조 증가가 불가능한 경우 null을 반환합니다.
         *
         * ```kotlin
         * val monotonic: ULID.Monotonic = ULID.monotonic()
         * val prev: ULID = ULID.nextULID()
         * val next: ULID? = monotonic.nextULIDStrict(prev)
         * ```
         */
        fun nextULIDStrict(
            previous: ULID,
            timestamp: Long = currentTimeMillis(),
        ): ULID?

        companion object: Monotonic by DefaultMonotonic
    }

    /**
     * 상태 기반 단조 증가 ULID 생성 인터페이스.
     *
     * 이전에 생성된 ULID를 내부적으로 추적하여 단조 증가를 보장합니다.
     *
     * ```kotlin
     * val stateful: ULID.StatefulMonotonic = ULID.statefulMonotonic()
     * val ulid1: ULID = stateful.nextULID()
     * val ulid2: ULID = stateful.nextULID()
     * // ulid2 > ulid1
     * ```
     */
    interface StatefulMonotonic: Factory {
        /**
         * 단조 증가가 불가능한 경우 null을 반환합니다.
         *
         * ```kotlin
         * val stateful: ULID.StatefulMonotonic = ULID.statefulMonotonic()
         * val ulid: ULID? = stateful.nextULIDStrict()
         * ```
         */
        fun nextULIDStrict(timestamp: Long = currentTimeMillis()): ULID?
    }

    companion object: Factory by ULIDFactory.Default {
        /**
         * 커스텀 [Random]을 사용하는 [Factory]를 생성합니다.
         *
         * ```kotlin
         * val factory: ULID.Factory = ULID.factory()
         * val ulidStr: String = factory.randomULID()
         * // ulidStr.length == 26
         * ```
         */
        fun factory(random: Random = Random): Factory = ULIDFactory(random)

        /**
         * [Monotonic] ULID 생성기를 생성합니다.
         *
         * ```kotlin
         * val monotonic: ULID.Monotonic = ULID.monotonic()
         * val prev: ULID = ULID.nextULID()
         * val next: ULID = monotonic.nextULID(prev)
         * // next > prev
         * ```
         */
        fun monotonic(factory: Factory = ULID): Monotonic = ULIDMonotonic(factory)

        /**
         * 상태 기반 단조 증가 [StatefulMonotonic] ULID 생성기를 생성합니다.
         *
         * ```kotlin
         * val stateful: ULID.StatefulMonotonic = ULID.statefulMonotonic()
         * val ulid: ULID = stateful.nextULID()
         * // ulid.timestamp > 0L
         * ```
         */
        fun statefulMonotonic(factory: Factory = ULID): StatefulMonotonic =
            ULIDStatefulMonotonic(factory = factory, monotonic = monotonic(factory))
    }
}
