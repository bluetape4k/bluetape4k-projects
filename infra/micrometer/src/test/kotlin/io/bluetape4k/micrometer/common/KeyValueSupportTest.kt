package io.bluetape4k.micrometer.common

import io.bluetape4k.logging.KLogging
import io.micrometer.common.KeyValue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KeyValueSupportTest {
    companion object: KLogging()

    @Test
    fun `keyValueOf - 키와 값으로 KeyValue 생성`() {
        val kv = keyValueOf("key1", "value1")

        kv.key shouldBeEqualTo "key1"
        kv.value shouldBeEqualTo "value1"
    }

    @Test
    fun `keyValueOf - 빈 키는 예외 발생`() {
        assertThrows<IllegalArgumentException> {
            keyValueOf("", "value")
        }
    }

    @Test
    fun `keyValueOf - 공백 키는 예외 발생`() {
        assertThrows<IllegalArgumentException> {
            keyValueOf("   ", "value")
        }
    }

    @Test
    fun `keyValueOf with validator - 유효한 값으로 생성`() {
        val kv = keyValueOf("count", 150) { it > 100 }

        kv.key shouldBeEqualTo "count"
        kv.value shouldBeEqualTo "150"
    }

    @Test
    fun `keyValuesOf - 문자열 가변 인자로 생성`() {
        val kvs = keyValuesOf("key1", "value1", "key2", "value2")

        kvs shouldHaveSize 2
        kvs.first { it.key == "key1" }.value shouldBeEqualTo "value1"
        kvs.first { it.key == "key2" }.value shouldBeEqualTo "value2"
    }

    @Test
    fun `keyValueOf - KeyValue 컬렉션으로 생성`() {
        val keyValues =
            listOf(
                KeyValue.of("k1", "v1"),
                KeyValue.of("k2", "v2"),
            )

        val kvs = keyValueOf(keyValues)

        kvs shouldHaveSize 2
    }

    @Test
    fun `keyValuesOf - KeyValue 가변 인자로 생성`() {
        val kvs =
            keyValuesOf(
                KeyValue.of("a", "1"),
                KeyValue.of("b", "2"),
            )

        kvs shouldHaveSize 2
    }

    @Test
    fun `keyValuesOf - Pair 가변 인자로 생성`() {
        val kvs =
            keyValuesOf(
                "key1" to "value1",
                "key2" to "value2",
            )

        kvs shouldHaveSize 2
        kvs.map { it.key } shouldContain "key1"
        kvs.map { it.key } shouldContain "key2"
    }

    @Test
    fun `keyValueOf - Map으로 생성`() {
        val map =
            mapOf(
                "x" to "1",
                "y" to "2",
            )

        val kvs = keyValueOf(map)

        kvs shouldHaveSize 2
    }
}
