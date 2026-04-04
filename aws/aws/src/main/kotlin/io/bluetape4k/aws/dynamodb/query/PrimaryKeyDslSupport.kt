package io.bluetape4k.aws.dynamodb.query

/**
 * DSL 에서 PrimaryKey 를 지원하기 위한 클래스
 *
 * [keyName]은 `QueryRequest.keyConditions`의 키로 사용됩니다.
 *
 * ```kotlin
 * val pk = PrimaryKey(keyName = "userId", equals = Equals("user-1"))
 * // pk.keyName == "userId"
 * ```
 */
@DynamoDslMarker
data class PrimaryKey(val keyName: String = "primaryKey", val equals: Equals)

/**
 * PrimaryKey 를 생성하기 위한 빌더 클래스
 *
 * ```kotlin
 * val builder = PrimaryKeyBuilder("userId")
 * builder eq "user-1"
 * val pk = builder.build()
 * // pk.keyName == "userId"
 * ```
 */
@DynamoDslMarker
class PrimaryKeyBuilder(val keyName: String = "primaryKey") {
    var comparator: Equals? = null

    /** 설정된 비교자를 기반으로 [PrimaryKey]를 생성합니다. */
    fun build(): PrimaryKey = PrimaryKey(keyName, comparator!!)
}

/** 파티션 키 비교식을 `EQ`로 설정합니다. */
infix fun PrimaryKeyBuilder.eq(value: Any) {
    comparator = Equals(value)
}
