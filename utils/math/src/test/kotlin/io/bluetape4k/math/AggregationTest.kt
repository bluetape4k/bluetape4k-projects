package io.bluetape4k.math

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.javatimes.days
import io.bluetape4k.javatimes.toEpochDay
import io.bluetape4k.javatimes.toEpochMillis
import io.bluetape4k.javatimes.todayInstant
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class AggregationTest {

    companion object: KLogging()

    data class Email(val subject: String, val sender: String)
    data class FieldDistribution(val subject: Map<String, Int>, val sender: Map<String, Int>)

    private val emails = listOf(
        Email("I make u offer", "princeofnigeria2000@aol.com"),
        Email("Congratulations!", "lotterybacklog@gmail.com"),
        Email("Your Inheritance is waiting!", "lotterybacklog@gmail.com"),
        Email("Hey", "jessica47@gmail.com")
    )

    @Test
    fun `countBy aggregation`() {
        val distributions = FieldDistribution(
            subject = emails.countBy { it.subject },
            sender = emails.countBy { it.sender }
        )

        log.debug { distributions }

        distributions.subject.size shouldBeEqualTo 4

        distributions.sender.size shouldBeEqualTo 3
        distributions.sender["jessica47@gmail.com"]!! shouldBeEqualTo 1
        distributions.sender["lotterybacklog@gmail.com"]!! shouldBeEqualTo 2
    }

    @Test
    fun `aggregate all char counts by string length`() {
        val strs = listOf("Alpha", "Beta", "Gamma", "Delta", "Epsilon")

        val grouping = strs
            .aggregateBy(
                { it.length },
                { list ->
                    list.flatMap { it.split("") }.count { it.isNotEmpty() }
                }
            )

        // 글자가 5인 놈들의 모든 글자 수 = 5 + 5 + 5
        // 글자가 4인 놈들의 모든 글자 수 = 4
        // 글자가 7인 놈들의 모든 글자 수 = 7
        grouping shouldContainSame mapOf(5 to 15, 4 to 4, 7 to 7)
    }

    data class Event(val timestamp: Long, val duration: Duration)

    @Test
    fun `aggregate for Object`() {
        val today = todayInstant()
        val days = listOf(today - 1.days(), today, today + 1.days())

        val events = listOf(
            Event(days[0].toEpochMillis(), Duration.ofSeconds(60)),
            Event(days[0].toEpochMillis(), Duration.ofSeconds(60)),
            Event(days[1].toEpochMillis(), Duration.ofSeconds(120)),
            Event(days[1].toEpochMillis(), Duration.ofSeconds(60)),
            Event(days[2].toEpochMillis(), Duration.ofSeconds(60)),
            Event(days[2].toEpochMillis(), Duration.ofSeconds(30)),
        )

        // Day 별 Duration의 합을 구한다 (sum(duration) group by day)
        val grouping: Map<Long, Long> = events.aggregateBy(
            { Instant.ofEpochMilli(it.timestamp).toEpochDay() },
            { it.duration.toMillis() / 1000L },
            { durations -> durations.sum() }
        )

        log.debug { "grouping=$grouping" }
        grouping.values.toFastList() shouldContainSame listOf(90, 120, 180)
    }
}
