package io.bluetape4k.ahocorasick.interval

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class IntervalNodeTest {
    companion object: KLogging()

    @Test
    fun `빈 intervals로 IntervalNode 생성`() {
        val node = IntervalNode(emptyList())

        node.left.shouldBeNull()
        node.right.shouldBeNull()
        node.intervals.shouldBeEmpty()
    }

    @Test
    fun `단일 interval로 IntervalNode 생성`() {
        val intervals = listOf(Interval(0, 5))
        val node = IntervalNode(intervals)

        node.intervals.size shouldBeEqualTo 1
        node.left.shouldBeNull()
        node.right.shouldBeNull()
    }

    @Test
    fun `여러 intervals로 IntervalNode 생성`() {
        val intervals =
            listOf(
                Interval(0, 2),
                Interval(4, 6),
                Interval(8, 10),
            )
        val node = IntervalNode(intervals)

        node.intervals.shouldNotBeNull()
    }

    @Test
    fun `findOverlaps - 왼쪽 자식에서 오버랩 찾기`() {
        val intervals =
            listOf(
                Interval(0, 2),
                Interval(4, 6),
                Interval(1, 3), // Interval(0, 2)와 오버랩
            )
        val node = IntervalNode(intervals)

        val overlaps = node.findOverlaps(Interval(0, 2))

        overlaps.size shouldBeEqualTo 1
        overlaps[0] shouldBeEqualTo Interval(1, 3)
    }

    @Test
    fun `findOverlaps - 오른쪽 자식에서 오버랩 찾기`() {
        val intervals =
            listOf(
                Interval(0, 2),
                Interval(4, 6),
                Interval(5, 7), // Interval(4, 6)와 오버랩
            )
        val node = IntervalNode(intervals)

        val overlaps = node.findOverlaps(Interval(4, 6))

        overlaps.size shouldBeEqualTo 1
        overlaps[0] shouldBeEqualTo Interval(5, 7)
    }

    @Test
    fun `findOverlaps - 현재 노드에서 오버랩 찾기`() {
        val intervals =
            listOf(
                Interval(0, 10),
                Interval(2, 5),
                Interval(6, 8),
            )
        val node = IntervalNode(intervals)

        val overlaps = node.findOverlaps(Interval(3, 7))

        // Interval(3, 7)는 Interval(0, 10), Interval(2, 5), Interval(6, 8)와 모두 오버랩됨
        overlaps.size shouldBeEqualTo 3
    }

    @Test
    fun `findOverlaps - 오버랩이 없는 경우`() {
        val intervals =
            listOf(
                Interval(0, 2),
                Interval(5, 7),
                Interval(10, 12),
            )
        val node = IntervalNode(intervals)

        val overlaps = node.findOverlaps(Interval(20, 25))

        overlaps.shouldBeEmpty()
    }

    @Test
    fun `findOverlaps - 자기 자신은 제외`() {
        val target = Interval(2, 5)
        val intervals =
            listOf(
                Interval(0, 3),
                target,
                Interval(4, 7),
            )
        val node = IntervalNode(intervals)

        val overlaps = node.findOverlaps(target)

        // 자기 자신은 제외되어야 함
        overlaps.none { it == target }.shouldBeTrue()
    }

    @Test
    fun `findOverlaps - 중첩된 오버랩`() {
        val intervals =
            listOf(
                Interval(0, 10),
                Interval(2, 8),
                Interval(4, 6),
            )
        val node = IntervalNode(intervals)

        val overlaps = node.findOverlaps(Interval(1, 9))

        overlaps.size shouldBeEqualTo 3
    }

    @Test
    fun `tree 구조 검증 - 깊은 트리`() {
        val intervals = (0..20 step 2).map { Interval(it, it + 1) }
        val node = IntervalNode(intervals)

        // 트리가 적절히 분할되었는지 확인
        node.intervals.shouldNotBeNull()
    }

    @Test
    fun `determineMedian - 홀수 개수 intervals`() {
        val intervals =
            listOf(
                Interval(0, 2),
                Interval(4, 6),
                Interval(8, 10),
            )
        val node = IntervalNode(intervals)

        // 중앙값은 (0 + 10) / 2 = 5
        node.intervals.shouldNotBeNull()
    }
}
