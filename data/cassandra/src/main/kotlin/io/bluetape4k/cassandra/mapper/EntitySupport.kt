package io.bluetape4k.cassandra.mapper

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.BoundStatement
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder
import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.mapper.entity.EntityHelper
import com.datastax.oss.driver.api.mapper.entity.saving.NullSavingStrategy

/**
 * [EntityHelper] 를 이용하여 Insert 용 [PreparedStatement] 를 빌드합니다.
 *
 * ```
 * val session: CqlSession = ...
 * val entityHelper: EntityHelper<User> = ...
 * val preparedStatement: PreparedStatement = entityHelper.prepareInsert(session)
 * ```
 *
 * @param session [CqlSession] 세션
 * @return [PreparedStatement] 인스턴스
 */
fun <T: Any> EntityHelper<T>.prepareInsert(session: CqlSession): PreparedStatement {
    return session.prepare(insert().asCql())
}

/**
 * [EntityHelper] 를 이용하여 신규 Insert 용 [PreparedStatement] 를 빌드합니다.
 *
 * ```
 * val session: CqlSession = ...
 * val entityHelper: EntityHelper<User> = ...
 * val preparedStatement: PreparedStatement = entityHelper.prepareInsertIfNotExists(session)
 * ```
 *
 * @param session [CqlSession] 세션
 * @return [PreparedStatement] 인스턴스
 */
fun <T: Any> EntityHelper<T>.prepareInsertIfNotExists(session: CqlSession): PreparedStatement {
    return session.prepare(insert().ifNotExists().asCql())
}

/**
 * [preparedStatement]에 변수를 바인딩합니다.
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
 * @param preparedStatement [PreparedStatement] 인스턴스
 * @param initializer [BoundStatementBuilder] 초기화 람다
 */
inline fun <T: Any> bindEntity(
    preparedStatement: PreparedStatement,
    @BuilderInference initializer: BoundStatementBuilder.() -> Unit,
): BoundStatement {
    return preparedStatement.boundStatementBuilder()
        .apply(initializer)
        .build()
}

/**
 * [EntityHelper] 를 이용하여 [PreparedStatement] 를 빌드하고, [BoundStatement] 를 생성합니다.
 *
 * ```
 * val session: CqlSession = ...
 * val entityHelper: EntityHelper<User> = ...
 * val preparedStatement: PreparedStatement = entityHelper.prepareInsert(session)
 * val user: User = ...
 * val boundStatement: BoundStatement = entityHelper.bind(preparedStatement, user)
 * ```
 *
 * @param preparedStatement [PreparedStatement] 인스턴스
 * @param entity [T] 엔티티 인스턴스
 * @param nullSavingStrategy [NullSavingStrategy] Null 저장 전략 (기본값: [NullSavingStrategy.DO_NOT_SET])
 * @param lenient [Boolean] 느슨한 바인딩 여부 (기본값: `true`)
 * @return [BoundStatement] 인스턴스
 */
fun <T: Any> EntityHelper<T>.bind(
    preparedStatement: PreparedStatement,
    entity: T,
    nullSavingStrategy: NullSavingStrategy = NullSavingStrategy.DO_NOT_SET,
    lenient: Boolean = true,
): BoundStatement {
    return preparedStatement.boundStatementBuilder()
        .apply {
            set(entity, this, nullSavingStrategy, lenient)
        }
        .build()
}

/**
 * [entityHelper]를 이용하여 [PreparedStatement]를 빌드합니다.
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
 * @param entityHelper [EntityHelper] 인스턴스
 * @param block [EntityHelper] 를 이용하여 CQL을 생성하는 람다
 * @return [PreparedStatement] 인스턴스
 */
inline fun <T: Any> CqlSession.prepare(
    entityHelper: EntityHelper<T>,
    @BuilderInference block: EntityHelper<T>.() -> String,
): PreparedStatement {
    return prepare(block(entityHelper))
}
