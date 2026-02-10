package io.bluetape4k.support

import io.bluetape4k.ValueObject
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
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
    fun `get default class loader`() {
        val defaultClassLoader = getDefaultClassLoader()
        defaultClassLoader shouldBeEqualTo getContextClassLoader()
    }

    @Test
    fun `get class loader from class type`() {
        getClassLoader(KLogging::class) shouldBeEqualTo getContextClassLoader()
        getClassLoader(ValueObject::class) shouldBeEqualTo getClassLoader<ValueObject>()
    }

    @Test
    fun `get class loader from bootstrap class`() {
        // bootstrap ClassLoader로 로드된 클래스(String 등)의 경우에도 non-null ClassLoader를 반환해야 함
        val classLoader = getClassLoader(String::class.java)
        classLoader.shouldNotBeNull()
    }
}