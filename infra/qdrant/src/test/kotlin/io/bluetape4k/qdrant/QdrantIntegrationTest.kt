package io.bluetape4k.qdrant

import io.bluetape4k.utils.ShutdownQueue
import io.qdrant.client.ConditionFactory.matchKeyword
import io.qdrant.client.PointIdFactory.id
import io.qdrant.client.QdrantClient
import io.qdrant.client.QdrantGrpcClient
import io.qdrant.client.QueryFactory.nearest
import io.qdrant.client.ValueFactory.value
import io.qdrant.client.VectorFactory.vector
import io.qdrant.client.VectorsFactory.namedVectors
import io.qdrant.client.VectorsFactory.vectors
import io.qdrant.client.grpc.Collections.Distance
import io.qdrant.client.grpc.Collections.VectorParams
import io.qdrant.client.grpc.Points.DeletePoints
import io.qdrant.client.grpc.Common.Filter
import io.qdrant.client.grpc.Points.PointStruct
import io.qdrant.client.grpc.Points.PointsSelector
import io.qdrant.client.grpc.Points.QueryPoints
import io.qdrant.client.grpc.Points.ScrollPoints
import io.qdrant.client.grpc.Points.UpsertPoints
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertInstanceOf
import io.grpc.StatusRuntimeException
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

@Tag("integration")
class QdrantIntegrationTest {

    companion object {
        // 서버 endpoint만 공유하며 SDK 클라이언트와 collection은 테스트에서 소유합니다.
        private val server by lazy {
            GenericContainer("qdrant/qdrant:v1.19.0")
                .withExposedPorts(6333, 6334)
                .waitingFor(Wait.forHttp("/readyz").forPort(6333))
                .withStartupTimeout(Duration.ofMinutes(2))
                .apply { start(); ShutdownQueue.register(this) }
        }
    }

    @Test
    fun `real server supports filtered query paginated scroll and deletion`() = runTest {
        val name = "coroutines_${UUID.randomUUID().toString().replace("-", "")}"
        val grpcClient = QdrantGrpcClient.newBuilder(server.host, server.getMappedPort(6334), false).build()
        QdrantClient(grpcClient).use { client ->
            val vectorParams = VectorParams.newBuilder().setSize(3).setDistance(Distance.Cosine).build()
            client.createCollectionAsync(name, vectorParams)
                .get(30, TimeUnit.SECONDS)
            try {
                val template = UpsertPoints.newBuilder().setCollectionName(name).setWait(true).build()
                val points = (1L..5L).map { number ->
                    PointStruct.newBuilder().setId(id(number)).setVectors(vectors(1f, 0f, 0f))
                        .putPayload("tenant", value(if (number <= 3) "alpha" else "beta")).build()
                }
                (client.upsertBatches(points.asFlow(), template, maxBatchItems = 2).toList().size) shouldBeEqualTo 3
                val filter = Filter.newBuilder().addMust(matchKeyword("tenant", "alpha")).build()
                val query = QueryPoints.newBuilder().setCollectionName(name).setQuery(nearest(1f, 0f, 0f))
                    .setFilter(filter).setLimit(10).build()
                (client.querySuspending(query).map { it.id.num }.toSet()) shouldBeEqualTo setOf(1L, 2L, 3L)
                val scroll = ScrollPoints.newBuilder().setCollectionName(name).setFilter(filter).setLimit(1).build()
                (client.scrollAsFlow(scroll).toList().map { it.id.num }) shouldBeEqualTo listOf(1L, 2L, 3L)
                client.deleteSuspending(DeletePoints.newBuilder().setCollectionName(name).setWait(true)
                    .setPoints(PointsSelector.newBuilder().setFilter(filter)).build())
                (client.scrollAsFlow(scroll).toList().map { it.id.num }) shouldBeEqualTo emptyList<Long>()
            } finally {
                client.deleteCollectionAsync(name).get(30, TimeUnit.SECONDS)
            }
        }
    }

    @Test
    fun `UUID IDs named vectors and dimension errors preserve SDK semantics`() = runTest {
        val name = "named_${UUID.randomUUID().toString().replace("-", "")}"
        val grpcClient = QdrantGrpcClient.newBuilder(server.host, server.getMappedPort(6334), false).build()
        QdrantClient(grpcClient).use { client ->
            val params = VectorParams.newBuilder().setSize(3).setDistance(Distance.Cosine).build()
            client.createCollectionAsync(name, mapOf("embedding" to params)).get(30, TimeUnit.SECONDS)
            try {
                val uuid = UUID.randomUUID()
                val point = PointStruct.newBuilder().setId(id(uuid))
                    .setVectors(namedVectors(mapOf("embedding" to vector(1f, 0f, 0f)))).build()
                client.upsertSuspending(UpsertPoints.newBuilder().setCollectionName(name)
                    .setWait(true).addPoints(point).build())
                val query = QueryPoints.newBuilder().setCollectionName(name).setUsing("embedding")
                    .setQuery(nearest(1f, 0f, 0f)).setLimit(1).build()
                client.querySuspending(query).single().id.uuid shouldBeEqualTo uuid.toString()
                val failure = runCatching {
                    client.querySuspending(query.toBuilder().setQuery(nearest(1f, 0f)).build())
                }.exceptionOrNull()
                assertInstanceOf(StatusRuntimeException::class.java, failure)
            } finally {
                client.deleteCollectionAsync(name).get(30, TimeUnit.SECONDS)
            }
        }
    }
}
