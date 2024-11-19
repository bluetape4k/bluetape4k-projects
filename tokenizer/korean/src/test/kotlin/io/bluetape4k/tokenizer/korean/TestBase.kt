package io.bluetape4k.tokenizer.korean

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.tokenizer.korean.utils.KoreanDictionaryProvider
import io.bluetape4k.tokenizer.utils.DictionaryProvider
import org.amshove.kluent.shouldBeFalse
import org.slf4j.Logger
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

abstract class TestBase {

    data class ParseTime(val time: Long, val chunk: String)

    companion object: KLogging() {

        const val BASE_PATH = KoreanDictionaryProvider.BASE_PATH

        inline fun time(block: () -> Unit): Long = measureTimeMillis(block)
        inline fun timeNano(block: () -> Unit): Long = measureNanoTime(block)

        inline fun assertExamples(
            exampleFiles: String,
            log: Logger,
            crossinline func: (String) -> String,
        ) {
            val input =
                DictionaryProvider.readFileByLineFromResources("$BASE_PATH/$exampleFiles")

            var notMatchCount = 0
            val (parseTimes, hasErrors) = input
                .fold(Pair(listOf<ParseTime>(), true)) { (l, output), line ->
                    val s = line.split("\t", limit = 2).map { it.trim() }
                    val (chunk, parse) = Pair(s[0], if (s.size == 2) s[1] else "")

                    val oldTokens = parse
                    val t0 = System.currentTimeMillis()
                    val newTokens = func(chunk)
                    val t1 = System.currentTimeMillis()

                    val oldParseMatches = oldTokens == newTokens
                    if (!oldParseMatches) {
                        notMatchCount++
                        log.debug {
                            """
                            |
                            |Example set match error:
                            |$chunk
                            |  - EXPECTED:$oldTokens 
                            |  - ACTUAL  :$newTokens
                            """.trimMargin()
                        }
                    }

                    Pair(listOf(ParseTime(t1 - t0, chunk)) + l, output && oldParseMatches)
                }

            val averageTime = parseTimes.sumOf { it.time }.toDouble() / parseTimes.size
            val maxItem = parseTimes.maxByOrNull { it.time }

            log.trace {
                """
                |
                |Parsed ${parseTimes.size}
                |Total time: ${parseTimes.map { it.time }.sum()} msec,
                |Average time: $averageTime msec,
                |Max time: ${maxItem?.time}, ${maxItem?.chunk}
                |Not Match count: $notMatchCount
                """.trimMargin()
            }
            hasErrors.shouldBeFalse()
        }
    }
}
