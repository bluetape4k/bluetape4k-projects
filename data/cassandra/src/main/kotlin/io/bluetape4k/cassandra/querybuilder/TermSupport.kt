package io.bluetape4k.cassandra.querybuilder

import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.type.DataType
import com.datastax.oss.driver.api.core.type.codec.TypeCodec
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.term.Term
import com.datastax.oss.driver.internal.querybuilder.CqlHelper

/**
 * 두 [Term]을 더하는 CQL 표현식을 반환합니다.
 *
 * ```kotlin
 * val a = QueryBuilder.literal(1)
 * val b = QueryBuilder.literal(2)
 * val sum = a + b
 * // sum.asCql() == "1+2"
 * ```
 */
operator fun Term.plus(rightOperand: Term): Term = QueryBuilder.add(this, rightOperand)

/**
 * 두 [Term]을 빼는 CQL 표현식을 반환합니다.
 *
 * ```kotlin
 * val a = QueryBuilder.literal(5)
 * val b = QueryBuilder.literal(3)
 * val diff = a - b
 * // diff.asCql() == "5-3"
 * ```
 */
operator fun Term.minus(rightOperand: Term): Term = QueryBuilder.subtract(this, rightOperand)

/**
 * 두 [Term]을 곱하는 CQL 표현식을 반환합니다.
 *
 * ```kotlin
 * val a = QueryBuilder.literal(3)
 * val b = QueryBuilder.literal(4)
 * val product = a * b
 * // product.asCql() == "3*4"
 * ```
 */
operator fun Term.times(rightOperand: Term): Term = QueryBuilder.multiply(this, rightOperand)

/**
 * 두 [Term]을 나누는 CQL 표현식을 반환합니다.
 *
 * ```kotlin
 * val a = QueryBuilder.literal(10)
 * val b = QueryBuilder.literal(2)
 * val quotient = a / b
 * // quotient.asCql() == "10/2"
 * ```
 */
operator fun Term.div(rigntOperand: Term): Term = QueryBuilder.divide(this, rigntOperand)

/**
 * 두 [Term]의 나머지를 구하는 CQL 표현식을 반환합니다.
 *
 * ```kotlin
 * val a = QueryBuilder.literal(10)
 * val b = QueryBuilder.literal(3)
 * val rem = a remainder b
 * // rem.asCql() == "10%3"
 * ```
 */
infix fun Term.remainder(rightOperand: Term): Term = QueryBuilder.remainder(this, rightOperand)

/**
 * 함수 이름과 vararg [Term] 인수로 CQL 함수 호출 [Term]을 생성합니다.
 *
 * ```kotlin
 * val term = functionTerm("myFunc", QueryBuilder.literal(1))
 * // term != null
 * ```
 */
fun functionTerm(
    functionName: String,
    vararg terms: Term,
): Term = QueryBuilder.function(functionName, *terms)

/**
 * 함수 이름과 [Iterable] [Term] 인수로 CQL 함수 호출 [Term]을 생성합니다.
 *
 * ```kotlin
 * val args = listOf(QueryBuilder.literal(1), QueryBuilder.literal(2))
 * val term = functionTerm("myFunc", args)
 * // term != null
 * ```
 */
fun functionTerm(
    functionName: String,
    terms: Iterable<Term>,
): Term = QueryBuilder.function(functionName, terms)

/**
 * 키스페이스 이름, 함수 이름, vararg [Term] 인수로 CQL 함수 호출 [Term]을 생성합니다.
 *
 * ```kotlin
 * val term = functionTerm("myKeyspace", "myFunc", QueryBuilder.literal(1))
 * // term != null
 * ```
 */
fun functionTerm(
    keyspaceName: String,
    functionName: String,
    vararg terms: Term,
): Term = QueryBuilder.function(keyspaceName, functionName, *terms)

/**
 * 키스페이스 이름, 함수 이름, [Iterable] [Term] 인수로 CQL 함수 호출 [Term]을 생성합니다.
 *
 * ```kotlin
 * val args = listOf(QueryBuilder.literal("x"))
 * val term = functionTerm("myKeyspace", "myFunc", args)
 * // term != null
 * ```
 */
