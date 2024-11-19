package io.bluetape4k.csv.coroutines

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.coroutines.flow.flow
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import java.io.StringWriter

class CoCsvRecordWriterTest {

    companion object: KLogging()

    @Test
    fun `write rows`() = runSuspendIO {
        StringWriter().use { sw ->
            CoCsvRecordWriter(sw).use { writer ->
                val rows = listOf(
                    listOf("row1", 1, 2, "3, 3"),
                    listOf("row2  ", 4, null, "6,6")
                )
                writer.writeAll(rows.asSequence())
            }

            val captured = sw.buffer.toString()

            log.trace { "captured=\n$captured" }
            captured shouldContain """row1,1,2,"3, 3""""
            captured shouldContain """row2,4,,"6,6""""
        }
    }

    @Test
    fun `write rows as Flow with headers`() = runSuspendIO {
        StringWriter().use { sw ->
            CoCsvRecordWriter(sw).use { writer ->
                writer.writeHeaders("col1", "col2", "col3", "col4")
                val rows = flow<List<Any>> {
                    repeat(10) {
                        emit(listOf("row$it", it, it + 1, it + 2))
                    }
                }
                writer.writeAll(rows)
            }

            val captured = sw.buffer.toString()

            log.trace { "captured=\n$captured" }
            captured shouldContain """col1,col2,col3,col4"""
            captured shouldContain """row1,1,2,3"""
            captured shouldContain """row2,2,3,4"""
        }
    }
}
