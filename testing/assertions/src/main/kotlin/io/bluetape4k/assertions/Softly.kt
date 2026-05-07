package io.bluetape4k.assertions

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.function.Executable

/**
 * Scope for collecting soft assertions.
 *
 * State is confined to a single instance — no thread-local, no synchronization.
 * Safe under virtual threads as long as the scope is not shared across threads
 * (the standard usage pattern via [assertSoftly] guarantees this).
 */
class SoftAssertionScope {
    private val _executables = mutableListOf<Executable>()

    /**
     * Register an assertion to be evaluated when the scope completes.
     */
    fun add(block: () -> Unit) {
        _executables.add(Executable { block() })
    }

    @PublishedApi
    internal fun executables(): List<Executable> = _executables.toList()
}

/**
 * Run a block of soft assertions and report all failures together.
 *
 * Failures are collected and thrown as a single `MultipleFailuresError`
 * via JUnit 5's [Assertions.assertAll]. If all assertions succeed, returns normally.
 */
inline fun assertSoftly(block: SoftAssertionScope.() -> Unit) {
    val scope = SoftAssertionScope()
    scope.block()
    Assertions.assertAll(scope.executables())
}
