package io.bluetape4k.testcontainers.storage

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContainAll
import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.qdrant.client.ConditionFactory.matchKeyword
import io.qdrant.client.PointIdFactory.id
import io.qdrant.client.QdrantClient
import io.qdrant.client.QdrantGrpcClient
import io.qdrant.client.QueryFactory.nearest
import io.qdrant.client.ValueFactory.value
import io.qdrant.client.VectorsFactory.vectors
import io.qdrant.client.grpc.Collections.Distance
import io.qdrant.client.grpc.Collections.VectorParams
import io.qdrant.client.grpc.Common.Filter
import io.qdrant.client.grpc.Points.DeletePoints
import io.qdrant.client.grpc.Points.PointStruct
import io.qdrant.client.grpc.Points.PointsSelector
import io.qdrant.client.grpc.Points.QueryPoints
import io.qdrant.client.grpc.Points.UpsertPoints
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

@Tag("integration")
class QdrantServerTest: AbstractContainerTest() {

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
        assertFailsWith<IllegalArgumentException> { QdrantServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { QdrantServer(tag = " ") }
    }

    @Test
    fun `default configuration is stopped non reusable and exposes both APIs`() {
        val server = QdrantServer()

        server.isRunning.shouldBeFalse()
        server.isShouldBeReused.shouldBeFalse()
        server.dockerImageName shouldBeEqualTo "${QdrantServer.IMAGE}:${QdrantServer.TAG}"
        server.exposedPorts shouldContainSame listOf(QdrantServer.HTTP_PORT, QdrantServer.GRPC_PORT)
    }

    @Test
    fun `property keys are available before start but properties require start`() {
        val server = QdrantServer(reuse = false)

        server.propertyKeys() shouldContainAll setOf("host", "port", "url", "http-port", "grpc-port")
        assertFailsWith<IllegalStateException> { server.properties() }
    }

    @Test
    fun `fixed ports bind the HTTP and gRPC APIs`() {
        QdrantServer(useDefaultPort = true, reuse = false).use { server ->
            server.start()

            server.isRunning.shouldBeTrue()
            server.httpPort shouldBeEqualTo QdrantServer.HTTP_PORT
            server.grpcPort shouldBeEqualTo QdrantServer.GRPC_PORT
            server.port shouldBeEqualTo server.httpPort
            server.url.startsWith("http://").shouldBeTrue()
            server.grpcHostAddress shouldBeEqualTo "${server.host}:${server.grpcPort}"
        }
    }

    @Test
    fun `서버에서 대표 작업을 수행하고 종료한다`() {
        val server = QdrantServer()
        server.use {
            server.start()
            assertStartedServer(server)
            val collection = "bluetape4k_${Base58.randomString(8).lowercase()}"
            val grpcClient = QdrantGrpcClient.newBuilder(server.host, server.grpcPort, false).build()
            QdrantClient(grpcClient).use { client ->
                client.createCollectionAsync(
                    collection,
                    VectorParams.newBuilder().setSize(3).setDistance(Distance.Cosine).build(),
                ).get(30, TimeUnit.SECONDS)
                try {
                    verifyFilteredQuery(client, collection)
                } finally {
                    client.deleteCollectionAsync(collection).get(30, TimeUnit.SECONDS)
                }
            }
        }
        server.isRunning.shouldBeFalse()
    }

    private fun assertStartedServer(server: QdrantServer) {
        server.isRunning.shouldBeTrue()
        (server.httpPort > 0).shouldBeTrue()
        (server.grpcPort > 0).shouldBeTrue()
        server.port shouldBeEqualTo server.httpPort
        server.properties() shouldBeEqualTo mapOf(
            "host" to server.host,
            "port" to server.port.toString(),
            "url" to server.url,
            "http-port" to server.httpPort.toString(),
            "grpc-port" to server.grpcPort.toString(),
        )
        server.properties().forEach { (key, value) ->
            System.getProperty("testcontainers.qdrant.$key") shouldBeEqualTo value
        }
        Socket().use { socket ->
            socket.connect(InetSocketAddress(server.host, server.grpcPort), 5_000)
            socket.isConnected.shouldBeTrue()
        }
    }

    private fun verifyFilteredQuery(client: QdrantClient, collection: String) {
        val points = listOf(
            PointStruct.newBuilder()
                .setId(id(1L))
                .setVectors(vectors(1f, 0f, 0f))
                .putPayload("tenant", value("alpha"))
                .build(),
            PointStruct.newBuilder()
                .setId(id(2L))
                .setVectors(vectors(0f, 1f, 0f))
                .putPayload("tenant", value("beta"))
                .build(),
        )
        client.upsertAsync(
            UpsertPoints.newBuilder()
                .setCollectionName(collection)
                .setWait(true)
                .addAllPoints(points)
                .build(),
        ).get(30, TimeUnit.SECONDS)

        val filter = Filter.newBuilder().addMust(matchKeyword("tenant", "alpha")).build()
        val query = QueryPoints.newBuilder()
            .setCollectionName(collection)
            .setQuery(nearest(1f, 0f, 0f))
            .setFilter(filter)
            .setLimit(10)
            .build()

        client.queryAsync(query).get(30, TimeUnit.SECONDS).map { it.id.num } shouldBeEqualTo listOf(1L)

        client.deleteAsync(
            DeletePoints.newBuilder()
                .setCollectionName(collection)
                .setWait(true)
                .setPoints(PointsSelector.newBuilder().setFilter(filter))
                .build(),
        ).get(30, TimeUnit.SECONDS)

        client.queryAsync(query).get(30, TimeUnit.SECONDS).shouldBeEmpty()
    }

    @Test
    fun `Launcher provides one started shared server`() {
        val first = QdrantServer.Launcher.qdrant
        val second = QdrantServer.Launcher.qdrant

        (first === second).shouldBeTrue()
        first.isRunning.shouldBeTrue()
        first.isShouldBeReused.shouldBeFalse()
    }

    private companion object {
        val propertyKeys = setOf(
            "testcontainers.qdrant.host",
            "testcontainers.qdrant.port",
            "testcontainers.qdrant.url",
            "testcontainers.qdrant.http-port",
            "testcontainers.qdrant.grpc-port",
        )
    }
}
