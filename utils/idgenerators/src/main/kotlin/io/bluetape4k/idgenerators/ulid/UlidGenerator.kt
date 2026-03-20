package io.bluetape4k.idgenerators.ulid

import io.bluetape4k.idgenerators.IdGenerator
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace

/**
 * ULID (Universally Unique Lexicographically Sortable Identifier) 생성기.
 *
 * 내부적으로 [ULID.StatefulMonotonic]을 사용하여 동일한 밀리초 내에서도
 * 단조 증가(monotonically increasing) ULID를 생성합니다.
 *
 * ## 특징
 * - 26자 Crockford Base32 인코딩 문자열 반환
 * - 타임스탬프 기반 사전순 정렬 보장
 * - 단조 증가 보장 (동일 밀리초 내 중복 없음)
 * - 스레드 안전 여부는 [ULID.StatefulMonotonic] 구현에 따름
 *
 * ## 사용 예
 * ```kotlin
 * // 기본 생성자로 즉시 사용
 * val generator = UlidGenerator()
 * val id: String = generator.nextId()        // "01ARZ3NDEKTSV4RRFFQ69G5FAV"
 * val ulid: ULID = generator.nextULID()      // ULID 값 객체
 *
 * // 커스텀 Random 주입
 * val factory = ULID.factory(myRandom)
 * val customGenerator = UlidGenerator(factory)
 * ```
 *
 * @param factory ULID 생성에 사용할 [ULID.Factory]. 기본값은 [ULID] companion object (기본 Random 사용)
 */
class UlidGenerator(
    factory: ULID.Factory = ULID,
) : IdGenerator<String> {
    companion object : KLogging()

    private val statefulMonotonic: ULID.StatefulMonotonic =
        ULID.statefulMonotonic(factory)

    /**
     * 다음 ULID 값 객체를 생성합니다.
     *
     * 현재 밀리초 타임스탬프를 기반으로 단조 증가하는 [ULID] 인스턴스를 반환합니다.
     *
     * ```kotlin
     * val ulid: ULID = generator.nextULID()
     * println(ulid.timestamp) // 생성 시각 (밀리초)
     * ```
     *
     * @return 새로 생성된 [ULID] 값 객체
     */
    fun nextULID(): ULID {
        val ulid = statefulMonotonic.nextULID()
        log.trace { "generated ulid=$ulid" }
        return ulid
    }

    /**
     * 다음 ULID를 26자 Crockford Base32 문자열로 반환합니다.
     *
     * 내부적으로 [nextULID]를 호출하여 문자열로 변환합니다.
     *
     * ```kotlin
     * val id: String = generator.nextId()
     * // id.length == 26
     * ```
     *
     * @return 26자 Crockford Base32 인코딩 ULID 문자열
     */
    override fun nextId(): String {
        val id = statefulMonotonic.nextULID().toString()
        log.trace { "generated ulid string=$id" }
        return id
    }

    /**
     * 다음 ULID를 문자열로 반환합니다. [nextId]와 동일합니다.
     *
     * ```kotlin
     * val id: String = generator.nextIdAsString()
     * // id.length == 26
     * ```
     *
     * @return 26자 Crockford Base32 인코딩 ULID 문자열
     */
    override fun nextIdAsString(): String = nextId()
}
