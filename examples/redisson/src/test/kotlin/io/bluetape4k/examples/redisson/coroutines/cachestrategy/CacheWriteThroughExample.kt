package io.bluetape4k.examples.redisson.coroutines.cachestrategy

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.examples.redisson.coroutines.cachestrategy.ActorSchema.Actor
import io.bluetape4k.examples.redisson.coroutines.cachestrategy.ActorSchema.ActorTable
import io.bluetape4k.idgenerators.snowflake.Snowflakers
import io.bluetape4k.junit5.awaitility.suspendUntil
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.RedissonCodecs
import io.bluetape4k.redis.redisson.coroutines.awaitAll
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.await
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.redisson.api.map.WriteMode
import org.redisson.api.options.LocalCachedMapOptions
import org.redisson.api.options.MapCacheOptions
import java.time.Duration

@Suppress("DEPRECATION")
class CacheWriteThroughExample: AbstractCacheExample() {

    companion object: KLoggingChannel() {
        const val ACTOR_SIZE = 50
        private val defaultCodec = RedissonCodecs.LZ4Fory
    }

    @BeforeEach
    fun setup() {
        transaction {
            ActorTable.deleteAll()
        }
    }

    @Nested
    inner class Synchronous {

        @Test
        fun `write through cache with redisson MapCache`() {
            val name = randomName()
            val options = MapCacheOptions.name<Long, Actor>(name)
                .writer(actorWriter)
                .writeMode(WriteMode.WRITE_THROUGH)
                .writeRetryAttempts(3) // 재시도 횟수
                .writeRetryInterval(Duration.ofMillis(100)) // 재시도 간격
                .codec(defaultCodec)

            // 캐시를 생성한다.
            val cache = redisson.getMapCache(options)

            try {

                // Write Through 모드로 설정했으므로, 캐시에 데이터를 삽입하면 DB에도 삽입된다.
                val writeIds = Snowflakers.Default.nextIds(ACTOR_SIZE).toFastList()
                writeIds.forEach { id ->
                    cache[id] = newActorDTO(id)
                }

                // DB에 삽입된 데이터를 확인한다. (options.loader() 가 없으므로, 캐시에는 저장되지 않는다)
                val dbActorCount = transaction {
                    ActorTable.selectAll().where { ActorTable.id inList writeIds }.count()
                }
                dbActorCount shouldBeEqualTo ACTOR_SIZE.toLong()

            } finally {
                // 캐시를 삭제한다.
                cache.delete()
            }
        }

        @Test
        fun `read and write through cache with redisson LocalCachedMap`() {
            val name = randomName()
            val options = LocalCachedMapOptions.name<Long, Actor>(name)
                .loader(actorLoader)
                .writer(actorWriter)
                .writeMode(WriteMode.WRITE_THROUGH)
                .writeRetryAttempts(3) // 재시도 횟수
                .writeRetryInterval(Duration.ofMillis(100)) // 재시도 간격
                .codec(defaultCodec)

            // 캐시를 생성한다.
            val cache = redisson.getLocalCachedMap(options)

            try {

                // Write Through 모드로 설정했으므로, 캐시에 데이터를 삽입하면 DB에도 삽입된다.
                val writeIds = Snowflakers.Default.nextIds(ACTOR_SIZE).toFastList()
                writeIds.forEach { id ->
                    // cache[id] = newActorDTO(id)
                    cache.fastPut(id, newActorDTO(id))
                }

                writeIds.forEach { id ->
                    cache[id].shouldNotBeNull()
                }

                // DB에 삽입된 데이터를 확인한다. (options.loader() 가 있으므로, 캐시에서 삭제되지 않는다)
                val dbActorCount = transaction {
                    ActorTable.selectAll().where { ActorTable.id inList writeIds }.count()
                }
                dbActorCount shouldBeEqualTo ACTOR_SIZE.toLong()

            } finally {
                // 캐시를 삭제한다.
                cache.delete()
            }
        }
    }

    @Nested
    inner class Asynchronous {

        @Test
        fun `write through cache with redisson MapCache in Coroutines`() = runSuspendIO {
            val name = randomName()
            val options = MapCacheOptions.name<Long, Actor>(name)
                .writerAsync(actorWriterAsync)
                .writeMode(WriteMode.WRITE_THROUGH)
                .writeRetryAttempts(3) // 재시도 횟수
                .writeRetryInterval(Duration.ofMillis(100)) // 재시도 간격
                .codec(defaultCodec)

            // 캐시를 생성한다.
            val cache = redisson.getMapCache(options)

            try {
                // Write Through 모드로 설정했으므로, 캐시에 데이터를 삽입하면 DB에도 삽입된다.
                val writeIds = Snowflakers.Default.nextIds(ACTOR_SIZE).toFastList()
                writeIds.map { id ->
                    // cache[id] = newActorDTO(id)
                    cache.fastPutAsync(id, newActorDTO(id))
                }.awaitAll()

                await suspendUntil {
                    newSuspendedTransaction {
                        // DB에 삽입된 데이터를 확인한다. (options.loader() 가 없으므로, 캐시에는 저장되지 않는다)
                        ActorTable.selectAll().where { ActorTable.id inList writeIds }.count()
                    } >= ACTOR_SIZE.toLong()
                }

                // DB에 삽입된 데이터를 확인한다.
                newSuspendedTransaction {
                    ActorTable.selectAll().where { ActorTable.id inList writeIds }.count()
                } shouldBeEqualTo ACTOR_SIZE.toLong()

            } finally {
                // 캐시를 삭제한다.
                cache.deleteAsync().suspendAwait()
            }
        }

        @Test
        fun `read and write through cache with redisson LocalCachedMap in Coroutines`() = runSuspendIO {
            val name = randomName()
            val options = LocalCachedMapOptions.name<Long, Actor>(name)
                .loaderAsync(actorLoaderAsync)
                .writerAsync(actorWriterAsync)
                .writeMode(WriteMode.WRITE_THROUGH)
                .writeRetryAttempts(3) // 재시도 횟수
                .writeRetryInterval(Duration.ofMillis(100)) // 재시도 간격
                .codec(defaultCodec)

            // 캐시를 생성한다.
            val cache = redisson.getLocalCachedMap(options)

            try {

                // Write Through 모드로 설정했으므로, 캐시에 데이터를 삽입하면 DB에도 삽입된다.
                val writeIds = Snowflakers.Default.nextIds(ACTOR_SIZE).toFastList()
                writeIds.map { id ->
                    // cache[id] = newActorDTO(id)
                    cache.fastPutAsync(id, newActorDTO(id))
                }.awaitAll()

                writeIds.forEach { id ->
                    cache[id].shouldNotBeNull()
                }

                await suspendUntil {
                    newSuspendedTransaction {
                        // DB에 삽입된 데이터를 확인한다. (options.loader() 가 없으므로, 캐시에는 저장되지 않는다)
                        ActorTable.selectAll().where { ActorTable.id inList writeIds }.count()
                    } >= ACTOR_SIZE.toLong()
                }

                // DB에 삽입된 데이터를 확인한다. (options.loader() 가 있으므로, 캐시에서 삭제되지 않는다)
                val dbActorCount = newSuspendedTransaction {
                    ActorTable.selectAll().where { ActorTable.id inList writeIds }.count()
                }
                dbActorCount shouldBeEqualTo ACTOR_SIZE.toLong()

            } finally {
                // 캐시를 삭제한다.
                cache.deleteAsync().suspendAwait()
            }
        }
    }
}
