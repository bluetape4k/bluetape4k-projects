package io.bluetape4k.images.filters

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class PaddingSupportTest {

    companion object: KLoggingChannel()

    @Test
    fun `동일 값으로 패딩을 생성한다`() {
        val padding = paddingOf(20)

        padding.top shouldBeEqualTo 20
        padding.right shouldBeEqualTo 20
        padding.bottom shouldBeEqualTo 20
        padding.left shouldBeEqualTo 20
    }

    @Test
    fun `개별 값으로 패딩을 생성한다`() {
        val padding = paddingOf(top = 10, right = 20, bottom = 30, left = 40)

        padding.top shouldBeEqualTo 10
        padding.right shouldBeEqualTo 20
        padding.bottom shouldBeEqualTo 30
        padding.left shouldBeEqualTo 40
    }

    @Test
    fun `0 값으로 패딩을 생성한다`() {
        val padding = paddingOf(0)

        padding.top shouldBeEqualTo 0
        padding.right shouldBeEqualTo 0
        padding.bottom shouldBeEqualTo 0
        padding.left shouldBeEqualTo 0
    }
}
