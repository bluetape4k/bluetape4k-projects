package io.bluetape4k.examples.idgenerator.controller

import io.bluetape4k.examples.idgenerator.service.IdGeneratorRegistry
import io.bluetape4k.examples.idgenerator.service.IdGeneratorService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * idgenerator Spring Boot demo용 REST controller입니다.
 *
 * ## Behavior
 * - The `/ids/{type}` family provides explicit endpoints that are easy to copy.
 * - The `/idgen/{type}` family provides generic endpoints that accept the type as a path variable.
 *
 * ```http
 * GET /ids/uuid-v7
 * GET /idgen/snowflake/batch?size=10
 * ```
 */
@RestController
class IdGeneratorController(
    private val service: IdGeneratorService,
    private val registry: IdGeneratorRegistry,
) {

    @GetMapping("/ids/uuid-v4")
    fun uuidV4(): IdResponse =
        service.generate("uuid-v4")

    @GetMapping("/ids/uuid-v7")
    fun uuidV7(): IdResponse =
        service.generate("uuid-v7")

    @GetMapping("/ids/ulid")
    fun ulid(): IdResponse =
        service.generate("ulid")

    @GetMapping("/ids/ksuid")
    fun ksuid(): IdResponse =
        service.generate("ksuid")

    @GetMapping("/ids/snowflake")
    fun snowflake(): IdResponse =
        service.generate("snowflake")

    @GetMapping("/ids/flake")
    fun flake(): IdResponse =
        service.generate("flake")

    @GetMapping("/ids/{type}/batch")
    fun batch(
        @PathVariable type: String,
        @RequestParam(required = false) size: Int?,
    ): IdBatchResponse =
        service.generateBatch(type, size)

    @GetMapping("/idgen/{type}")
    fun generate(
        @PathVariable type: String,
    ): IdResponse =
        service.generate(type)

    @GetMapping("/idgen/{type}/batch")
    fun generateBatch(
        @PathVariable type: String,
        @RequestParam(required = false) size: Int?,
    ): IdBatchResponse =
        service.generateBatch(type, size)

    @GetMapping("/generators")
    fun generators(): GeneratorsResponse =
        service.generators()

    @GetMapping("/health")
    fun health(): HealthResponse =
        HealthResponse(status = "UP", supportedTypes = registry.supportedTypes)
}
