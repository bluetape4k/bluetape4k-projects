package io.bluetape4k.testcontainers.infra

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContainAll
import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import java.net.InetSocketAddress
import java.net.Socket

@Tag("integration")
class OpenFgaServerTest: AbstractContainerTest() {

    private lateinit var propertySnapshot: Map<String, String?>

    @BeforeEach
    fun snapshotSystemProperties() {
        propertySnapshot = propertyKeys.associateWith { System.getProperty(it) }
    }

    @AfterEach
    fun restoreSystemProperties() {
        propertyKeys.forEach { key ->
            System.clearProperty(key)
            propertySnapshot[key]?.let { System.setProperty(key, it) }
        }
    }

    @Test
    fun `blank image and tag are rejected`() {
        assertFailsWith<IllegalArgumentException> { OpenFgaServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { OpenFgaServer(tag = " ") }
    }

    @Test
    fun `default configuration is stopped non reusable and exposes both APIs`() {
        val server = OpenFgaServer()

        server.isRunning.shouldBeFalse()
        server.isShouldBeReused.shouldBeFalse()
        server.dockerImageName shouldBeEqualTo "${OpenFgaServer.IMAGE}:${OpenFgaServer.TAG}"
        server.exposedPorts shouldContainSame listOf(OpenFgaServer.HTTP_PORT, OpenFgaServer.GRPC_PORT)
    }

    @Test
    fun `property keys are available before start but properties require start`() {
        val server = OpenFgaServer(reuse = false)

        server.propertyKeys() shouldContainAll setOf("host", "port", "url", "http-port", "grpc-port")
        assertFailsWith<IllegalStateException> { server.properties() }
    }

    @Test
    fun `fixed ports bind the HTTP and gRPC APIs`() {
        OpenFgaServer(useDefaultPort = true, reuse = false).use { server ->
            server.start()

            server.isRunning.shouldBeTrue()
            server.httpPort shouldBeEqualTo OpenFgaServer.HTTP_PORT
            server.grpcPort shouldBeEqualTo OpenFgaServer.GRPC_PORT
            server.port shouldBeEqualTo server.httpPort
            server.url shouldBeEqualTo server.httpEndpoint
            server.url.startsWith("http://").shouldBeTrue()
            server.grpcEndpoint.startsWith("http://").shouldBeTrue()
        }
    }

    @Test
    fun `서버에서 대표 작업을 수행하고 종료한다`() {
        val server = OpenFgaServer(reuse = false)
        server.use {
            it.start()
            assertStartedServer(it)

            var storeId: String? = null
            try {
                storeId = createStore(it.url)
                val modelId = createModel(it.url, storeId)
                writeTuple(it.url, storeId, modelId)
                checkPermission(it.url, storeId, modelId, "user:anne").shouldBeTrue()
                checkPermission(it.url, storeId, modelId, "user:bob").shouldBeFalse()
            } finally {
                storeId?.let { id -> deleteStore(it.url, id) }
            }
        }
        server.isRunning.shouldBeFalse()
    }

    private fun assertStartedServer(server: OpenFgaServer) {
        server.isRunning.shouldBeTrue()
        (server.httpPort > 0).shouldBeTrue()
        (server.grpcPort > 0).shouldBeTrue()
        server.port shouldBeEqualTo server.httpPort
        server.url shouldBeEqualTo server.httpEndpoint
        server.properties() shouldBeEqualTo mapOf(
            "host" to server.host,
            "port" to server.port.toString(),
            "url" to server.url,
            "http-port" to server.httpPort.toString(),
            "grpc-port" to server.grpcPort.toString(),
        )
        server.properties().forEach { (key, value) ->
            System.getProperty("testcontainers.openfga.$key") shouldBeEqualTo value
        }
        Socket().use { socket ->
            socket.connect(InetSocketAddress(server.host, server.grpcPort), 5_000)
            socket.isConnected.shouldBeTrue()
        }
    }

    private fun createStore(baseUri: String): String = given()
        .baseUri(baseUri)
        .contentType(ContentType.JSON)
        .body("""{"name":"bluetape4k-${Base58.randomString(8).lowercase()}"}""")
        .post("/stores")
        .then()
        .statusCode(201)
        .extract()
        .path("id")

    private fun createModel(baseUri: String, storeId: String): String = given()
        .baseUri(baseUri)
        .contentType(ContentType.JSON)
        .body(
            """
            {
              "schema_version": "1.1",
              "type_definitions": [
                {"type": "user"},
                {
                  "type": "document",
                  "relations": {"reader": {"this": {}}},
                  "metadata": {
                    "relations": {
                      "reader": {"directly_related_user_types": [{"type": "user"}]}
                    }
                  }
                }
              ]
            }
            """.trimIndent(),
        )
        .post("/stores/$storeId/authorization-models")
        .then()
        .statusCode(201)
        .extract()
        .path("authorization_model_id")

    private fun writeTuple(baseUri: String, storeId: String, modelId: String) {
        given()
            .baseUri(baseUri)
            .contentType(ContentType.JSON)
            .body(
                mapOf(
                    "writes" to mapOf(
                        "tuple_keys" to listOf(
                            mapOf(
                                "user" to "user:anne",
                                "relation" to "reader",
                                "object" to "document:budget",
                            ),
                        ),
                    ),
                    "authorization_model_id" to modelId,
                ),
            )
            .post("/stores/$storeId/write")
            .then()
            .statusCode(200)
    }

    private fun checkPermission(baseUri: String, storeId: String, modelId: String, user: String): Boolean = given()
        .baseUri(baseUri)
        .contentType(ContentType.JSON)
        .body(
            mapOf(
                "tuple_key" to mapOf(
                    "user" to user,
                    "relation" to "reader",
                    "object" to "document:budget",
                ),
                "authorization_model_id" to modelId,
            ),
        )
        .post("/stores/$storeId/check")
        .then()
        .statusCode(200)
        .extract()
        .path("allowed")

    private fun deleteStore(baseUri: String, storeId: String) {
        given().baseUri(baseUri).delete("/stores/$storeId").then().statusCode(204)
    }

    @Test
    fun `Launcher provides one started shared server`() {
        val first = OpenFgaServer.Launcher.openFga
        val second = OpenFgaServer.Launcher.openFga

        (first === second).shouldBeTrue()
        first.isRunning.shouldBeTrue()
        first.isShouldBeReused.shouldBeFalse()
    }

    private companion object {
        val propertyKeys = setOf(
            "testcontainers.openfga.host",
            "testcontainers.openfga.port",
            "testcontainers.openfga.url",
            "testcontainers.openfga.http-port",
            "testcontainers.openfga.grpc-port",
        )
    }
}
