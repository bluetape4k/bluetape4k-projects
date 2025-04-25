package io.bluetape4k.examples.redisson.coroutines.cachestrategy

import io.bluetape4k.examples.redisson.coroutines.cachestrategy.ActorSchema.Actor
import io.bluetape4k.examples.redisson.coroutines.cachestrategy.ActorSchema.ActorTable
import io.bluetape4k.idgenerators.snowflake.Snowflakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.RedissonCodecs
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.redisson.api.map.WriteMode
import org.redisson.api.options.LocalCachedMapOptions
import org.redisson.api.options.MapCacheOptions

class CacheWriteBehindExample: AbstractCacheExample() {

    companion object: KLogging() {
        const val ACTOR_SIZE = 50
    }

    @BeforeEach
    fun setup() {
        transaction {
            ActorTable.deleteAll()
        }
    }

    @Test
    fun `write behind cache with redisson MapCache`() {
        val name = randomName()
        val options = MapCacheOptions.name<Long, Actor>(name)
            .writer(actorWriter)
            .writeMode(WriteMode.WRITE_BEHIND)              // delay를 두고, batch로 insert 한다
            .writeBehindBatchSize(20)  // 기본 batchSize 는 50 입니다.
            .writeBehindDelay(100)        // 기본 delay 는 1000 ms 입니다.
            .codec(RedissonCodecs.LZ4Fury)

        // 캐시를 생성한다.
        val cache = redisson.getMapCache(options)

        try {

            // Write Through 모드로 설정했으므로, 캐시에 데이터를 삽입하면 DB에도 삽입된다.
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
            .writeMode(WriteMode.WRITE_THROUGH)
            .codec(RedissonCodecs.LZ4Fury)

        // 캐시를 생성한다.
        val cache = redisson.getLocalCachedMap(options)

        try {

            // Write Through 모드로 설정했으므로, 캐시에 데이터를 삽입하면 DB에도 삽입된다.
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

        } finally {
            // 캐시를 삭제한다.
            cache.delete()
        }
    }
}
