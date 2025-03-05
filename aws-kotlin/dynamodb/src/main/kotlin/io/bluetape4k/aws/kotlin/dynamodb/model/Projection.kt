package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.Projection
import aws.sdk.kotlin.services.dynamodb.model.ProjectionType

/**
 * DynamoDb 테이블의 Projection 설정을 생성합니다.
 *
 * ```
 * val projection = projectionOf(ProjectionType.ALL, listOf("name", "age"))
 * ```
 *
 * @param projectionType 프로젝션 타입
 * @param nonKeyAttributes 프로젝션에 포함할 속성 목록
 * @param configurer 프로젝션 설정
 * @return [Projection] 인스턴스
 */
inline fun projectionOf(
    projectionType: ProjectionType,
    nonKeyAttributes: List<String>? = null,
    crossinline configurer: Projection.Builder.() -> Unit = {},
): Projection = Projection {
    this.projectionType = projectionType
    this.nonKeyAttributes = nonKeyAttributes
    configurer()
}
