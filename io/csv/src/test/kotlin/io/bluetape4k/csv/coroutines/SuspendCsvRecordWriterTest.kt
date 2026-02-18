package io.bluetape4k.csv.coroutines

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.flow
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.StringWriter

class SuspendCsvRecordWriterTest {

    companion object: KLoggingChannel()

    @Nested
    inner class CSV {

        @Test
        fun `write rows`() = runSuspendIO {
            StringWriter().use { sw ->
                SuspendCsvRecordWriter(sw).use { writer ->
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
        fun `write rows with Iterable`() = runSuspendIO {
            StringWriter().use { sw ->
                SuspendCsvRecordWriter(sw).use { writer ->
                    writer.writeHeaders("name", "age", "city")
                    val rows = listOf(
                        listOf("Alice", 20, "Seoul"),
                        listOf("Bob", 30, "Busan"),
                    )
                    writer.writeAll(rows)
                }

                val captured = sw.buffer.toString()

                log.debug { "captured=\n$captured" }
                captured shouldContain "name,age,city"
                captured shouldContain "Alice,20,Seoul"
                captured shouldContain "Bob,30,Busan"
            }
        }

        @Test
        fun `write entities with Iterable and transform`() = runSuspendIO {
            data class Person(val name: String, val age: Int, val city: String)

            val people = listOf(
                Person("Alice", 20, "Seoul"),
                Person("Bob", 30, "Busan"),
            )

            StringWriter().use { sw ->
                SuspendCsvRecordWriter(sw).use { writer ->
                    writer.writeHeaders("name", "age", "city")
                    writer.writeAll(people) { person ->
                        listOf(person.name, person.age, person.city)
                    }
                }

                val captured = sw.buffer.toString()

                log.debug { "captured=\n$captured" }
                captured shouldContain "Alice,20,Seoul"
                captured shouldContain "Bob,30,Busan"
            }
        }

        @Test
        fun `write rows as Flow with headers`() = runSuspendIO {
            StringWriter().use { sw ->
                SuspendCsvRecordWriter(sw).use { writer ->
                    writer.writeHeaders("col1", "col2", "col3", "col4")
                    val rows = flow<List<Any>> {
                        repeat(10) {
                            emit(listOf("row$it", it, it + 1, it + 2))
                        }
                    }
                    writer.writeAll(rows)
                }

                val captured = sw.buffer.toString()

                log.debug { "captured=\n$captured" }
                captured shouldContain """col1,col2,col3,col4"""
                captured shouldContain """row1,1,2,3"""
                captured shouldContain """row2,2,3,4"""
            }
        }
    }

    @Nested
    inner class TSV {
        @Test
        fun `write rows`() = runSuspendIO {
            StringWriter().use { sw ->
                SuspendTsvRecordWriter(sw).use { writer ->
                    val rows = listOf(
                        listOf("row1", 1, 2, 3, 3),
                        listOf("row2", 4, null, 6, 6)
                    )
                    writer.writeAll(rows)
                }

                val captured = sw.buffer.toString()

                log.debug { "captured=\n$captured" }
                captured shouldContain "row1\t1\t2\t3\t3"
                captured shouldContain "row2\t4\t\t6\t6"
            }
        }

        @Test
        fun `write rows with Iterable`() = runSuspendIO {
            StringWriter().use { sw ->
                SuspendTsvRecordWriter(sw).use { writer ->
                    writer.writeHeaders("name", "age", "city")
                    val rows = listOf(
                        listOf("Alice", 20, "Seoul"),
                        listOf("Bob", 30, "Busan"),
                    )
                    writer.writeAll(rows)
                }

                val captured = sw.buffer.toString()

                log.debug { "captured=\n$captured" }
                captured shouldContain "name\tage\tcity"
                captured shouldContain "Alice\t20\tSeoul"
                captured shouldContain "Bob\t30\tBusan"
            }
        }

        @Test
        fun `write entities with Iterable and transform`() = runSuspendIO {
            data class Person(val name: String, val age: Int, val city: String)

            val people = listOf(
                Person("Alice", 20, "Seoul"),
                Person("Bob", 30, "Busan"),
            )

            StringWriter().use { sw ->
                SuspendTsvRecordWriter(sw).use { writer ->
                    writer.writeHeaders("name", "age", "city")
                    writer.writeAll(people) { person ->
                        listOf(person.name, person.age, person.city)
                    }
                }

                val captured = sw.buffer.toString()

                log.debug { "captured=\n$captured" }
                captured shouldContain "Alice\t20\tSeoul"
                captured shouldContain "Bob\t30\tBusan"
            }
        }

        @Test
        fun `write rows as Flow with headers`() = runSuspendIO {
            StringWriter().use { sw ->
                SuspendTsvRecordWriter(sw).use { writer ->
                    writer.writeHeaders("col1", "col2", "col3", "col4")
                    val rows = flow<List<Any>> {
                        repeat(10) {
                            emit(listOf("row$it", it, it + 1, it + 2))
                        }
                    }
                    writer.writeAll(rows)
                }

                val captured = sw.buffer.toString()

                log.debug { "captured=\n$captured" }
                captured shouldContain "col1\tcol2\tcol3\tcol4"
                captured shouldContain "row1\t1\t2\t3"
                captured shouldContain "row2\t2\t3\t4"
            }
        }
    }
}
