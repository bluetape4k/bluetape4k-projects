package io.bluetape4k.tokenizer.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class CharArraySetTest {
    companion object: KLogging() {
        private const val BASE_PATH = "dictionary"

        private val files =
            listOf(
                "auxiliary/exclamation.txt",
                "noun/nouns.txt",
                "noun/foreign.txt",
                "verb/verb.txt",
            )
    }

    @Test
    fun `read all words and put to CharArraySet`() {
        files
            .map { "$BASE_PATH/$it" }
            .forEach { file ->
                val set = CharArraySet(1_000)

                DictionaryProvider
                    .readFileByLineFromResources(file)
                    .forEach {
                        set.add(it).shouldBeTrue()
                    }

                log.debug { "size=${set.size}" }
                set.shouldNotBeEmpty()

                var count = 0
                set.forEach { key ->
                    set.contains(key).shouldBeTrue()
                    count++
                }
                println("$file count=$count")
            }
    }

    @Test
    fun `create empty set`() {
        val set = CharArraySet(16)
        set.size shouldBeEqualTo 0
    }

    @Test
    fun `add and contains`() {
        val set = CharArraySet(16)

        set.add("hello").shouldBeTrue()
        set.add("world").shouldBeTrue()

        set.size shouldBeEqualTo 2
        set.contains("hello").shouldBeTrue()
        set.contains("world").shouldBeTrue()
        set.contains("notexist").shouldBeFalse()
    }

    @Test
    fun `add duplicate returns false`() {
        val set = CharArraySet(16)

        set.add("test").shouldBeTrue()
        set.add("test").shouldBeFalse()

        set.size shouldBeEqualTo 1
    }

    @Test
    fun `add with CharArray`() {
        val set = CharArraySet(16)
        val key = "test".toCharArray()

        set.add(key).shouldBeTrue()
        set.contains(key).shouldBeTrue()
        set.contains("test").shouldBeTrue()
    }

    @Test
    fun `add with CharSequence`() {
        val set = CharArraySet(16)
        val key: CharSequence = "test"

        set.add(key).shouldBeTrue()
        set.contains(key).shouldBeTrue()
        set.contains("test").shouldBeTrue()
    }

    @Test
    fun `contains with offset and length`() {
        val set = CharArraySet(16)
        set.add("hello")
        set.add("world")

        val text = "hello world".toCharArray()
        set.contains(text, 0, 5).shouldBeTrue()
        set.contains(text, 6, 5).shouldBeTrue()
    }

    @Test
    fun `clear set`() {
        val set = CharArraySet(16)
        set.add("key1")
        set.add("key2")

        set.clear()

        set.size shouldBeEqualTo 0
        set.contains("key1").shouldBeFalse()
    }

    @Test
    fun `copy constructor`() {
        val original = CharArraySet(16)
        original.add("key1")
        original.add("key2")

        val copy = CharArraySet(original)

        copy.size shouldBeEqualTo 2
        copy.contains("key1").shouldBeTrue()
        copy.contains("key2").shouldBeTrue()
    }

    @Test
    fun `iterator`() {
        val set = CharArraySet(16)
        set.add("a")
        set.add("b")
        set.add("c")

        val items = mutableListOf<String>()
        set.forEach {
            when (it) {
                is CharArray -> items.add(String(it))
                is String    -> items.add(it)
            }
        }

        items.size shouldBeEqualTo 3
        items.contains("a").shouldBeTrue()
        items.contains("b").shouldBeTrue()
        items.contains("c").shouldBeTrue()
    }

    @Test
    fun `rehash on many insertions`() {
        val set = CharArraySet(8)

        repeat(100) { i ->
            set.add("key$i").shouldBeTrue()
        }

        set.size shouldBeEqualTo 100

        repeat(100) { i ->
            set.contains("key$i").shouldBeTrue()
        }
    }

    @Test
    fun `remove element`() {
        val set = CharArraySet(16)
        set.add("test")

        set.remove("test").shouldBeTrue()
        set.contains("test").shouldBeFalse()
    }

    @Test
    fun `collision handling`() {
        val set = CharArraySet(16)

        set.add("Aa")
        set.add("BB")
        set.add("C#")

        set.contains("Aa").shouldBeTrue()
        set.contains("BB").shouldBeTrue()
        set.contains("C#").shouldBeTrue()
    }

    @Test
    fun `case sensitivity`() {
        val set = CharArraySet(16)

        set.add("Hello")
        set.add("hello")

        set.size shouldBeEqualTo 2
        set.contains("Hello").shouldBeTrue()
        set.contains("hello").shouldBeTrue()
    }

    @Test
    fun `remove multiple elements from large set`() {
        val set = CharArraySet(8)
        // 대용량 셋 구성
        repeat(200) { i ->
            set.add("word$i")
        }
        set.size shouldBeEqualTo 200

        // 2개 추가
        set.add("19禁")
        set.add("29禁")
        set.size shouldBeEqualTo 202
        set.contains("19禁").shouldBeTrue()
        set.contains("29禁").shouldBeTrue()

        // 개별 삭제
        set.remove("19禁").shouldBeTrue()
        set.contains("19禁").shouldBeFalse()
        set.size shouldBeEqualTo 201

        set.remove("29禁").shouldBeTrue()
        set.contains("29禁").shouldBeFalse()
        set.size shouldBeEqualTo 200
    }

    @Test
    fun `removeAll multiple elements from large set`() {
        val set = CharArraySet(8)
        repeat(200) { i ->
            set.add("word$i")
        }
        set.add("19禁")
        set.add("29禁")

        // removeAll로 한번에 삭제
        set.removeAll(listOf("19禁", "29禁"))
        set.contains("19禁").shouldBeFalse()
        set.contains("29禁").shouldBeFalse()
        set.size shouldBeEqualTo 200
    }

    @Test
    fun `addAll collection`() {
        val set = CharArraySet(16)
        val items = listOf("a", "b", "c")

        set.addAll(items).shouldBeTrue()

        set.size shouldBeEqualTo 3
        set.contains("a").shouldBeTrue()
        set.contains("b").shouldBeTrue()
        set.contains("c").shouldBeTrue()
    }
}
