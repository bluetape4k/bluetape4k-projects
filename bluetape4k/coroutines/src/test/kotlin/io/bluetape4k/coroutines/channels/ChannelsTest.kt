package io.bluetape4k.coroutines.channels

import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.set.mutable.UnifiedSet
import org.junit.jupiter.api.Test

class ChannelsTest {

    @Test
    fun `toFastList는 채널 요소를 순서대로 수집한다`() = runTest {
        val channel = produce {
            send(1)
            send(2)
            send(3)
        }

        channel.toFastList() shouldBeEqualTo mutableListOf(1, 2, 3)
    }

    @Test
    fun `toFastList destination 오버로드는 동일 인스턴스를 반환한다`() = runTest {
        val channel = produce {
            send("a")
            send("b")
        }

        val destination = FastList.newList<String>()
        val result = channel.toFastList(destination)

        result shouldBe destination
        result shouldBeEqualTo mutableListOf("a", "b")
    }

    @Test
    fun `toUnifiedSet는 중복을 제거해 수집한다`() = runTest {
        val channel = produce {
            send(1)
            send(1)
            send(2)
        }

        channel.toUnifiedSet() shouldBeEqualTo mutableSetOf(1, 2)
    }

    @Test
    fun `toUnifiedSet destination 오버로드는 동일 인스턴스를 반환한다`() = runTest {
        val channel = produce {
            send("a")
            send("a")
            send("b")
        }

        val destination = UnifiedSet.newSet<String>()
        val result = channel.toUnifiedSet(destination)

        result shouldBe destination
        result shouldBeEqualTo mutableSetOf("a", "b")
    }
}
