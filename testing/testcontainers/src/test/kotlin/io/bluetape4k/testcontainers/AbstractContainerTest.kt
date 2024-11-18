package io.bluetape4k.testcontainers

import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractContainerTest {

    companion object: KLogging()
}
