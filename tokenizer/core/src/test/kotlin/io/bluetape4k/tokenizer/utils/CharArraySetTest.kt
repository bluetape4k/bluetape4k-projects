package io.bluetape4k.tokenizer.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class CharArraySetTest {

    companion object: KLogging() {
        private const val BASE_PATH = "dictionary"

        private val files = listOf(
            "auxiliary/exclamation.txt",
            "noun/nouns.txt",
            "noun/foreign.txt",
            "verb/verb.txt"
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
}
