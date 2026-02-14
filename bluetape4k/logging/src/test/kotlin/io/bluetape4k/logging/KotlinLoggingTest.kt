package io.bluetape4k.logging

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
        val loggerByName = KotlinLogging.logger("Logger")
        val loggerByKClass = KotlinLogging.logger(KotlinLoggingTest::class)

        logger.name shouldBeEqualTo loggerName
        loggerByName.name shouldBeEqualTo "Logger"
        loggerByKClass.name shouldBeEqualTo loggerName

        logger.info { "Logger" }
        loggerByName.info { "Logger by Name" }
        loggerByKClass.info { "Logger by KClass" }
    }

    @Test
    fun `blank logger name은 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            KotlinLogging.logger("  ")
        }
    }

    @Test
    fun `local class도 logger를 생성할 수 있다`() {
        class LocalClass

        val logger = KotlinLogging.logger(LocalClass::class)
        assertTrue(logger.name.isNotBlank())
    }
}
