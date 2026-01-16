package io.bluetape4k.io.okio

import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.KLogging
import okio.buffer
import okio.sink
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

@TempFolderTest
class TeeSinkTest: AbstractOkioTest() {

    companion object: KLogging()

    private lateinit var temp: TempFolder

    @BeforeAll
    fun beforeAll(tempFolder: TempFolder) {
        temp = tempFolder
    }

    @Test
    fun `정보를 2개의 sink예 씁니다`() {
        val a = System.out.sink()
        val b = temp.createFile().sink()

        TeeSink(a, b).buffer().use { teeSink ->
            teeSink.writeUtf8("hello\n")
            teeSink.flush()
            teeSink.writeUtf8("world!")
        }
    }

    class TeeSink(
        private val sinkA: okio.Sink,
        private val sinkB: okio.Sink,
    ): okio.Sink {

        override fun write(source: okio.Buffer, byteCount: Long) {
            // Writing to sink mutates source. Work around that.
            sinkA.timeout().intersectWith(timeout()) {
                val buffer = okio.Buffer()
                source.copyTo(buffer, byteCount = byteCount)
                sinkA.write(buffer, byteCount)
            }

            sinkB.timeout().intersectWith(timeout()) {
                sinkB.write(source, byteCount)
            }
        }

        override fun flush() {
            sinkA.flush()
            sinkB.flush()
        }

        override fun close() {
            val result1 = runCatching { sinkA.close() }
            val result2 = runCatching { sinkB.close() }

            if (result1.isFailure || result2.isFailure) {
                val tA = result1.exceptionOrNull()
                val tB = result2.exceptionOrNull()
                if (tA != null && tB != null) {
                    tA.addSuppressed(tB)
                }
                throw tA ?: tB ?: IllegalStateException("Both sinks failed to close")
            }
        }

        override fun timeout() = sinkA.timeout()
    }
}
