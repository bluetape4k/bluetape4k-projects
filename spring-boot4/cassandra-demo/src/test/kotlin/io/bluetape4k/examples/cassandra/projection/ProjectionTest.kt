package io.bluetape4k.examples.cassandra.projection

import io.bluetape4k.examples.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.projection.TargetAware

@SpringBootTest(classes = [ProjectionTestConfiguration::class])
class ProjectionTest(
    @param:Autowired private val repository: CustomerRepository,
): AbstractCassandraCoroutineTest("projection") {

    companion object: KLogging()

    private lateinit var dave: Customer
    private lateinit var carter: Customer

    @BeforeEach
    fun setup() {
        repository.deleteAll()

        dave = repository.save(Customer("d", "Dave", "Mattews"))
        carter = repository.save(Customer("c", "Carter", "Beauford"))
    }

    @Test
    fun `projects entity into interface`() {
        val result = repository.findAllProjectedBy()

        result.size shouldBeEqualTo 2
        result.first().firstname shouldBeEqualTo "Carter"
        result.map { it.firstname } shouldBeEqualTo listOf(carter.firstname, dave.firstname)
    }

    @Test
    fun `projects dynamically`() {
        val result = repository.findById("d", CustomerProjection::class.java)

        result.size shouldBeEqualTo 1
        result.first().firstname shouldBeEqualTo dave.firstname
    }

    @Test
    fun `projects individual dynamically`() {
        val result = repository.findProjectedById(dave.id, CustomerSummary::class.java)

        result.shouldNotBeNull()
        result.firstname shouldBeEqualTo dave.firstname + ' ' + dave.lastname

        // NOTE: Projection 대상을 이렇게 찾을 수 있다
        (result as TargetAware).target shouldBeInstanceOf Customer::class
    }

    @Test
    fun `project individual instance`() {
        val result = repository.findProjectedById(dave.id)

        result.shouldNotBeNull()
        result.firstname shouldBeEqualTo dave.firstname
        (result as TargetAware).target shouldBeInstanceOf Customer::class
    }
}
