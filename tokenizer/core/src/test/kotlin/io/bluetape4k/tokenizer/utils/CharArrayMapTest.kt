package io.bluetape4k.tokenizer.utils

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * CharArrayMap 테스트
 */
class CharArrayMapTest {
    companion object: KLogging()

    @Test
    fun `create empty CharArrayMap`() {
        val map = CharArrayMap<String>(16)
        map.size shouldBeEqualTo 0
        map.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun `put and get values`() {
        val map = CharArrayMap<String>(16)

        map["hello"] = "world"
        map["test"] = "value"

        map.size shouldBeEqualTo 2
        map["hello"] shouldBeEqualTo "world"
        map["test"] shouldBeEqualTo "value"
    }

    @Test
    fun `put with CharArray`() {
        val map = CharArrayMap<String>(16)
        val key = "test".toCharArray()

        map[key] = "value"

        map[key] shouldBeEqualTo "value"
        map["test"] shouldBeEqualTo "value"
    }

    @Test
    fun `get with CharArray and offset`() {
        val map = CharArrayMap<String>(16)
        map["hello"] = "value1"
        map["world"] = "value2"

        val text = "hello world".toCharArray()
        map.get(text, 0, 5) shouldBeEqualTo "value1"
        map.get(text, 6, 5) shouldBeEqualTo "value2"
    }

    @Test
    fun `update existing key`() {
        val map = CharArrayMap<String>(16)

        map["key"] = "value1"
        map["key"] = "value2"

        map.size shouldBeEqualTo 1
        map["key"] shouldBeEqualTo "value2"
    }

    @Test
    fun `containsKey with String`() {
        val map = CharArrayMap<String>(16)
        map["test"] = "value"

        map.containsKey("test").shouldBeTrue()
        map.containsKey("notexist").shouldBeFalse()
    }

    @Test
    fun `containsKey with CharArray`() {
        val map = CharArrayMap<String>(16)
        map["test"] = "value"

        map.containsKey("test".toCharArray()).shouldBeTrue()
        map.containsKey("other".toCharArray()).shouldBeFalse()
    }

    @Test
    fun `containsKey with offset and length`() {
        val map = CharArrayMap<String>(16)
        val text = "hello world".toCharArray()
        map["hello"] = "value1"
        map["world"] = "value2"

        map.containsKey(text, 0, 5).shouldBeTrue()
        map.containsKey(text, 6, 5).shouldBeTrue()
        map.containsKey(text, 0, 3).shouldBeFalse()
    }

    @Test
    fun `remove entry`() {
        val map = CharArrayMap<String>(16)
        map["test"] = "value"
        map.size shouldBeEqualTo 1

        map.remove("test")

        // Remove가 호출되었으므로 키가 더 이상 존재하지 않아야 함
        map.containsKey("test").shouldBeFalse()
    }

    @Test
    fun `remove non-existent key`() {
        val map = CharArrayMap<String>(16)

        map.remove("nonexistent")

        map.size shouldBeEqualTo 0
    }

    @Test
    fun `clear map`() {
        val map = CharArrayMap<String>(16)
        map["key1"] = "value1"
        map["key2"] = "value2"

        map.clear()

        map.size shouldBeEqualTo 0
        map.isEmpty() shouldBeEqualTo true
        map.containsKey("key1").shouldBeFalse()
    }

    @Test
    fun `get non-existent key`() {
        val map = CharArrayMap<String>(16)

        map["nonexistent"].shouldBeNull()
    }

    @Test
    fun `rehash on many insertions`() {
        val map = CharArrayMap<String>(8)

        // Insert many elements to trigger rehash
        repeat(100) { i ->
            map["key$i"] = "value$i"
        }

        map.size shouldBeEqualTo 100

        // Verify all entries are still accessible
        repeat(100) { i ->
            map["key$i"] shouldBeEqualTo "value$i"
        }
    }

    @Test
    fun `copy constructor`() {
        val original = CharArrayMap<String>(16)
        original["key1"] = "value1"
        original["key2"] = "value2"

        val copy = CharArrayMap(original)

        copy.size shouldBeEqualTo original.size
        copy["key1"] shouldBeEqualTo "value1"
        copy["key2"] shouldBeEqualTo "value2"
    }

    @Test
    fun `copy from regular Map`() {
        val regularMap =
            mutableMapOf<Any, String>(
                "key1" as Any to "value1",
                "key2" as Any to "value2",
            )

        val charArrayMap = CharArrayMap(regularMap)

        charArrayMap.size shouldBeEqualTo 2
        charArrayMap["key1"] shouldBeEqualTo "value1"
        charArrayMap["key2"] shouldBeEqualTo "value2"
    }

    @Test
    fun `empty map singleton`() {
        val empty1 = CharArrayMap.emptyMap<String>()

        empty1.isEmpty() shouldBeEqualTo true
        empty1.size shouldBeEqualTo 0
    }

    @Test
    fun `keys returns key set`() {
        val map = CharArrayMap<String>(16)
        map["key1"] = "value1"
        map["key2"] = "value2"

        val keys = map.keys

        keys.size shouldBeEqualTo 2
    }

    @Test
    fun `originalKeySet returns all keys`() {
        val map = CharArrayMap<String>(16)
        map["key1"] = "value1"
        map["key2"] = "value2"

        val keys = map.originalKeySet

        keys.size shouldBeEqualTo 2
    }

    @Test
    fun `values collection`() {
        val map = CharArrayMap<String>(16)
        map["key1"] = "value1"
        map["key2"] = "value2"

        val values = map.values

        values.size shouldBeEqualTo 2
    }

    @Test
    fun `entries`() {
        val map = CharArrayMap<String>(16)
        map["key1"] = "value1"

        val entries = map.entries

        entries.size shouldBeEqualTo 1
    }

    @Test
    fun `putAll adds all entries`() {
        val map = CharArrayMap<String>(16)
        val other =
            mutableMapOf<Any, String>(
                "key1" as Any to "value1",
                "key2" as Any to "value2",
            )

        map.putAll(other)

        map.size shouldBeEqualTo 2
        map["key1"] shouldBeEqualTo "value1"
        map["key2"] shouldBeEqualTo "value2"
    }

    @Test
    fun `collision handling - different keys`() {
        val map = CharArrayMap<String>(16)

        // Insert elements that might have hash collisions
        map["Aa"] = "value1"
        map["BB"] = "value2"

        map["Aa"] shouldBeEqualTo "value1"
        map["BB"] shouldBeEqualTo "value2"
    }
}
