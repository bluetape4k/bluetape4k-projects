package io.bluetape4k.examples.idgenerator.controller

import java.time.Instant

/**
 * Response for a single issued ID.
 *
 * ## Behavior
 * - [type] is the requested generator type.
 * - [id] is the newly issued ID serialized as a string.
 */
data class IdResponse(
    val type: String,
    val id: String,
)

/**
 * Response for a batch of issued IDs.
 *
 * ## Behavior
 * - [size] is the number of IDs actually generated.
 * - [ids] contains generated string IDs in request order.
 */
data class IdBatchResponse(
    val type: String,
    val size: Int,
    val ids: List<String>,
)

/**
 * Response listing supported generators.
 */
data class GeneratorsResponse(
    val generators: List<GeneratorResponse>,
)

/**
 * Description for one generator.
 */
data class GeneratorResponse(
    val type: String,
    val description: String,
)

/**
 * example application health response입니다.
 */
data class HealthResponse(
    val status: String,
    val supportedTypes: Set<String>,
)

/**
 * Response for input validation failures.
 */
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val timestamp: Instant = Instant.now(),
)
