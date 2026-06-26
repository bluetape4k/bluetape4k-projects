package io.bluetape4k.cassandra.mapper

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.BoundStatement
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder
import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.mapper.entity.EntityHelper
import com.datastax.oss.driver.api.mapper.entity.saving.NullSavingStrategy

/**
 * Builds an insert [PreparedStatement] from this DataStax [EntityHelper].
 *
 * ```
 * val session: CqlSession = ...
 * val entityHelper: EntityHelper<User> = ...
 * val preparedStatement: PreparedStatement = entityHelper.prepareInsert(session)
 * ```
 *
 * @param session Cassandra [CqlSession] used to prepare the generated CQL.
 * @return Prepared insert statement for the entity table.
 */
fun <T: Any> EntityHelper<T>.prepareInsert(session: CqlSession): PreparedStatement {
    return session.prepare(insert().asCql())
}

/**
 * Builds an insert-if-not-exists [PreparedStatement] from this DataStax [EntityHelper].
 *
 * ```
 * val session: CqlSession = ...
 * val entityHelper: EntityHelper<User> = ...
 * val preparedStatement: PreparedStatement = entityHelper.prepareInsertIfNotExists(session)
 * ```
 *
 * @param session Cassandra [CqlSession] used to prepare the generated CQL.
 * @return Prepared conditional insert statement for the entity table.
 */
fun <T: Any> EntityHelper<T>.prepareInsertIfNotExists(session: CqlSession): PreparedStatement {
    return session.prepare(insert().ifNotExists().asCql())
}

/**
 * Creates a [BoundStatement] by applying [builder] to [preparedStatement].
 *
 * ```
 * val preparedStatement: PreparedStatement = ...
 * val boundStatement: BoundStatement = bindEntity(preparedStatement) {
 *    setString("name", "debop")
 *    setInt("age", 30)
 *    setString("email", "debop@example.com")
 * }
 * ```
 *
 * @param preparedStatement Prepared statement to bind.
 * @param builder Bound statement builder customizer.
 * @return Bound statement produced by the customized builder.
 */
inline fun <T: Any> bindEntity(
    preparedStatement: PreparedStatement,
    builder: BoundStatementBuilder.() -> Unit,
): BoundStatement {
    return preparedStatement.boundStatementBuilder()
        .apply(builder)
        .build()
}

/**
 * Binds [entity] to [preparedStatement] through this DataStax [EntityHelper].
 *
 * ```
 * val session: CqlSession = ...
 * val entityHelper: EntityHelper<User> = ...
 * val preparedStatement: PreparedStatement = entityHelper.prepareInsert(session)
 * val user: User = ...
 * val boundStatement: BoundStatement = entityHelper.bind(preparedStatement, user)
 * ```
 *
 * @param preparedStatement Prepared statement to bind.
 * @param entity Entity instance used as the binding source.
 * @param nullSavingStrategy Strategy used when entity properties are null.
 * @param lenient Whether missing columns should be tolerated by the mapper.
 * @return Bound statement containing values extracted from [entity].
 */
fun <T: Any> EntityHelper<T>.bind(
    preparedStatement: PreparedStatement,
    entity: T,
    nullSavingStrategy: NullSavingStrategy = NullSavingStrategy.DO_NOT_SET,
    lenient: Boolean = true,
): BoundStatement {
    return preparedStatement.boundStatementBuilder()
        .also { builder ->
            set(entity, builder, nullSavingStrategy, lenient)
        }
        .build()
}

/**
 * Prepares CQL generated from [entityHelper].
 *
 * ```
 * val session: CqlSession = ...
 * val entityHelper: EntityHelper<User> = ...
 * val preparedStatement: PreparedStatement = session.prepare(entityHelper) {
 *   insert().asCql()
 *   // or
 *   select().asCql()
 *   // or
 *   delete().asCql()
 * }
 * ```
 *
 * @param entityHelper DataStax mapper helper used to build CQL.
 * @param builder Lambda that creates CQL from [entityHelper].
 * @return Prepared statement for the generated CQL.
 */
inline fun <T: Any> CqlSession.prepare(
    entityHelper: EntityHelper<T>,
    builder: EntityHelper<T>.() -> String,
): PreparedStatement {
    return this@prepare.prepare(builder(entityHelper))
}
