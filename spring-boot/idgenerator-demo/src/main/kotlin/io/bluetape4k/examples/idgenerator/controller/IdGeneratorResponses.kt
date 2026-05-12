package io.bluetape4k.examples.idgenerator.controller

import java.time.Instant

/**
 * 단건 ID 발급 응답입니다.
 *
 * ## 동작/계약
 * - [type]은 요청한 generator type입니다.
 * - [id]는 문자열로 직렬화된 새 ID입니다.
 */
data class IdResponse(
    val type: String,
    val id: String,
)

/**
 * 배치 ID 발급 응답입니다.
 *
 * ## 동작/계약
 * - [size]는 실제 생성된 ID 개수입니다.
 * - [ids]는 요청 순서대로 생성된 문자열 ID 목록입니다.
 */
data class IdBatchResponse(
    val type: String,
    val size: Int,
    val ids: List<String>,
)

/**
 * 지원 generator 목록 응답입니다.
 */
data class GeneratorsResponse(
    val generators: List<GeneratorResponse>,
)

/**
 * 단일 generator 설명 응답입니다.
 */
data class GeneratorResponse(
    val type: String,
    val description: String,
)

/**
 * 예제 애플리케이션 상태 응답입니다.
 */
data class HealthResponse(
    val status: String,
    val supportedTypes: Set<String>,
)

/**
 * 입력 검증 실패 응답입니다.
 */
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val timestamp: Instant = Instant.now(),
)
