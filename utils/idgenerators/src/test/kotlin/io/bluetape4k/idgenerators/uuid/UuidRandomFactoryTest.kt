package io.bluetape4k.idgenerators.uuid

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import org.junit.jupiter.api.Test
import java.util.Random

class UuidRandomFactoryTest {
    @Test
    fun `각 기본 생성기의 첫 UUID가 충돌하지 않는다`() {
        val ids = List(10_000) { Uuid.random().nextId() }
        ids.toSet() shouldHaveSize ids.size
        ids.forEach { it.version() shouldBeEqualTo 4 }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `이전 기본 생성기의 첫 UUID도 충돌하지 않는다`() {
        val ids = List(10_000) { RandomUuidGenerator().nextId() }
        ids.toSet() shouldHaveSize ids.size
    }

    @Suppress("DEPRECATION")
    @Test
    fun `명시적 시드는 기존의 결정론적 결과를 유지한다`() {
        val first = Uuid.random(Random(42L))
        val second = Uuid.random(Random(42L))
        val legacy = RandomUuidGenerator(Random(42L))
        repeat(10) {
            val expected = first.nextId()
            second.nextId() shouldBeEqualTo expected
            legacy.nextId() shouldBeEqualTo expected
        }
    }
}
