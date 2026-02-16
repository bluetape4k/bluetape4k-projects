package io.bluetape4k.examples.redisson.coroutines.cachestrategy

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.examples.redisson.coroutines.cachestrategy.ActorSchema.ActorRecord
import io.bluetape4k.examples.redisson.coroutines.cachestrategy.ActorSchema.ActorTable
import io.bluetape4k.idgenerators.snowflake.Snowflakers
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.RedissonCodecs
import kotlinx.coroutines.delay
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RMap
import org.redisson.api.RMapCache
import org.redisson.api.options.LocalCachedMapOptions
import org.redisson.api.options.MapCacheOptions
import java.time.Duration
import kotlin.system.measureTimeMillis

@Suppress("DEPRECATION")
class CacheReadThroughExample: AbstractCacheExample() {

    companion object: KLogging() {
        const val ACTOR_SIZE = 500

        private val defaultCodec = RedissonCodecs.LZ4Fory
    }

    @BeforeEach
    fun setup() {
        transaction {
            ActorTable.deleteAll()
            populateSampleData()

            Thread.sleep(1)

            // 데이터 전체 로딩 시간을 측정하기 위해, 샘플 데이터를 ACTOR_SIZE 만큼 추가합니다.
            val writeIds = Snowflakers.Global.nextIds(ACTOR_SIZE).toFastList()
            ActorTable.batchInsert(writeIds, shouldReturnGeneratedValues = false) { id ->
                this[ActorTable.id] = id
                this[ActorTable.firstname] = faker.name().firstName()
                this[ActorTable.lastname] = faker.name().lastName()
            }
        }
    }

    @Nested
    inner class Synchronous {

        @Test
        fun `read through with redisson MapCache`() {
            val name = randomName()
            val options = MapCacheOptions.name<Long, ActorRecord>(name)
                .loader(actorRecordLoader)
                .retryAttempts(3)
                .retryDelay { attempt -> Duration.ofMillis(attempt * 10L) }
                .codec(defaultCodec)

            val cache: RMapCache<Long, ActorRecord?> = redisson.getMapCache(options)
            try {
                checkReadThroughCache(cache)
            } finally {
                cache.delete()
            }
        }

        @Test
        fun `read through with redisson RLocalCachedMap`() {
            val name = randomName()
            val options = LocalCachedMapOptions.name<Long, ActorRecord>(name)
                .loader(actorRecordLoader)
                .retryAttempts(3)
                .retryDelay { attempt -> Duration.ofMillis(attempt * 10L) }
                .timeToLive(Duration.ofSeconds(10))   // 로컬 캐시의 TTL
                .codec(defaultCodec)

            val cache: RLocalCachedMap<Long, ActorRecord?> = redisson.getLocalCachedMap(options)
            try {
                checkReadThroughCache(cache)
            } finally {
                cache.delete()
            }
        }

        private fun checkReadThroughCache(cache: RMap<Long, ActorRecord?>) {
            // 아직 캐시에 로딩된 데이터가 없다.
            cache.keys shouldHaveSize 0

            val actorIds = transaction {
                ActorTable
                    .select(ActorTable.id)
                    .orderBy(ActorTable.id)
                    .map { it[ActorTable.id].value }
            }

            // CacheApplicationListener 에서 5명의 Actor를 추가해 놓았다.
            // 캐시에서 1번 키를 요청하면, DB에서 로딩된다.
            cache[actorIds.first()] shouldBeEqualTo ActorRecord(actorIds.first(), "Sunghyouk", "Bae")
            cache.keys shouldHaveSize 1

            // DB에 있는 모든 Actor를 한번에 로드하여 캐시에 저장한다
            val readTimeFromDB = measureTimeMillis {
                // NOTE: parallelism 을 2 이상은 redisson connection 이 불안하다.
                cache.loadAll(true, 1)
                actorIds.forEach { id ->
                    cache[id].shouldNotBeNull()
                }
            }

            // DB에 없는 것은 null 로 리턴된다.
            cache[0].shouldBeNull()

            // DB에 있는 모든 Actor를 한번에 로드하여 캐시에 저장한다. 이미 캐시에 있는 것은 교체한다
            cache.fastRemove(*actorIds.toTypedArray())
            // NOTE: parallelism 을 2 이상은 redisson connection 이 불안하다.
            cache.loadAll(true, 1)

            // 캐시에서 4명의 Actor를 요청하면, DB에서 로딩되지 않는다.
            val readTimeFromCache = measureTimeMillis {
                actorIds.drop(1).forEach { id ->
                    cache[id].shouldNotBeNull()
                }
            }

            log.debug { "Read DB=$readTimeFromDB ms, Read Cache=$readTimeFromCache ms" }
            // readTimeFromCache shouldBeLessOrEqualTo readTimeFromDB
        }
    }

