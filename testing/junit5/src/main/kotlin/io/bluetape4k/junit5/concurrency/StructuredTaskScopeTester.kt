package io.bluetape4k.junit5.concurrency

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import java.util.concurrent.StructuredTaskScope
import java.util.concurrent.ThreadFactory

class StructuredTaskScopeTester {

    companion object: KLogging() {
        const val DEFAULT_ROUNDS_PER_TASK: Int = 4
        const val MIN_ROUNDS_PER_TASK: Int = 1
        const val MAX_ROUNDS_PER_TASK: Int = 100_000
    }

    private var roundsPerTask: Int = DEFAULT_ROUNDS_PER_TASK
    private val testBlocks = mutableListOf<() -> Unit>()
    private var factory: ThreadFactory? = null

    fun withFactory(factory: ThreadFactory) = apply {
        this.factory = factory
    }

    fun roundsPerTask(value: Int) = apply {
        require(value in MIN_ROUNDS_PER_TASK..MAX_ROUNDS_PER_TASK) {
            "Invalid roundsPerTask: [$value] -- must be range in $MIN_ROUNDS_PER_TASK..$MAX_ROUNDS_PER_TASK"
        }
        roundsPerTask = value
    }

    fun add(testBlock: () -> Unit) = apply {
        testBlocks.add(testBlock)
    }

    fun addAll(vararg testBlocks: () -> Unit) = apply {
        this.testBlocks.addAll(testBlocks)
    }

    fun run() {
        check(testBlocks.isNotEmpty()) {
            "No test blocks added. Please add test blocks using add() method."
        }

        val factory = this.factory ?: Thread.ofVirtual().factory()
        StructuredTaskScope.ShutdownOnFailure("a", factory).use { scope ->
            repeat(roundsPerTask) {
                testBlocks.forEach { testBlocks ->
                    scope.fork {
                        testBlocks.invoke()
                    }
                }
            }

            scope.join().throwIfFailed {
                log.error(it) { "Test blocks failed with exception: $it" }
                throw it
            }
        }
    }
}
