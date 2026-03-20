package io.bluetape4k.examples.cassandra.multitenancy.row

import io.bluetape4k.examples.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [RowMultitenantTestConfiguration::class])
class RowMultitenantTest(
    @param:Autowired private val repository: EmployeeRepository,
): AbstractCassandraCoroutineTest("mt-table") {

    companion object: KLoggingChannel() {
        private const val REPEAT_TIMES = 5
    }

    private val employees = listOf(
        Employee("apple", "Debop"),
        Employee("apple", "Steve"),
        Employee("amazon", "Jeff"),
    )

    @BeforeEach
    fun beforeEach() = runSuspendTest {
        repository.deleteAll()

        val saved = repository.saveAll(employees.asFlow()).toList()
        saved.size shouldBeEqualTo employees.size
    }

    @Test
    fun `find all by tenantId and name`() = runSuspendIO {

        // tenant 정보를 제공하여 tenantId 를 검색 조건에 들도록 합니다.
        TenantIdProvider.tenantId.set("apple")

        val job1 = launch(Dispatchers.IO + TenantIdProvider.tenantId.asContextElement()) {
            repeat(REPEAT_TIMES) {
                val loaded = repository.findAllByName("Steve").toList()

                loaded.size shouldBeEqualTo 1
                loaded.first() shouldBeEqualTo Employee("apple", "Steve")
            }
        }

        // tenant 정보를 제공하여 tenantId 를 검색 조건에 들도록 합니다.
        TenantIdProvider.tenantId.set("amazon")
        val job2 = launch(Dispatchers.IO + TenantIdProvider.tenantId.asContextElement()) {
            repeat(REPEAT_TIMES) {
                val loaded = repository.findAllByName("Steve").toList()
                loaded.shouldBeEmpty()
            }
        }

        job1.join()
        job2.join()
    }
}
