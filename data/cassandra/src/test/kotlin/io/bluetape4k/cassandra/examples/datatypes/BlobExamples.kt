package io.bluetape4k.cassandra.examples.datatypes

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.data.ByteUtils
import io.bluetape4k.cassandra.AbstractCassandraTest
import io.bluetape4k.cassandra.data.getMap
import io.bluetape4k.io.erase
import io.bluetape4k.io.getBytes
import io.bluetape4k.io.toByteBuffer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Resourcex
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer

class BlobExamples: AbstractCassandraTest() {

    companion object: KLoggingChannel() {
        private const val BLOB_TABLE =
            """CREATE TABLE IF NOT EXISTS blobs(k int PRIMARY KEY, b blob, m map<text, blob>)"""

        private val CASSANDRA_LOGO = Resourcex.getInputStream("cassandra_logo.png")
    }

    @Test
    fun `insert blob column`() {
        createSchema(session)
        allocateAndInsert(session)
        retrieveSampleColumn(session)
        retrieveMapColumn(session)

        insertConcurrent(session)
        retrieveFromFileAndInsertInto(session)
    }

    private fun createSchema(session: CqlSession) {
        log.debug { "Execute $BLOB_TABLE" }
        session.execute(BLOB_TABLE).wasApplied().shouldBeTrue()
    }

    private fun allocateAndInsert(session: CqlSession) {
        //        val buffer = ByteBuffer.allocate(16).apply {
        //            while (hasRemaining()) {
        //                put(0xFF.toByte())
        //            }
        //        }
        val buffer = ByteBuffer.allocate(16).apply { erase(0xFF.toByte()) }
        buffer.limit() - buffer.position() shouldBeEqualTo 0
        buffer.flip()
        buffer.limit() - buffer.position() shouldBeEqualTo 16

        val map = hashMapOf("test" to buffer)

        val ps = session.prepare("INSERT INTO blobs (k, b, m) VALUES (1, ?, ?)")
        session.execute(ps.bind(buffer, map)).wasApplied().shouldBeTrue()
    }

    private fun retrieveSampleColumn(session: CqlSession) {
        val row = session.execute("SELECT b, m FROM blobs WHERE k = 1").one()
        row.shouldNotBeNull()

        val buffer = row.getByteBuffer("b")
        buffer.shouldNotBeNull()
        buffer.limit() - buffer.position() shouldBeEqualTo 16

        // 절대 자표로 값을 가져온다. 0부터가 아니라 현 position 부터 limit 전까지 요소를 가져온다
        for (i in buffer.position() until buffer.limit()) {
            val b = buffer.get(i)
            b shouldBeEqualTo 0xFF.toByte()
        }
        // 다른 방식으로 Buffer의 값을 가져온다
        while (buffer.hasRemaining()) {
            val b = buffer.get()
            b shouldBeEqualTo 0xFF.toByte()
        }

        buffer.position() shouldBeEqualTo buffer.limit()
        buffer.flip()

        val array = buffer.getBytes()
        array.size shouldBeEqualTo 16
        array.all { it == 0xFF.toByte() }.shouldBeTrue()
    }

    private fun retrieveMapColumn(session: CqlSession) {
        val row = session.execute("SELECT b, m FROM blobs WHERE k = 1").one()
        row.shouldNotBeNull()

        val map = row.getMap<String, ByteBuffer>("m")
        map.shouldNotBeNull()

        val buffer = map["test"]!!
        log.debug { "buffer limit=${buffer.limit()}, position=${buffer.position()}" }
        buffer.limit() - buffer.position() shouldBeEqualTo 16

        val array = buffer.getBytes()
        array.size shouldBeEqualTo 16
        array.all { it == 0xFF.toByte() }.shouldBeTrue()
    }

    private fun insertConcurrent(session: CqlSession) {
        val ps = session.prepare("INSERT INTO blobs (k, b) VALUES (1, :b)")

        val buffer = ByteUtils.fromHexString("0xFFFFFF")

        val boundStmt = ps.bind().setByteBuffer("b", buffer)

        // 원본 조작을 하더라도, statement에 binding 이후라 statement에는 영향이 없다
        buffer.position(buffer.limit())

        session.execute(boundStmt)

        val row = session.execute("SELECT b from blobs WHERE k=1").one()!!
        ByteUtils.toHexString(row.getByteBuffer("b")) shouldBeEqualTo "0xffffff"

        // 원본 ByteBuffer 내용을 변경하고 다시 저장하면 반영됩니다.
        buffer.flip()
        buffer.put(0, 0xaa.toByte())

        session.execute(boundStmt)

        val row2 = session.execute("SELECT b from blobs WHERE k=1").one()!!
        ByteUtils.toHexString(row2.getByteBuffer("b")) shouldBeEqualTo "0xaaffff"


        val startPosition = buffer.position()
        val buffer2 = ByteBuffer.allocate(buffer.limit() - startPosition)
        buffer2.put(buffer)
        buffer.position(startPosition)
        buffer2.flip()

        boundStmt.setByteBuffer("b", buffer2)
        session.execute(boundStmt)
    }

    private fun retrieveFromFileAndInsertInto(session: CqlSession) {
        val buffer = readAll(CASSANDRA_LOGO!!)
        buffer.hasRemaining().shouldBeTrue()

        val ps = session.prepare("INSERT INTO blobs (k, b) VALUES (1, ?)")
        session.execute(ps.bind(buffer))

        val tmpFile = File.createTempFile("blob", ".jpg")
        log.debug { "Write retrieved buffer to ${tmpFile.absoluteFile}" }

        val row = session.execute("SELECT b FROM blobs WHERE k=1").one()
        row.shouldNotBeNull()
        writeAll(row.getByteBuffer("b")!!, tmpFile)
    }

    private fun readAll(inputStream: InputStream): ByteBuffer {
        return inputStream.readBytes().toByteBuffer()
    }

    private fun writeAll(buffer: ByteBuffer, file: File) {
        FileOutputStream(file).use { fos ->
            fos.channel.write(buffer)
        }
    }
}
