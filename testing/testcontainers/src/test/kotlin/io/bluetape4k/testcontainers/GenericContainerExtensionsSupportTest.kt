package io.bluetape4k.testcontainers

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class GenericContainerExtensionsSupportTest {
    companion object : KLogging()

    @Test
    fun `resolvePortBindings 는 중복 포트를 제거한다`() {
        val bindings = resolvePortBindings(listOf(8080, 8080, 9090))

        bindings.size shouldBeEqualTo 2
        bindings[0].binding.hostPortSpec shouldBeEqualTo "8080"
        bindings[1].binding.hostPortSpec shouldBeEqualTo "9090"
    }

    @Test
    fun `resolvePortBindings 는 0 이하 포트를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            resolvePortBindings(listOf(0, 8080))
        }
    }

    @Test
    fun `resolvePortBindings 는 음수 포트를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            resolvePortBindings(listOf(-1, 8080))
        }
    }

    @Test
    fun `resolvePortBindings 는 빈 입력이면 빈 바인딩을 반환한다`() {
        val bindings = resolvePortBindings(emptyList())
        bindings.isEmpty().shouldBeTrue()
    }

    @Test
    fun `resolvePortBindings 는 단일 포트를 처리한다`() {
        val bindings = resolvePortBindings(listOf(8080))
        bindings.size shouldBeEqualTo 1
        bindings[0].binding.hostPortSpec shouldBeEqualTo "8080"
    }

    @Test
    fun `resolvePortBindings 는 첫 등장 순서를 유지하며 중복을 제거한다`() {
        val bindings = resolvePortBindings(listOf(9092, 8080, 9092, 8080, 19092))

        bindings.size shouldBeEqualTo 3
        bindings[0].binding.hostPortSpec shouldBeEqualTo "9092"
        bindings[1].binding.hostPortSpec shouldBeEqualTo "8080"
        bindings[2].binding.hostPortSpec shouldBeEqualTo "19092"
    }

    @Test
    fun `resolvePortBindings 결과는 비어있지 않다`() {
        val bindings = resolvePortBindings(listOf(8080))
        bindings.isEmpty().shouldBeFalse()
    }
}
