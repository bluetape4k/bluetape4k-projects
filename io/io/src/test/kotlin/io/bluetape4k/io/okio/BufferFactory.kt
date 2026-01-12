package io.bluetape4k.io.okio

import okio.Buffer
import kotlin.random.Random

interface BufferFactory {

    fun newBuffer(): Buffer

    companion object {
        val EMPTY: BufferFactory = object: BufferFactory {
            override fun newBuffer(): Buffer = Buffer()
        }
        val SMALL_BUFFER: BufferFactory = object: BufferFactory {
            override fun newBuffer(): Buffer = bufferOf("abcde")
        }
        val SMALL_SEGMENTED_BUFFER: BufferFactory = object: BufferFactory {
            override fun newBuffer(): Buffer = TestUtil.bufferWithSegments("abc", "defg", "hijkl")
        }
        val LARGE_BUFFER: BufferFactory = object: BufferFactory {
            override fun newBuffer(): Buffer {
                val largeByteArray = ByteArray(512 * 1024)
                Random.nextBytes(largeByteArray)
                return Buffer().write(largeByteArray)
            }
        }
        val LARGE_BUFFER_WITH_RANDOM_LAYOUT: BufferFactory = object:
            BufferFactory {
            override fun newBuffer(): Buffer {
                val largeByteArray = ByteArray(512 * 1024)
                Random.nextBytes(largeByteArray)
                return TestUtil.bufferWithRandomSegmentLayout(largeByteArray)
            }
        }

        val factories: List<BufferFactory> = listOf(
            EMPTY,
            SMALL_BUFFER,
            SMALL_SEGMENTED_BUFFER,
            LARGE_BUFFER,
            LARGE_BUFFER_WITH_RANDOM_LAYOUT
        )
    }
}
