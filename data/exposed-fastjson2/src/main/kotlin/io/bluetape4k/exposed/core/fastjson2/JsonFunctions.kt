package io.bluetape4k.exposed.core.fastjson2

import io.bluetape4k.fastjson2.FastjsonSerializer
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.InternalApi
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.resolveColumnType
import org.jetbrains.exposed.v1.core.vendors.currentDialect

/**
 * JSON 경로에서 값을 추출하는 SQL 함수 표현식입니다.
 *
 * ## 동작/계약
 * - SQL 생성은 Dialect의 `jsonExtract(...)` 구현으로 위임됩니다.
 * - [toScalar]가 `true`이면 scalar/text 추출 모드, `false`이면 JSON 객체 추출 모드로 렌더링됩니다.
 * - [path]는 가변 인자로 전달되며, 벤더별로 다중 경로 지원 여부가 다를 수 있습니다.
 *
 * ```kotlin
 * val fn = Extract<String>(expr, "$.name", toScalar = true, jsonType = expr.columnType, columnType = textType)
 * // fn is org.jetbrains.exposed.v1.core.Function<String>
 * ```
 *
 * @param expression 추출 대상 JSON 표현식입니다.
 * @param path 추출 경로 목록입니다.
 * @param toScalar scalar/text 추출 여부입니다.
 * @param jsonType JSON 캐스트에 사용할 원본 컬럼 타입입니다.
 * @param columnType 추출 결과의 Exposed 컬럼 타입입니다.
 */
class Extract<T>(
    val expression: Expression<*>,
    vararg val path: String,
    val toScalar: Boolean,
    val jsonType: IColumnType<*>,
    columnType: IColumnType<T & Any>,
): org.jetbrains.exposed.v1.core.Function<T>(columnType) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) =
        currentDialect.functionProvider.jsonExtract(expression, path = path, toScalar, jsonType, queryBuilder)
}

/**
 * JSON 표현식에서 경로 값을 추출하는 [Extract] 함수를 생성합니다.
 *
 * ## 동작/계약
 * - 반환 타입 [T]에 맞는 컬럼 타입을 `resolveColumnType`으로 계산하며, 기본 타입으로 [FastjsonColumnType]을 사용합니다.
 * - [serializer]의 역직렬화 결과가 `null`이면 `!!` 때문에 `NullPointerException`이 발생합니다.
 * - 반환값은 SQL 함수 표현식이며, 실제 계산은 쿼리 실행 시 수행됩니다.
 *
 * ```kotlin
 * val nameExpr = table.payload.extract<String>("$.name")
 * // nameExpr is Extract<String>
 * ```
 *
 * @param path JSON 경로 목록입니다. 비워두면 벤더 기본 루트 경로 규칙을 따릅니다.
 * @param toScalar `true`이면 scalar/text, `false`이면 JSON 객체 모드로 추출합니다.
 * @param serializer 추출 결과를 `T`로 매핑할 때 사용할 Fastjson2 serializer입니다.
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
            { serializer.deserializeFromString<T>(it)!! }
        )
    )
    return Extract(this, path = path, toScalar, this.columnType, columnType)
}
