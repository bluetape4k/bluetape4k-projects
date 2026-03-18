package io.bluetape4k.naivebayes

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanTokenizer
import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

abstract class AbstractNaiveBayesClassifierTest {

    companion object: KLogging()

    fun String.splitWords(): Sequence<String> =
        split(Regex("\\s"))
            .asSequence()
            .map { it.replace(Regex("[^A-Za-z]"), "").lowercase() }
            .filter { it.isNotEmpty() }

    fun String.tokenize(): Sequence<String> {
        return KoreanTokenizer.tokenize(this@tokenize)
            .asSequence()
            .filter { it.pos == KoreanPos.Noun || it.pos == KoreanPos.Adjective }
            .map { it.text.lowercase() }
    }

    data class Email(
        val message: String,
        val isSpam: Boolean,
    ): Serializable

    data class BankTransaction(
        val date: LocalDate,
        val amount: Double,
        val memo: String,
        val category: String? = null,
        val time: LocalDateTime = LocalDateTime.now(),
    ): Serializable
}
