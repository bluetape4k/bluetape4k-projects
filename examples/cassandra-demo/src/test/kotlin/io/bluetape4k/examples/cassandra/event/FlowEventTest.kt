package io.bluetape4k.examples.cassandra.event

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.examples.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.core.CassandraOperations
import org.springframework.data.cassandra.core.ReactiveCassandraOperations
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.cassandra.core.select
import org.springframework.data.cassandra.core.stream
import org.springframework.data.cassandra.core.truncate

@SpringBootTest(classes = [EventTestConfiguration::class])
class FlowEventTest(
    @param:Autowired private val operations: CassandraOperations,
    @param:Autowired private val reactiveOperations: ReactiveCassandraOperations,
): AbstractCassandraCoroutineTest("event") {

    companion object: KLoggingChannel()

    @BeforeEach
    fun beforeEach() {
        operations.truncate<User>()
    }

    @Test
    fun `Stream 방식으로 데이터 로딩하기`() {
        insertEntities()

        val userStream = operations.stream<User>(Query.empty())
        val users = userStream.toList()
        users.forEach {
            log.debug { "user=$it" }
        }
        users shouldHaveSize 3
    }

    @Test
    fun `List 로 데이터 로딩하기`() {
        insertEntities()

        val users = operations.select<User>(Query.empty())
        users shouldHaveSize 3
        users.forEach { log.debug { "user=$it" } }
    }

    @Test
    fun `Flow 로 데이터 로딩하기`() = runSuspendIO {
        withContext(Dispatchers.IO) {
            insertEntities()
        }

        val userFlow = reactiveOperations.select<User>(Query.empty()).asFlow()
        userFlow.count() shouldBeEqualTo 3
        userFlow.collect { log.debug { "userFlow=$it" } }
    }

    private fun insertEntities() {
        val walter = User(1, "Walter", "White")
        val skyler = User(2, "Skyler", "White")
        val jesse = User(3, "Jesse Pinkman", "Jesse Pinkman")

        operations.insert(walter)
        operations.insert(skyler)
        operations.insert(jesse)
    }
}
