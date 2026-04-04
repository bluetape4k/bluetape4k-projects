package io.bluetape4k.vertx.sqlclient.mybatis

import org.mybatis.dynamic.sql.delete.DeleteModel
import org.mybatis.dynamic.sql.delete.render.DeleteStatementProvider
import org.mybatis.dynamic.sql.insert.BatchInsertModel
import org.mybatis.dynamic.sql.insert.GeneralInsertModel
import org.mybatis.dynamic.sql.insert.InsertModel
import org.mybatis.dynamic.sql.insert.InsertSelectModel
import org.mybatis.dynamic.sql.insert.MultiRowInsertModel
import org.mybatis.dynamic.sql.insert.render.BatchInsert
import org.mybatis.dynamic.sql.insert.render.GeneralInsertStatementProvider
import org.mybatis.dynamic.sql.insert.render.InsertSelectStatementProvider
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider
import org.mybatis.dynamic.sql.insert.render.MultiRowInsertStatementProvider
import org.mybatis.dynamic.sql.render.TableAliasCalculator
import org.mybatis.dynamic.sql.select.SelectModel
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider
import org.mybatis.dynamic.sql.update.UpdateModel
import org.mybatis.dynamic.sql.update.render.UpdateStatementProvider
import org.mybatis.dynamic.sql.where.WhereModel
import org.mybatis.dynamic.sql.where.render.WhereClauseProvider
import java.util.*

/**
 * [VERTX_SQL_CLIENT_RENDERING_STRATEGY]를 사용하여 Select 구문을 렌더링합니다.
 *
 * ```kotlin
 * val provider = select(person.id, person.firstName) {
 *     from(person)
 *     where { person.id isEqualTo 1 }
 * }.renderForVertx()
 * // provider.selectStatement == "SELECT id, first_name FROM Person WHERE id = #{id}"
 * ```
 */
fun SelectModel.renderForVertx(): SelectStatementProvider = render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

/**
 * [VERTX_SQL_CLIENT_RENDERING_STRATEGY]를 사용하여 Delete 구문을 렌더링합니다.
 *
 * ```kotlin
 * val provider = deleteFrom(person) {
 *     where { person.id isEqualTo 1 }
 * }.renderForVertx()
 * // provider.deleteStatement == "DELETE FROM Person WHERE id = #{id}"
 * ```
 */
fun DeleteModel.renderForVertx(): DeleteStatementProvider = render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

/**
 * [VERTX_SQL_CLIENT_RENDERING_STRATEGY]를 사용하여 Insert 구문을 렌더링합니다.
 *
 * ```kotlin
 * val person1 = PersonRecord(1, "Joe", "Jones")
 * val provider = insert(person1) {
 *     into(person)
 *     map(person.id) toProperty PersonRecord::id.name
 *     map(person.firstName) toProperty PersonRecord::firstName.name
 * }.renderForVertx()
 * // provider.insertStatement == "INSERT INTO Person (id, first_name) VALUES (#{id}, #{firstName})"
 * ```
 */
fun <T> InsertModel<T>.renderForVertx(): InsertStatementProvider<T> = render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

/**
 * [VERTX_SQL_CLIENT_RENDERING_STRATEGY]를 사용하여 General Insert 구문을 렌더링합니다.
 *
 * ```kotlin
 * val provider = insertInto(person) {
 *     set(person.id).toValue(1)
 *     set(person.firstName).toValue("Joe")
 * }.renderForVertx()
 * // provider.insertStatement == "INSERT INTO Person (id, first_name) VALUES (#{p1}, #{p2})"
 * ```
 */
fun GeneralInsertModel.renderForVertx(): GeneralInsertStatementProvider = render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

/**
 * [VERTX_SQL_CLIENT_RENDERING_STRATEGY]를 사용하여 Batch Insert 구문을 렌더링합니다.
 *
 * ```kotlin
 * val records = listOf(PersonRecord(1, "Joe"), PersonRecord(2, "Jane"))
 * val batchInsert = insertBatch(records) {
 *     into(person)
 *     map(person.id) toProperty PersonRecord::id.name
 *     map(person.firstName) toProperty PersonRecord::firstName.name
 * }.renderForVertx()
 * // batchInsert.insertStatementSQL == "INSERT INTO Person (id, first_name) VALUES (#{id}, #{firstName})"
 * ```
 */
fun <T> BatchInsertModel<T>.renderForVertx(): BatchInsert<T> = render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

