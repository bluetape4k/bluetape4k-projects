package io.bluetape4k.cassandra.querybuilder

import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.type.UserDefinedType
import com.datastax.oss.driver.api.querybuilder.BindMarker
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.Raw

/**
 * [QueryBuilder]를 이용해 Named [BindMarker] 를 생성합니다.
 *
 * ```
 * val bindMarker: BindMarker = "name".bindMarker()
 * ```
 */
fun String?.bindMarker(): BindMarker = QueryBuilder.bindMarker(this)

/**
 * [QueryBuilder]를 이용해 Named [BindMarker] 를 생성합니다.
 *
 * ```
 * val bindMarker: BindMarker = CqlIdentifier.fromCql("name").bindMarker()
 * ```
 */
fun CqlIdentifier?.bindMarker(): BindMarker = QueryBuilder.bindMarker(this)

/**
 * [QueryBuilder]를 이용해 [Raw] CQL snippet을 생성합니다.
 *
 * ```
 * val raw: Raw = "ttl(?)".raw()
 * ```
 */
fun String.raw(): Raw = QueryBuilder.raw(this)

/**
 * [QueryBuilder]를 이용해 [UserDefinedType]을 생성합니다.
 *
 * ```
 * val udt: UserDefinedType = "address".udt()
 * ```
 */
fun String.udt(): UserDefinedType = QueryBuilder.udt(this)

/**
 * [QueryBuilder]를 이용해 [CqlIdentifier]를 가지는 [UserDefinedType]을 생성합니다.
 *
 * ```
 * val udt: UserDefinedType = CqlIdentifier.fromCql("address").udt()
 * ```
 */
fun CqlIdentifier.udt(): UserDefinedType = QueryBuilder.udt(this)
