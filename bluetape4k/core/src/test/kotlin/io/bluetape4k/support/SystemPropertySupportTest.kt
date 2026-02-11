package io.bluetape4k.support

import io.bluetape4k.AbstractCoreTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class SystemPropertySupportTest: AbstractCoreTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Test
    fun `System property 를 읽고 쓰기를 수행한다`() {
        sysProperty["foo"] = "bar"
        sysProperty["foo"] shouldBeEqualTo "bar"

        sysProperty["foo"] = EMPTY_STRING
        sysProperty["foo"] shouldBeEqualTo EMPTY_STRING
    }
}
