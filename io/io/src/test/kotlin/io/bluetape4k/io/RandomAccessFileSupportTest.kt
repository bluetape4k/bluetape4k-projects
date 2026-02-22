package io.bluetape4k.io

import io.bluetape4k.logging.KLogging
import io.bluetape4k.utils.Resourcex
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.file.Files

class RandomAccessFileSupportTest: AbstractIOTest() {

    companion object: KLogging()

    private val file = File("src/test/resources/files/Utf8Samples.txt")
    private val expected: ByteArray by lazy { Resourcex.getBytes("/files/Utf8Samples.txt") }

    @Test
    fun `RandomAccessFile의 내용일 읽어 ByteBuffer에 넣기`() {
        val buffer = ByteBuffer.allocate(file.length().toInt())
        readToByteBuffer(buffer)

        buffer.flip()
        buffer.getAllBytes() shouldBeEqualTo expected
    }

    @Test
    fun `RandomAccessFile의 내용일 읽어 Direct ByteBuffer에 넣기`() {
        val buffer = ByteBuffer.allocateDirect(file.length().toInt())
        readToByteBuffer(buffer)

        buffer.flip()
        buffer.getAllBytes() shouldBeEqualTo expected
    }

    private fun readToByteBuffer(destBuffer: ByteBuffer) {
        RandomAccessFile(file, "r").use { raf ->
            val reads = raf.readTo(destBuffer)
            reads shouldBeEqualTo file.length().toInt()
        }
    }

    @Test
    fun `RandomAccessFile의 내용을 비교하기`() {
        val raf1 = RandomAccessFile(file, "r")
        val raf2 = RandomAccessFile(file, "rw")

        raf1.contentEquals(raf2).shouldBeTrue()

        raf1.close()
        raf2.close()
    }

    @Test
    fun `RandomAccessFile의 내용을 읽어 ByteArray로 반환`() {
        RandomAccessFile(file, "r").use { raf ->
            val bytes = raf.read(0, file.length().toInt())
            bytes shouldBeEqualTo expected
        }

        RandomAccessFile(file, "r").use { raf ->
            val bytes2 = ByteArray(file.length().toInt())
            raf.read(bytes2)
            bytes2 shouldBeEqualTo expected
        }
    }

    @Test
    fun `RandomAccessFile readTo는 direct buffer에서도 실제 읽은 바이트만 기록한다`() {
        val tempFile = Files.createTempFile("raf-read-to-", ".txt").toFile().apply {
            writeText("ABCDEFGHIJ")
            deleteOnExit()
        }
        val dst = ByteBuffer.allocateDirect(4)

        RandomAccessFile(tempFile, "r").use { raf ->
            val readCount = raf.readTo(dst, limit = 4)
            readCount shouldBeEqualTo 4
        }

        dst.flip()
        dst.getAllBytes().decodeToString() shouldBeEqualTo "ABCD"
    }
}
