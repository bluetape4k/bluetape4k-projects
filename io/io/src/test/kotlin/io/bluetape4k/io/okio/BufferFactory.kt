package io.bluetape4k.io.okio

import io.bluetape4k.io.okio.TestUtil.bufferWithRandomSegmentLayout
import io.bluetape4k.io.okio.TestUtil.bufferWithSegments
import okio.Buffer
import kotlin.random.Random

enum class BufferFactory {

    EMPTY {
        override fun newBuffer(): Buffer {
            return Buffer()
        }
    },

    SMALL_BUFFER {
        override fun newBuffer(): Buffer {
            return Buffer().writeUtf8("abcde")
        }
    },
    SMALL_SEGMENTED_BUFFER {
        override fun newBuffer(): Buffer {
            return bufferWithSegments("abc", "defg", "hijkl")
        }
    },
    LARGE_BUFFER {
        override fun newBuffer(): Buffer {
            val dice = Random(0)
            val largeByteArray = ByteArray(512 * 1024)
            dice.nextBytes(largeByteArray)

            return Buffer().write(largeByteArray)
        }
    },

    LARGE_BUFFER_WITH_RANDOM_LAYOUT {
        override fun newBuffer(): Buffer {
            val dice = Random(0)
            val largeByteArray = ByteArray(512 * 1024)
            dice.nextBytes(largeByteArray)

            return bufferWithRandomSegmentLayout(dice, largeByteArray)
        }
    };

    abstract fun newBuffer(): Buffer
}
