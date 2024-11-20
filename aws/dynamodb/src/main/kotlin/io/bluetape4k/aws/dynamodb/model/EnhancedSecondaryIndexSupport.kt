package io.bluetape4k.aws.dynamodb.model

import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedLocalSecondaryIndex
import software.amazon.awssdk.services.dynamodb.model.Projection

/**
 * [EnhancedGlobalSecondaryIndex] 인스턴스를 생성합니다.
 *
 * ```
 * val index = EnhancedGlobalSecondaryIndex {
 *   indexName("indexName")
 *   projection(projection)
 *   // ...
 * }
 * ```
 * @param initializer EnhancedGlobalSecondaryIndex.Builder 초기화 람다
 * @return [EnhancedGlobalSecondaryIndex] 인스턴스
 */
inline fun EnhancedGlobalSecondaryIndex(
    initializer: EnhancedGlobalSecondaryIndex.Builder.() -> Unit,
): EnhancedGlobalSecondaryIndex {
    return EnhancedGlobalSecondaryIndex.builder().apply(initializer).build()
}

/**
 * [EnhancedGlobalSecondaryIndex] 인스턴스를 생성합니다.
 *
 * ```
 * val index = enhancedGlobalSecondaryIndexOf("indexName", projection)
 * ```
 *
 * @param indexName 인덱스 이름
 * @param projection 프로젝션 설정
 * @return [EnhancedGlobalSecondaryIndex] 인스턴스
 */
fun enhancedGlobalSecondaryIndexOf(
    indexName: String,
    projection: Projection,
): EnhancedGlobalSecondaryIndex = EnhancedGlobalSecondaryIndex {
    indexName(indexName)
    projection(projection)
}

/**
 * [EnhancedLocalSecondaryIndex] 인스턴스를 생성합니다.
 *
 * ```
 * val index = EnhancedLocalSecondaryIndex {
 *   indexName("indexName")
 *   projection(projection)
 *   // ...
 * }
 * ```
 * @param initializer EnhancedLocalSecondaryIndex.Builder 초기화 람다
 * @return [EnhancedLocalSecondaryIndex] 인스턴스
 */
inline fun EnhancedLocalSecondaryIndex(
    initializer: EnhancedLocalSecondaryIndex.Builder.() -> Unit,
): EnhancedLocalSecondaryIndex {
    return EnhancedLocalSecondaryIndex.builder().apply(initializer).build()
}

/**
 * [EnhancedLocalSecondaryIndex] 인스턴스를 생성합니다.
 *
 * ```
 * val index = enhancedLocalSecondaryIndexOf("indexName", projection)
 * ```
 *
 * @param indexName 인덱스 이름
 * @param projection 프로젝션 설정
 * @return [EnhancedLocalSecondaryIndex] 인스턴스
 */
fun enhancedLocalSecondaryIndexOf(
    indexName: String,
    projection: Projection,
): EnhancedLocalSecondaryIndex = EnhancedLocalSecondaryIndex {
    indexName(indexName)
    projection(projection)
}
