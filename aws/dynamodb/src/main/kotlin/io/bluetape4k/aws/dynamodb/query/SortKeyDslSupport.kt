package io.bluetape4k.aws.dynamodb.query

/**
 * DynamoDB DSL 에서 SortKey 를 지원하기 위한 클래스
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
    fun build(): SortKey = SortKey(keyName, comparator!!)
}

fun SortKeyBuilder.between(values: Pair<Any, Any>) {
    comparator = Between(values.first, values.second)
}

infix fun SortKeyBuilder.eq(value: Any) {
    comparator = Equals(value)
}
