package io.bluetape4k.cassandra.querybuilder

import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.type.DataType
import com.datastax.oss.driver.api.core.type.codec.TypeCodec
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.term.Term
import com.datastax.oss.driver.internal.querybuilder.CqlHelper

operator fun Term.plus(rightOperand: Term): Term = QueryBuilder.add(this, rightOperand)

operator fun Term.minus(rightOperand: Term): Term = QueryBuilder.subtract(this, rightOperand)

operator fun Term.times(rightOperand: Term): Term = QueryBuilder.multiply(this, rightOperand)

operator fun Term.div(rigntOperand: Term): Term = QueryBuilder.divide(this, rigntOperand)

infix fun Term.remainder(rightOperand: Term): Term = QueryBuilder.remainder(this, rightOperand)

fun functionTerm(
    functionName: String,
    vararg terms: Term,
): Term = QueryBuilder.function(functionName, *terms)

fun functionTerm(
    functionName: String,
    terms: Iterable<Term>,
): Term = QueryBuilder.function(functionName, terms)

fun functionTerm(
    keyspaceName: String,
    functionName: String,
    vararg terms: Term,
): Term = QueryBuilder.function(keyspaceName, functionName, *terms)

fun functionTerm(
    keyspaceName: String,
    functionName: String,
    terms: Iterable<Term>,
): Term = QueryBuilder.function(keyspaceName, functionName, terms)

fun functionTerm(
    functionId: CqlIdentifier,
    vararg terms: Term,
): Term = QueryBuilder.function(functionId, *terms)

fun functionTerm(
    functionId: CqlIdentifier,
    terms: Iterable<Term>,
): Term = QueryBuilder.function(functionId, terms)

fun functionTerm(
    keyspaceId: CqlIdentifier,
    functionId: CqlIdentifier,
    vararg terms: Term,
): Term = QueryBuilder.function(keyspaceId, functionId, *terms)

fun functionTerm(
    keyspaceId: CqlIdentifier,
    functionId: CqlIdentifier,
    terms: Iterable<Term>,
): Term = QueryBuilder.function(keyspaceId, functionId, terms)

fun nowTerm(): Term = QueryBuilder.now()

fun currentTimestampTerm(): Term = QueryBuilder.currentTimestamp()

fun currentDateTerm(): Term = QueryBuilder.currentDate()

fun currentTimeTerm(): Term = QueryBuilder.currentTime()

fun currentTimeUuidTerm(): Term = QueryBuilder.currentTimeUuid()

fun tupleTerm(vararg args: Any): Term = QueryBuilder.tuple(args.map { (it as? Term) ?: it.literal() })

fun Iterable<Term>.tuple(): Term = QueryBuilder.tuple(this)

fun Term.minTimeUuid(): Term = QueryBuilder.minTimeUuid(this)

fun Term.maxTimeUuid(): Term = QueryBuilder.maxTimeUuid(this)

fun Term.toDate(): Term = QueryBuilder.toDate(this)

fun Term.toTimestamp(): Term = QueryBuilder.toTimestamp(this)

fun Term.toUnixTimestamp(): Term = QueryBuilder.toUnixTimestamp(this)

fun Term.negate(): Term = QueryBuilder.negate(this)

fun Term.typeHint(targetType: DataType): Term = QueryBuilder.typeHint(this, targetType)

internal fun Term.asCql(): String = buildString { appendTo(this) }

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
