package io.bluetape4k.text.search

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull

/**
 * Aho-Corasick 테스트 공통 기반 클래스.
 *
 * 서브클래스는 `companion object : KLogging()`를 각자 선언해야 한다.
 */
abstract class AbstractAhoCorasickTest {

    companion object : KLogging()

    /**
     * 단일 [AhoCorasickMatch]의 start, end, keyword를 검증한다.
     *
     * @param match 검증할 매치 결과
     * @param start 예상 시작 위치 (inclusive)
     * @param end 예상 종료 위치 (inclusive)
     * @param keyword 예상 키워드 (정규화 후)
     */
    protected fun assertMatch(
        match: AhoCorasickMatch<*>,
        start: Int,
        end: Int,
        keyword: String,
    ) {
        match.shouldNotBeNull()
        match.start shouldBeEqualTo start
        match.end shouldBeEqualTo end
        match.keyword shouldBeEqualTo keyword
    }

    /**
     * 매치 리스트를 [expected] Triple 목록과 비교한다.
     *
     * Triple 형식: `Triple(start, end, keyword)` — 정규화 후 keyword 기준.
     *
     * @param actual 실제 매치 결과 리스트
     * @param expected 예상 (start, end, keyword) 트리플 가변인자
     */
    protected fun assertMatches(
        actual: List<AhoCorasickMatch<*>>,
        vararg expected: Triple<Int, Int, String>,
    ) {
        actual.size shouldBeEqualTo expected.size
        actual.zip(expected).forEachIndexed { idx, (match, triple) ->
            val (start, end, keyword) = triple
            assertMatch(match, start, end, keyword)
        }
    }
}
