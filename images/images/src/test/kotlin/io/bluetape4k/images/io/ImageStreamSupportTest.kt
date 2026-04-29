package io.bluetape4k.images.io

import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.stream.ImageInputStream
import javax.imageio.stream.ImageOutputStream
import javax.imageio.stream.MemoryCacheImageInputStream
import javax.imageio.stream.MemoryCacheImageOutputStream
import kotlin.test.assertFailsWith

class ImageStreamSupportTest {

    @Test
    fun `image input stream usingSuspend should close`() = runTest {
        val stream = MemoryCacheImageInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3)))

        stream.useSuspending { input: ImageInputStream ->
            input.read() shouldBeEqualTo 1
        }

        assertFailsWith<IOException> {
            stream.read()
        }
    }

    @Test
    fun `image output stream usingSuspend should close`() = runTest {
        val stream = MemoryCacheImageOutputStream(ByteArrayOutputStream())

        stream.useSuspending { output: ImageOutputStream ->
            output.write(byteArrayOf(1, 2, 3))
        }

        assertFailsWith<IOException> {
            stream.write(1)
        }
    }
}
