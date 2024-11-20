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
 * @return [Projection] 인스턴스
 */
fun projectionOf(
    projectionType: ProjectionType,
    nonKeyAttributes: List<String>? = null,
): Projection = Projection.invoke {
    this.projectionType = projectionType
    this.nonKeyAttributes = nonKeyAttributes
}
