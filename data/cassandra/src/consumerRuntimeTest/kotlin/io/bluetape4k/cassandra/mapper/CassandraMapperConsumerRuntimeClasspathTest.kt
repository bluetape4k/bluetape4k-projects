package io.bluetape4k.cassandra.mapper

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.mapper.entity.EntityHelper
import com.datastax.oss.driver.api.mapper.entity.saving.NullSavingStrategy
import org.junit.jupiter.api.Test

class CassandraMapperConsumerRuntimeClasspathTest {

    class ConsumerEntity(val id: Int)

    @Test
    fun `mapper helper APIs compile from consumer runtime classpath`() {
        // The source-set classpath intentionally excludes module compileOnly dependencies.
    }

    @Suppress("unused")
    private fun assertMapperApiCompiles(
        session: CqlSession,
        entityHelper: EntityHelper<ConsumerEntity>,
        preparedStatement: PreparedStatement,
        entity: ConsumerEntity,
    ) {
        entityHelper.prepareInsert(session)
        entityHelper.prepareInsertIfNotExists(session)
        entityHelper.bind(
            preparedStatement = preparedStatement,
            entity = entity,
            nullSavingStrategy = NullSavingStrategy.DO_NOT_SET,
        )
        session.prepare(entityHelper) {
            insert().asCql()
        }
    }
}
