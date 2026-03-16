package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ScanRequest
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 DynamoDB [ScanRequest]를 빌드합니다 ([AttributeValue] 시작 키 오버로드).
 *
 * ## 동작/계약
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [exclusiveStartKey]를 지정하면 해당 키 이후부터 페이지네이션이 시작된다.
 * - [builder] 블록으로 필터 표현식, 세그먼트 등 추가 필드를 설정할 수 있다.
 *
 * ```kotlin
 * val req = scanRequestOf(
 *     tableName = "users",
 *     attributesToGet = listOf("id", "name"),
 *     indexName = "status-index"
 * )
 * // req.tableName == "users"
 * // req.indexName == "status-index"
 * ```
 *
 * @param tableName 스캔할 DynamoDB 테이블 이름 (blank이면 예외)
 * @param attributesToGet 반환할 속성 이름 목록 (레거시 방식, 프로젝션 표현식 권장)
 * @param exclusiveStartKey 페이지네이션 시작 기준 키
 * @param indexName 스캔할 보조 인덱스 이름
 */
@JvmName("scanRequestOfAttributeValue")
inline fun scanRequestOf(
    tableName: String,
    attributesToGet: List<String>? = null,
    exclusiveStartKey: Map<String, AttributeValue>? = null,
    indexName: String? = null,
    @BuilderInference crossinline builder: ScanRequest.Builder.() -> Unit = {},
): ScanRequest {
    tableName.requireNotBlank("tableName")

    return ScanRequest {
        this.tableName = tableName
        this.attributesToGet = attributesToGet
        this.exclusiveStartKey = exclusiveStartKey
        this.indexName = indexName

        builder()
    }
}

/**
 * DSL 블록으로 DynamoDB [ScanRequest]를 빌드합니다 (Any? 시작 키 오버로드).
 *
 * ## 동작/계약
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [exclusiveStartKey]의 각 값은 [toAttributeValueMap]을 통해 [AttributeValue]로 자동 변환된다.
 * - [builder] 블록으로 추가 필드를 설정할 수 있다.
 *
 * ```kotlin
 * val req = scanRequestOf(
 *     tableName = "users",
 *     exclusiveStartKey = mapOf("id" to "u100")
 * )
 * // req.exclusiveStartKey?.get("id") == AttributeValue.S("u100")
 * ```
 *
 * @param tableName 스캔할 DynamoDB 테이블 이름 (blank이면 예외)
 * @param attributesToGet 반환할 속성 이름 목록
 * @param exclusiveStartKey 페이지네이션 시작 기준 키 (자동으로 [AttributeValue]로 변환)
 * @param indexName 스캔할 보조 인덱스 이름
 */
@JvmName("scanRequestOfAny")
inline fun scanRequestOf(
    tableName: String,
    attributesToGet: List<String>? = null,
    exclusiveStartKey: Map<String, Any?>? = null,
    indexName: String? = null,
    @BuilderInference crossinline builder: ScanRequest.Builder.() -> Unit = {},
): ScanRequest {
    tableName.requireNotBlank("tableName")

    return scanRequestOf(
        tableName,
        attributesToGet,
        exclusiveStartKey?.toAttributeValueMap(),
        indexName,
        builder
    )
}
