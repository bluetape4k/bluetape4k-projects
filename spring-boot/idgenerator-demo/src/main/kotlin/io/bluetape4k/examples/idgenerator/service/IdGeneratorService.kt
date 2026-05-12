package io.bluetape4k.examples.idgenerator.service

import io.bluetape4k.examples.idgenerator.config.IdGeneratorProperties
import io.bluetape4k.examples.idgenerator.controller.GeneratorResponse
import io.bluetape4k.examples.idgenerator.controller.GeneratorsResponse
import io.bluetape4k.examples.idgenerator.controller.IdBatchResponse
import io.bluetape4k.examples.idgenerator.controller.IdResponse
import org.springframework.stereotype.Service

/**
 * ID 발급 API의 애플리케이션 유스케이스를 담당합니다.
 *
 * ## 동작/계약
 * - 단건 발급은 registry의 generator를 한 번 호출합니다.
 * - 배치 발급은 `1..maxBatchSize` 범위로 검증한 뒤 요청 수만큼 ID를 생성합니다.
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
 * batch size가 예제 API의 허용 범위를 벗어났을 때 발생하는 예외입니다.
 */
class InvalidBatchSizeException(
    val batchSize: Int,
    val maxBatchSize: Int,
): IllegalArgumentException("Batch size must be between 1 and $maxBatchSize: $batchSize")
