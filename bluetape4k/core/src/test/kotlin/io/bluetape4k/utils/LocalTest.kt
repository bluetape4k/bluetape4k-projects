package io.bluetape4k.utils

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread

class LocalTest {

    companion object: KLogging()

    @Test
    fun `local value in thread local`() {
        Local["a"] = "Alpha"
        Local["b"] = "Beta"

        Local["a"] shouldBeEqualTo "Alpha"
        Local["b"] shouldBeEqualTo "Beta"

        Local.clearAll()
        Local["a"].shouldBeNull()
        Local["b"].shouldBeNull()
    }

    @Test
    fun `local value in different thread`() {
        val thread1 = thread {
            Local["a"] = "Alpha"
            Thread.sleep(10)
            Local["a"] shouldBeEqualTo "Alpha"
            Local["b"].shouldBeNull()
        }

        val thread2 = thread {
            Local["b"] = "Beta"
            Thread.sleep(5)
            Local["b"] shouldBeEqualTo "Beta"
            Local["a"].shouldBeNull()
        }

        thread1.join()
        thread2.join()
    }

    @Test
    fun `thread local value in multi-threading`() {
        MultithreadingTester()
            .workers(64)
            .rounds(4)
            .add {
                Local["a"] = "에이"
                Thread.sleep(1)
                Local["a"] shouldBeEqualTo "에이"
                Local["b"].shouldBeNull()
            }
            .add {
                Local["a"] = "Alpha"
                Thread.sleep(1)
                Local["a"] shouldBeEqualTo "Alpha"
                Local["b"].shouldBeNull()
            }
            .run()
    }

    @Test
    fun `save and restore local values`() {
        Local.clearAll()
        Local["a"] = "Alpha"

        val snapshot = Local.save()
        Local["a"] = "Beta"
        Local["a"] shouldBeEqualTo "Beta"

        Local.restore(snapshot)
        Local["a"] shouldBeEqualTo "Alpha"
    }

    @Test
    fun `getOrPut and remove local value`() {
        Local.clearAll()

        val created = Local.getOrPut("a") { "Alpha" }
        created shouldBeEqualTo "Alpha"

        val existing = Local.getOrPut("a") { "Beta" }
        existing shouldBeEqualTo "Alpha"

        val removed = Local.remove<String>("a")
        removed.shouldNotBeNull()
        removed shouldBeEqualTo "Alpha"
        Local["a"].shouldBeNull()
    }
}
