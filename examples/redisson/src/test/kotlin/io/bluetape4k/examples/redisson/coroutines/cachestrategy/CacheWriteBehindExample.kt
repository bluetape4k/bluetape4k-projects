package io.bluetape4k.examples.redisson.coroutines.cachestrategy

import io.bluetape4k.examples.redisson.coroutines.cachestrategy.ActorSchema.Actor
import io.bluetape4k.examples.redisson.coroutines.cachestrategy.ActorSchema.ActorTable
import io.bluetape4k.idgenerators.snowflake.Snowflakers
import io.bluetape4k.junit5.awaitility.coUntil
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.RedissonCodecs
import io.bluetape4k.redis.redisson.coroutines.awaitAll
import io.bluetape4k.redis.redisson.coroutines.coAwait
import kotlinx.coroutines.delay
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.redisson.api.RLocalCachedMap
import org.redisson.api.map.WriteMode
import org.redisson.api.options.LocalCachedMapOptions
import org.redisson.api.options.MapCacheOptions
import java.time.Duration
import java.util.concurrent.TimeUnit

class CacheWriteBehindExample: AbstractCacheExample() {

    companion object: KLoggingChannel() {
        const val ACTOR_SIZE = 50
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
        fun `write behind cache with redisson MapCache`() {
            val name = randomName()
            val options = MapCacheOptions.name<Long, Actor>(name)
                .loader(actorLoader)
                .writer(actorWriter)
                .writeMode(WriteMode.WRITE_BEHIND)              // delay를 두고, batch로 insert 한다
                .writeBehindBatchSize(20)  // 기본 batchSize 는 50 입니다.
                .writeBehindDelay(100)        // 기본 delay 는 1000 ms 입니다.
                .writeRetryAttempts(3) // 재시도 횟수
                .writeRetryInterval(Duration.ofMillis(100)) // 재시도 간격
                .codec(RedissonCodecs.LZ4Fury)

            // 캐시를 생성한다.
            val cache = redisson.getMapCache(options)

            try {

                // Write Behind 모드로 설정했으므로, 캐시에 데이터를 삽입하면 DB에도 삽입된다.
                val writeIds = Snowflakers.Default.nextIds(ACTOR_SIZE).toList()
                writeIds.forEach { id ->
                    cache[id] = newActorDTO(id)
                }

                await until { getActorCountFromDB() >= ACTOR_SIZE }

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
        fun `read through and write behind cache with redisson LocalCachedMap`() {
            val name = randomName()
            val options = LocalCachedMapOptions.name<Long, Actor>(name)
                .loader(actorLoader)
                .writer(actorWriter)
                .writeMode(WriteMode.WRITE_BEHIND)         // delay를 두고, batch로 insert 한다
                .writeBehindBatchSize(20)  // 기본 batchSize 는 50 입니다. (INSERT, DELETE 모두 적용됨)
                .writeBehindDelay(100)        // 기본 delay 는 1000 ms 입니다.
                .writeRetryAttempts(3) // 재시도 횟수
                .writeRetryInterval(Duration.ofMillis(100)) // 재시도 간격
                .codec(RedissonCodecs.LZ4Fury)

            // 캐시를 생성한다.
            val cache: RLocalCachedMap<Long, Actor?> = redisson.getLocalCachedMap(options)

            try {

                // Write Behind 모드로 설정했으므로, 캐시에 데이터를 삽입하면 DB에도 삽입된다.
                val writeIds = Snowflakers.Default.nextIds(ACTOR_SIZE).toList()

                writeIds.forEach { id ->
                    cache[id] = newActorDTO(id)
                }

                writeIds.forEach { id ->
                    cache[id].shouldNotBeNull()
                }

                await until { getActorCountFromDB() >= ACTOR_SIZE }

                // DB에 삽입된 데이터를 확인한다. (options.loader() 가 있으므로, 캐시에서 삭제되지 않는다)
                val dbActorCount = transaction {
                    ActorTable.selectAll().where { ActorTable.id inList writeIds }.count()
                }
                dbActorCount shouldBeEqualTo ACTOR_SIZE.toLong()

                // 캐시의 데이터를 모두 삭제한다 -> DB의 데이터도 삭제된다 !!!
                cache.fastRemove(*writeIds.toTypedArray())

                await until {
                    val dbActorCount2 = transaction {
                        ActorTable.selectAll().where { ActorTable.id inList writeIds }.count()
                    }
                    dbActorCount2 == 0L
                }
            } finally {
                // 캐시를 삭제한다.
                cache.delete()
            }
        }
    }

    @Nested
    inner class Asynchronous {
        @Test
        fun `write behind cache with redisson MapCache in Coroutines`() = runSuspendIO {
            val name = randomName()
            val options = MapCacheOptions.name<Long, Actor>(name)
                .loaderAsync(actorLoaderAsync)
                .writerAsync(actorWriterAsync)
                .writeMode(WriteMode.WRITE_BEHIND)              // delay를 두고, batch로 insert 한다
                .writeBehindBatchSize(20)  // 기본 batchSize 는 50 입니다.
                .writeBehindDelay(100)        // 기본 delay 는 1000 ms 입니다.
                .writeRetryAttempts(3) // 재시도 횟수
                .writeRetryInterval(Duration.ofMillis(100)) // 재시도 간격
                .codec(RedissonCodecs.LZ4Fury)

            // 캐시를 생성한다.
            val cache = redisson.getMapCache(options)

            try {
                // Write Behind 모드로 설정했으므로, 캐시에 데이터를 삽입하면 DB에도 삽입된다.
                val writeIds = Snowflakers.Default.nextIds(ACTOR_SIZE).toList()
                writeIds
                    .map { id ->
                        cache.fastPutAsync(id, newActorDTO(id), 1, TimeUnit.SECONDS)
                    }
                    .awaitAll()

                await coUntil { getActorCountFromDBSuspended() >= ACTOR_SIZE }

                // DB에 삽입된 데이터를 확인한다. (options.loader() 가 없으므로, 캐시에는 저장되지 않는다)
                val dbActorCount = newSuspendedTransaction {
                    ActorTable.selectAll().where { ActorTable.id inList writeIds }.count()
                }
                dbActorCount shouldBeEqualTo ACTOR_SIZE.toLong()

                // 캐시만 Expired 되기를 기다렸다가 다시 로드한다.
                await coUntil {
                    delay(100);
                    cache.size < ACTOR_SIZE
                }

                // 캐시의 데이터가 Expired 되었지만, 다시 DB에서 로드한다.
                writeIds.forEach { id ->
                    cache[id].shouldNotBeNull()
                }

            } finally {
                // 캐시를 삭제한다.
                cache.deleteAsync().coAwait()
            }
        }

        @Test
        fun `read through and write behind cache with redisson LocalCachedMap in Coroutines`() = runSuspendIO {
            val name = randomName()
            val options = LocalCachedMapOptions.name<Long, Actor>(name)
                .loaderAsync(actorLoaderAsync)
                .writerAsync(actorWriterAsync)
                .writeMode(WriteMode.WRITE_BEHIND)         // delay를 두고, batch로 insert 한다
                .writeBehindBatchSize(20)  // 기본 batchSize 는 50 입니다. (INSERT, DELETE 모두 적용됨)
                .writeBehindDelay(100)        // 기본 delay 는 1000 ms 입니다.
                .writeRetryAttempts(3) // 재시도 횟수
                .writeRetryInterval(Duration.ofMillis(100)) // 재시도 간격
                .codec(RedissonCodecs.LZ4Fury)
                .timeToLive(Duration.ofSeconds(1))   // 로컬 캐시의 TTL

            // 캐시를 생성한다.
            val cache: RLocalCachedMap<Long, Actor?> = redisson.getLocalCachedMap(options)

            try {

                // Write Through 모드로 설정했으므로, 캐시에 데이터를 삽입하면 DB에도 삽입된다.
                val writeIds = Snowflakers.Default.nextIds(ACTOR_SIZE).toList()

                writeIds.map { id ->
                    cache.fastPutAsync(id, newActorDTO(id))
                }.awaitAll()

                writeIds.forEach { id ->
                    cache[id].shouldNotBeNull()
                }

                await coUntil { getActorCountFromDBSuspended() >= ACTOR_SIZE }

                // DB에 삽입된 데이터를 확인한다. (options.loader() 가 있으므로, 캐시에서 삭제되지 않는다)
                val dbActorCount = transaction {
                    ActorTable.selectAll().where { ActorTable.id inList writeIds }.count()
                }
                dbActorCount shouldBeEqualTo ACTOR_SIZE.toLong()

                // 캐시의 데이터를 모두 삭제한다 -> DB의 데이터도 삭제된다 !!!
                cache.fastRemoveAsync(*writeIds.toTypedArray()).coAwait()

                await coUntil {
                    delay(100)
                    newSuspendedTransaction {
                        ActorTable.selectAll().where { ActorTable.id inList writeIds }.count()
                    } == 0L
                }

                // 캐시와 DB의 데이터가 모두 삭제되었는지 확인한다.
                writeIds.forEach { id ->
                    cache[id].shouldBeNull()
                }

            } finally {
                // 캐시를 삭제한다.
                cache.delete()
            }
        }
    }
}
