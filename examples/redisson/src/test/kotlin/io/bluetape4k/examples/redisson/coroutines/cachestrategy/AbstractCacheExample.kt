package io.bluetape4k.examples.redisson.coroutines.cachestrategy

import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.examples.redisson.coroutines.cachestrategy.ActorSchema.Actor
import io.bluetape4k.examples.redisson.coroutines.cachestrategy.ActorSchema.ActorTable
import io.bluetape4k.examples.redisson.coroutines.cachestrategy.ActorSchema.toActor
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.redisson.api.AsyncIterator
import org.redisson.api.map.MapLoader
import org.redisson.api.map.MapLoaderAsync
import org.redisson.api.map.MapWriter
import org.redisson.api.map.MapWriterAsync
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

@SpringBootTest(
    classes = [CacheApplication::class],
    properties = [
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.exposed.generate-ddl=true",
        "spring.exposed.show-sql=true"
    ]
)
abstract class AbstractCacheExample: AbstractRedissonCoroutineTest() {

    companion object: KLogging() {
        @JvmStatic
        val faker = Fakers.faker
    }

    protected fun newActorDTO(id: Long): Actor {
        return Actor(
            id = id,
            firstname = faker.name().firstName(),
            lastname = faker.name().lastName(),
        ).also {
            it.description = faker.lorem().sentence(1024, 128)
        }
    }

    /**
     * MapLoader 를 구현하여 DB에서 데이터를 로딩한다.
     */
    protected val actorLoader: MapLoader<Long, Actor?> = object: MapLoader<Long, Actor?>, KLogging() {
        override fun load(key: Long): Actor? {
            log.debug { "Loading actor with key $key" }
            return transaction {
                ActorTable
                    .selectAll()
                    .where { ActorTable.id eq key }
                    .singleOrNull()
                    ?.toActor()
            }
        }

        override fun loadAllKeys(): Iterable<Long> {
            log.debug { "Loading all actor keys ..." }
            return transaction {
                ActorTable
                    .select(ActorTable.id)
                    .map { it[ActorTable.id].value }
            }
        }
    }

    /**
     * [MapLoaderAsync] 를 구현하여 DB에서 데이터를 로딩한다.
     */
    protected val actorLoaderAsync: MapLoaderAsync<Long, Actor?> = object: MapLoaderAsync<Long, Actor?>, KLogging() {
        override fun load(key: Long?): CompletionStage<Actor?>? {
            log.debug { "Loading actor async with key $key" }
            val scope = CoroutineScope(Dispatchers.IO)
            return scope.async {
                newSuspendedTransaction {
                    ActorTable
                        .selectAll()
                        .where { ActorTable.id eq key }
                        .singleOrNull()
                        ?.toActor()
                }
            }.asCompletableFuture()
        }

        override fun loadAllKeys(): AsyncIterator<Long>? {
            log.debug { "Loading all actor keys async ..." }
            return object: AsyncIterator<Long> {
                private val batchSize = 25

                val actorIds = getActorByFetchBatched()

                override fun hasNext(): CompletionStage<Boolean> {
                    return CompletableFuture.supplyAsync {
                        actorIds.iterator().hasNext()
                    }
                }

                override fun next(): CompletionStage<Long> {
                    return CompletableFuture.supplyAsync {
                        actorIds.iterator().next()
                    }
                }

                private fun getActorByFetchBatched(): Sequence<Long> {
                    return ActorTable.select(ActorTable.id)
                        .fetchBatchedResults(batchSize = batchSize)
                        .asSequence()
                        .map { list ->
                            list.map { it[ActorTable.id].value }
                        }
                        .flatten()
                }
            }
        }
    }

    /**
     * [MapWriter]를 구현하여 DB에 데이터를 저장한다.
     */
    protected val actorWriter: MapWriter<Long, Actor> = object: MapWriter<Long, Actor>, KLogging() {
        override fun write(map: Map<Long, Actor?>) {
            log.debug { "Writing actors ... count=${map.size}, ids=${map.keys}" }
            val entryToInsert = map.values.mapNotNull { it }
            transaction {

                ActorTable.batchInsert(entryToInsert, shouldReturnGeneratedValues = false) { actor ->
                    this[ActorTable.id] = actor.id
                    this[ActorTable.firstname] = actor.firstname
                    this[ActorTable.lastname] = actor.lastname
                    this[ActorTable.description] = actor.description
                }
            }
        }

        override fun delete(keys: Collection<Long>) {
            log.debug { "Deleteing actors ... ids=$keys" }
            transaction {
                ActorTable.deleteWhere { ActorTable.id inList keys }
            }
        }
    }

    /**
     * [MapWriterAsync]를 구현하여 DB에 데이터를 저장한다.
     */
    protected val actorWriterAsync: MapWriterAsync<Long, Actor> = object: MapWriterAsync<Long, Actor>, KLogging() {
        override fun write(map: Map<Long, Actor?>): CompletionStage<Void> {
            log.debug { "Writing actors async... count=${map.size}, ids=${map.keys}" }

            val scope = CoroutineScope(Dispatchers.IO)
            return scope.async {
                val entryToInsert = map.values.mapNotNull { it }
                newSuspendedTransaction {
                    ActorTable.batchInsert(entryToInsert, shouldReturnGeneratedValues = false) { actor ->
                        this[ActorTable.id] = actor.id
                        this[ActorTable.firstname] = actor.firstname
                        this[ActorTable.lastname] = actor.lastname
                        this[ActorTable.description] = actor.description
                    }
                }
                log.debug { "Inserted actor count=${entryToInsert.size}" }
                null
            }.asCompletableFuture()
        }

        override fun delete(keys: Collection<Long>): CompletionStage<Void> {
            log.debug { "Deleteing actors async... id count=${keys.size}, ids=$keys" }
            val scope = CoroutineScope(Dispatchers.IO)
            return scope.async {
                val deletedRows = newSuspendedTransaction {
                    ActorTable.deleteWhere { ActorTable.id inList keys }
                }
                log.debug { "Deleted actor count=$deletedRows" }
                null
            }.asCompletableFuture()
        }
    }

    protected fun getActorCountFromDB(): Long = transaction {
        ActorTable
            .selectAll()
            .count()
    }

    protected suspend fun getActorCountFromDBSuspended(): Long = newSuspendedTransaction {
        ActorTable
            .selectAll()
            .count()
    }

    protected fun populateSampleData() {
        transaction {
            ActorSchema.ActorEntity.new {
                firstname = "Sunghyouk"
                lastname = "Bae"
                description = faker.lorem().sentence(1024, 128)
            }
            ActorSchema.ActorEntity.new {
                firstname = "Misook"
                lastname = "Kwon"
                description = faker.lorem().sentence(1024, 128)
            }
            ActorSchema.ActorEntity.new {
                firstname = "Jehyoung"
                lastname = "Bae"
                description = faker.lorem().sentence(1024, 128)
            }
            ActorSchema.ActorEntity.new {
                firstname = "Jinseok"
                lastname = "Kwon"
                description = faker.lorem().sentence(1024, 128)
            }
            ActorSchema.ActorEntity.new {
                firstname = "Kildong"
                lastname = "Hong"
                description = faker.lorem().sentence(1024, 128)
            }
        }
    }
}
