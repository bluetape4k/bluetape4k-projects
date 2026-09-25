package io.bluetape4k.examples.cassandra.reactive.auditing

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInRange
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.examples.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.examples.cassandra.reactive.auditing.AuditingTestConfiguration.Companion.CURRENT_USER
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import kotlinx.coroutines.delay
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

@SpringBootTest(classes = [AuditingTestConfiguration::class])
class AuditingTest(
    @param:Autowired private val repository: OrderRepository,
    @param:Autowired private val customRepo: CustomAuditingRepository,
): AbstractCassandraCoroutineTest("auditing") {

    companion object: KLoggingChannel() {

    }

    @BeforeEach
    fun setup() = runSuspendTest {
        repository.deleteAll()
    }

    @Test
    fun `should update auditor`() = runSuspendIO {
        val order = Order("4711")
        order.createdAt.shouldBeNull()
        order.isNew.shouldBeTrue()

        val instantRange = Instant.now().minusSeconds(60)..Instant.now().plusSeconds(60)

        val actual = repository.save(order)
        log.debug { "Actual createdAt=${actual.createdAt}, lastModifiedAt=${actual.lastModifiedAt}" }

        actual.createdBy shouldBeEqualTo CURRENT_USER
        actual.createdAt.shouldNotBeNull().shouldBeInRange(instantRange)

        actual.lastModifiedBy shouldBeEqualTo CURRENT_USER
        actual.lastModifiedAt.shouldNotBeNull().shouldBeInRange(instantRange)

        delay(100.milliseconds)

        val loaded = repository.findById("4711").shouldNotBeNull()
        log.debug { "loaded createdAt=${loaded.createdAt}, lastModifiedAt=${loaded.lastModifiedAt}" }
        loaded.isNew.shouldBeFalse()

        val ssaved = repository.save(loaded)
        log.debug { "Actual createdAt=${actual.createdAt}, lastModifiedAt=${actual.lastModifiedAt}" }

        ssaved.createdBy shouldBeEqualTo CURRENT_USER
        ssaved.createdAt.shouldNotBeNull() shouldBeEqualTo loaded.createdAt

        ssaved.lastModifiedBy shouldBeEqualTo CURRENT_USER
        ssaved.lastModifiedAt.shouldNotBeNull().shouldBeInRange(instantRange)
    }

    @Test
    fun `should update auditor for custom auditable order`() = runSuspendIO {
        val order = CustomAuditableOrder("4242")
        order.createdAt.shouldBeNull()
        order.isNew.shouldBeTrue()

        val instantRange = Instant.now().minusSeconds(60)..Instant.now().plusSeconds(60)
        customRepo.save(order).let { actual ->
            log.debug { "Actual createdAt=${actual.createdAt}, lastModifiedAt=${actual.modifiedAt}" }

            actual.createdBy shouldBeEqualTo CURRENT_USER
            actual.createdAt.shouldNotBeNull().shouldBeInRange(instantRange)

            actual.modifiedBy shouldBeEqualTo CURRENT_USER
            actual.modifiedAt.shouldNotBeNull().shouldBeInRange(instantRange)
        }

        delay(100.milliseconds)

        val loaded = customRepo.findById("4242")!!
        log.info { "loaded createdAt=${loaded.createdAt}, lastModifiedAt=${loaded.modifiedAt}" }
        loaded.isNew.shouldBeFalse()

        customRepo.save(loaded).let { actual ->
            log.debug { "Actual createdAt=${actual.createdAt}, lastModifiedAt=${actual.modifiedAt}" }

            actual.createdBy shouldBeEqualTo CURRENT_USER
            actual.createdAt.shouldNotBeNull() shouldBeEqualTo loaded.createdAt

            actual.modifiedBy shouldBeEqualTo CURRENT_USER
            actual.modifiedAt.shouldNotBeNull().shouldBeInRange(instantRange)
        }
    }
}
