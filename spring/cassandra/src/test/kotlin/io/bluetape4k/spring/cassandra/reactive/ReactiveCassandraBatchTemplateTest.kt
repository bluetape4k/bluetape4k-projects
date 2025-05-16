package io.bluetape4k.spring.cassandra.reactive

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.spring.cassandra.cql.coQueryForResultSet
import io.bluetape4k.spring.cassandra.cql.insertOptions
import io.bluetape4k.spring.cassandra.cql.writeOptions
import io.bluetape4k.spring.cassandra.domain.ReactiveDomainTestConfiguration
import io.bluetape4k.spring.cassandra.domain.model.FlatGroup
import io.bluetape4k.spring.cassandra.domain.model.Group
import io.bluetape4k.spring.cassandra.domain.model.GroupKey
import io.bluetape4k.spring.cassandra.insertFlow
import io.bluetape4k.spring.cassandra.updateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.ReactiveResultSet
import org.springframework.data.cassandra.core.InsertOptions
import org.springframework.data.cassandra.core.ReactiveCassandraOperations
import org.springframework.data.cassandra.core.cql.WriteOptions
import org.springframework.data.cassandra.core.selectOneById
import org.springframework.data.cassandra.core.truncate
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith

@SpringBootTest(classes = [ReactiveDomainTestConfiguration::class])
@EnableReactiveCassandraRepositories
class ReactiveCassandraBatchTemplateTest(
    @Autowired private val operations: ReactiveCassandraOperations,
): AbstractCassandraCoroutineTest("reactive-batch-template") {

    companion object: KLoggingChannel()

    private val group1 = Group(GroupKey("users", "0x01", faker.internet().username()))
    private val group2 = Group(GroupKey("users", "0x01", faker.internet().username()))

    private fun newGroup(): Group {
        return Group(
            GroupKey(
                faker.internet().domainName(),
                faker.internet().domainWord(),
                faker.internet().username()
            )
        ).apply {
            email = faker.internet().emailAddress()
            age = faker.random().nextInt(10, 80)
        }
    }

    private fun newFlatGroup(): FlatGroup {
        return FlatGroup(
            faker.internet().domainName(),
            faker.internet().domainWord(),
            faker.internet().username()
        ).apply {
            email = faker.internet().emailAddress()
            age = faker.random().nextInt(10, 80)
        }
    }

    @BeforeEach
    fun beforeEach() {
        runBlocking {
            operations.truncate<Group>().awaitSingleOrNull()
            operations.truncate<FlatGroup>().awaitSingleOrNull()

            operations.insert(group1).awaitSingle()
            operations.insert(group2).awaitSingle()
        }
    }

    @Test
    fun `insert entities in batch ops`() = runSuspendIO {
        val g1 = newGroup()
        val g2 = newGroup()

        operations.batchOps()
            .insert(g1)
            .insert(g2)
            .execute()
            .awaitSingle()

        val loaded = operations.selectOneById<Group>(g1.id).awaitSingle()
        loaded shouldBeEqualTo g1
    }


    @Test
    fun `insert vararg entities reject query options`() = runSuspendIO {
        // entity 만 가능합니다. 
        assertFailsWith<IllegalArgumentException> {
            operations.batchOps().insert(group1, group2, InsertOptions.empty())
        }
    }

    @Test
    fun `insert entities with LWT`() = runSuspendIO {
        val lwtOptions = insertOptions { withIfNotExists() }

        val prevGroup1 = group1.copy().apply { age = 54 }
        operations.insert(prevGroup1).awaitSingle()

        group1.age = 100

        val writeResult = operations.batchOps().insert(group1, lwtOptions).insert(group2).execute().awaitSingle()
        writeResult.wasApplied().shouldBeFalse()
        writeResult.executionInfo.isNotEmpty()
        writeResult.rows.shouldNotBeEmpty()

        val loadedDebop = operations.selectOneById<Group>(group1.id).awaitSingle()
        loadedDebop shouldBeEqualTo prevGroup1
        loadedDebop.age shouldNotBeEqualTo group1.age

        val loadedMike = operations.selectOneById<Group>(group2.id).awaitSingle()
        loadedMike shouldBeEqualTo group2
    }

    @Test
    fun `insert entity flow with ttl`() = runSuspendIO {
        group1.email = "debop@example.com"
        group2.email = "mike@example.com"

        val writeOptions = WriteOptions.builder().ttl(30).build()

        operations.batchOps().insertFlow(flowOf(group1, group2), writeOptions).execute().awaitSingle()

        val resultSet: ReactiveResultSet = operations.reactiveCqlOperations
            .coQueryForResultSet("SELECT TTL(email) FROM groups")

        resultSet.rows().asFlow()
            .onEach { row -> println("ttl= ${row.getInt(0)}") }
            .onEach { row ->
                row.getInt(0) shouldBeInRange 1..30
            }
            .collect()
    }

    @Test
    fun `update vararg entities should reject query options`() = runSuspendIO {
        assertFailsWith<IllegalArgumentException> {
            operations.batchOps().update(group1, group2, InsertOptions.empty())
        }
    }

    @Test
    fun `update entities`() = runSuspendIO {
        group1.email = "debop@example.com"
        group2.email = "mike@example.com"

        operations.batchOps().update(group1).update(group2).execute().awaitSingle()

        val loaded = operations.selectOneById<Group>(group1.id).awaitSingle()
        loaded.email shouldBeEqualTo group1.email
    }

    @Test
    fun `update entity flow`() = runSuspendIO {
        group1.email = "debop@example.com"
        group2.email = "mike@example.com"

        operations.batchOps().updateFlow(flowOf(group1, group2)).execute().awaitSingle()

        val loaded = operations.selectOneById<Group>(group1.id).awaitSingle()
        loaded.email shouldBeEqualTo group1.email
    }

    @Test
    fun `update entity with ttl`() = runSuspendIO {
        group1.email = "debop@example.com"
        group2.email = "mike@example.com"

        val writeOptions = writeOptions { ttl(30) }

        operations.batchOps().update(group1, writeOptions).execute().awaitSingle()

        val resultSet: ReactiveResultSet = operations.reactiveCqlOperations
            .queryForResultSet("SELECT TTL(email), email FROM groups")
            .awaitSingle()

        resultSet.rows().asFlow()
            .onEach { row -> println("ttl= ${row.getInt(0)}") }
            .onEach { row ->
                if (row.getString("email") == group1.email) {
                    row.getInt(0) shouldBeInRange 1..30
                } else {
                    row.getInt(0) shouldBeEqualTo 0
                }
            }
            .collect()
    }

    @Test
    fun `update flow of entities`() = runSuspendIO {
        val flatGroup1 = newFlatGroup()
        val flatGroup2 = newFlatGroup()

        operations.insert(flatGroup1).awaitSingle()
        operations.insert(flatGroup2).awaitSingle()

        flatGroup1.email = faker.internet().emailAddress()
        flatGroup2.email = faker.internet().emailAddress()

        operations.batchOps().updateFlow(flowOf(flatGroup1, flatGroup2)).execute().awaitSingle()

        val loaded = operations.selectOneById<FlatGroup>(flatGroup1).awaitSingle()
        loaded.email shouldBeEqualTo flatGroup1.email
    }

    @Test
    fun `delete as vararg reject query options`() = runSuspendIO {
        assertFailsWith<IllegalArgumentException> {
            operations.batchOps().delete(group1, group2, InsertOptions.empty())
        }
    }

    @Test
    fun `deelete entities`() = runSuspendIO {
        operations.batchOps().delete(group1).delete(group2).execute().awaitSingleOrNull()

        val loaded = operations.selectOneById<Group>(group1.id).awaitSingleOrNull()
        loaded.shouldBeNull()
    }

    @Test
    fun `apply timestamp to all entities`() = runSuspendIO {
        group1.email = faker.internet().emailAddress()
        group2.email = faker.internet().emailAddress()

        // timestamp 는 nano seconds 단위이다.
        val timestamp = (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)) * 1000

        operations.batchOps()
            .insert(group1)
            .insert(group2)
            .withTimestamp(timestamp)
            .execute()
            .awaitSingle()

        val resultSet: ReactiveResultSet = operations.reactiveCqlOperations
            .queryForResultSet("SELECT writetime(email) FROM groups")
            .awaitSingle()

        resultSet.rows().asFlow()
            .onEach { row -> println("timestamp= ${row.getLong(0)}") }
            .onEach { row ->
                row.getLong(0) shouldBeEqualTo timestamp
            }
            .collect()
    }

    @Test
    fun `batchOps 는 중복 실행이 안됩니다`() = runSuspendIO {
        val batchOps = operations.batchOps()
        batchOps.insert(group1).execute().awaitSingle()

        assertFailsWith<IllegalStateException> {
            batchOps.execute().awaitSingle()
        }
    }

    @Test
    fun `batchOps 는 실행 후 변경은 허용되지 않습니다`() = runSuspendIO {
        val batchOps = operations.batchOps()
        batchOps.insert(group1).execute().awaitSingle()

        assertFailsWith<IllegalStateException> {
            batchOps.update(Group())
        }
    }
}
