package io.bluetape4k.support

import io.bluetape4k.ValueObject
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ClassLoaderSupportTest {

    companion object: KLogging()

    @Test
    fun `load ClassLoader by current context`() {
        val currentClassLoader = getContextClassLoader()
        val systemClassLoader = getSystemClassLoader()

        currentClassLoader shouldBeEqualTo systemClassLoader
    }

    @Test
    fun `get class loader from class type`() {
        getClassLoader(KLogging::class) shouldBeEqualTo getContextClassLoader()
        getClassLoader(ValueObject::class) shouldBeEqualTo getClassLoader<ValueObject>()
    }
}
