package io.bluetape4k.coroutines.channels

import io.bluetape4k.collections.eclipse.emptyFastList
import io.bluetape4k.collections.eclipse.emptyUnifiedSet
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.unifiedSetOf
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ChannelsExclipseExtensionsTest {

    @Test
    fun `toFastList는 채널 요소를 순서대로 수집한다`() = runTest {
        val channel = produce {
            send(1)
            send(2)
            send(3)
        }

        channel.toFastList() shouldBeEqualTo fastListOf(1, 2, 3)
    }

    @Test
    fun `toFastList destination 오버로드는 동일 인스턴스를 반환한다`() = runTest {
        val channel = produce {
            send("a")
            send("b")
        }

        val destination = emptyFastList<String>()
        val result = channel.toFastList(destination)

        result shouldBe destination
        result shouldBeEqualTo fastListOf("a", "b")
    }

    @Test
    fun `toUnifiedSet는 중복을 제거해 수집한다`() = runTest {
        val channel = produce {
            send(1)
            send(1)
            send(2)
        }

        channel.toUnifiedSet() shouldBeEqualTo unifiedSetOf(1, 2)
    }

    @Test
    fun `toUnifiedSet destination 오버로드는 동일 인스턴스를 반환한다`() = runTest {
        val channel = produce {
            send("a")
            send("a")
            send("b")
        }

        val destination = emptyUnifiedSet<String>()
        val result = channel.toUnifiedSet(destination)

        result shouldBe destination
        result shouldBeEqualTo unifiedSetOf("a", "b")
    }
}
