package io.bluetape4k.idgenerators.uuid

import com.fasterxml.uuid.Generators
import io.bluetape4k.codec.Url62
import io.bluetape4k.idgenerators.IdGenerator
import io.bluetape4k.support.assertPositiveNumber
import java.util.Random
import java.util.UUID

/**
 * UUID 생성 전략을 통합 제공하는 진입점.
 *
 * UUID v1~v7 표준 버전을 싱글턴 객체로 제공하며, 커스텀 Random·이름 기반 팩토리 함수도 포함합니다.
 *
 * ## 사용 예
 * ```kotlin
 * // 표준 버전 싱글턴 사용
 * val id: UUID = Uuid.V7.nextUUID()
 * val base62: String = Uuid.V7.nextBase62()
 *
 * // 커스텀 Random 사용
 * val gen = Uuid.random(SecureRandom())
 * val id2: UUID = gen.nextId()
 *
 * // 이름 기반 결정론적 UUID v5
 * val gen2 = Uuid.namebased("my-namespace")
 * val id3: UUID = gen2.nextId()
 * ```
 */
object Uuid {
    /**
     * UUID 생성기 공통 인터페이스.
     *
     * [IdGenerator]를 확장하여 UUID 전용 편의 메서드를 추가합니다.
     * 모든 문자열 반환 메서드는 URL-safe Base62 인코딩([Url62])을 사용합니다.
     */
    interface Generator : IdGenerator<UUID> {
        /**
         * 다음 UUID를 생성합니다. [nextId]와 동일합니다.
         *
         * ```kotlin
         * val id: UUID = generator.nextUUID()
         * ```
         */
        fun nextUUID(): UUID = nextId()

        /**
         * 다음 UUID를 Base62 문자열로 반환합니다.
         *
         * ```kotlin
         * val s: String = generator.nextBase62()
         * // s.isNotBlank() == true
         * ```
         */
        fun nextBase62(): String = Url62.encode(nextId())

        /**
         * 지정한 개수만큼 UUID를 지연 생성합니다.
         *
         * @param size 생성할 UUID 개수
         *
         * ```kotlin
         * val ids: Sequence<UUID> = generator.nextUUIDs(10)
         * ```
         */
        fun nextUUIDs(size: Int): Sequence<UUID> = nextIds(size)

        /**
         * 지정한 개수만큼 Base62 문자열을 지연 생성합니다.
         *
         * @param size 생성할 문자열 개수
         *
         * ```kotlin
         * val strs: Sequence<String> = generator.nextBase62s(5)
         * ```
         */
        fun nextBase62s(size: Int): Sequence<String> {
            size.assertPositiveNumber("size")
            return generateSequence { nextBase62() }.take(size)
        }
    }

    /**
     * UUID v1: MAC 주소 + Gregorian timestamp 기반 생성기.
     *
     * JUG `timeBasedGenerator()`를 사용합니다.
     *
     * ```kotlin
     * val id: UUID = Uuid.V1.nextUUID()
     * // id.version() == 1
     * ```
     */
    object V1 : Generator {
        private val generator by lazy { Generators.timeBasedGenerator() }

        override fun nextId(): UUID = generator.generate()

        override fun nextIdAsString(): String = Url62.encode(nextId())
    }

    /**
     * UUID v4: 완전 랜덤 기반 생성기. 기본 난수원은 [java.security.SecureRandom]입니다.
     *
     * JUG `randomBasedGenerator()`를 사용합니다.
     *
     * ```kotlin
     * val id: UUID = Uuid.V4.nextUUID()
     * // id.version() == 4
     * ```
     */
    object V4 : Generator {
        private val generator by lazy { Generators.randomBasedGenerator() }

        override fun nextId(): UUID = generator.generate()

        override fun nextIdAsString(): String = Url62.encode(nextId())
    }

