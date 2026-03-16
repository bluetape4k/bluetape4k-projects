package io.bluetape4k.vertx.sqlclient.mybatis

import io.bluetape4k.vertx.sqlclient.SqlLogger
import io.bluetape4k.vertx.sqlclient.templates.LONG_ROW_MAPPER
import io.bluetape4k.vertx.sqlclient.templates.toParameters
import io.bluetape4k.vertx.sqlclient.templates.tupleMapperOfRecord
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.SqlResult
import io.vertx.sqlclient.templates.RowMapper
import io.vertx.sqlclient.templates.SqlTemplate
import org.mybatis.dynamic.sql.BasicColumn
import org.mybatis.dynamic.sql.SqlBuilder
import org.mybatis.dynamic.sql.SqlTable
import org.mybatis.dynamic.sql.delete.render.DeleteStatementProvider
import org.mybatis.dynamic.sql.insert.render.BatchInsert
import org.mybatis.dynamic.sql.insert.render.GeneralInsertStatementProvider
import org.mybatis.dynamic.sql.insert.render.InsertSelectStatementProvider
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider
import org.mybatis.dynamic.sql.insert.render.MultiRowInsertStatementProvider
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider
import org.mybatis.dynamic.sql.update.render.UpdateStatementProvider
import org.mybatis.dynamic.sql.util.kotlin.CountCompleter
import org.mybatis.dynamic.sql.util.kotlin.DeleteCompleter
import org.mybatis.dynamic.sql.util.kotlin.GeneralInsertCompleter
import org.mybatis.dynamic.sql.util.kotlin.InsertSelectCompleter
import org.mybatis.dynamic.sql.util.kotlin.KotlinBatchInsertCompleter
import org.mybatis.dynamic.sql.util.kotlin.KotlinCountBuilder
import org.mybatis.dynamic.sql.util.kotlin.KotlinDeleteBuilder
import org.mybatis.dynamic.sql.util.kotlin.KotlinInsertBuilder
import org.mybatis.dynamic.sql.util.kotlin.KotlinInsertCompleter
import org.mybatis.dynamic.sql.util.kotlin.KotlinMultiRowInsertCompleter
import org.mybatis.dynamic.sql.util.kotlin.SelectCompleter
import org.mybatis.dynamic.sql.util.kotlin.UpdateCompleter

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Count를 구하는 확장 함수입니다.
 *
 * @param countProvider Count를 구하는 [SelectStatementProvider]
 */
