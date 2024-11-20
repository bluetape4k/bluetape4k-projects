package io.bluetape4k.aws.dynamodb.model

import software.amazon.awssdk.services.dynamodb.model.Projection
import software.amazon.awssdk.services.dynamodb.model.ProjectionType

/**
 * DynamoDB 테이블의 프로젝션 설정을 생성합니다.
 *
 * ```
 * val projection = projection {
 *    projectionType(ProjectionType.ALL)
 *    nonKeyAttributes("name", "age")
 * }
 * ```
 *
 * @return [Projection] 인스턴스
 */
inline fun Projection(initializer: Projection.Builder.() -> Unit): Projection {
    return Projection.builder().apply(initializer).build()
}

/**
 * DynamoDB 테이블의 프로젝션 설정을 생성합니다.
 *
 * ```
 * val projection = projectionOf(ProjectionType.ALL, listOf("name", "age"))
 * ```
 *
 * @param projectionType 프로젝션 타입
 * @param nonKeyAttrs 프로젝션에 포함할 속성 목록
 *
 * @return [Projection] 인스턴스
 */
fun projectionOf(
    projectionType: ProjectionType = ProjectionType.ALL,
    nonKeyAttrs: Collection<String>? = null,
): Projection {
    return Projection {
        projectionType(projectionType)
        nonKeyAttributes(nonKeyAttrs)
    }
}

/**
 * DynamoDB 테이블의 프로젝션 설정을 생성합니다.
 *
 * ```
 * val projection = projectionOf("ALL", listOf("name", "age"))
 * ```
 *
 * @param projectionType 프로젝션 타입을 나타내는 문자열
 * @param nonKeyAttrs 프로젝션에 포함할 속성 목록
 *
 * @return [Projection] 인스턴스
 */
fun projectionOf(
    projectionType: String,
    nonKeyAttrs: Collection<String>? = null,
): Projection {
    return Projection {
        projectionType(projectionType)
        nonKeyAttributes(nonKeyAttrs)
    }
}
