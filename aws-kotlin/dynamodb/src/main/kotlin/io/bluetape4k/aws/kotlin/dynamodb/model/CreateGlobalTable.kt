package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.CreateGlobalTableRequest
import aws.sdk.kotlin.services.dynamodb.model.Replica
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 DynamoDB [CreateGlobalTableRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [globalTableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [replicationGroup]이 null이면 복제 그룹 없이 요청이 생성된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = createGlobalTableRequestOf(
 *     globalTableName = "global-users",
 *     replicationGroup = listOf(replicaOf("us-east-1"), replicaOf("ap-northeast-2"))
 * )
 * // req.globalTableName == "global-users"
 * // req.replicationGroup?.size == 2
 * ```
 *
 * @param globalTableName 생성할 글로벌 테이블 이름 (blank이면 예외)
 * @param replicationGroup 복제할 리전의 [Replica] 목록
 */
inline fun createGlobalTableRequestOf(
    globalTableName: String,
    replicationGroup: List<Replica>? = null,
    @BuilderInference crossinline builder: CreateGlobalTableRequest.Builder.() -> Unit = {},
): CreateGlobalTableRequest {
    globalTableName.requireNotBlank("globalTableName")

    return CreateGlobalTableRequest {
        this.globalTableName = globalTableName
        this.replicationGroup = replicationGroup

        builder()
    }
}
