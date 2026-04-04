package io.bluetape4k.vertx.sqlclient.mybatis

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.mybatis.dynamic.sql.BindableColumn
import org.mybatis.dynamic.sql.render.RenderingStrategies
import org.mybatis.dynamic.sql.render.RenderingStrategy

/**
 * Vert.x SQL Client Template 용 전역 [RenderingStrategy] 싱글턴입니다.
 *
 * ```kotlin
 * val provider = selectModel.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)
 * // provider.selectStatement == "SELECT ... WHERE id = #{id}"
 * ```
 */
@JvmField
val VERTX_SQL_CLIENT_RENDERING_STRATEGY: RenderingStrategy = VertxSqlClientRenderingStrategy.INSTANCE

/**
 * Vertx 의  SqlClient Template 에서 사용할 수 있도록 SQL Statement용 Parameter를 rendering 합니다.
 *
 * ```kotlin
 * // NOTE: 대상 Entity가 없다면 Parameter가 `#{p1}`, `#{p2}` 형태로 rendering 됩니다.
 * val insert = insertInto(person) {
 *      set(people.id).toValue(1)
 *      set(people.firstName).toValue("Elon")
 *      set(people.lastName).toValue("Musk")
 * }.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)
 *
 * // insert.insertStatement == "insert into Person (id, first_name, last_name) values (#{p1}, #{p2}, #{p3})"
 *
 * // entity 로 insert 하는 경우
 * val person1 = PersonRecord(100, "Joe", "Jones", Date(), true, "Developer", 1)
 * val insertModel = insert(person1) {
 *     into(person)
 *     map(person.id) toProperty PersonRecord::id.name
 *     map(person.firstName) toProperty PersonRecord::firstName.name
 * }
 * val insertRendered = insertModel.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)
 * // NOTE: Entity를 직접 insert 하는 경우 parameter name 이 property name 과 같게 생성됩니다.
 * // insertRendered.insertStatement == "insert into Person (id, first_name) values (#{id}, #{firstName})"
 * ```
 *
 * @see [RenderingStrategy]
 * @see [RenderingStrategies.SPRING_NAMED_PARAMETER]
 * @see [RenderingStrategies.MYBATIS3]
 */
class VertxSqlClientRenderingStrategy: RenderingStrategy() {

    companion object: KLoggingChannel() {
        @JvmField
        val INSTANCE = VertxSqlClientRenderingStrategy()
    }

    override fun getFormattedJdbcPlaceholder(
        column: BindableColumn<*>?,
        prefix: String?,
        parameterName: String?,
    ): String {
        log.debug { "Get formatted jdbc placeholder. prefix=$prefix, parameterName=$parameterName" }

        var placeHolder = parameterName
        if (prefix != null) {
            if (prefix == "record") {
                placeHolder = "$parameterName"
            } else if (prefix == "records[%s]") {
                placeHolder = "${parameterName}%s"
            }

        }
        return "#{$placeHolder}"
    }

    override fun getFormattedJdbcPlaceholder(prefix: String?, parameterName: String?): String {
        log.debug { "parameterName=$parameterName" }
        return "#{$parameterName}"
    }

    override fun getRecordBasedInsertBinding(column: BindableColumn<*>?, parameterName: String?): String {
        return getFormattedJdbcPlaceholder(column, "record", parameterName)
    }
}
