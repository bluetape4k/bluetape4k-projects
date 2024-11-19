package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.korean.TestBase
import io.bluetape4k.tokenizer.utils.CharArraySet
import io.bluetape4k.tokenizer.utils.DictionaryProvider
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class CharArraySetTest: TestBase() {

    companion object: KLogging() {
        private val files = listOf(
            "auxiliary/exclamation.txt",
            "noun/nouns.txt",
            "noun/foreign.txt",
            "verb/verb.txt"
        ).map { "$BASE_PATH/$it" }
    }

    @Test
    fun `read all words and put to KharArraySet`() = runTest {
        files.forEach { file ->
            val set = CharArraySet(10000)

            DictionaryProvider.readFileByLineFromResources(file)
                .asFlow()
                .collect {
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
