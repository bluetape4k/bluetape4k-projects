package io.bluetape4k.utils

import io.bluetape4k.junit5.output.InMemoryLogbackAppender
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test

class SingletonHolderTest {

    class Manager private constructor(private val name: String) {
        companion object: SingletonHolder<Manager>({ Manager("manager") }) {
            val log = KotlinLogging.logger {}
        }

        fun doStuff() {
            log.debug { "name=$name" }
        }
    }

    private val appender = InMemoryLogbackAppender()

    @Test
    fun `get singleton manager`() {
        val manager = Manager.getInstance()
        manager.doStuff()

        if (Manager.log.isDebugEnabled) {
            appender.lastMessage!! shouldContain "name=manager"
        }
    }

    @Test
    fun `get singleton manager with parallel mode`() {
        val managers = List(100) { it }
            .parallelStream()
            .map { Manager.getInstance() }
            .distinct()
            .toList()

        managers.size shouldBeEqualTo 1
    }
}