    /**
     * UUID v5: 랜덤 name 입력 기반 SHA-1 생성기.
     *
     * 호출마다 랜덤 UUID를 이름 값으로 공급하여 name-based UUID를 생성합니다.
     * 동일한 입력을 재현하는 결정론적 생성기가 필요하면 [namebased]를 사용하세요.
     *
     * ```kotlin
     * val id: UUID = Uuid.V5.nextUUID()
     * // id.version() == 5
     * ```
     */
    object V5 : Generator {
        private val namebasedGenerator by lazy { Generators.nameBasedGenerator() }
        private val randomGenerator by lazy { Generators.randomBasedGenerator() }

        override fun nextId(): UUID = namebasedGenerator.generate(randomGenerator.generate().toString())

        override fun nextIdAsString(): String = Url62.encode(nextId())
    }

    /**
     * UUID v6: 재정렬된 timestamp 기반 생성기. 데이터베이스 PK 정렬에 최적화됩니다.
     *
     * JUG `timeBasedReorderedGenerator()`를 사용합니다.
     *
     * ```kotlin
     * val id: UUID = Uuid.V6.nextUUID()
     * // id.version() == 6
     * ```
     */
    object V6 : Generator {
        private val generator by lazy { Generators.timeBasedReorderedGenerator() }

        override fun nextId(): UUID = generator.generate()

        override fun nextIdAsString(): String = Url62.encode(nextId())
    }

    /**
     * UUID v7: Unix epoch timestamp + random 기반 생성기. 현재 표준 권장 버전입니다.
     *
     * JUG `timeBasedEpochGenerator()`를 사용합니다.
     *
     * ```kotlin
     * val id: UUID = Uuid.V7.nextUUID()
     * // id.version() == 7
     * ```
     */
    object V7 : Generator {
        private val generator by lazy { Generators.timeBasedEpochGenerator() }

        override fun nextId(): UUID = generator.generate()

        override fun nextIdAsString(): String = Url62.encode(nextId())
    }

    /**
     * 커스텀 [Random]을 사용하는 UUID v4 생성기를 반환합니다.
     *
     * @param random 사용할 난수원. 기본값은 현재 시각 시드의 [Random]
     *
     * ```kotlin
     * val gen = Uuid.random(SecureRandom())
     * val id: UUID = gen.nextId()
     * ```
     */
    fun random(random: Random = Random(System.currentTimeMillis())): Generator = RandomGenerator(random)

    /**
     * 커스텀 [Random]을 사용하는 UUID v7 생성기를 반환합니다.
     *
     * @param random 사용할 난수원
     *
     * ```kotlin
     * val gen = Uuid.epochRandom(SecureRandom())
     * val id: UUID = gen.nextId()
     * ```
     */
    fun epochRandom(random: Random): Generator = EpochRandomGenerator(random)

    /**
     * 고정 [name]으로 결정론적 UUID v5를 생성하는 생성기를 반환합니다.
     *
     * 동일한 [name]으로 생성한 생성기는 내부 name-based 생성기가 동일하므로,
     * 랜덤 UUID를 이름 값으로 공급하여 호출마다 다른 UUID를 생성합니다.
     *
     * @param name 네임스페이스로 사용할 문자열
     *
     * ```kotlin
     * val gen = Uuid.namebased("my-service")
     * val id: UUID = gen.nextId()
     * ```
     */
    fun namebased(name: String): Generator = NamebasedGenerator(name)

    // ── 내부 구현 클래스 ────────────────────────────────────────────────────

    private class RandomGenerator(
        random: Random,
    ) : Generator {
        private val generator by lazy { Generators.randomBasedGenerator(random) }

        override fun nextId(): UUID = generator.generate()

        override fun nextIdAsString(): String = Url62.encode(nextId())
    }

    private class EpochRandomGenerator(
        random: Random,
    ) : Generator {
        private val generator by lazy { Generators.timeBasedEpochGenerator(random) }

        override fun nextId(): UUID = generator.generate()

        override fun nextIdAsString(): String = Url62.encode(nextId())
    }

    private class NamebasedGenerator(
        private val name: String,
    ) : Generator {
        private val namebasedGenerator by lazy { Generators.nameBasedGenerator() }

        override fun nextId(): UUID = namebasedGenerator.generate(name)

        override fun nextIdAsString(): String = Url62.encode(nextId())
    }
}
