package io.bluetape4k.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldStartWith
import org.junit.jupiter.api.Test

class ResourcexTest {

    companion object: KLogging() {
        private const val RESOURCE_PATH = "/files/Utf8Samples.txt"
    }

    @Test
    fun `load existing resource as stream`() {
        Resourcex.getInputStream(RESOURCE_PATH).use { inputStream ->
            inputStream.shouldNotBeNull()
        }

        Resourcex.getInputStream(RESOURCE_PATH.removePrefix("/")).use { inputStream ->
            inputStream.shouldNotBeNull()
        }
        Resourcex.getInputStream(RESOURCE_PATH, Thread.currentThread().contextClassLoader).use { inputStream ->
            inputStream.shouldNotBeNull()
        }
    }

    @Test
    fun `load not existing resource as stream`() {
        Resourcex.getInputStream("not-exists-file").use { inputStream ->
            inputStream.shouldBeNull()
        }
        Resourcex.getInputStream("/files/not-exists-file").use { inputStream ->
            inputStream.shouldBeNull()
        }
    }

    @Test
    fun `load resource as String`() {
        val text = Resourcex.getString(RESOURCE_PATH)
        text.shouldNotBeEmpty() shouldStartWith "Kon nie 'n tydelike lêer skep vir storing van:"
    }

    @Test
    fun `load resources as ByteArray`() {
        val bytes = Resourcex.getBytes(RESOURCE_PATH)
        bytes.shouldNotBeEmpty()

        bytes.toUtf8String() shouldBeEqualTo Resourcex.getString(RESOURCE_PATH)
    }
}
