package io.bluetape4k.logging

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class KotlinLoggingTest {

    private val log = KotlinLogging.logger {}

    private val loggerName = KotlinLoggingTest::class.qualifiedName!!

    @Test
    fun `logging trace`() {
        log.name shouldBeEqualTo loggerName
    }

    @Test
    fun `create logger`() {
        val logger = KotlinLogging.logger {}
        val loggerByName = KotlinLogging.logger(KotlinLoggingTest::class.java.name)
        val loggerByKClass = KotlinLogging.logger(KotlinLoggingTest::class)

        logger.name shouldBeEqualTo loggerName
        loggerByName.name shouldBeEqualTo loggerName
        loggerByKClass.name shouldBeEqualTo loggerName
    }
}
