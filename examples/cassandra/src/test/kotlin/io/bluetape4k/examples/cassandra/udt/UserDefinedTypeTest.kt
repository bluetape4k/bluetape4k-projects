package io.bluetape4k.examples.cassandra.udt

import io.bluetape4k.examples.cassandra.AbstractCassandraTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.core.CassandraAdminOperations
import org.springframework.data.cassandra.core.CassandraOperations
import org.springframework.data.cassandra.core.selectOne

@SpringBootTest(classes = [UserDefinedTypeTestConfiguration::class])
class UserDefinedTypeTest: AbstractCassandraTest() {

    companion object: KLogging() {
        private const val PERSON_TABLE_NAME = "udt_type_person"
        private const val UDT_ADDRESS_NAME = "udt_address"
    }

    @Autowired
    private lateinit var operations: CassandraOperations

    @Autowired
    private lateinit var adminOperations: CassandraAdminOperations

    @BeforeEach
    fun beforeEach() {
        operations.cqlOperations.execute("TRUNCATE $PERSON_TABLE_NAME")
    }

    @Test
    fun `insert mapped udt`() {
        val person = Person(42, "Homer", "Simpson").apply {
            current = Address("미사강변대로 165", "12914", "하남시")
            previous = listOf(Address("둔촌동", "12345", "서울특별시"))
        }

        operations.insert(person)

        val loaded = operations.selectOne<Person>("SELECT * FROM $PERSON_TABLE_NAME WHERE id=${person.id}")

        loaded.shouldNotBeNull()
        loaded.current shouldBeEqualTo person.current
        loaded.previous shouldBeEqualTo person.previous
    }

    @Test
    fun `insert raw udt`() {
        val keyspaceMetadata = adminOperations.keyspaceMetadata
        val addressUdt = keyspaceMetadata.getUserDefinedType(UDT_ADDRESS_NAME).get()

        val udtValue = addressUdt.newValue()
            .setString("street", "미사강변대로 165")
            .setString("zip", "12914")
            .setString("city", "하남시")

        val person = Person(42, "Homer", "Simpson").apply {
            alternative = udtValue
        }
        operations.insert(person)

        val loaded = operations.selectOne<Person>("SELECT * FROM $PERSON_TABLE_NAME WHERE id=${person.id}")

        loaded.shouldNotBeNull()
        loaded.alternative?.getString("zip") shouldBeEqualTo person.alternative?.getString("zip")
    }
}
