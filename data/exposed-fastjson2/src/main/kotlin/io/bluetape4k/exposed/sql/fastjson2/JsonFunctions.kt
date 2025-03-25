package io.bluetape4k.exposed.sql.fastjson2

import io.bluetape4k.fastjson2.FastjsonSerializer
import io.bluetape4k.fastjson2.deserialize
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.ExpressionWithColumnType
import org.jetbrains.exposed.sql.Function
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.InternalApi
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.resolveColumnType
import org.jetbrains.exposed.sql.vendors.currentDialect

// Function Classes

/**
 * 특정 [path]의 JSON 표현 또는 scalar 값인 JSON 객체에서 추출된 데이터를 반환하는 SQL 함수를 나타냅니다.
 */
class Extract<T>(
    /**
     * [path]에 매치되는 JSON 하위 구성 요소를 추출할 표현식입니다.
     */
    val expression: Expression<*>,

    /**
     * 추출할 필드에 대한 JSON 경로/키를 나타내는 문자열입니다.
     */
    vararg val path: String,

    /**
     * 추출한 결과가 scalar 또는 text 값인지 여부입니다. `false`인 경우 JSON 객체입니다.
     */
    val toScalar: Boolean,

    /**
     * [expression]의 JSONB로 캐스팅할 경우 필요한 열 유형입니다.
     */
    val jsonType: IColumnType<*>,

    columnType: IColumnType<T & Any>,
): Function<T>(columnType) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) =
        currentDialect.functionProvider.jsonExtract(expression, path = path, toScalar, jsonType, queryBuilder)
}

// Extension Functions

/**
 * 특정 [path]의 JSON 표현 또는 scalar 값인 JSON 객체에서 추출된 데이터를 반환합니다.
 *
 * @param path 매치할 필드에 대한 JSON 경로/키를 나타내는 문자열입니다.
 *             만약 제공되지 않는다면, 루트 컨텍스트 항목인 `'$'`가 기본값으로 사용됩니다.
 *             **Note:** 모든 벤더에서 복수 [path] 인수는 지원되지 않습니다. 문서를 확인하세요.
 *
 * @param toScalar `true` 라면, 추출된 결과는 scalar 또는 text 값입니다. 그렇지 않다면 JSON 객체입니다.
 */
inline fun <reified T: Any> ExpressionWithColumnType<*>.extract(
    vararg path: String,
    toScalar: Boolean = true,
    serializer: FastjsonSerializer = FastjsonSerializer.Default,
): Extract<T> {
    @OptIn(InternalApi::class)
    val columnType = resolveColumnType(
        T::class,
        defaultType = FastjsonColumnType(
            { serializer.serializeAsString(it) },
            { serializer.deserialize<T>(it)!! }
        )
    )
    return Extract(this, path = path, toScalar, this.columnType, columnType)
}
