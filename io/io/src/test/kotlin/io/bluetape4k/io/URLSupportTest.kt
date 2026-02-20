package io.bluetape4k.io

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.File

class URLSupportTest {

    @Test
    fun `URL toByteArray reads file content`() {
        val temp = File.createTempFile("urlsupport", ".txt").apply {
            writeText("hello-url")
            deleteOnExit()
        }

        val bytes = temp.toURI().toURL().toByteArray()
        bytes.decodeToString() shouldBeEqualTo "hello-url"
    }

    @Test
    fun `URLConnection toByteArray reads content`() {
        val temp = File.createTempFile("urlsupport-conn", ".txt").apply {
            writeText("hello-conn")
            deleteOnExit()
        }

        val conn = temp.toURI().toURL().openConnection()
        val bytes = conn.toByteArray()
        bytes.decodeToString() shouldBeEqualTo "hello-conn"
    }
}
