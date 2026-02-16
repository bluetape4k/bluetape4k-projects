package io.bluetape4k.ahocorasick.interval

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.collections.eclipse.fastListOf
import java.util.*

/**
 * Interval 트리의 노드 클래스.
 *
 * 중앙값(median)을 기준으로 Interval을 왼쪽/오른쪽 자식 노드로 분할합니다.
 *
 * @param inputs 이 노드에 저장할 Interval 컬렉션
 * @see IntervalTree
 */
class IntervalNode(
    inputs: Collection<Intervalable>,
): AbstractValueObject() {
    /**
     * 트리 탐색 방향 열거형.
     */
    enum class Direction { LEFT, RIGHT }

    /**
     * 왼쪽 자식 노드.
     */
    var left: IntervalNode? = null

    /**
     * 오른쪽 자식 노드.
     */
    var right: IntervalNode? = null

    /**
     * 이 노드에 저장된 Interval 리스트.
     */
    val intervals = LinkedList<Intervalable>()

    /**
     * 이 노드의 중앙값.
     */
    val median: Int

    init {
        median = determineMedian(inputs)
        buildTree(inputs)
    }

    /**
     * Interval 컬렉션의 중앙값을 계산합니다.
     *
     * @param inputs Interval 컬렉션
     * @return 중앙값
     */
    private fun determineMedian(inputs: Collection<Intervalable>): Int {
        val start = inputs.minOfOrNull { it.start } ?: 0
        val end = inputs.maxOfOrNull { it.end } ?: 0
        return (start + end) / 2
    }

    /**
     * Interval 컬렉션으로부터 트리를 구성합니다.
     *
     * @param inputs Interval 컬렉션
     */
    private fun buildTree(inputs: Collection<Intervalable>) {
        if (inputs.isEmpty()) {
            return
        }

        val toLeft = fastListOf<Intervalable>()
        val toRight = fastListOf<Intervalable>()

        inputs.forEach { input ->
            when {
                input.end < median -> toLeft.add(input)
                input.start > median -> toRight.add(input)
                else -> intervals.add(input)
            }
        }
        if (toLeft.isNotEmpty()) {
            this.left = IntervalNode(toLeft)
        }
        if (toRight.isNotEmpty()) {
            this.right = IntervalNode(toRight)
        }
    }

    /**
     * 주어진 Interval과 오버랩되는 모든 Interval을 찾습니다.
     *
     * @param interval 기준이 되는 Interval
     * @param destination 결과를 저장할 리스트 (기본값: 빈 리스트)
     * @return 오버랩되는 Interval 리스트
     */
    fun findOverlaps(
        interval: Intervalable,
        destination: MutableList<Intervalable> = fastListOf(),
    ): MutableList<Intervalable> {
        when {
            interval.start > median -> {
                addToOverlaps(interval, destination, findOverlappingRanges(right, interval))
                addToOverlaps(interval, destination, checkForOverlapsToRight(interval))
            }

            interval.end < median -> {
                addToOverlaps(interval, destination, findOverlappingRanges(left, interval))
                addToOverlaps(interval, destination, checkForOverlapsToLeft(interval))
            }

            else -> {
                addToOverlaps(interval, destination, this.intervals)
                addToOverlaps(interval, destination, findOverlappingRanges(left, interval))
                addToOverlaps(interval, destination, findOverlappingRanges(right, interval))
            }
        }
        return destination
    }

    /**
     * 오버랩 목록에 새로운 오버랩을 추가합니다.
     *
     * @param interval 기준이 되는 Interval
     * @param overlaps 기존 오버랩 리스트
     * @param newOverlaps 추가할 오버랩 리스트
     */
    private fun addToOverlaps(
        interval: Intervalable,
        overlaps: MutableList<Intervalable>,
        newOverlaps: List<Intervalable>,
    ) {
        overlaps.addAll(newOverlaps.filter { it != interval })
    }

    /**
     * 왼쪽 방향의 오버랩을 확인합니다.
     *
     * @param interval 기준이 되는 Interval
     * @return 오버랩되는 Interval 리스트
     */
    private fun checkForOverlapsToLeft(interval: Intervalable): List<Intervalable> =
        checkForOverlaps(interval, Direction.LEFT)

    /**
     * 오른쪽 방향의 오버랩을 확인합니다.
     *
     * @param interval 기준이 되는 Interval
     * @return 오버랩되는 Interval 리스트
     */
    private fun checkForOverlapsToRight(interval: Intervalable): List<Intervalable> =
        checkForOverlaps(interval, Direction.RIGHT)

    /**
     * 주어진 방향으로 오버랩을 확인합니다.
     *
     * @param interval 기준이 되는 Interval
     * @param direction 확인할 방향
     * @return 오버랩되는 Interval 리스트
     */
    private fun checkForOverlaps(
        interval: Intervalable,
        direction: Direction,
    ): List<Intervalable> {
        val overlaps = LinkedList<Intervalable>()

        this.intervals.forEach {
            when (direction) {
                Direction.LEFT  -> {
                    if (it.start <= interval.end) {
                        overlaps.add(it)
                    }
                }

                Direction.RIGHT -> {
                    if (it.end >= interval.start) {
                        overlaps.add(it)
                    }
                }
            }
        }
        return overlaps
    }

    /**
     * 자식 노드에서 오버랩을 찾습니다.
     *
     * @param node 자식 노드
     * @param interval 기준이 되는 Interval
     * @return 오버랩되는 Interval 리스트
     */
    private fun findOverlappingRanges(
        node: IntervalNode?,
        interval: Intervalable,
    ): List<Intervalable> = node?.findOverlaps(interval) ?: emptyList()

    /**
     * 동등성 비교를 위한 속성을 반환합니다.
     *
     * @param other 비교할 객체
     * @return 동등 여부
     */
    override fun equalProperties(other: Any): Boolean =
        other is IntervalNode &&
                left == other.left &&
                right == other.right &&
                median == other.median &&
                intervals == other.intervals
}
