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

/** [VERTX_SQL_CLIENT_RENDERING_STRATEGY]를 사용하여 Select 구문을 렌더링합니다. */
fun SelectModel.renderForVertx(): SelectStatementProvider = render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

/** [VERTX_SQL_CLIENT_RENDERING_STRATEGY]를 사용하여 Delete 구문을 렌더링합니다. */
fun DeleteModel.renderForVertx(): DeleteStatementProvider = render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

/** [VERTX_SQL_CLIENT_RENDERING_STRATEGY]를 사용하여 Insert 구문을 렌더링합니다. */
fun <T> InsertModel<T>.renderForVertx(): InsertStatementProvider<T> = render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

/** [VERTX_SQL_CLIENT_RENDERING_STRATEGY]를 사용하여 General Insert 구문을 렌더링합니다. */
fun GeneralInsertModel.renderForVertx(): GeneralInsertStatementProvider = render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

/** [VERTX_SQL_CLIENT_RENDERING_STRATEGY]를 사용하여 Batch Insert 구문을 렌더링합니다. */
fun <T> BatchInsertModel<T>.renderForVertx(): BatchInsert<T> = render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

/** [VERTX_SQL_CLIENT_RENDERING_STRATEGY]를 사용하여 Insert Select 구문을 렌더링합니다. */
fun InsertSelectModel.renderForVertx(): InsertSelectStatementProvider = render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

/** [VERTX_SQL_CLIENT_RENDERING_STRATEGY]를 사용하여 Multi Row Insert 구문을 렌더링합니다. */
fun <T> MultiRowInsertModel<T>.renderForVertx(): MultiRowInsertStatementProvider<T> =
    render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

/** [VERTX_SQL_CLIENT_RENDERING_STRATEGY]를 사용하여 Update 구문을 렌더링합니다. */
fun UpdateModel.renderForVertx(): UpdateStatementProvider = render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

/** [VERTX_SQL_CLIENT_RENDERING_STRATEGY]를 사용하여 Where 절을 렌더링합니다. */
fun WhereModel.renderForVertx(): Optional<WhereClauseProvider> = render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)

/** [VERTX_SQL_CLIENT_RENDERING_STRATEGY]와 [tableAliasCalculator]를 사용하여 Where 절을 렌더링합니다. */
fun WhereModel.renderForVertx(tableAliasCalculator: TableAliasCalculator): Optional<WhereClauseProvider> =
    render(VERTX_SQL_CLIENT_RENDERING_STRATEGY, tableAliasCalculator)

/** [VERTX_SQL_CLIENT_RENDERING_STRATEGY]와 [parameterName]을 사용하여 Where 절을 렌더링합니다. */
fun WhereModel.renderForVertx(parameterName: String): Optional<WhereClauseProvider> =
    render(VERTX_SQL_CLIENT_RENDERING_STRATEGY, parameterName)

/**
 * [VERTX_SQL_CLIENT_RENDERING_STRATEGY]와 [tableAliasCalculator], [parameterName]을 사용하여 Where 절을 렌더링합니다.
 */
fun WhereModel.renderForVertx(
    tableAliasCalculator: TableAliasCalculator,
    parameterName: String,
): Optional<WhereClauseProvider> = render(VERTX_SQL_CLIENT_RENDERING_STRATEGY, tableAliasCalculator, parameterName)
