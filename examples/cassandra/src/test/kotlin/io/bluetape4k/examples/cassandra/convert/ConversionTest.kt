package io.bluetape4k.examples.cassandra.convert

import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom
import io.bluetape4k.cassandra.data.getList
import io.bluetape4k.examples.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.cassandra.suspendInsert
import io.bluetape4k.spring.cassandra.suspendSelectOne
import io.bluetape4k.spring.cassandra.suspendTruncate
import kotlinx.coroutines.reactor.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.core.ReactiveCassandraOperations
import java.util.*

@SpringBootTest(classes = [ConversionTestConfiguration::class])
class ConversionTest(
    @Autowired private val operations: ReactiveCassandraOperations,
): AbstractCassandraCoroutineTest("conversion") {

    companion object: KLoggingChannel()

    private fun newContact(): Contact =
        Contact(faker.name().firstName(), faker.name().lastName())

    private fun newAddressbook(): Addressbook =
        Addressbook(
            id = "private",
            me = Contact(faker.name().firstName(), faker.name().lastName()),
            friends = mutableListOf(newContact(), newContact())
        )

    @BeforeEach
    fun setup() = runSuspendTest {
        operations.suspendTruncate<Addressbook>()
    }

    @Test
    fun `context loading`() {
        operations.shouldNotBeNull()
    }

    @Test
    fun `write Addressbook`() = runSuspendIO {
        val addressbook = Addressbook(
            id = "private",
            me = Contact("Debop", "Bae"),
            friends = mutableListOf(newContact(), newContact())
        )
        operations.suspendInsert(addressbook)

        val row = operations.suspendSelectOne<Row>(selectFrom("addressbook").all().build())

        row.getString("id") shouldBeEqualTo "private"
        row.getString("me")!! shouldContain """"firstname":"Debop""""
        row.getList<String>("friends")!!.size shouldBeEqualTo 2
    }

    @Test
    fun `read Addressbook`() = runSuspendIO {
        val addressbook = Addressbook(
            id = "private",
            me = Contact("Debop", "Bae"),
            friends = mutableListOf(newContact(), newContact())
        )
        operations.insert(addressbook).awaitSingle()

        val loaded = operations.suspendSelectOne<Addressbook>(selectFrom("addressbook").all().build())

        loaded.me shouldBeEqualTo addressbook.me
        loaded.friends shouldBeEqualTo addressbook.friends
    }

    @Test
    fun `write converted maps and user defined type`() = runSuspendIO {
        val addressbook = Addressbook(
            id = "private",
            me = Contact("Debop", "Bae"),
            friends = mutableListOf(newContact(), newContact()),
            address = Address("165 Misa", "Hanam", "12914"),
            preferredCurrencies = mutableMapOf(
                1 to Currency.getInstance("USD"),
                2 to Currency.getInstance("KRW")
            )
        )

        operations.insert(addressbook).awaitSingle()

        val loaded = operations.suspendSelectOne<Addressbook>(selectFrom("addressbook").all().build())

        loaded.me shouldBeEqualTo addressbook.me
        loaded.friends shouldBeEqualTo addressbook.friends
        loaded.address shouldBeEqualTo addressbook.address
        loaded.preferredCurrencies shouldBeEqualTo addressbook.preferredCurrencies
    }
}
