package io.bluetape4k.openfga

import dev.openfga.sdk.api.OpenFgaApi
import dev.openfga.sdk.api.configuration.Configuration
import dev.openfga.sdk.api.model.CreateStoreRequest
import dev.openfga.sdk.api.model.Metadata
import dev.openfga.sdk.api.model.RelationMetadata
import dev.openfga.sdk.api.model.RelationReference
import dev.openfga.sdk.api.model.TypeDefinition
import dev.openfga.sdk.api.model.Userset
import dev.openfga.sdk.api.model.WriteAuthorizationModelRequest
import dev.openfga.sdk.api.model.WriteRequest
import dev.openfga.sdk.api.model.WriteRequestWrites
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.testcontainers.infra.OpenFgaServer
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * SDK와 무관하게 endpoint만 공유하는 OpenFGA 서버 fixture입니다.
 */
@Tag("integration")
class OpenFgaIntegrationTest {

    @Test
    fun `real server supports model tuple check and paginated read`() = runSuspendIO {
        val api = OpenFgaApi(Configuration().apiUrl(server.url))
        (api is AutoCloseable).shouldBeFalse()

        var storeId: String? = null
        try {
            val createdStoreId = api.createStore(
                CreateStoreRequest().name("bluetape4k-${UUID.randomUUID()}"),
            ).await().data.getId()
            storeId = createdStoreId

            val modelId = api.createTestModel(createdStoreId)
            val scope = OpenFgaScope(createdStoreId, modelId)
            val tuple = openFgaTuple("user:anne", "reader", "document:budget")
            val secondTuple = openFgaTuple("user:anne", "reader", "document:roadmap")

            api.writeSuspending(
                scope,
                WriteRequest()
                    .writes(
                        WriteRequestWrites()
                            .addTupleKeysItem(tuple.toTupleKey())
                            .addTupleKeysItem(secondTuple.toTupleKey()),
                    ),
            )

            val allowed = api.checkSuspending(
                scope,
                dev.openfga.sdk.api.model.CheckRequest().tupleKey(tuple.toCheckRequestTupleKey()),
            )
            (allowed.data.getAllowed() == true).shouldBeTrue()

            val denied = api.checkSuspending(
                scope,
                dev.openfga.sdk.api.model.CheckRequest().tupleKey(
                    openFgaTuple("user:bob", "reader", "document:budget").toCheckRequestTupleKey(),
                ),
            )
            (denied.data.getAllowed() == false).shouldBeTrue()

            val tuples = api.readTuplesFlow(
                scope,
                pageSize = 1,
            ).toList()
            tuples shouldHaveSize 2
            tuples.map { it.key.getObject() }.toSet() shouldBeEqualTo
                setOf(tuple.objectId, secondTuple.objectId)
            tuples.all { it.key.getRelation() == tuple.relation }.shouldBeTrue()
        } finally {
            storeId?.let { api.deleteStore(it).await() }
        }
    }

    private suspend fun OpenFgaApi.createTestModel(storeId: String): String =
        writeAuthorizationModel(
            storeId,
            WriteAuthorizationModelRequest()
                .schemaVersion("1.1")
                .addTypeDefinitionsItem(TypeDefinition().type("user"))
                .addTypeDefinitionsItem(
                    TypeDefinition()
                        .type("document")
                        .putRelationsItem("reader", Userset()._this(emptyMap<String, Any>()))
                        .metadata(
                            Metadata().putRelationsItem(
                                "reader",
                                RelationMetadata().addDirectlyRelatedUserTypesItem(
                                    RelationReference().type("user"),
                                ),
                            ),
                        ),
                ),
        ).await().data.getAuthorizationModelId()

    private companion object {
        val server by lazy { OpenFgaServer.Launcher.openFga }
    }
}
