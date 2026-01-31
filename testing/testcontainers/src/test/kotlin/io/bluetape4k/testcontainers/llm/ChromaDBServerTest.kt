package io.bluetape4k.testcontainers.llm

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.When
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ChromaDBServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Nested
    inner class UseDockerPort {
        @Test
        fun `launch chromaDB server`() {
            ChromaDBServer().use { chromadb ->
                chromadb.start()
                assertChromaDBServerIsRunning(chromadb)
            }
        }
    }

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `launch chromaDB server with default port`() {
            ChromaDBServer(useDefaultPort = true).use { chromadb ->
                chromadb.start()
                assertChromaDBServerIsRunning(chromadb)
            }
        }
    }

    private fun assertChromaDBServerIsRunning(chromadb: ChromaDBServer) {

        // test database 를 생성
        given()
            .baseUri(chromadb.endpoint)
            .When {
                body("""{"name":"test"}""")
                contentType(ContentType.JSON)
                post("/api/v1/databases")
            }
            .then()
            .statusCode(200)

        // test database 가 생성되었는지 확인
        given()
            .baseUri(chromadb.endpoint)
            .When {
                get("/api/v1/databases/test")
            }
            .then()
            .statusCode(200)
    }
}
