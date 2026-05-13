package io.bluetape4k.examples.idgenerator.service

import io.bluetape4k.examples.idgenerator.config.IdGeneratorProperties
import io.bluetape4k.examples.idgenerator.controller.GeneratorResponse
import io.bluetape4k.examples.idgenerator.controller.GeneratorsResponse
import io.bluetape4k.examples.idgenerator.controller.IdBatchResponse
import io.bluetape4k.examples.idgenerator.controller.IdResponse
import org.springframework.stereotype.Service

/**
 * Handles the application use cases for the ID issuing API.
 *
 * ## Behavior
 * - Single ID issuing invokes the selected registry generator once.
 * - Batch issuing validates the size against `1..maxBatchSize`, then generates the requested number of IDs.
 *
 * ```kotlin
 * val response = idGeneratorService.generate("uuid-v7")
 * ```
 */
@Service
class IdGeneratorService(
    private val registry: IdGeneratorRegistry,
    private val properties: IdGeneratorProperties,
) {

    fun generate(type: String): IdResponse {
        val entry = registry.get(type)
        return IdResponse(type = entry.type, id = entry.nextId())
    }

    fun generateBatch(type: String, size: Int?): IdBatchResponse {
        val entry = registry.get(type)
        val batchSize = validateBatchSize(size ?: properties.defaultBatchSize)
        val ids = List(batchSize) { entry.nextId() }

        return IdBatchResponse(type = entry.type, size = ids.size, ids = ids)
    }

    fun generators(): GeneratorsResponse =
        GeneratorsResponse(
            generators =
                registry
                    .all()
                    .map { entry ->
                        GeneratorResponse(type = entry.type, description = entry.description)
                    },
        )

    private fun validateBatchSize(size: Int): Int {
        if (size !in 1..properties.maxBatchSize) {
            throw InvalidBatchSizeException(size, properties.maxBatchSize)
        }
        return size
    }
}

/**
 * Exception raised when the requested batch size is outside the example API range.
 */
class InvalidBatchSizeException(
    val batchSize: Int,
    val maxBatchSize: Int,
): IllegalArgumentException("Batch size must be between 1 and $maxBatchSize: $batchSize")
