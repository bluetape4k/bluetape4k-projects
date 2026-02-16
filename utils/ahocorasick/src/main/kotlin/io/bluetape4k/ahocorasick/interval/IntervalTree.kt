package io.bluetape4k.ahocorasick.interval

import io.bluetape4k.ValueObject
import io.bluetape4k.ahocorasick.interval.IntervalableComparators.PositionComparator
import io.bluetape4k.ahocorasick.interval.IntervalableComparators.ReverseSizeComparator
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace

/**
 * Interval 트리 자료구조.
 *
 * 이진 트리 기반의 구조로 Interval 간의 오버랩(겹침)을 효율적으로 찾을 수 있습니다.
 * 주로 Aho-Corasick 알고리즘에서 중첩된 키워드 매칭을 제거하는 데 사용됩니다.
 *
 * @property rootNode 트리의 루트 노드
 * @see IntervalNode
 * @see Intervalable
 */
class IntervalTree private constructor(
    private val rootNode: IntervalNode,
): ValueObject {
    companion object: KLogging() {
        /**
         * IntervalNode를 사용하여 IntervalTree를 생성합니다.
         *
         * @param rootNode 트리의 루트 노드
         * @return 생성된 IntervalTree 인스턴스
         */
        @JvmStatic
        operator fun invoke(rootNode: IntervalNode): IntervalTree = IntervalTree(rootNode)

        /**
         * Interval 리스트를 사용하여 IntervalTree를 생성합니다.
         *
         * @param intervals Interval 리스트
         * @return 생성된 IntervalTree 인스턴스
         */
        @JvmStatic
        operator fun invoke(intervals: List<Intervalable>): IntervalTree = invoke(IntervalNode(intervals))
    }

    /**
     * 주어진 Interval과 오버랩되는 모든 Interval을 찾습니다.
     *
     * @param interval 기준이 되는 Interval
     * @return 오버랩되는 Interval 리스트 (시작 위치 순으로 정렬됨)
     */
    fun findOverlaps(interval: Intervalable): List<Intervalable> =
        rootNode.findOverlaps(interval).sortedWith(PositionComparator)

    /**
     * 오버랩되는 Interval을 제거하고 결과를 반환합니다.
     *
     * 크기가 큰 Interval을 우선적으로 유지하고, 작은 Interval 중에서 오버랩되는 것을 제거합니다.
     * 결과는 시작 위치 순으로 정렬됩니다.
     *
     * @param intervals 처리할 Interval 컬렉션
     * @return 오버랩이 제거된 Interval 리스트
     */
    fun <T: Intervalable> removeOverlaps(intervals: Collection<T>): MutableList<T> {
        // size가 큰 것부터
        val results = intervals.sortedWith(ReverseSizeComparator).toMutableList()
        val removed = mutableSetOf<Intervalable>()

        // 꼭 Sequence 방식으로 수행해야 updated된 removed를 사용할 수 있습니다.
        results
            .asSequence()
            .filterNot { removed.contains(it) }
            .forEach { target ->
                val overlaps = findOverlaps(target)
                log.debug { "target=$target, overlaps=$overlaps" }
                removed.addAll(overlaps)
            }

        // overlap 된 interval들을 삭제합니다.
        log.trace { "overlap 된 interval들을 삭제=$removed" }
        results.removeAll(removed)

        // sort the intervals, now on left-most position only
        results.sortWith(PositionComparator)
        return results
    }
}