fun functionTerm(
    keyspaceName: String,
    functionName: String,
    terms: Iterable<Term>,
): Term = QueryBuilder.function(keyspaceName, functionName, terms)

/**
 * [CqlIdentifier] 함수 ID와 vararg [Term] 인수로 CQL 함수 호출 [Term]을 생성합니다.
 *
 * ```kotlin
 * val id = CqlIdentifier.fromCql("myFunc")
 * val term = functionTerm(id, QueryBuilder.literal(1))
 * // term != null
 * ```
 */
fun functionTerm(
    functionId: CqlIdentifier,
    vararg terms: Term,
): Term = QueryBuilder.function(functionId, *terms)

/**
 * [CqlIdentifier] 함수 ID와 [Iterable] [Term] 인수로 CQL 함수 호출 [Term]을 생성합니다.
 *
 * ```kotlin
 * val id = CqlIdentifier.fromCql("myFunc")
 * val args = listOf(QueryBuilder.literal(1))
 * val term = functionTerm(id, args)
 * // term != null
 * ```
 */
fun functionTerm(
    functionId: CqlIdentifier,
    terms: Iterable<Term>,
): Term = QueryBuilder.function(functionId, terms)

/**
 * 키스페이스 ID, 함수 ID, vararg [Term] 인수로 CQL 함수 호출 [Term]을 생성합니다.
 *
 * ```kotlin
 * val ksId = CqlIdentifier.fromCql("myKeyspace")
 * val fnId = CqlIdentifier.fromCql("myFunc")
 * val term = functionTerm(ksId, fnId, QueryBuilder.literal(1))
 * // term != null
 * ```
 */
fun functionTerm(
    keyspaceId: CqlIdentifier,
    functionId: CqlIdentifier,
    vararg terms: Term,
): Term = QueryBuilder.function(keyspaceId, functionId, *terms)

/**
 * 키스페이스 ID, 함수 ID, [Iterable] [Term] 인수로 CQL 함수 호출 [Term]을 생성합니다.
 *
 * ```kotlin
 * val ksId = CqlIdentifier.fromCql("myKeyspace")
 * val fnId = CqlIdentifier.fromCql("myFunc")
 * val args = listOf(QueryBuilder.literal(1))
 * val term = functionTerm(ksId, fnId, args)
 * // term != null
 * ```
 */
fun functionTerm(
    keyspaceId: CqlIdentifier,
    functionId: CqlIdentifier,
    terms: Iterable<Term>,
): Term = QueryBuilder.function(keyspaceId, functionId, terms)

/**
 * CQL `now()` 함수 [Term]을 반환합니다.
 *
 * ```kotlin
 * val term = nowTerm()
 * // term.asCql() == "now()"
 * ```
 */
fun nowTerm(): Term = QueryBuilder.now()

/**
 * CQL `currentTimestamp()` 함수 [Term]을 반환합니다.
 *
 * ```kotlin
 * val term = currentTimestampTerm()
 * // term.asCql() == "currentTimestamp()"
 * ```
 */
fun currentTimestampTerm(): Term = QueryBuilder.currentTimestamp()

/**
 * CQL `currentDate()` 함수 [Term]을 반환합니다.
 *
 * ```kotlin
 * val term = currentDateTerm()
 * // term.asCql() == "currentDate()"
 * ```
 */
fun currentDateTerm(): Term = QueryBuilder.currentDate()

/**
 * CQL `currentTime()` 함수 [Term]을 반환합니다.
 *
 * ```kotlin
 * val term = currentTimeTerm()
 * // term.asCql() == "currentTime()"
 * ```
 */
fun currentTimeTerm(): Term = QueryBuilder.currentTime()

/**
 * CQL `currentTimeUuid()` 함수 [Term]을 반환합니다.
 *
 * ```kotlin
 * val term = currentTimeUuidTerm()
 * // term.asCql() == "currentTimeUuid()"
 * ```
 */
fun currentTimeUuidTerm(): Term = QueryBuilder.currentTimeUuid()

/**
 * 임의의 값들로부터 CQL 튜플 [Term]을 생성합니다. [Term]이 아닌 값은 [literal]로 변환합니다.
 *
 * ```kotlin
 * val term = tupleTerm(1, "hello", true)
 * // term != null
 * ```
 */