/**
 * [VERTX_SQL_CLIENT_RENDERING_STRATEGY]를 사용하여 Insert Select 구문을 렌더링합니다.
 *
 * ```kotlin
 * val provider = insertSelect {
 *     into(archive)
 *     withColumnList(archive.id, archive.name)
 *     withSelectStatement {
 *         select(person.id, person.firstName) { from(person) }
 *     }
 * }.renderForVertx()
 * // provider.insertStatement 에 SELECT 기반 INSERT 구문이 생성됩니다.
 * ```
 */
fun InsertSelectModel.renderForVertx(): InsertSelectStatementProvider = render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

/**
 * [VERTX_SQL_CLIENT_RENDERING_STRATEGY]를 사용하여 Multi Row Insert 구문을 렌더링합니다.
 *
 * ```kotlin
 * val records = listOf(PersonRecord(1, "Joe"), PersonRecord(2, "Jane"))
 * val provider = insertMultiple(records) {
 *     into(person)
 *     map(person.id) toProperty PersonRecord::id.name
 *     map(person.firstName) toProperty PersonRecord::firstName.name
 * }.renderForVertx()
 * // provider.insertStatement == "INSERT INTO Person (id, first_name) VALUES (#{id0}, #{firstName0}), (#{id1}, #{firstName1})"
 * ```
 */
fun <T> MultiRowInsertModel<T>.renderForVertx(): MultiRowInsertStatementProvider<T> =
    render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

/**
 * [VERTX_SQL_CLIENT_RENDERING_STRATEGY]를 사용하여 Update 구문을 렌더링합니다.
 *
 * ```kotlin
 * val provider = update(person) {
 *     set(person.firstName) equalToValue "Jane"
 *     where { person.id isEqualTo 1 }
 * }.renderForVertx()
 * // provider.updateStatement == "UPDATE Person SET first_name = #{p1} WHERE id = #{p2}"
 * ```
 */
fun UpdateModel.renderForVertx(): UpdateStatementProvider = render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

/**
 * [VERTX_SQL_CLIENT_RENDERING_STRATEGY]를 사용하여 Where 절을 렌더링합니다.
 *
 * ```kotlin
 * val whereClause = where { person.id isEqualTo 1 }.renderForVertx()
 * // whereClause.isPresent == true
 * // whereClause.get().whereClause == "WHERE id = #{id}"
 * ```
 */
fun WhereModel.renderForVertx(): Optional<WhereClauseProvider> = render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

/**
 * [VERTX_SQL_CLIENT_RENDERING_STRATEGY]와 [tableAliasCalculator]를 사용하여 Where 절을 렌더링합니다.
 *
 * ```kotlin
 * val aliasCalculator = TableAliasCalculator.of(person, "p")
 * val whereClause = where { person.id isEqualTo 1 }.renderForVertx(aliasCalculator)
 * // whereClause.get().whereClause == "WHERE p.id = #{id}"
 * ```
 */
fun WhereModel.renderForVertx(tableAliasCalculator: TableAliasCalculator): Optional<WhereClauseProvider> =
    render(VERTX_SQL_CLIENT_RENDERING_STRATEGY, tableAliasCalculator)

/**
 * [VERTX_SQL_CLIENT_RENDERING_STRATEGY]와 [parameterName]을 사용하여 Where 절을 렌더링합니다.
 *
 * ```kotlin
 * val whereClause = where { person.id isEqualTo 1 }.renderForVertx("p")
 * // whereClause.get().whereClause == "WHERE id = #{p.id}"
 * ```
 */
fun WhereModel.renderForVertx(parameterName: String): Optional<WhereClauseProvider> =
    render(VERTX_SQL_CLIENT_RENDERING_STRATEGY, parameterName)

/**
 * [VERTX_SQL_CLIENT_RENDERING_STRATEGY]와 [tableAliasCalculator], [parameterName]을 사용하여 Where 절을 렌더링합니다.
 *
 * ```kotlin
 * val aliasCalculator = TableAliasCalculator.of(person, "p")
 * val whereClause = where { person.id isEqualTo 1 }.renderForVertx(aliasCalculator, "rec")
 * // whereClause.get().whereClause == "WHERE p.id = #{rec.id}"
 * ```
 */
fun WhereModel.renderForVertx(
    tableAliasCalculator: TableAliasCalculator,
    parameterName: String,
): Optional<WhereClauseProvider> = render(VERTX_SQL_CLIENT_RENDERING_STRATEGY, tableAliasCalculator, parameterName)
