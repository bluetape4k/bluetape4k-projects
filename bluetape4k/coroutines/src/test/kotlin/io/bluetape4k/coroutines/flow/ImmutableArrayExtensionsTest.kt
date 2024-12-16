package io.bluetape4k.coroutines.flow

import com.danrusu.pods4k.immutableArrays.immutableArrayOf
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ImmutableArrayExtensionsTest {
    companion object: KLogging()

    @Test
    fun `string flow to immutable array`() = runTest {
        val array = flowOf("one", "two", "three").toImmutableArray()
        array shouldBeEqualTo immutableArrayOf("one", "two", "three")
    }

    @Test
    fun `byte flow to immutable byte array`() = runTest {
        val array = flowOf(1.toByte(), 2.toByte(), 3.toByte()).toImmutableByteArray()
        array shouldBeEqualTo immutableArrayOf(1.toByte(), 2.toByte(), 3.toByte())
    }

    @Test
    fun `char flow to immutable char array`() = runTest {
        val array = flowOf('a', 'b', 'c').toImmutableCharArray()
        array shouldBeEqualTo immutableArrayOf('a', 'b', 'c')
    }

    @Test
    fun `short flow to immutable short array`() = runTest {
        val array = flowOf(1.toShort(), 2.toShort(), 3.toShort()).toImmutableShortArray()
        array shouldBeEqualTo immutableArrayOf(1.toShort(), 2.toShort(), 3.toShort())
    }

    @Test
    fun `int flow to immutable int array`() = runTest {
        val array = flowOf(1, 2, 3).toImmutableIntArray()
        array shouldBeEqualTo immutableArrayOf(1, 2, 3)
    }

    @Test
    fun `long flow to immutable long array`() = runTest {
        val array = flowOf(1L, 2L, 3L).toImmutableLongArray()
        array shouldBeEqualTo immutableArrayOf(1L, 2L, 3L)
    }

    @Test
    fun `float flow to immutable float array`() = runTest {
        val array = flowOf(1.0f, 2.0f, 3.0f).toImmutableFloatArray()
        array shouldBeEqualTo immutableArrayOf(1.0f, 2.0f, 3.0f)
    }

    @Test
    fun `double flow to immutable double array`() = runTest {
        val array = flowOf(1.0, 2.0, 3.0).toImmutableDoubleArray()
        array shouldBeEqualTo immutableArrayOf(1.0, 2.0, 3.0)
    }
}