fun tupleTerm(vararg args: Any): Term = QueryBuilder.tuple(args.map { (it as? Term) ?: it.literal() })

/**
 * [Term] 컬렉션으로부터 CQL 튜플 [Term]을 생성합니다.
 *
 * ```kotlin
 * val terms = listOf(QueryBuilder.literal(1), QueryBuilder.literal("a"))
 * val tuple = terms.tuple()
 * // tuple != null
 * ```
 */
fun Iterable<Term>.tuple(): Term = QueryBuilder.tuple(this)

/**
 * [Term]에 대해 `minTimeuuid()` 함수를 적용한 [Term]을 반환합니다.
 *
 * ```kotlin
 * val ts = QueryBuilder.literal("2023-01-01")
 * val term = ts.minTimeUuid()
 * // term != null
 * ```
 */
fun Term.minTimeUuid(): Term = QueryBuilder.minTimeUuid(this)

/**
 * [Term]에 대해 `maxTimeuuid()` 함수를 적용한 [Term]을 반환합니다.
 *
 * ```kotlin
 * val ts = QueryBuilder.literal("2023-12-31")
 * val term = ts.maxTimeUuid()
 * // term != null
 * ```
 */
fun Term.maxTimeUuid(): Term = QueryBuilder.maxTimeUuid(this)

/**
 * [Term]에 대해 `toDate()` 함수를 적용한 [Term]을 반환합니다.
 *
 * ```kotlin
 * val ts = nowTerm()
 * val dateTerm = ts.toDate()
 * // dateTerm != null
 * ```
 */
fun Term.toDate(): Term = QueryBuilder.toDate(this)

/**
 * [Term]에 대해 `toTimestamp()` 함수를 적용한 [Term]을 반환합니다.
 *
 * ```kotlin
 * val dateTerm = currentDateTerm()
 * val tsTerm = dateTerm.toTimestamp()
 * // tsTerm != null
 * ```
 */
fun Term.toTimestamp(): Term = QueryBuilder.toTimestamp(this)

/**
 * [Term]에 대해 `toUnixTimestamp()` 함수를 적용한 [Term]을 반환합니다.
 *
 * ```kotlin
 * val ts = nowTerm()
 * val unixTerm = ts.toUnixTimestamp()
 * // unixTerm != null
 * ```
 */
fun Term.toUnixTimestamp(): Term = QueryBuilder.toUnixTimestamp(this)

/**
 * [Term]을 부정하는 CQL 표현식을 반환합니다.
 *
 * ```kotlin
 * val a = QueryBuilder.literal(5)
 * val neg = a.negate()
 * // neg.asCql() == "-5"
 * ```
 */
fun Term.negate(): Term = QueryBuilder.negate(this)

/**
 * [Term]에 타입 힌트를 적용한 [Term]을 반환합니다.
 *
 * ```kotlin
 * val term = QueryBuilder.literal(42)
 * val hinted = term.typeHint(DataTypes.INT)
 * // hinted != null
 * ```
 */
fun Term.typeHint(targetType: DataType): Term = QueryBuilder.typeHint(this, targetType)

internal fun Term.asCql(): String = buildString { appendTo(this) }

/**
 * 임의의 값을 CQL 리터럴 [Term]으로 변환합니다.
 * 컬렉션 타입(List, Set, Map)은 재귀적으로 각 요소를 변환합니다.
 *
 * ```kotlin
 * val term = "hello".literal()
 * // term != null
 * val listTerm = listOf(1, 2, 3).literal()
 * // listTerm != null
 * ```
 *
 * @return 변환된 [Term]
 */
fun Any?.literal(): Term =
    when (this) {
        is List<*> -> ListTerm(map { it.literal() })
        is Set<*> -> SetTerm(map { it.literal() })
        is Map<*, *> -> MapTerm(entries.associate { (k, v) -> k.literal() to v.literal() })
        else -> QueryBuilder.literal(this)
    }

/**
 * [CodecRegistry]를 사용하여 값을 CQL 리터럴 [Term]으로 변환합니다.
 * 컬렉션 타입(List, Set, Map)은 재귀적으로 각 요소를 변환합니다.
 *
 * ```kotlin
 * val registry = CodecRegistry.DEFAULT
 * val term = "hello".literal(registry)
 * // term != null
 * ```
 *
 * @param codecRegistry 타입 변환에 사용할 코덱 레지스트리
 * @return 변환된 [Term]
 */
