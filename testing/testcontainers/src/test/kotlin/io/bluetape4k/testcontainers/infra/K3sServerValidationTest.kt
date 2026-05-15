package io.bluetape4k.testcontainers.infra

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.junit.jupiter.api.Test

/**
 * Validation tests for [K3sServer] that do not require a running container.
 * These run in regular CI without a privileged Docker runner.
 */
class K3sServerValidationTest: AbstractContainerTest() {

    companion object: KLogging()

    @Test
    fun `blank image is not allowed`() {
        assertFailsWith<IllegalArgumentException> { K3sServer(image = " ") }
    }

    @Test
    fun `blank tag is not allowed`() {
        assertFailsWith<IllegalArgumentException> { K3sServer(tag = " ") }
    }
}
