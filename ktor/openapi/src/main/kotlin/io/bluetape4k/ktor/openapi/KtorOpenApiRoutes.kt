package io.bluetape4k.ktor.openapi

import io.bluetape4k.support.requireNotBlank
import io.ktor.server.plugins.openapi.OpenAPIConfig
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.swagger.SwaggerConfig
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.Route

/**
 * Adds an OpenAPI documentation endpoint backed by Ktor's official OpenAPI plugin.
 *
 * ## Contract
 * - The endpoint is explicit and route-scoped; this function does not install global application plugins.
 * - [swaggerFile] is resolved by Ktor from application resources first and then the file system.
 * - Applications own the OpenAPI document and route metadata lifecycle.
 *
 * ```kotlin
 * routing {
 *     bluetape4kOpenApi(swaggerFile = "openapi/documentation.yaml")
 * }
 * ```
 */
fun Route.bluetape4kOpenApi(
    path: String = DEFAULT_OPENAPI_PATH,
    swaggerFile: String = DEFAULT_OPENAPI_FILE,
    configure: OpenAPIConfig.() -> Unit = {},
): Route {
    path.requireNotBlank("path")
    swaggerFile.requireNotBlank("swaggerFile")
    return openAPI(path = path, swaggerFile = swaggerFile, block = configure)
}

/**
 * Adds a Swagger UI endpoint backed by Ktor's official Swagger UI plugin.
 *
 * ## Contract
 * - The endpoint is explicit and route-scoped.
 * - The OpenAPI document stays caller-owned through [swaggerFile] or [configure].
 * - This helper does not generate route behavior or mutate existing routes.
 *
 * ```kotlin
 * routing {
 *     bluetape4kSwaggerUi(swaggerFile = "openapi/documentation.yaml")
 * }
 * ```
 */
fun Route.bluetape4kSwaggerUi(
    path: String = DEFAULT_SWAGGER_UI_PATH,
    swaggerFile: String = DEFAULT_OPENAPI_FILE,
    configure: SwaggerConfig.() -> Unit = {},
): Route {
    path.requireNotBlank("path")
    swaggerFile.requireNotBlank("swaggerFile")
    return swaggerUI(path = path, swaggerFile = swaggerFile, block = configure)
}

const val DEFAULT_OPENAPI_PATH: String = "openapi"
const val DEFAULT_SWAGGER_UI_PATH: String = "swagger"
const val DEFAULT_OPENAPI_FILE: String = "openapi/documentation.yaml"
