package io.bluetape4k.csv

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import java.io.StringWriter

class CsvRecordWriterTest {

    companion object: KLogging()

    @Test
    fun `write rows`() {
        StringWriter().use { sw ->
            CsvRecordWriter(sw).use { writer ->
                val rows = listOf(
                    listOf("row1", 1, 2, "3, 3"),
                    listOf("row2  ", 4, null, "6,6")
                )
                writer.writeAll(rows.asSequence())
            }

            val captured = sw.buffer.toString()

            log.debug { "captured=\n$captured" }
            captured shouldContain """row1,1,2,"3, 3""""
            captured shouldContain """row2,4,,"6,6""""
        }
    }

    @Test
    fun `write rows with headers`() {
        StringWriter().use { sw ->
            CsvRecordWriter(sw).use { writer ->

                writer.writeHeaders("col1", "col2", "col3", "col4")

                repeat(10) {
                    writer.writeRow(listOf("row$it", it, it + 1, it + 2))
                }
            }

            val captured = sw.buffer.toString()

            log.debug { "captured=\n$captured" }
            captured shouldContain """col1,col2,col3,col4"""
            captured shouldContain """row1,1,2,3"""
            captured shouldContain """row2,2,3,4"""
        }
    }
}
