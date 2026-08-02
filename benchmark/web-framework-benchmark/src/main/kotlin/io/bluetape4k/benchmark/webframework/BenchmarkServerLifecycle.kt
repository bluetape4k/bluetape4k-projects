package io.bluetape4k.benchmark.webframework

/**
 * Keeps benchmark server startup and cleanup failures observable to JMH.
 *
 * The first resource is always closed when the second factory or the combiner
 * fails. Cleanup continues after a failure; the startup failure remains the
 * primary exception and cleanup failures are attached as suppressed exceptions.
 */
internal object BenchmarkServerLifecycle {

    fun <F: AutoCloseable, S: AutoCloseable, R> start(
        firstFactory: () -> F,
        secondFactory: () -> S,
        combine: (F, S) -> R,
    ): R {
        val first = firstFactory()
        var second: S? = null

        return try {
            second = secondFactory()
            combine(first, second)
        } catch (startupFailure: Throwable) {
            second?.let { closeAndSuppress(it, startupFailure) }
            closeAndSuppress(first, startupFailure)
            throw startupFailure
        }
    }

    fun closeAll(first: AutoCloseable, second: AutoCloseable) {
        var primaryFailure: Throwable? = null

        try {
            first.close()
        } catch (failure: Throwable) {
            primaryFailure = failure
        }

        try {
            second.close()
        } catch (failure: Throwable) {
            if (primaryFailure == null) {
                primaryFailure = failure
            } else if (primaryFailure !== failure) {
                primaryFailure?.addSuppressed(failure)
            }
        }

        primaryFailure?.let { throw it }
    }

    private fun closeAndSuppress(resource: AutoCloseable, primaryFailure: Throwable) {
        try {
            resource.close()
        } catch (cleanupFailure: Throwable) {
            if (cleanupFailure !== primaryFailure) {
                primaryFailure.addSuppressed(cleanupFailure)
            }
        }
    }
}
