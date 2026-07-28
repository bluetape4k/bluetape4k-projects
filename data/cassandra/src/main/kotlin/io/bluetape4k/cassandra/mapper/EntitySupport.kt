package io.bluetape4k.cassandra.mapper

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.BoundStatement
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder
import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.mapper.entity.EntityHelper
import com.datastax.oss.driver.api.mapper.entity.saving.NullSavingStrategy

/**
 * 이 DataStax [EntityHelper]에서 insert [PreparedStatement]를 만듭니다.
 *
 * ```
 * val session: CqlSession = ...
 * val entityHelper: EntityHelper<User> = ...
 * val preparedStatement: PreparedStatement = entityHelper.prepareInsert(session)
 * ```
 *
 * @param session 생성된 CQL을 prepare할 Cassandra [CqlSession]입니다.
 * @return entity table에 대한 prepared insert statement입니다.
 */
fun <T: Any> EntityHelper<T>.prepareInsert(session: CqlSession): PreparedStatement {
    return session.prepare(insert().asCql())
}

/**
 * 이 DataStax [EntityHelper]에서 insert-if-not-exists [PreparedStatement]를 만듭니다.
 *
 * ```
 * val session: CqlSession = ...
 * val entityHelper: EntityHelper<User> = ...
 * val preparedStatement: PreparedStatement = entityHelper.prepareInsertIfNotExists(session)
 * ```
 *
 * @param session 생성된 CQL을 prepare할 Cassandra [CqlSession]입니다.
 * @return entity table에 대한 prepared conditional insert statement입니다.
 */
fun <T: Any> EntityHelper<T>.prepareInsertIfNotExists(session: CqlSession): PreparedStatement {
    return session.prepare(insert().ifNotExists().asCql())
}

/**
 * [preparedStatement]에 [builder]를 적용해 [BoundStatement]를 생성합니다.
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
 * @param preparedStatement bind 대상 prepared statement입니다.
 * @param builder bound statement builder를 보정하는 customizer입니다.
 * @return customizer가 반영된 bound statement입니다.
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
 * 이 DataStax [EntityHelper]를 통해 [entity]를 [preparedStatement]에 bind합니다.
 *
 * ```
 * val session: CqlSession = ...
 * val entityHelper: EntityHelper<User> = ...
 * val preparedStatement: PreparedStatement = entityHelper.prepareInsert(session)
 * val user: User = ...
 * val boundStatement: BoundStatement = entityHelper.bind(preparedStatement, user)
 * ```
 *
 * @param preparedStatement bind 대상 prepared statement입니다.
 * @param entity binding source로 사용할 entity instance입니다.
 * @param nullSavingStrategy entity property가 null일 때 사용할 저장 전략입니다.
 * @param lenient mapper가 누락된 column을 허용할지 여부입니다.
 * @return [entity]에서 추출한 값을 담은 bound statement입니다.
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
 * [entityHelper]에서 생성한 CQL을 prepare합니다.
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
 * @param entityHelper CQL 생성에 사용할 DataStax mapper helper입니다.
 * @param builder [entityHelper]에서 CQL을 생성하는 lambda입니다.
 * @return 생성된 CQL에 대한 prepared statement입니다.
 */
inline fun <T: Any> CqlSession.prepare(
    entityHelper: EntityHelper<T>,
    builder: EntityHelper<T>.() -> String,
): PreparedStatement {
    return this@prepare.prepare(builder(entityHelper))
}
