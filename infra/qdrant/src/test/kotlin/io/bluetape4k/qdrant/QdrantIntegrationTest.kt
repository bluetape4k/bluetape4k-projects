package io.bluetape4k.qdrant

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.qdrant.client.grpc.deletePointsOf
import io.bluetape4k.qdrant.client.grpc.filter
import io.bluetape4k.qdrant.client.grpc.pointIdOf
import io.bluetape4k.qdrant.client.grpc.pointStruct
import io.bluetape4k.qdrant.client.grpc.pointsSelector
import io.bluetape4k.qdrant.client.grpc.queryPoints
import io.bluetape4k.qdrant.client.grpc.scrollPoints
import io.bluetape4k.qdrant.client.grpc.upsertPointsOf
import io.bluetape4k.qdrant.client.grpc.vectorParams
import io.bluetape4k.qdrant.client.qdrantGrpcClient
import io.bluetape4k.testcontainers.storage.QdrantServer
import io.grpc.StatusRuntimeException
import io.qdrant.client.ConditionFactory.matchKeyword
import io.qdrant.client.QdrantClient
import io.qdrant.client.QueryFactory.nearest
import io.qdrant.client.ValueFactory.value
import io.qdrant.client.VectorFactory.vector
import io.qdrant.client.VectorsFactory.namedVectors
import io.qdrant.client.VectorsFactory.vectors
import io.qdrant.client.grpc.Collections.Distance
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.TimeUnit

@Tag("integration")
class QdrantIntegrationTest {

    companion object: KLogging() {
        // 서버 endpoint만 공유하며 SDK 클라이언트와 collection은 테스트에서 소유합니다.
        private val server by lazy { QdrantServer.Launcher.qdrant }
    }


    @Test
    fun `real server supports filtered query paginated scroll and deletion`() = runSuspendIO {
        val name = "coroutines_${Base58.randomString(8)}"
        val grpcClient = qdrantGrpcClient(server.host, server.grpcPort)

        QdrantClient(grpcClient).use { client ->
            val vectorParams = vectorParams { size = 3; distance = Distance.Cosine }
            client.createCollectionAsync(name, vectorParams).get(30, TimeUnit.SECONDS)

            try {
                val template = upsertPointsOf(name)
                val points = List(5) { number ->
                    pointStruct {
                        this.id = pointIdOf(number.toLong() + 1L)
                        this.vectors = vectors(1f, 0f, 0f)
                        putPayload("tenant", value(if (number <= 2) "alpha" else "beta"))
                    }
                }

                client.upsertBatches(
                    points.asFlow(), template, maxBatchItems = 2
                ).toList() shouldHaveSize 3

                val filter = filter { addMust(matchKeyword("tenant", "alpha")) }
                val query = queryPoints {
                    this.collectionName = name
                    this.query = nearest(1f, 0f, 0f)
                    this.filter = filter
                    this.limit = 10
                }

                client.querySuspending(query).map { it.id.num }.toSet() shouldBeEqualTo setOf(1L, 2L, 3L)

                val scroll = scrollPoints {
                    this.collectionName = name
                    this.filter = filter
                    this.limit = 1
                }

                client.scrollAsFlow(scroll).toList().map { it.id.num } shouldBeEqualTo listOf(1L, 2L, 3L)

                client.deleteSuspending(
                    deletePointsOf(name, true) {
                        this.points = pointsSelector { this.filter = filter }
                    })
                client.scrollAsFlow(scroll).toList().map { it.id.num }.shouldBeEmpty()
            } finally {
                client.deleteCollectionAsync(name).get(30, TimeUnit.SECONDS)
            }
        }
    }

    @Test
    fun `UUID IDs named vectors and dimension errors preserve SDK semantics`() = runSuspendIO {
        val name = "named_${Base58.randomString(8)}"
        val grpcClient = qdrantGrpcClient(server.host, server.grpcPort)

        QdrantClient(grpcClient).use { client ->
            val params = vectorParams { size = 3; distance = Distance.Cosine }
            client
                .createCollectionAsync(
                    name,
                    mapOf("embedding" to params)
                )
                .get(30, TimeUnit.SECONDS)

            try {
                val uuid = UUID.randomUUID()
                val point = pointStruct {
                    this.id = pointIdOf(uuid)
                    this.vectors = namedVectors(mapOf("embedding" to vector(1f, 0f, 0f)))
                }
                client.upsertSuspending(
                    upsertPointsOf(name, true) {
                        addPoints(point)
                    })

                val query = queryPoints {
                    this.collectionName = name
                    this.using = "embedding"
                    this.query = nearest(1f, 0f, 0f)
                    this.limit = 1
                }
                client.querySuspending(query).single().id.uuid shouldBeEqualTo uuid.toString()

                assertFailsWith<StatusRuntimeException> {
                    client.querySuspending(query.toBuilder().setQuery(nearest(1f, 0f)).build())
                }
            } finally {
                client.deleteCollectionAsync(name).get(30, TimeUnit.SECONDS)
            }
        }
    }
}
