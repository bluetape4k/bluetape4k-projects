package io.bluetape4k.vertx.sqlclient.mybatis

import org.mybatis.dynamic.sql.DerivedColumn
import org.mybatis.dynamic.sql.SqlColumn

infix fun <T> SqlColumn<T>.alias(alias: String): SqlColumn<T> = `as`(alias)
infix fun <T> DerivedColumn<T>.alias(alias: String): DerivedColumn<T> = `as`(alias)
