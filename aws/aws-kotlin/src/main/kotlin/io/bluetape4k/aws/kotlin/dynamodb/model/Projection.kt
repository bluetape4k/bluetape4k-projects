package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.Projection
import aws.sdk.kotlin.services.dynamodb.model.ProjectionType

/**
 * DSL 블록으로 DynamoDB [Projection]을 빌드합니다.
 *
 * ## 동작/계약
 * - [projectionType] 기본값은 [ProjectionType.All]로, 모든 속성을 프로젝션한다.
 * - [nonKeyAttributes]는 [ProjectionType.Include]일 때만 유효하며, null이면 설정되지 않는다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val projection = projectionOf(ProjectionType.Include, listOf("name", "age"))
 * // projection.projectionType == ProjectionType.Include
 * // projection.nonKeyAttributes == listOf("name", "age")
 * ```
 *
 * @param projectionType 프로젝션 타입 (기본값: [ProjectionType.All])
 * @param nonKeyAttributes 프로젝션에 포함할 비키 속성 목록 ([ProjectionType.Include]일 때 유효)
 */
inline fun projectionOf(
    projectionType: ProjectionType = ProjectionType.All,
    nonKeyAttributes: List<String>? = null,
    crossinline builder: Projection.Builder.() -> Unit = {},
): Projection = Projection {
    this.projectionType = projectionType
    this.nonKeyAttributes = nonKeyAttributes

    builder()
}
