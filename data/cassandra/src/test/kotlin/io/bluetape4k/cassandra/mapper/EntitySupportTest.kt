package io.bluetape4k.cassandra.mapper

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.BoundStatement
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder
import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.mapper.entity.EntityHelper
import com.datastax.oss.driver.api.mapper.entity.saving.NullSavingStrategy
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EntitySupportTest {

    data class TestEntity(val id: Int)

    private val entityHelper = mockk<EntityHelper<TestEntity>>(relaxed = true)
    private val session = mockk<CqlSession>(relaxed = true)
    private val prepared = mockk<PreparedStatement>(relaxed = true)
    private val boundBuilder = mockk<BoundStatementBuilder>(relaxed = true)
    private val bound = mockk<BoundStatement>(relaxed = true)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `prepareInsert 는 insert CQL로 세션 prepare 를 호출한다`() {
        val cql = "INSERT INTO test (id) VALUES (?)"

        every { entityHelper.insert().asCql() } returns cql
        every { session.prepare(cql) } returns prepared

        entityHelper.prepareInsert(session) shouldBeEqualTo prepared
        verify(exactly = 1) { session.prepare(cql) }
    }

    @Test
    fun `prepareInsertIfNotExists 는 IF NOT EXISTS CQL로 세션 prepare 를 호출한다`() {
        val cql = "INSERT INTO test (id) VALUES (?) IF NOT EXISTS"

        every { entityHelper.insert().ifNotExists().asCql() } returns cql
        every { session.prepare(cql) } returns prepared

        entityHelper.prepareInsertIfNotExists(session) shouldBeEqualTo prepared
        verify(exactly = 1) { session.prepare(cql) }
    }

    @Test
    fun `bindEntity 는 builder를 적용해 BoundStatement 를 생성한다`() {
        var applied = false

        every { prepared.boundStatementBuilder() } returns boundBuilder
        every { boundBuilder.build() } returns bound

        val result = bindEntity<TestEntity>(prepared) {
            applied = true
        }

        result shouldBeEqualTo bound
        applied.shouldBeTrue()
        verify(exactly = 1) { prepared.boundStatementBuilder() }
        verify(exactly = 1) { boundBuilder.build() }
    }

    @Test
    fun `EntityHelper bind 는 set 을 호출하고 BoundStatement 를 생성한다`() {
        val entity = TestEntity(1)

        every { prepared.boundStatementBuilder() } returns boundBuilder
        every { boundBuilder.build() } returns bound

        val result = entityHelper.bind(
            preparedStatement = prepared,
            entity = entity,
            nullSavingStrategy = NullSavingStrategy.SET_TO_NULL,
            lenient = false
        )

        result shouldBeEqualTo bound
        verify(exactly = 1) { prepared.boundStatementBuilder() }
        verify(exactly = 1) { entityHelper.set(entity, boundBuilder, NullSavingStrategy.SET_TO_NULL, false) }
        verify(exactly = 1) { boundBuilder.build() }
    }

    @Test
    fun `CqlSession prepare(entityHelper, builder) 는 builder 결과 CQL로 prepare 한다`() {
        val cql = "SELECT id FROM test"

        every { session.prepare(cql) } returns prepared

        val result = session.prepare(entityHelper) { cql }

        result shouldBeEqualTo prepared
        verify(exactly = 1) { session.prepare(cql) }
    }
}
