package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.tokenizer.korean.TestBase
import io.bluetape4k.tokenizer.utils.CharArraySet
import io.bluetape4k.tokenizer.utils.DictionaryProvider
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class KoreanConjugationTest: TestBase() {

    @Test
    fun `동사 활용`() {
        assertConjudations("$BASE_PATH/verb_conjugate.txt", false)
    }

    @Test
    fun `형용사 활용`() {
        assertConjudations("$BASE_PATH/adj_conjugate.txt", true)
    }

    private fun assertConjudations(filename: String, isAdjective: Boolean) {
        log.debug { "load file=[$filename], isAdjective=$isAdjective" }

        val input = DictionaryProvider.readWordsAsSequence(filename)
        val loaded = input
            .associate {
                val sp = it.split("\t")
                Pair(sp[0], sp[1])
            }

        val result = loaded.all { (predicate, goldensetExpanded) ->
            val conjugated = KoreanConjugation.conjugatePredicatesToCharArraySet(setOf(predicate), isAdjective)
            val matched = matchGoldenset(predicate, conjugated, goldensetExpanded)
            matched
        }
        result.shouldBeTrue()
    }

    private fun matchGoldenset(predicate: String, newExpanded: CharArraySet, examples: String): Boolean {
        val newExpandedString = newExpanded.map { String(it as CharArray) }.sorted().joinToString()
        val isSameToGoldenset = newExpandedString == examples

        if (!isSameToGoldenset) {
            val prevSet = examples.split(", ").toHashSet()
            val newSet = newExpandedString.split(", ").toHashSet()

            log.error {
                """
                |predicate=$predicate
                |${(prevSet - newSet).sorted().joinToString(", ")},
                |${(newSet - prevSet).sorted().joinToString(", ")}
                """.trimMargin()
            }
        }
        return isSameToGoldenset
    }
}
