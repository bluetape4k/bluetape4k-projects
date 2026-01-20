package io.bluetape4k.examples.cassandra.optimisticlocking

import io.bluetape4k.examples.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.cassandra.cql.updateOptions
import io.bluetape4k.spring.cassandra.query.eq
import io.bluetape4k.spring.cassandra.suspendInsert
import io.bluetape4k.spring.cassandra.suspendTruncate
import kotlinx.coroutines.reactor.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.cassandra.core.CassandraOperations
import org.springframework.data.cassandra.core.ReactiveCassandraOperations
import org.springframework.data.cassandra.core.query.Criteria
import kotlin.test.assertFailsWith

@SpringBootTest(classes = [OptimisticLockTestConfiguration::class])
class OptimisticLockTest @Autowired constructor(
    private val operations: CassandraOperations,
    private val reactiveOps: ReactiveCassandraOperations,
    private val repository: OptimisticPersonRepository,
): AbstractCassandraCoroutineTest("optimistic-locking") {

    companion object: KLoggingChannel()

    @BeforeEach
    fun setup() = runSuspendTest {
        reactiveOps.suspendTruncate<SimplePerson>()
        repository.deleteAll()
    }

    @Test
    fun `insert should increment version`() = runSuspendIO {
        val person = OptimisticPerson(42L, "bart")
        person.version shouldBeEqualTo 0L

        // 저장 후 다시 Load 한다
        val saved = repository.save(person)

        person.version shouldBeEqualTo 0L
        saved.version shouldBeGreaterThan 0L
        saved shouldNotBeEqualTo person
    }

    @Test
    fun `update should detect change entity`() = runSuspendIO {
        val person = OptimisticPerson(42L, "bart")

        // 저장 후 다시 Load 한다
        val saved = repository.save(person)

        // 새롭게 로드한 후 복사해서 저장한다 (Id 가 같으므로 Version이 증가한다)
        val anotherPerson = repository.findById(person.id)!!.withName("homer")
        repository.save(anotherPerson)

        // 이미 새로운 값이 저장되어 있으므로, `ourSaved` 는 stale 된 정보이다
        val ourSaved = saved.withName("lisa")
        assertFailsWith<OptimisticLockingFailureException> {
            repository.save(ourSaved)
        }
    }

    @Test
    fun `update using lightweight transactions`() {
        val person = SimplePerson(42L, "bart")

        operations.insert(person)

        val success = operations.update(
            person,
            updateOptions {
                ifCondition(Criteria.where("name").eq("bart")).build()
            }
        )

        success.wasApplied().shouldBeTrue()

        // person 으로 Update 하려는데, id=42L 이고 name="homer" 인 row 는 없다
        val failed = operations.update(
            person,
            updateOptions {
                ifCondition(Criteria.where("name").eq("homer")).build()
            }
        )
        // Update가 되지 않았음
        failed.wasApplied().shouldBeFalse()
    }

    @Test
    fun `update using lightweight transactions in coroutines`() = runSuspendIO {
        val person = SimplePerson(42L, "bart")

        reactiveOps.suspendInsert(person)

        val success = reactiveOps.update(
            person,
            updateOptions {
                ifCondition(Criteria.where("name").eq("bart")).build()
            }
        ).awaitSingle()

        success.wasApplied().shouldBeTrue()

        // person 으로 Update 하려는데, id=42L 이고 name="homer" 인 row 는 없다
        val failed = reactiveOps.update(
            person,
            updateOptions {
                ifCondition(Criteria.where("name").eq("homer")).build()
            }
        ).awaitSingle()

        // Update가 되지 않았음
        failed.wasApplied().shouldBeFalse()
    }
}