    @Nested
    inner class Asyncrhronous {
        @Test
        fun `read through with redisson MapCache`() = runSuspendIO {
            val name = randomName()
            val options = MapCacheOptions.name<Long, ActorRecord>(name)
                .loaderAsync(actorRecordLoaderAsync)
                .retryAttempts(3)
                .retryDelay { attempt -> Duration.ofMillis(attempt * 10L) }
                .codec(defaultCodec)

            val cache: RMapCache<Long, ActorRecord?> = redisson.getMapCache(options)
            try {
                checkReadThroughCacheAsync(cache)
            } finally {
                cache.deleteAsync().suspendAwait()
            }
        }

        @Test
        fun `read through with redisson RLocalCachedMap`() = runSuspendIO {
            val name = randomName()
            val options = LocalCachedMapOptions.name<Long, ActorRecord>(name)
                .loaderAsync(actorRecordLoaderAsync)
                .retryAttempts(3)
                .retryDelay { attempt -> Duration.ofMillis(attempt * 10L) }
                .timeToLive(Duration.ofSeconds(10))   // 로컬 캐시의 TTL
                .codec(defaultCodec)

            val cache: RLocalCachedMap<Long, ActorRecord?> = redisson.getLocalCachedMap(options)
            try {
                checkReadThroughCacheAsync(cache)
            } finally {
                cache.deleteAsync().suspendAwait()
            }
        }

        private suspend fun checkReadThroughCacheAsync(cache: RMap<Long, ActorRecord?>) {
            // 아직 캐시에 로딩된 데이터가 없다.
            cache.keys shouldHaveSize 0

            val actorIds = newSuspendedTransaction {
                ActorTable
                    .select(ActorTable.id)
                    .orderBy(ActorTable.id)
                    .map { it[ActorTable.id].value }
            }

            // 모든 테스트에 500 개 이상의 Actor 가 이미 DB에 존재한다.
            // 캐시에서 1번 키를 요청하면, DB에서 로딩된다.
            cache[actorIds.first()] shouldBeEqualTo ActorRecord(actorIds.first(), "Sunghyouk", "Bae")
            cache.keys shouldHaveSize 1

            // 나머지 4명의 Actor는 캐시로 로딩한다
            val readTimeFromDB = measureTimeMillis {
                actorIds.drop(1).forEach { id ->
                    cache[id].shouldNotBeNull()
                }
            }

            // DB에 존재하지 않는 ID에 접근하면 NULL 이 리턴된다.
            cache[0].shouldBeNull()

            // DB에 있는 모든 Actor를 한번에 로드하여 캐시에 저장한다. 이미 캐시에 있는 것은 교체한다
            cache.fastRemoveAsync(*actorIds.toTypedArray()).suspendAwait()

            // NOTE: parallelism 을 2 이상은 redisson connection 이 불안하다.
            cache.loadAll(true, 1)

            delay(100)

            // 캐시에 이미 로딩된 데이터를 요청한다.
            val readTimeFromCache = measureTimeMillis {
                actorIds.drop(1).forEach { id ->
                    cache[id].shouldNotBeNull()
                }
            }

            log.debug { "Read DB=$readTimeFromDB ms, Read Cache=$readTimeFromCache ms" }
            readTimeFromCache shouldBeLessOrEqualTo readTimeFromDB
        }
    }
}
