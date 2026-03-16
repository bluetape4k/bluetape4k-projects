package io.bluetape4k.vertx.sqlclient.mybatis

import org.mybatis.dynamic.sql.DerivedColumn
import org.mybatis.dynamic.sql.SqlColumn

infix fun <T: Any> SqlColumn<T>.alias(alias: String): SqlColumn<T> = `as`(alias)
infix fun <T: Any> DerivedColumn<T>.alias(alias: String): DerivedColumn<T> = `as`(alias)