fun Any?.literal(codecRegistry: CodecRegistry): Term =
    when (this) {
        is List<*> -> ListTerm(map { it.literal(codecRegistry) })
        is Set<*> -> SetTerm(map { it.literal(codecRegistry) })
        is Map<*, *> -> MapTerm(entries.associate { (k, v) -> k.literal(codecRegistry) to v.literal(codecRegistry) })
        else -> QueryBuilder.literal(this, codecRegistry)
    }

/**
 * [TypeCodec]을 사용하여 값을 CQL 리터럴 [Term]으로 변환합니다.
 *
 * ```kotlin
 * val codec = TypeCodecs.TEXT
 * val term = "hello".literal(codec)
 * // term != null
 * ```
 *
 * @param codec 타입 변환에 사용할 코덱
 * @return 변환된 [Term]
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any> T.literal(codec: TypeCodec<out T>): Term =
    when (this) {
        is List<*> -> ListTerm(map { it!!.literal(codec) })
        is Set<*> -> SetTerm(map { it!!.literal(codec) })
        is Map<*, *> -> MapTerm(entries.associate { (k, v) -> k!!.literal(codec) to v!!.literal(codec) })
        else -> QueryBuilder.literal(this, codec as TypeCodec<T>)
    }

/**
 * CQL 리스트 리터럴을 나타내는 [Term] 구현체입니다.
 * `[elem1,elem2,...]` 형식의 CQL을 생성합니다.
 *
 * ```kotlin
 * val listTerm = ListTerm(listOf(QueryBuilder.literal(1), QueryBuilder.literal(2)))
 * // listTerm.asCql() == "[1,2]"
 * ```
 *
 * @param components 리스트 요소 [Term] 목록
 */
class ListTerm(
    private val components: Collection<Term>,
) : Term {
    override fun appendTo(builder: StringBuilder) {
        if (components.isEmpty()) {
            builder.append("[]")
        } else {
            CqlHelper.append(components, builder, "[", ",", "]")
        }
    }

    override fun isIdempotent(): Boolean = components.all { it.isIdempotent }
}

/**
 * CQL 세트 리터럴을 나타내는 [Term] 구현체입니다.
 * `{elem1,elem2,...}` 형식의 CQL을 생성합니다.
 *
 * ```kotlin
 * val setTerm = SetTerm(listOf(QueryBuilder.literal("a"), QueryBuilder.literal("b")))
 * // setTerm.asCql() == "{'a','b'}"
 * ```
 *
 * @param components 세트 요소 [Term] 목록
 */
class SetTerm(
    private val components: Collection<Term>,
) : Term {
    override fun appendTo(builder: StringBuilder) {
        if (components.isEmpty()) {
            builder.append("{}")
        } else {
            CqlHelper.append(components, builder, "{", ",", "}")
        }
    }

    override fun isIdempotent(): Boolean = components.all { it.isIdempotent }
}

/**
 * CQL 맵 리터럴을 나타내는 [Term] 구현체입니다.
 * `{key1:val1,key2:val2,...}` 형식의 CQL을 생성합니다.
 *
 * ```kotlin
 * val mapTerm = MapTerm(mapOf(QueryBuilder.literal("k") to QueryBuilder.literal("v")))
 * // mapTerm.asCql() == "{'k':'v'}"
 * ```
 *
 * @param components 키-값 [Term] 쌍의 맵
 */
class MapTerm(
    private val components: Map<Term, Term>,
) : Term {
    override fun appendTo(builder: StringBuilder) {
        if (components.isEmpty()) {
            builder.append("{}")
        } else {
            builder.append("{")
            var isFirst = true
            components.forEach { (key, value) ->
                if (isFirst) isFirst = false else builder.append(",")

                key.appendTo(builder)
                builder.append(":")
                value.appendTo(builder)
            }
            builder.append("}")
        }
    }

    override fun isIdempotent(): Boolean =
        components.all {
            it.key.isIdempotent && it.value.isIdempotent
        }
}
