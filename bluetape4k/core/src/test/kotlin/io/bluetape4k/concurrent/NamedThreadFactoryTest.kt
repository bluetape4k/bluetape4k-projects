package io.bluetape4k.concurrent

import io.bluetape4k.LibraryName
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test

class NamedThreadFactoryTest {

    companion object: KLogging()

    @Test
    fun `create named thread`() {
        val factory = NamedThreadFactory(LibraryName)

        val thread1 = factory.newThread { Thread.sleep(100) }
        thread1.name shouldBeEqualTo "$LibraryName-1"

        val thread2 = factory.newThread { Thread.sleep(100) }
        thread2.name shouldBeEqualTo "$LibraryName-2"
    }
}
