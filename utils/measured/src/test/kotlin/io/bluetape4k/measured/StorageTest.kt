package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

@RandomizedTest
class StorageTest {
    @Test
    fun `저장 용량 단위 변환이 동작한다`() {
        (1.gbytes() `in` Storage.megaBytes).shouldBeEqualTo(1024.0)
        (2048.bytes() `in` Storage.kiloBytes).shouldBeEqualTo(2.0)
    }

    @Test
    fun `저장 용량 toHuman 이 자동 단위를 선택한다`() {
        1536.bytes().toHuman() shouldBeEqualTo "1.5 KB"
    }
}
