package io.bluetape4k.examples.ktor.idgenerator

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.ktor.testing.decodeJsonBody
import io.bluetape4k.ktor.testing.shouldHaveStatus
import io.bluetape4k.utils.Runtimex
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdGeneratorKtorApplicationTest {

    companion object {
        private val ExplicitTypes = listOf(
            "uuid-v4",
            "uuid-v7",
            "ulid",
            "ksuid",
            "snowflake",
            "flake"
        )
        private const val BATCH_SIZE = 7
        private const val CONCURRENCY_COUNT = 512
    }

    @Test
    fun `all explicit single id endpoints return non blank ids`() = testApplication {
        application {
            idGeneratorKtorModule()
        }

        ExplicitTypes.forEach { type ->
            val response = client.get("/ids/$type")
            response shouldHaveStatus HttpStatusCode.OK

            val body = response.decodeJsonBody<IdResponse>()
            body.type shouldBeEqualTo type
            body.id.isNotBlank() shouldBeEqualTo true
        }
    }

    @Test
    fun `all explicit batch endpoints return requested unique ids`() = testApplication {
        application {
            idGeneratorKtorModule()
        }

        ExplicitTypes.forEach { type ->
            val response = client.get("/ids/$type/batch?size=$BATCH_SIZE")
            response shouldHaveStatus HttpStatusCode.OK

            val body = response.decodeJsonBody<IdBatchResponse>()
            body.type shouldBeEqualTo type
            body.size shouldBeEqualTo BATCH_SIZE
            body.ids shouldHaveSize BATCH_SIZE
            body.ids.distinct() shouldHaveSize BATCH_SIZE
            body.ids.all { it.isNotBlank() } shouldBeEqualTo true
        }
    }

    @Test
    fun `generic idgen routes use the same registry as explicit routes`() = testApplication {
        val counter = AtomicInteger()
        val registry = IdGeneratorRegistry(
            mapOf("uuid-v7" to { "shared-${counter.incrementAndGet()}" })
        )
        application {
            idGeneratorKtorModule(registry)
        }

        val explicit = client.get("/ids/uuid-v7").decodeJsonBody<IdResponse>()
        val generic = client.get("/idgen/uuid-v7").decodeJsonBody<IdResponse>()
        val genericBatch = client.get("/idgen/uuid-v7/batch?size=3").decodeJsonBody<IdBatchResponse>()

        explicit.id shouldBeEqualTo "shared-1"
        generic.id shouldBeEqualTo "shared-2"
        genericBatch.ids shouldBeEqualTo listOf("shared-3", "shared-4", "shared-5")
    }

    @Test
    fun `metadata and health endpoints return supported generators`() = testApplication {
        application {
            idGeneratorKtorModule()
        }

        val health = client.get("/health").decodeJsonBody<HealthResponse>()
        health.status shouldBeEqualTo "UP"

        val generators = client.get("/generators").decodeJsonBody<GeneratorsResponse>()
        generators.generators.map { it.type } shouldBeEqualTo ExplicitTypes
        generators.genericEndpoints shouldBeEqualTo listOf(
            "/idgen/{type}",
            "/idgen/{type}/batch?size=10"
        )
    }

    @Test
    fun `bad type and bad size return bad request`() = testApplication {
        application {
            idGeneratorKtorModule()
        }

        client.get("/idgen/unknown").status shouldBeEqualTo HttpStatusCode.BadRequest
        client.get("/idgen/uuid-v7/batch?size=0").status shouldBeEqualTo HttpStatusCode.BadRequest
        client.get("/idgen/uuid-v7/batch?size=101").status shouldBeEqualTo HttpStatusCode.BadRequest
        client.get("/ids/uuid-v7/batch?size=not-number").status shouldBeEqualTo HttpStatusCode.BadRequest
    }

    @Test
    fun `SuspendedJobTester proves selected endpoints return unique ids concurrently`() = testApplication {
        application {
            idGeneratorKtorModule()
        }

        listOf("/ids/uuid-v7", "/ids/snowflake").forEach { endpoint ->
            val idMap = ConcurrentHashMap<String, Int>()

            SuspendedJobTester()
                .workers(Runtimex.availableProcessors)
                .rounds(CONCURRENCY_COUNT)
                .add {
                    val response = client.get(endpoint)
                    response shouldHaveStatus HttpStatusCode.OK

                    val id = response.decodeJsonBody<IdResponse>().id
                    id.shouldNotBeNull()
                    idMap.putIfAbsent(id, 1).shouldBeNull()
                }.run()

            idMap.keys shouldHaveSize CONCURRENCY_COUNT
        }
    }
}
