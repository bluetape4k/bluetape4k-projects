package io.bluetape4k.math

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class GroupingSupportTest {

    companion object: KLogging()

    data class Email(val subject: String, val sender: String)
    data class FieldDistribution(val subject: Map<String, Int>, val sender: Map<String, Int>)

    private val emails = sequenceOf(
        Email("I make u offer", "princeofnigeria2000@aol.com"),
        Email("Congratulations!", "lotterybacklog@gmail.com"),
        Email("Your Inheritance is waiting!", "lotterybacklog@gmail.com"),
        Email("Hey", "jessica47@gmail.com")
    )

    @Test
    fun `groupingCount operation`() {
        // grouping 시 sequence 처럼 처리
        val countBySender: Map<String, Int> = emails.groupingCount { it.sender }
        log.debug { "count by sender=$countBySender" }

        // groping 시 iterator 처럼 처리
        val countBySender2: Map<String, Int> = emails.aggregateBy({ it.sender }, { it.count() })
        countBySender2 shouldBeEqualTo countBySender
    }

    @Test
    fun `grouping fold operation`() {
        val fold = emails.groupingFold(
            keySelector = { it.sender },
            initialValue = 0
        ) { accumulator: Int, element: Email ->
            accumulator + element.subject.length
        }

        log.debug { "fold=$fold" }
        fold["princeofnigeria2000@aol.com"]!! shouldBeEqualTo 14
        fold["lotterybacklog@gmail.com"]!! shouldBeEqualTo 44
        fold["jessica47@gmail.com"]!! shouldBeEqualTo 3
    }

    @Test
    fun `grouping reduce operation`() {
        val reduce: Map<String, Email> = emails.groupingReduce({ it.sender }) { _, accumulator, element ->
            element.copy(subject = accumulator.subject + element.subject)
        }

        reduce["princeofnigeria2000@aol.com"]!!.subject.length shouldBeEqualTo 14
        reduce["lotterybacklog@gmail.com"]!!.subject.length shouldBeEqualTo 44
        reduce["jessica47@gmail.com"]!!.subject.length shouldBeEqualTo 3
    }
}
