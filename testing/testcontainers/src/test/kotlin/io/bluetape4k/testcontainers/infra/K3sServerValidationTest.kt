package io.bluetape4k.testcontainers.infra

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.junit.jupiter.api.Test

/**
 * 실행 중인 container가 필요 없는 [K3sServer] validation test입니다.
 * privileged Docker runner 없이 일반 CI에서 실행됩니다.
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
