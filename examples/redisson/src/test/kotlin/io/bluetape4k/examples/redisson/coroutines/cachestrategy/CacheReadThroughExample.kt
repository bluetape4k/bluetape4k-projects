package io.bluetape4k.examples.redisson.coroutines.cachestrategy

import io.bluetape4k.examples.redisson.coroutines.cachestrategy.ActorSchema.Actor
import io.bluetape4k.examples.redisson.coroutines.cachestrategy.ActorSchema.ActorTable
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.RedissonCodecs
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RMap
import org.redisson.api.RMapCache
import org.redisson.api.options.LocalCachedMapOptions
import org.redisson.api.options.MapCacheOptions
import java.time.Duration
import kotlin.system.measureTimeMillis

class CacheReadThroughExample: AbstractCacheExample() {

    @BeforeEach
    fun setup() {
        transaction {
            ActorTable.deleteAll()
            populateSampleData()
        }
    }

    @Test
    fun `read through with redisson MapCache`() {
        val name = randomName()
        val options = MapCacheOptions.name<Long, Actor>(name)
            .loader(actorLoader)
            .codec(RedissonCodecs.LZ4Fury)

        val cache: RMapCache<Long, Actor?> = redisson.getMapCache(options)
        try {
            checkReadThroughCache(cache)
        } finally {
            cache.delete()
        }
    }

    @Test
    fun `read through with redisson RLocalCachedMap`() {
        val name = randomName()
        val options = LocalCachedMapOptions.name<Long, Actor>(name)
            .loader(actorLoader)
            .codec(RedissonCodecs.LZ4Fury)
            .timeToLive(Duration.ofMinutes(10))   // 로컬 캐시의 TTL

        val cache: RLocalCachedMap<Long, Actor?> = redisson.getLocalCachedMap(options)
        try {
            checkReadThroughCache(cache)
        } finally {
            cache.delete()
        }
    }

    private fun checkReadThroughCache(cache: RMap<Long, Actor?>) {
        // 아직 캐시에 로딩된 데이터가 없다.
        cache.keys shouldHaveSize 0

        val actorIds = transaction {
            ActorTable.select(ActorTable.id).map { it[ActorTable.id].value }
        }

        // CacheApplicationListener 에서 5명의 Actor를 추가해 놓았다.
        // 캐시에서 1번 키를 요청하면, DB에서 로딩된다.
        cache[actorIds.first()] shouldBeEqualTo Actor(actorIds.first(), "Sunghyouk", "Bae")
        cache.keys shouldHaveSize 1

        // 나머지 4명의 Actor는 캐시로 로딩한다
        val readTimeFromDB = measureTimeMillis {
            actorIds.drop(1).forEach { id ->
                cache[id].shouldNotBeNull()
            }
        }

        // DB에 없는 것은 null 로 리턴된다.
        cache[0].shouldBeNull()

        // 캐시에서 4명의 Actor를 요청하면, DB에서 로딩되지 않는다.
        val readTimeFromCache = measureTimeMillis {
            actorIds.drop(1).forEach { id ->
                cache[id].shouldNotBeNull()
            }
        }

        log.debug { "Read DB=$readTimeFromDB ms, Read Cache=$readTimeFromCache ms" }
        readTimeFromCache shouldBeLessThan readTimeFromDB

    }
}
