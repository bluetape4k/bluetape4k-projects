package io.bluetape4k.tokenizer.model

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.AbstractCoreTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ServerityTest: AbstractCoreTest() {

    companion object: KLogging()

    @Test
    fun `severity values`() {
        // Severity는 단순 enum 값들
        Severity.LOW shouldBeEqualTo Severity.LOW
        Severity.MIDDLE shouldBeEqualTo Severity.MIDDLE
        Severity.HIGH shouldBeEqualTo Severity.HIGH
    }

    @Test
    fun `severity default is LOW`() {
        Severity.DEFAULT shouldBeEqualTo Severity.LOW
    }

    @Test
    fun `severity entries count`() {
        Severity.entries.size shouldBeEqualTo 3
    }
}
