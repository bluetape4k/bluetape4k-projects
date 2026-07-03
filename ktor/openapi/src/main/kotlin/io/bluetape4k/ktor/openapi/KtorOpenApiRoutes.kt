package io.bluetape4k.ktor.openapi

import io.bluetape4k.support.requireNotBlank
import io.ktor.server.plugins.openapi.OpenAPIConfig
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.swagger.SwaggerConfig
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.Route

/**
 * Adds an OpenAPI documentation endpoint backed by Ktor's official OpenAPI plugin.
 *
 * ## Contract
 * - The endpoint is explicit and route-scoped; this function does not install global application plugins.
 * - [swaggerFile] is used when [configure] leaves Ktor's default document source unchanged.
 * - A caller-owned [OpenAPIConfig.source] from [configure] is preserved for routing-tree or generated metadata.
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
    val checkedSwaggerFile = swaggerFile.requireNotBlank("swaggerFile")
    return openAPI(path = path) {
        configure()
        if (source.isDefaultDocumentSource()) {
            source = OpenApiDocSource.File(checkedSwaggerFile)
        }
    }
}

/**
 * Adds a Swagger UI endpoint backed by Ktor's official Swagger UI plugin.
 *
 * ## Contract
 * - The endpoint is explicit and route-scoped.
 * - [swaggerFile] is used as both the document source and Swagger UI remote path when [configure] leaves
 *   Ktor's default document source unchanged.
 * - Nested relative specification paths, such as `openapi/documentation.yaml`, are preserved.
 * - A caller-owned [SwaggerConfig.source] from [configure] is preserved for routing-tree or generated metadata.
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
    val checkedSwaggerFile = swaggerFile.requireNotBlank("swaggerFile")
    return swaggerUI(path = path) {
        configure()
        if (source.isDefaultDocumentSource()) {
            source = OpenApiDocSource.File(checkedSwaggerFile)
            remotePath = checkedSwaggerFile
        }
    }
}

const val DEFAULT_OPENAPI_PATH: String = "openapi"
const val DEFAULT_SWAGGER_UI_PATH: String = "swagger"
const val DEFAULT_OPENAPI_FILE: String = "openapi/documentation.yaml"

private fun OpenApiDocSource.isDefaultDocumentSource(): Boolean =
    this is OpenApiDocSource.FirstOf &&
        options.size == 2 &&
        options[0].isFileSource(DEFAULT_OPENAPI_FILE) &&
        options[1] is OpenApiDocSource.Routing

private fun OpenApiDocSource.isFileSource(path: String): Boolean =
    this is OpenApiDocSource.File && this.path == path
