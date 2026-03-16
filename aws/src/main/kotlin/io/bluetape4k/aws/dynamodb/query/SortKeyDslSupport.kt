package io.bluetape4k.aws.dynamodb.query

/**
 * DynamoDB DSL 에서 SortKey 를 지원하기 위한 클래스
 *
 * [comparisonOperator]는 Enhanced Query DSL에서 [software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional]
 * 생성 분기에 사용됩니다.
 */
data class SortKey(
    val sortKeyName: String = "sortKey",
    val comparisonOperator: DynamoComparator,
): ComparableBuilder

/**
 * SortKey 를 생성하기 위한 빌더 클래스
 */
class SortKeyBuilder(val keyName: String = "sortKey") {
    var comparator: DynamoComparator? = null

    /** 설정된 비교 연산자를 기반으로 [SortKey]를 생성합니다. */
    fun build(): SortKey = SortKey(keyName, comparator!!)
}

/** 정렬 키를 `BETWEEN` 비교식으로 설정합니다. */
fun SortKeyBuilder.between(values: Pair<Any, Any>) {
    comparator = Between(values.first, values.second)
}

/** 정렬 키를 `EQ` 비교식으로 설정합니다. */
infix fun SortKeyBuilder.eq(value: Any) {
    comparator = Equals(value)
}
