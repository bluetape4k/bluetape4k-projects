package io.bluetape4k.collections.eclipse.ranges

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.primitives.intArrayListOf
import io.bluetape4k.collections.eclipse.primitives.longArrayListOf
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList
import org.junit.jupiter.api.Test

class IntervalExtensionsTest {

    companion object: KLogging()

    @Test
    fun `intIntervalOf는 올바른 IntInterval을 반환한다`() {
        val interval = intIntervalOf(1, 5)
        interval.size() shouldBeEqualTo 5
        interval[0] shouldBeEqualTo 1
        interval[4] shouldBeEqualTo 5
    }

    @Test
    fun `IntInterval toIntArrayList는 IntArrayList로 변환한다`() {
        val interval = intIntervalOf(1, 3)
        val list = interval.toIntArrayList()
        list.size() shouldBeEqualTo 3
        list[0] shouldBeEqualTo 1
        list[2] shouldBeEqualTo 3
    }

    @Test
    fun `IntInterval toLongArrayList는 LongArrayList로 변환한다`() {
        val interval = intIntervalOf(1, 2)
        val list = interval.toLongArrayList()
        list.size() shouldBeEqualTo 2
        list[0] shouldBeEqualTo 1L
        list[1] shouldBeEqualTo 2L
    }

    @Test
    fun `IntInterval forEach는 모든 요소에 대해 block을 실행한다`() {
        val interval = intIntervalOf(1, 3)
        val result = fastListOf<Int>()
        interval.forEach { result.add(it) }
        result shouldBeEqualTo fastListOf(1, 2, 3)
    }

    @Test
    fun `IntInterval windowed는 올바른 윈도우 시퀀스를 반환한다`() {
        val interval = intIntervalOf(1, 5)
        val windows = interval.windowed(size = 2).toFastList()
        windows.size shouldBeEqualTo 4
        windows[0] shouldBeEqualTo IntArrayList().apply { add(1); add(2) }
        windows[3] shouldBeEqualTo IntArrayList().apply { add(4); add(5) }
    }

    @Test
    fun `IntInterval chunked는 올바른 청크 시퀀스를 반환한다`() {
        val interval = intIntervalOf(1, 5)
        val chunks = interval.chunked(chunkSize = 2).toFastList()
        chunks.size shouldBeEqualTo 3
        chunks[0] shouldBeEqualTo IntArrayList().apply { add(1); add(2) }
        chunks[2] shouldBeEqualTo IntArrayList().apply { add(5) }
    }

    @Test
    fun `IntInterval sliding은 올바른 슬라이딩 윈도우 시퀀스를 반환한다`() {
        val interval = intIntervalOf(1, 4)
        val sliding = interval.sliding(size = 3).toFastList()
        sliding.size shouldBeEqualTo 4
        sliding[0] shouldBeEqualTo intArrayListOf(1, 2, 3)
        sliding[1] shouldBeEqualTo intArrayListOf(2, 3, 4)
        sliding[2] shouldBeEqualTo intArrayListOf(3, 4)
        sliding[3] shouldBeEqualTo intArrayListOf(4)

        val slidingNonPartial = interval.sliding(size = 3, partialWindows = false).toFastList()
        slidingNonPartial.size shouldBeEqualTo 2
        slidingNonPartial[0] shouldBeEqualTo intArrayListOf(1, 2, 3)
        slidingNonPartial[1] shouldBeEqualTo intArrayListOf(2, 3, 4)
    }

    @Test
    fun `longIntervalOf는 올바른 LongInterval을 반환한다`() {
        val interval = longIntervalOf(10L, 13L)
        interval.size() shouldBeEqualTo 4
        interval[0] shouldBeEqualTo 10L
        interval[3] shouldBeEqualTo 13L
    }

    @Test
    fun `LongInterval toLongArrayList는 LongArrayList로 변환한다`() {
        val interval = longIntervalOf(5L, 7L)
        val list = interval.toLongArrayList()
        list.size() shouldBeEqualTo 3
        list[0] shouldBeEqualTo 5L
        list[2] shouldBeEqualTo 7L
    }

    @Test
    fun `LongInterval forEach는 모든 요소에 대해 block을 실행한다`() {
        val interval = longIntervalOf(1L, 3L)
        val result = fastListOf<Long>()
        interval.forEach { result.add(it) }
        result shouldBeEqualTo fastListOf(1L, 2L, 3L)
    }

    @Test
    fun `LongInterval windowed는 올바른 윈도우 시퀀스를 반환한다`() {
        val interval = longIntervalOf(1L, 4L)
        val windows = interval.windowed(size = 2).toFastList()
        windows.size shouldBeEqualTo 3
        windows[0] shouldBeEqualTo LongArrayList().apply { add(1L); add(2L) }
        windows[2] shouldBeEqualTo LongArrayList().apply { add(3L); add(4L) }
    }

    @Test
    fun `LongInterval chunked는 올바른 청크 시퀀스를 반환한다`() {
        val interval = longIntervalOf(1L, 5L)
        val chunks = interval.chunked(chunkSize = 2).toFastList()
        chunks.size shouldBeEqualTo 3
        chunks[0] shouldBeEqualTo LongArrayList().apply { add(1L); add(2L) }
        chunks[2] shouldBeEqualTo LongArrayList().apply { add(5L) }
    }

    @Test
    fun `LongInterval sliding은 올바른 슬라이딩 윈도우 시퀀스를 반환한다`() {
        val interval = longIntervalOf(1L, 4L)
        val sliding = interval.sliding(size = 3).toList()
        sliding.size shouldBeEqualTo 4
        sliding[0] shouldBeEqualTo longArrayListOf(1, 2, 3)
        sliding[1] shouldBeEqualTo longArrayListOf(2, 3, 4)
        sliding[2] shouldBeEqualTo longArrayListOf(3, 4)
        sliding[3] shouldBeEqualTo longArrayListOf(4)

        val slidingNonPartial = interval.sliding(size = 3, partialWindows = false).toFastList()
        slidingNonPartial.size shouldBeEqualTo 2
        slidingNonPartial[0] shouldBeEqualTo longArrayListOf(1, 2, 3)
        slidingNonPartial[1] shouldBeEqualTo longArrayListOf(2, 3, 4)
    }
}
