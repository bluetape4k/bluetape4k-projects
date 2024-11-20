package io.bluetape4k.aws.dynamodb.query

/**
 * DSL 에서 PrimaryKey 를 지원하기 위한 클래스
 */
@DynamoDslMarker
data class PrimaryKey(val keyName: String = "primaryKey", val equals: Equals)

/**
 * PrimaryKey 를 생성하기 위한 빌더 클래스
 */
@DynamoDslMarker
class PrimaryKeyBuilder(val keyName: String = "primaryKey") {
    var comparator: Equals? = null
    fun build(): PrimaryKey = PrimaryKey(keyName, comparator!!)
}

infix fun PrimaryKeyBuilder.eq(value: Any) {
    comparator = Equals(value)
}
