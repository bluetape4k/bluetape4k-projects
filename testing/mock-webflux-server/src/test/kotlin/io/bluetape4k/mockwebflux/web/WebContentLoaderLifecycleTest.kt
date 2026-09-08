package io.bluetape4k.mockwebflux.web

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

class WebContentLoaderLifecycleTest {
    @Test
    fun `읽기를 마치면 입력 스트림을 닫는다`() {
        val stream = TrackingStream(false)
        withResource(stream) { WebContentLoader().load("home") shouldBeEqualTo "한글 HTML" }
        stream.closes shouldBeEqualTo 1
    }

    @Test
    fun `읽기에 실패해도 입력 스트림을 닫는다`() {
        val stream = TrackingStream(true)
        withResource(stream) {
            assertFailsWith<IOException> { WebContentLoader().load("home") }
        }
        stream.closes shouldBeEqualTo 1
    }

    private fun withResource(stream: InputStream, block: () -> Unit) {
        val thread = Thread.currentThread()
        val original = thread.contextClassLoader
        thread.contextClassLoader = object: ClassLoader(original) {
            override fun getResourceAsStream(name: String): InputStream? =
                if (name == "web/html/home.html") stream else super.getResourceAsStream(name)
        }
        try {
            block()
        } finally {
            thread.contextClassLoader = original
        }
    }

    private class TrackingStream(private val fail: Boolean): InputStream() {
        private val delegate = ByteArrayInputStream("한글 HTML".toByteArray(Charsets.UTF_8))
        var closes = 0
            private set

        override fun read(): Int {
            if (fail) throw IOException("read failed")
            return delegate.read()
        }

        override fun close() {
            closes++
            delegate.close()
        }
    }
}
