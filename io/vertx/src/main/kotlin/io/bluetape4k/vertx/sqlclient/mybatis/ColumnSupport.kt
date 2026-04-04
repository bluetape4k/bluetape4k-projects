package io.bluetape4k.vertx.sqlclient.mybatis

import org.mybatis.dynamic.sql.DerivedColumn
import org.mybatis.dynamic.sql.SqlColumn

/**
 * [SqlColumn]에 SQL 별칭(alias)을 지정합니다.
 *
 * ```kotlin
 * val col = person.firstName alias "first"
 * // select 시 `firstName AS first`로 렌더링됨
 * ```
 *
 * @param alias 지정할 컬럼 별칭
 * @return 별칭이 적용된 [SqlColumn]
 */
infix fun <T: Any> SqlColumn<T>.alias(alias: String): SqlColumn<T> = `as`(alias)

/**
 * [DerivedColumn]에 SQL 별칭(alias)을 지정합니다.
 *
 * ```kotlin
 * val col = count(person.id) alias "cnt"
 * // select 시 `COUNT(id) AS cnt`로 렌더링됨
 * ```
 *
 * @param alias 지정할 컬럼 별칭
 * @return 별칭이 적용된 [DerivedColumn]
 */
infix fun <T: Any> DerivedColumn<T>.alias(alias: String): DerivedColumn<T> = `as`(alias)
