package io.bluetape4k.ktor.openapi

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.ktor.core.Bluetape4kKtorCoreConfig
import io.bluetape4k.ktor.core.installBluetape4kKtorCore
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.Components
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.ExperimentalKtorApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KtorOpenApiRoutesTest {

    @Test
    fun `openapi endpoint serves static application specification`() = testApplication {
        application {
            installOpenApiTestCore()
            routing {
                bluetape4kOpenApi {
                    outputPath = "build/openapi-test-docs"
                }
            }
        }

        val response = client.get("/openapi")
        val body = response.bodyAsText()

        response.status shouldBeEqualTo HttpStatusCode.OK
        body.contains("Bluetape4k Ktor OpenAPI Test") shouldBeEqualTo true
        body.contains("/healthz") shouldBeEqualTo true
        body.contains("/widgets/{id}") shouldBeEqualTo true
    }

    @Test
    fun `swagger ui endpoint serves static application specification`() = testApplication {
        application {
            installOpenApiTestCore()
            routing {
                bluetape4kSwaggerUi()
            }
        }

        val response = client.get("/swagger")
        val body = response.bodyAsText()

        response.status shouldBeEqualTo HttpStatusCode.OK
        body.contains("swagger") shouldBeEqualTo true
    }

    @Test
    fun `openapi endpoint preserves caller owned document source`() = testApplication {
        application {
            installOpenApiTestCore()
            routing {
                runtimeMetadataRoute()
                bluetape4kOpenApi {
                    info = OpenApiInfo("Runtime Owned OpenAPI Test", "1.0.0")
                    components = Components(schemas = emptyMap())
                    outputPath = "build/openapi-runtime-docs"
                    source = OpenApiDocSource.Routing()
                }
            }
        }

        val response = client.get("/openapi")
        val body = response.bodyAsText()

        response.status shouldBeEqualTo HttpStatusCode.OK
        body.contains("Runtime Owned OpenAPI Test") shouldBeEqualTo true
        body.contains("/runtime") shouldBeEqualTo true
    }

    @Test
    fun `swagger ui endpoint preserves caller owned document source`() = testApplication {
        application {
            installOpenApiTestCore()
            routing {
                runtimeMetadataRoute()
                bluetape4kSwaggerUi {
                    info = OpenApiInfo("Runtime Owned OpenAPI Test", "1.0.0")
                    remotePath = "runtime.json"
                    source = OpenApiDocSource.Routing()
                }
            }
        }

        val response = client.get("/swagger/runtime.json")
        val body = response.bodyAsText()

        response.status shouldBeEqualTo HttpStatusCode.OK
        body.contains("Runtime Owned OpenAPI Test") shouldBeEqualTo true
        body.contains("/runtime") shouldBeEqualTo true
    }

    @Test
    fun `openapi endpoint rejects blank path`() = testApplication {
        application {
            installOpenApiTestCore()
            routing {
                assertFailsWith<IllegalArgumentException> {
                    bluetape4kOpenApi(path = " ")
                }
            }
        }
    }

    @Test
    fun `swagger ui endpoint rejects blank specification file`() = testApplication {
        application {
            installOpenApiTestCore()
            routing {
                assertFailsWith<IllegalArgumentException> {
                    bluetape4kSwaggerUi(swaggerFile = " ")
                }
            }
        }
    }

    private fun io.ktor.server.application.Application.installOpenApiTestCore() {
        installBluetape4kKtorCore(
            Bluetape4kKtorCoreConfig(
                installStatusPages = false,
                installHealthRoutes = false
            )
        )
    }

    @OptIn(ExperimentalKtorApi::class)
    private fun io.ktor.server.routing.Route.runtimeMetadataRoute() {
        get("/runtime") {
            call.respondText("runtime")
        }.describe {
            summary = "Runtime metadata route"
            responses {
                HttpStatusCode.OK {
                    description = "Runtime metadata response"
                }
            }
        }
    }
}