suspend fun SqlClient.count(
    countProvider: SelectStatementProvider,
): Long {
    SqlLogger.logSQL(countProvider.selectStatement, countProvider.parameters)

    return SqlTemplate.forQuery(this, countProvider.selectStatement)
        .mapTo(LONG_ROW_MAPPER)
        .execute(countProvider.parameters)
        .coAwait()
        .firstOrNull() ?: -1L
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Count를 구하는 확장 함수입니다.
 *
 * @param column Count를 구할 [BasicColumn]
 * @param completer Count를 구하는 조건을 설정하는 람다 함수
 */
suspend fun SqlClient.count(
    column: BasicColumn,
    completer: CountCompleter,
): Long {
    val model = KotlinCountBuilder(SqlBuilder.countColumn(column)).apply(completer).build()
    val provider = model.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)
    return count(provider)
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Distinct Count를 구하는 확장 함수입니다.
 *
 * @param column Count를 구할 [BasicColumn]
 * @param completer Count를 구하는 조건을 설정하는 람다 함수
 */
suspend fun SqlClient.countDistinct(
    column: BasicColumn,
    completer: CountCompleter,
): Long {
    val model = KotlinCountBuilder(SqlBuilder.countDistinctColumn(column)).apply(completer).build()
    val provider = model.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)
    return count(provider)
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Table Count를 구하는 확장 함수입니다.
 */
suspend fun SqlClient.countFrom(table: SqlTable, completer: CountCompleter): Long {
    val model =
        KotlinCountBuilder(SqlBuilder.countColumn(SqlBuilder.constant<Long>("*"))).from(table).apply(completer)
            .build()
    val provider = model.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)
    return count(provider)
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Delete를 수행하는 확장 함수입니다.
 */
suspend fun SqlClient.delete(deleteProvider: DeleteStatementProvider): SqlResult<Void> {
    SqlLogger.logSQL(deleteProvider.deleteStatement, deleteProvider.parameters)

    return SqlTemplate.forUpdate(this, deleteProvider.deleteStatement)
        .execute(deleteProvider.parameters)
        .coAwait()
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 [table]로부터 Delete를 수행하는 확장 함수입니다.
 */
suspend fun SqlClient.deleteFrom(table: SqlTable, completer: DeleteCompleter): SqlResult<Void> {
    val model = KotlinDeleteBuilder(SqlBuilder.deleteFrom(table)).apply(completer).build()
    val provider = model.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)
    return delete(provider)
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Batch Insert를 수행하는 확장 함수입니다.
 *
 * 참고: [Vertx SqlClient Templates executeBatch](https://vertx.io/docs/vertx-sql-client-templates/java/#_parameters_mapping)
 *
 * @param T Batch Insert 대상 Entity Type
 * @param batchInsert Batch Insert 정보
 * @return [SqlResult]
 */
suspend fun <T: Any> SqlClient.insertBatch(batchInsert: BatchInsert<T>): SqlResult<Void> {
    SqlLogger.logSQL(batchInsert.insertStatementSQL, batchInsert.records)

    return SqlTemplate.forUpdate(this, batchInsert.insertStatementSQL)
        .mapFrom<T>(tupleMapperOfRecord())
        .executeBatch(batchInsert.records)
        .coAwait()
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Batch Insert를 수행하는 확장 함수입니다.
 *
 * @param records Batch Insert 대상 Entity List
 * @param completer Batch Insert 정보 설정 람다 함수
 */
suspend fun <T: Any> SqlClient.insertBatch(
    vararg records: T,
    completer: KotlinBatchInsertCompleter<T>,
): SqlResult<Void> {
    return insertBatch(records.asList(), completer)
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Batch Insert를 수행하는 확장 함수입니다.
 *
 * @param records Batch Insert 대상 Entity List
 * @param completer Batch Insert 정보 설정 람다 함수
 */
suspend fun <T: Any> SqlClient.insertBatch(
    records: List<T>,
    completer: KotlinBatchInsertCompleter<T>,
): SqlResult<Void> {
    val model = org.mybatis.dynamic.sql.util.kotlin.model.insertBatch(records, completer)
    val batchInsert = model.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)
    return insertBatch(batchInsert)
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Insert를 수행하는 확장 함수입니다.
 */
suspend fun <T: Any> SqlClient.insert(insertProvider: InsertStatementProvider<T>): SqlResult<Void> {
    SqlLogger.logSQL(insertProvider.insertStatement, insertProvider.row)

    return SqlTemplate.forUpdate(this, insertProvider.insertStatement)
        .mapFrom(tupleMapperOfRecord<T>())
        .execute(insertProvider.row)
        .coAwait()
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Insert를 수행하는 확장 함수입니다.
 *
 * @param entity Insert 대상 Entity
 * @param completer Insert 정보 설정 람다 함수
 */
suspend fun <T: Any> SqlClient.insert(entity: T, completer: KotlinInsertCompleter<T>): SqlResult<Void> {
    val model = KotlinInsertBuilder(entity).apply(completer).build()
    val provider = model.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)
    return insert(provider)
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 General Insert를 수행하는 확장 함수입니다.
 *
 * @param insertProvider General Insert 정보
 */
suspend fun SqlClient.generalInsert(insertProvider: GeneralInsertStatementProvider): SqlResult<Void> {
    SqlLogger.logSQL(insertProvider.insertStatement, insertProvider.parameters)

    return SqlTemplate.forUpdate(this, insertProvider.insertStatement)
        .execute(insertProvider.parameters)
        .coAwait()
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 General Insert를 수행하는 확장 함수입니다.
 *
 * @param table Insert 대상 Table
 * @param completer General Insert 정보 설정 람다 함수
 */
suspend fun SqlClient.generalInsert(table: SqlTable, completer: GeneralInsertCompleter): SqlResult<Void> {
    val model = org.mybatis.dynamic.sql.util.kotlin.model.insertInto(table, completer)
    val provider = model.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)
    return generalInsert(provider)
}


/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Insert Multiple를 수행하는 확장 함수입니다.
 *
 * @param insertProvider Multiple row insert 정보
 */
suspend fun <T: Any> SqlClient.insertMultiple(
    insertProvider: MultiRowInsertStatementProvider<T>,
): SqlResult<Void> {
    SqlLogger.logSQL(insertProvider.insertStatement, insertProvider.records)

    return SqlTemplate.forUpdate(this, insertProvider.insertStatement)
        .execute(insertProvider.records.toParameters())
        .coAwait()
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Insert Select를 수행하는 확장 함수입니다.
 *
 * @param records Insert Select 정보
 * @param completer Insert Select 정보 설정 람다 함수
 */
suspend fun <T: Any> SqlClient.insertMultiple(
    vararg records: T,
    completer: KotlinMultiRowInsertCompleter<T>,
): SqlResult<Void> {
    return insertMultiple(records.asList(), completer)
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Insert Multiple를 수행하는 확장 함수입니다.
 *
 * @param records Insert 할 Entity List
 * @param completer Multiple row insert 정보 설정 람다 함수
 */
suspend fun <T: Any> SqlClient.insertMultiple(
    records: List<T>,
    completer: KotlinMultiRowInsertCompleter<T>,
): SqlResult<Void> {
    val model = org.mybatis.dynamic.sql.util.kotlin.model.insertMultiple(records, completer)
    val provider = model.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

    return insertMultiple(provider)
}


/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Insert Select를 수행하는 확장 함수입니다.
 *
 * @param provider Insert Select 정보
 */
suspend fun SqlClient.insertSelect(provider: InsertSelectStatementProvider): SqlResult<Void> {
    SqlLogger.logSQL(provider.insertStatement, provider.parameters)

    return SqlTemplate.forUpdate(this, provider.insertStatement)
        .execute(provider.parameters)
        .coAwait()
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Insert Select를 수행하는 확장 함수입니다.
 *
 * @param completer Insert Select 정보 설정 람다 함수
 */
suspend fun SqlClient.insertSelect(completer: InsertSelectCompleter): SqlResult<Void> {
    val model = org.mybatis.dynamic.sql.util.kotlin.model.insertSelect(completer)
    val provider = model.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)
    return insertSelect(provider)
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Select를 수행하는 확장 함수입니다.
 *
 * @param provider Select 정보
 */
suspend fun SqlClient.select(provider: SelectStatementProvider): RowSet<Row> {
    SqlLogger.logSQL(provider.selectStatement, provider.parameters)

    return SqlTemplate.forQuery(this, provider.selectStatement)
        .execute(provider.parameters)
        .coAwait()
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Select를 수행하는 확장 함수입니다.
 *
 * @param columns Select 대상 Column List
 * @param completer Select 정보 설정 람다 함수
 */
suspend fun SqlClient.select(vararg columns: BasicColumn, completer: SelectCompleter): RowSet<Row> {
    return select(columns.asList(), completer)
}

suspend fun SqlClient.select(columns: List<BasicColumn>, completer: SelectCompleter): RowSet<Row> {
    val model = org.mybatis.dynamic.sql.util.kotlin.model.select(columns, completer)
    val provider = model.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)
    return select(provider)
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Select를 수행하는 확장 함수입니다.
 *
 * @param provider Select 정보
 * @param rowMapper Row Mapper
 */
suspend fun <T: Any> SqlClient.select(provider: SelectStatementProvider, rowMapper: RowMapper<T>): RowSet<T> {
    SqlLogger.logSQL(provider.selectStatement, provider.parameters)

    return SqlTemplate.forQuery(this, provider.selectStatement)
        .mapTo(rowMapper)
        .execute(provider.parameters)
        .coAwait()
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Select를 수행하는 확장 함수입니다.
 *
 * @param columns Select 대상 Column List
 * @param rowMapper Row Mapper
 * @param completer Select 정보 설정 람다 함수
 */
suspend fun <T: Any> SqlClient.select(
    columns: List<BasicColumn>,
    rowMapper: RowMapper<T>,
    completer: SelectCompleter,
): RowSet<T> {
    val model = org.mybatis.dynamic.sql.util.kotlin.model.select(columns, completer)
    val provider = model.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

    return select(provider, rowMapper)
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Select를 수행하는 확장 함수입니다.
 *
 * @param provider Select 정보
 */
suspend inline fun <reified T: Any> SqlClient.selectAs(provider: SelectStatementProvider): RowSet<T> {
    SqlLogger.logSQL(provider.selectStatement, provider.parameters)

    return SqlTemplate.forQuery(this, provider.selectStatement)
        .mapTo(T::class.java)
        .execute(provider.parameters)
        .coAwait()
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Select를 수행하는 확장 함수입니다.
 *
 * @param columns Select 대상 Column List
 * @param completer Select 정보 설정 람다 함수
 */
suspend inline fun <reified T: Any> SqlClient.selectAs(
    columns: List<BasicColumn>,
    noinline completer: SelectCompleter,
): RowSet<T> {
    val model = org.mybatis.dynamic.sql.util.kotlin.model.select(columns, completer)
    val provider = model.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

    return selectAs(provider)
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Select Distinct를 수행하는 확장 함수입니다.
 *
 * @param provider Select Distinct 정보
 */
internal suspend fun SqlClient.selectDistinct(provider: SelectStatementProvider): RowSet<Row> {
    SqlLogger.logSQL(provider.selectStatement, provider.parameters)

    return SqlTemplate.forQuery(this, provider.selectStatement)
        .execute(provider.parameters)
        .coAwait()
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Select Distinct를 수행하는 확장 함수입니다.
 *
 * @param columns Select Distinct 대상 Column List
 * @param completer Select Distinct 정보 설정 람다 함수
 */
suspend fun SqlClient.selectDistinct(vararg columns: BasicColumn, completer: SelectCompleter): RowSet<Row> {
    return selectDistinct(columns.asList(), completer)
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Select Distinct를 수행하는 확장 함수입니다.
 *
 * @param columns Select Distinct 대상 Column List
 * @param completer Select Distinct 정보 설정 람다 함수
 */
suspend fun SqlClient.selectDistinct(columns: List<BasicColumn>, completer: SelectCompleter): RowSet<Row> {
    val model = org.mybatis.dynamic.sql.util.kotlin.model.selectDistinct(columns, completer)
    val provider = model.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)
    return selectDistinct(provider)
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Select를 수행하는 확장 함수입니다.
 *
 * @param provider Select Distinct 정보
 */
suspend inline fun <reified T: Any> SqlClient.selectListAs(provider: SelectStatementProvider): List<T> {
    return SqlTemplate.forQuery(this, provider.selectStatement)
        .mapTo(T::class.java)
        .execute(provider.parameters)
        .coAwait()
        .map { it }
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Select를 수행하는 확장 함수입니다.
 *
 * @param provider Select statement provider
 * @param mapper Row Mapper
 */
suspend fun <T: Any> SqlClient.selectList(
    provider: SelectStatementProvider,
    mapper: RowMapper<T>,
): List<T> {
    SqlLogger.logSQL(provider.selectStatement, provider.parameters)

    return SqlTemplate.forQuery(this, provider.selectStatement)
        .mapTo(mapper)
        .execute(provider.parameters)
        .coAwait()
        .map { it }
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Select를 수행하는 확장 함수입니다.
 *
 * @param columns Select 대상 Column List
 * @param rowMapper Row Mapper
 * @param completer Select 정보 설정 람다 함수
 */
suspend fun <T: Any> SqlClient.selectList(
    columns: List<BasicColumn>,
    rowMapper: RowMapper<T>,
    completer: SelectCompleter,
): List<T> {
    val model = org.mybatis.dynamic.sql.util.kotlin.model.selectDistinct(columns, completer)
    val provider = model.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)
    return selectList(provider, rowMapper)
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Select One를 수행하는 확장 함수입니다.
 *
 * @param provider select statement provider
 */
suspend fun SqlClient.selectOne(provider: SelectStatementProvider): Row? {
    SqlLogger.logSQL(provider.selectStatement, provider.parameters)

    return SqlTemplate.forQuery(this, provider.selectStatement)
        .execute(provider.parameters)
        .coAwait()
        .firstOrNull()
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Select One를 수행하는 확장 함수입니다.
 *
 * @param columns Select 대상 Column List
 * @param completer Select 정보 설정 람다 함수
 */
suspend fun SqlClient.selectOne(
    vararg columns: BasicColumn,
    completer: SelectCompleter,
): Row? {
    return selectOne(columns.asList(), completer)
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Select One를 수행하는 확장 함수입니다.
 *
 * @param columns Select 대상 Column List
 * @param completer Select 정보 설정 람다 함수
 */
suspend fun SqlClient.selectOne(columns: List<BasicColumn>, completer: SelectCompleter): Row? {
    val model = org.mybatis.dynamic.sql.util.kotlin.model.select(columns, completer)
    val provider = model.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)
    return selectOne(provider)
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Select One를 수행하는 확장 함수입니다.
 *
 * @param T Select 결과 타입
 * @param provider Select statement provider
 * @param mapper Row Mapper
 */
suspend fun <T: Any> SqlClient.selectOne(provider: SelectStatementProvider, mapper: RowMapper<T>): T? {
    SqlLogger.logSQL(provider.selectStatement, provider.parameters)

    return SqlTemplate.forQuery(this, provider.selectStatement)
        .mapTo(mapper)
        .execute(provider.parameters)
        .coAwait()
        .firstOrNull()
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Select One를 수행하는 확장 함수입니다.
 *
 * @param T Select 결과 타입
 * @param provider Select statement provider
 */
suspend inline fun <reified T: Any> SqlClient.selectOneAs(provider: SelectStatementProvider): T? {
    // logSQL(provider.selectStatement, provider.parameters)
    return SqlTemplate.forQuery(this, provider.selectStatement)
        .mapTo(T::class.java)
        .execute(provider.parameters)
        .coAwait()
        .firstOrNull()
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Select One를 수행하는 확장 함수입니다.
 *
 * @param columns Select 대상 Column List
 * @param rowMapper Row Mapper
 * @param completer Select 정보 설정 람다 함수
 */
suspend fun <T: Any> SqlClient.selectOne(
    columns: List<BasicColumn>,
    rowMapper: RowMapper<T>,
    completer: SelectCompleter,
): T? {
    return selectOne(columns, completer)?.let { rowMapper.map(it) }
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 Update를 수행하는 확장 함수입니다.
 *
 * @param provider Update 정보 ([UpdateStatementProvider])
 */
suspend fun SqlClient.update(provider: UpdateStatementProvider): SqlResult<Void> {
    SqlLogger.logSQL(provider.updateStatement, provider.parameters)

    return SqlTemplate.forUpdate(this, provider.updateStatement)
        .execute(provider.parameters)
        .coAwait()
}

/**
 * [SqlClient]를 Mybatis Dynamic SQL 을 이용하여 Coroutine 환경에서 [table]로부터 Update를 수행하는 확장 함수입니다.
 *
 * @param table Update 대상 Table
 * @param completer Update 정보 설정 람다 함수
 */
suspend fun SqlClient.update(table: SqlTable, completer: UpdateCompleter): SqlResult<Void> {
    val model = org.mybatis.dynamic.sql.util.kotlin.model.update(table, completer)
    val provider = model.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)
    return update(provider)
}
