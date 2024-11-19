package io.bluetape4k.cassandra.examples.datatypes

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.type.codec.ExtraTypeCodecs
import com.datastax.oss.driver.api.core.type.codec.MappingCodec
import com.datastax.oss.driver.api.core.type.codec.TypeCodecs
import com.datastax.oss.driver.api.core.type.reflect.GenericType
import io.bluetape4k.cassandra.AbstractCassandraTest
import io.bluetape4k.cassandra.CqlSessionProvider
import io.bluetape4k.cassandra.data.getList
import io.bluetape4k.cassandra.data.getValue
import io.bluetape4k.cassandra.data.setValue
import io.bluetape4k.io.getBytes
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.time.ZonedDateTime
import java.util.*

class CustomCodecExamples: AbstractCassandraTest() {

    companion object: KLogging() {
        private val VIDEO_TABLE =
            """
            CREATE TABLE IF NOT EXISTS videos(
                pk int PRIMARY KEY,
                contents blob,
                uploaded tuple<timestamp, text>, 
                tags list<text>,
                week_day text,
                ip inet
            )
            """.trimIndent()

        private val OPTIONAL_OF_INET: GenericType<Optional<InetAddress>> =
            GenericType.optionalOf(InetAddress::class.java)
    }

    class CqlIntToStringCodec: MappingCodec<Int, String>(TypeCodecs.INT, GenericType.STRING) {
        override fun innerToOuter(value: Int?): String? = value?.toString()
        override fun outerToInner(value: String?): Int? = value?.toInt()
    }

    enum class WeekDay {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    @Test
    fun `사용자 정의 Codec 사용 예제`() {
        val session = CqlSessionProvider.getOrCreateSession(
            "customs",
            { newCqlSessionBuilder() },
            { registerCodecs() }
        )

        createSchema(session)
        insertData(session)
        retrieveData(session)
    }

    private fun CqlSessionBuilder.registerCodecs() {
        addTypeCodecs(
            ExtraTypeCodecs.BLOB_TO_ARRAY,
            ExtraTypeCodecs.ZONED_TIMESTAMP_PERSISTED,
            ExtraTypeCodecs.listToArrayOf(TypeCodecs.TEXT),
            ExtraTypeCodecs.enumNamesOf(WeekDay::class.java),
            ExtraTypeCodecs.optionalOf(TypeCodecs.INET),
            CqlIntToStringCodec()
        )
    }

    private fun createSchema(session: CqlSession) {
        session.execute(VIDEO_TABLE).wasApplied().shouldBeTrue()
    }

    private fun insertData(session: CqlSession) {
        val query = """
            INSERT INTO videos (pk, contents, uploaded, tags, week_day, ip)
            VALUES (:pk, :contents, :uploaded, :tags, :week_day, :ip)
            """.trimIndent()

        val ps = session.prepare(query)

        val contents = byteArrayOf(1, 2, 3, 4)
        val uploaded = ZonedDateTime.now()
        val tags = arrayOf("comedy", "US")
        val weekDay = WeekDay.SATURDAY
        val maybeIp = Optional.empty<InetAddress>()

        val boundStatement = ps.bind()
            .setString("pk", "1")              // use CqlIntToStringCodec
            .setValue("contents", contents)                  // TypeCodecs.BLOB_SIMPLE
            .setValue("uploaded", uploaded)                  // TypeCodecs.ZONED_TIMESTAMP_PERSISTED
            .setValue("tags", tags)                          // TypeCodecs.arrayOf(TypeCodecs.TEXT)
            .setValue("week_day", weekDay)                   // TypeCodecs.enumNameOf(WeekDay::class.java)
            .set("ip", maybeIp, OPTIONAL_OF_INET) // TypeCodecs.optionalOf(TypeCodecs.INET)

        session.execute(boundStatement).wasApplied().shouldBeTrue()
    }

    private fun retrieveData(session: CqlSession) {
        val query = """SELECT pk, contents, uploaded, tags, week_day, ip FROM videos WHERE pk = ?"""
        val stmt = SimpleStatement.newInstance(query, 1)

        val row = session.execute(stmt).one()
        row.shouldNotBeNull()

        val pk = row.getString("pk")
        val contents = row.getValue<ByteArray>("contents")
        val uploaded = row.getValue<ZonedDateTime>("uploaded")
        val tags = row.getValue<Array<String>>("tags")
        val weekDay = row.getValue<WeekDay>("week_day")
        val maybeIp = row.get("ip", OPTIONAL_OF_INET)

        println("pk=$pk")
        println("contents=${contents?.joinToString()}")
        println("uploaded=$uploaded")
        println("tags=${tags?.joinToString()}")
        println("week_day=$weekDay")
        println("ip=$maybeIp")

        pk.shouldNotBeNull() shouldBeEqualTo "1"
        contents.shouldNotBeNull() shouldBeEqualTo byteArrayOf(1, 2, 3, 4)
        uploaded.shouldNotBeNull()
        tags.shouldNotBeNull() shouldBeEqualTo arrayOf("comedy", "US")
        weekDay.shouldNotBeNull() shouldBeEqualTo WeekDay.SATURDAY
        maybeIp.shouldNotBeNull()

        // Retrieve value with built-in codecs
        row.getInt("pk") shouldBeEqualTo 1
        row.getByteBuffer("contents")!!.getBytes() shouldBeEqualTo byteArrayOf(1, 2, 3, 4)
        row.getTupleValue("uploaded")!!.formattedContents.shouldNotBeEmpty()
        row.getList<String>("tags")!! shouldBeEqualTo listOf("comedy", "US")
        row.getString("week_day")!! shouldBeEqualTo WeekDay.SATURDAY.toString()
        row.getInetAddress("ip").shouldBeNull()
    }
}
