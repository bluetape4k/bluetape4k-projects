package io.bluetape4k.spring.cassandra.convert

import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.data.TupleValue
import com.datastax.oss.driver.api.core.type.DataTypes
import com.datastax.oss.driver.api.core.type.TupleType
import com.datastax.oss.driver.api.core.uuid.Uuids
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.spring.cassandra.convert.model.CounterEntity
import io.bluetape4k.spring.cassandra.convert.model.TimeEntity
import io.bluetape4k.spring.cassandra.domain.model.AllPossibleTypes
import io.bluetape4k.spring.cassandra.domain.model.Condition
import io.bluetape4k.spring.cassandra.schema.SchemaGenerator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.core.CassandraOperations
import org.springframework.data.cassandra.core.selectOneById
import java.math.BigDecimal
import java.math.BigInteger
import java.net.InetAddress
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@SpringBootTest(classes = [ConvertTestConfiguration::class])
@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CassandraTypeMappingTest(
    @param:Autowired private val operations: CassandraOperations,
): io.bluetape4k.spring.cassandra.AbstractCassandraTest() {

    private val cleanupActions = mutableListOf<() -> Unit>()

    /**
     * 매핑 대상 테이블을 이 클래스에서 명시적으로 보장합니다.
     * 테스트는 고유한 time-based id를 사용하고 JUnit same-thread 계약으로 실행해
     * 공유 keyspace를 truncate하지 않아 다른 Cassandra 테스트와 상태를 섞지 않습니다.
     */
    @BeforeAll
    fun createSchema() {
        SchemaGenerator.potentiallyCreateTableFor<CounterEntity>(operations)
        SchemaGenerator.potentiallyCreateTableFor<TimeEntity>(operations)
        SchemaGenerator.potentiallyCreateTableFor<AllPossibleTypes>(operations)
    }

    @AfterEach
    fun cleanupRows() {
        cleanupActions.asReversed().forEach { it() }
        cleanupActions.clear()
    }

    @Test
    @Order(1)
    fun `context loading`() {
        operations.shouldNotBeNull()
        session.shouldNotBeNull()
    }

    @Test
    @Order(2)
    fun `time values round trip through Cassandra`() {
        val expected = TimeEntity(
            id = Uuids.timeBased().toString(),
            time = 42_000_000_000L,
        )

        trackCleanup(expected.id, TimeEntity::class.java)
        operations.insert(expected)

        val actual = operations.selectOneById<TimeEntity>(expected.id)
        actual shouldBeEqualTo expected
    }

    @Test
    @Order(3)
    fun `all supported scalar collection tuple and temporal values round trip`() {
        val instant = Instant.parse("2026-08-25T05:00:00Z")
        val expected = AllPossibleTypes(
            id = Uuids.timeBased().toString(),
            inet = InetAddress.getByName("127.0.0.1"),
            uuid = Uuids.timeBased(),
            justNumber = 123 as java.lang.Number?,
            boxedByte = 7.toByte() as java.lang.Byte?,
            primitiveByte = 8,
            boxedLong = 9_000L as java.lang.Long?,
            primitiveLong = 10_000L,
            boxedInteger = 11 as java.lang.Integer?,
            primitiveInteger = 12,
            boxedFloat = 13.5F as java.lang.Float?,
            primitiveFloat = 14.5F,
            boxedDouble = 15.5 as java.lang.Double?,
            primitiveDouble = 16.5,
            boxedBoolean = true as java.lang.Boolean?,
            primitiveBoolean = false,
            instant = instant,
            date = LocalDate.of(2026, 8, 25),
            time = LocalTime.of(5, 6, 7, 8_000_000),
            timestamp = Date.from(instant),
            bigDecimal = BigDecimal("12345.6789"),
            bigInteger = BigInteger("9876543210"),
            blob = ByteBuffer.wrap(byteArrayOf(1, 2, 3, 4)),
            setOfString = mutableSetOf("alpha", "beta"),
            listOfString = mutableListOf("first", "second"),
            onEnum = Condition.MINT,
            setOfEnum = mutableSetOf(Condition.MINT),
            listOfEnum = mutableListOf(Condition.MINT),
            tupleValue = tupleValue("tuple", 17L),
            localDateTime = LocalDateTime.of(2026, 8, 25, 5, 6, 7, 8_000_000),
            zoneId = ZoneId.of("UTC"),
        )

        trackCleanup(expected.id, AllPossibleTypes::class.java)
        operations.insert(expected)

        val actual = operations.selectOneById<AllPossibleTypes>(expected.id)
        actual shouldBeEqualTo expected
    }

    @Test
    @Order(4)
    fun `counter values round trip after a real counter update`() {
        val expected = CounterEntity(
            id = Uuids.timeBased().toString(),
            coount = 7L,
        )
        val tableName = operations.getTableName(CounterEntity::class.java).asCql(false)

        trackCleanup(expected.id, CounterEntity::class.java)
        session.execute(
            SimpleStatement.newInstance(
                "UPDATE $tableName SET coount = coount + ? WHERE id = ?",
                expected.coount,
                expected.id,
            )
        )

        val raw = session.execute(
            SimpleStatement.newInstance(
                "SELECT coount FROM $tableName WHERE id = ?",
                expected.id,
            )
        ).one()
        raw.shouldNotBeNull()
        raw.getLong("coount") shouldBeEqualTo expected.coount

        val actual = operations.selectOneById<CounterEntity>(expected.id)
        actual shouldBeEqualTo expected
    }

    private fun tupleValue(text: String, number: Long): TupleValue {
        val tupleType: TupleType = DataTypes.tupleOf(DataTypes.TEXT, DataTypes.BIGINT)
        return tupleType.newValue()
            .setString(0, text)
            .setLong(1, number)
    }

    private fun trackCleanup(id: String, entityClass: Class<*>) {
        cleanupActions += { operations.deleteById(id, entityClass) }
    }
}
