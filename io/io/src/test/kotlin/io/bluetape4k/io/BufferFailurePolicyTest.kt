package io.bluetape4k.io

import com.esotericsoftware.kryo.io.KryoBufferOverflowException
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import java.nio.BufferOverflowException
import java.util.concurrent.CancellationException

class BufferFailurePolicyTest {

    @Test
    fun `no failures produces no public failure`() {
        BufferFailurePolicy.classify(null, null).shouldBeNull()
    }

    @Test
    fun `direct operation overflow remains the same primary`() {
        val operation = BufferOverflowException()
        val cleanup = IllegalStateException("cleanup")

        val actual = BufferFailurePolicy.classify(operation, cleanup)

        actual shouldBeSameInstanceAs operation
        operation.suppressed.single() shouldBeSameInstanceAs cleanup
    }

    @Test
    fun `nested native operation overflow becomes public overflow with complete roots`() {
        val native = KryoBufferOverflowException("native")
        val operation = IllegalStateException("operation", native)
        val cleanup = IllegalArgumentException("cleanup")

        val actual = BufferFailurePolicy.classify(operation, cleanup)

        actual?.javaClass shouldBeEqualTo BufferOverflowException::class.java
        actual?.cause shouldBeSameInstanceAs operation
        actual?.suppressed?.single() shouldBeSameInstanceAs cleanup
    }

    @Test
    fun `cleanup overflow does not replace the ordinary operation root as cause`() {
        val operation = IllegalStateException("operation")
        val nestedOverflow = BufferOverflowException()
        val cleanup = IllegalArgumentException("cleanup", nestedOverflow)

        val actual = BufferFailurePolicy.classify(operation, cleanup)

        actual?.javaClass shouldBeEqualTo BufferOverflowException::class.java
        actual?.cause shouldBeSameInstanceAs operation
        actual?.suppressed?.single() shouldBeSameInstanceAs cleanup
    }

    @Test
    fun `direct cleanup overflow after success remains the same instance`() {
        val cleanup = BufferOverflowException()

        BufferFailurePolicy.classify(null, cleanup) shouldBeSameInstanceAs cleanup
    }

    @Test
    fun `nested cleanup overflow after success keeps the complete cleanup root as cause`() {
        val cleanup = IllegalStateException("cleanup", BufferOverflowException())

        val actual = BufferFailurePolicy.classify(null, cleanup)

        actual?.javaClass shouldBeEqualTo BufferOverflowException::class.java
        actual?.cause shouldBeSameInstanceAs cleanup
    }

    @Test
    fun `operation Error remains the same fatal primary`() {
        val operation = AssertionError("fatal")
        val cleanup = BufferOverflowException()

        val actual = BufferFailurePolicy.classify(operation, cleanup)

        actual shouldBeSameInstanceAs operation
        operation.suppressed.single() shouldBeSameInstanceAs cleanup
    }

    @Test
    fun `nested Error wins over overflow elsewhere in the operation graph`() {
        val fatal = AssertionError("fatal")
        val operation = IllegalStateException("operation", BufferOverflowException()).apply {
            addSuppressed(fatal)
        }

        BufferFailurePolicy.classify(operation, null) shouldBeSameInstanceAs fatal
    }

    @Test
    fun `cleanup Error supersedes an ordinary operation failure`() {
        val operation = IllegalStateException("operation")
        val cleanup = LinkageError("fatal cleanup")

        val actual = BufferFailurePolicy.classify(operation, cleanup)

        actual shouldBeSameInstanceAs cleanup
        cleanup.suppressed.single() shouldBeSameInstanceAs operation
    }

    @Test
    fun `nested operation cancellation becomes the exact primary instance`() {
        val cancellation = CancellationException("cancelled")
        val operation = IllegalStateException("operation", cancellation)
        val cleanup = IllegalArgumentException("cleanup")

        val actual = BufferFailurePolicy.classify(operation, cleanup)

        actual shouldBeSameInstanceAs cancellation
        cancellation.suppressed.single() shouldBeSameInstanceAs cleanup
    }

    @Test
    fun `Error still outranks nested cancellation`() {
        val cancellation = CancellationException("cancelled")
        val fatal = AssertionError("fatal")
        val operation = IllegalStateException("operation", cancellation).apply {
            addSuppressed(fatal)
        }

        BufferFailurePolicy.classify(operation, null) shouldBeSameInstanceAs fatal
    }

    @Test
    fun `control failure query returns Error before cancellation`() {
        val cancellation = CancellationException("cancelled")
        val fatal = AssertionError("fatal")
        val failure = IllegalStateException("wrapper", cancellation).apply {
            addSuppressed(fatal)
        }

        BufferFailurePolicy.findControlFailure(failure) shouldBeSameInstanceAs fatal
    }

    @Test
    fun `control failure query returns nested cancellation`() {
        val cancellation = CancellationException("cancelled")
        val failure = IllegalStateException("wrapper", cancellation)

        BufferFailurePolicy.findControlFailure(failure) shouldBeSameInstanceAs cancellation
    }

    @Test
    fun `control failure query ignores JDK and Kryo overflow graphs`() {
        val jdkOverflow = IllegalStateException("wrapper", BufferOverflowException())
        val kryoOverflow = IllegalStateException("wrapper", KryoBufferOverflowException("native"))

        BufferFailurePolicy.findControlFailure(jdkOverflow).shouldBeNull()
        BufferFailurePolicy.findControlFailure(kryoOverflow).shouldBeNull()
    }

    @Test
    fun `control failure query terminates on cycles`() {
        val failure = IllegalStateException("wrapper")
        val cycle = IllegalArgumentException("cycle")
        failure.initCause(cycle)
        cycle.addSuppressed(failure)

        BufferFailurePolicy.findControlFailure(failure).shouldBeNull()
    }

    @Test
    fun `cancellation classification does not introduce an identity cycle`() {
        val cancellation = CancellationException("cancelled")
        val operation = IllegalStateException("operation", cancellation)
        val cleanup = IllegalArgumentException("cleanup", cancellation)

        val actual = BufferFailurePolicy.classify(operation, cleanup)

        actual shouldBeSameInstanceAs cancellation
        cancellation.suppressed.isEmpty().shouldBeTrue()
    }

    @Test
    fun `ordinary dual failure preserves operation primary`() {
        val operation = IllegalStateException("operation")
        val cleanup = IllegalArgumentException("cleanup")

        val actual = BufferFailurePolicy.classify(operation, cleanup)

        actual shouldBeSameInstanceAs operation
        operation.suppressed.single() shouldBeSameInstanceAs cleanup
    }

    @Test
    fun `cyclic cause and suppressed graphs terminate and find overflow`() {
        val operation = IllegalStateException("operation")
        val cycle = IllegalArgumentException("cycle")
        operation.initCause(cycle)
        cycle.addSuppressed(operation)
        cycle.addSuppressed(BufferOverflowException())

        val actual = BufferFailurePolicy.classify(operation, null)

        actual?.javaClass shouldBeEqualTo BufferOverflowException::class.java
        actual?.cause shouldBeSameInstanceAs operation
    }

    @Test
    fun `suppression does not introduce an identity cycle`() {
        val operation = IllegalStateException("operation")
        val cleanup = IllegalArgumentException("cleanup")
        cleanup.addSuppressed(operation)

        val actual = BufferFailurePolicy.classify(operation, cleanup)

        actual shouldBeSameInstanceAs operation
        operation.suppressed.isEmpty().shouldBeTrue()
    }
}
