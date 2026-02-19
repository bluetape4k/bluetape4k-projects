package io.bluetape4k.examples.redisson.coroutines.readwritethrough

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.jdbc.sql.extract
import io.bluetape4k.jdbc.sql.runQuery
import io.bluetape4k.jdbc.sql.withConnect
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.redis.redisson.RedissonCodecs
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.redisson.api.map.MapLoader
import org.redisson.api.map.MapWriter
import org.redisson.api.map.WriteMode
import org.redisson.api.options.MapCacheOptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Duration
import javax.sql.DataSource
import kotlin.system.measureTimeMillis


/**
 * Redisson Map read/write through 기능 예제
 *
 * Map에 요소가 없으면 영구저장소로부터 read through 하고, 새로운 Item 에 대해서는 write through 를 수행합니다.
 * JPA 를 쓸 수도 있고, hibernate-reactive 를 이용하여 비동기 작업도 가능하리라 봅니다.
 */
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [JdbcConfiguration::class])
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class MapReadWriteThroughTest: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel() {

        const val ACTOR_SIZE = 30

        const val SELECT_ACTORS = "SELECT * FROM Actors"
        const val SELECT_ACTOR_IDS = "SELECT id FROM Actors"
        const val SELECT_ACTOR_BY_ID = "SELECT * FROM Actors WHERE id=?"
        const val INSERT_ACTOR = "INSERT INTO Actors(id, firstname, lastname) VALUES(?, ?, ?)"
        const val DELETE_ACTOR = "DELETE FROM Actors WHERE id=?"

        const val SELECT_ACTOR_COUNT = "SELECT count(*) as cnt FROM Actors"
    }

    @Autowired
    private lateinit var dataSource: DataSource

    private fun newActor(id: Int): Actor {
        return Actor(
            id = id,
            firstname = faker.name().firstName(),
            lastname = faker.name().lastName()
        )
    }

    private val actorWriter = object: MapWriter<Int, Actor> {
        override fun write(map: MutableMap<Int, Actor>) {
            try {
                dataSource.withConnect { conn ->
                    log.debug { "Insert Actor to DB. actors=${map.values.joinToString()}" }
                    conn.prepareStatement(INSERT_ACTOR).use { ps ->
                        map.entries.forEach { entry ->
                            ps.setInt(1, entry.value.id)
                            ps.setString(2, entry.value.firstname)
                            ps.setString(3, entry.value.lastname)
                            ps.addBatch()
                        }
                        ps.executeBatch()
                    }
                }
            } catch (e: Exception) {
                throw IllegalStateException("Fail to insert. map=$map", e)
            }
        }

        override fun delete(keys: MutableCollection<Int>) {
            try {
                dataSource.withConnect { conn ->
                    conn.prepareStatement(DELETE_ACTOR).use { ps ->
                        log.debug { "Delete actor from DB. keys=$keys" }
                        keys.forEach { key ->
                            ps.setInt(1, key)
                            ps.addBatch()
                        }
                        ps.executeBatch()
                    }
                }
            } catch (e: Exception) {
                throw IllegalStateException("Fail to delete. keys=$keys", e)
            }
        }
    }

    private val actorLoader = object: MapLoader<Int, Actor> {
        override fun load(key: Int): Actor? {
            return dataSource.withConnect { conn ->
                conn.prepareStatement(SELECT_ACTOR_BY_ID).use { ps ->
                    log.debug { "Load actor from DB. actor id=$key" }
                    ps.setInt(1, key)
                    val resultSet = ps.executeQuery()

                    resultSet
                        .extract {
                            Actor(
                                int[Actor::id.name],
                                string[Actor::firstname.name],
                                string[Actor::lastname.name]
                            )
                        }
                }
            }.firstOrNull()
        }

        override fun loadAllKeys(): Iterable<Int> {
            return dataSource.runQuery(SELECT_ACTOR_IDS) { rs ->
                log.debug { "Load all actor ids." }
                rs.extract {
                    int[Actor::id.name]
                }
            }
        }
    }

    @Test
    @Order(0)
    fun `read through by redisson map`() {
        val name = randomName()
        val options = MapCacheOptions.name<Int, Actor>(name)
            .loader(actorLoader)

        // DB에 5개의 record가 있고, Redis에는 아무 것도 없다
        val map = redisson.getMapCache(options)

        // Id=1 을 read through 로 메모리에 올린다.
        map[1] shouldBeEqualTo Actor(1, "Sunghyouk", "Bae")
        map.keys.size shouldBeEqualTo 1

        // 나머지를 read through 로 메모리에 올린다.
        val readTimeDB = measureTimeMillis {
            map[2].shouldNotBeNull()
            map[3].shouldNotBeNull()
            map[4].shouldNotBeNull()
            map[5].shouldNotBeNull()
        }

        // 해당 Id의 Actor 가 DB에 없다
        map[100_000].shouldBeNull()

        map[2].shouldNotBeNull()
        map[3].shouldNotBeNull()
        map[4].shouldNotBeNull()
        map[5].shouldNotBeNull()

        val readTimeRedis = measureTimeMillis {
            map[2].shouldNotBeNull()
            map[3].shouldNotBeNull()
            map[4].shouldNotBeNull()
            map[5].shouldNotBeNull()
        }

        log.info { "Read DB=$readTimeDB ms, Read Redis=$readTimeRedis ms" }

        map.delete()
    }

    @Test
    @Order(1)
    fun `write through by redisson map`() {
        val name = randomName()
        val options = MapCacheOptions.name<Int, Actor>(name)
            .loader(actorLoader)
            .writer(actorWriter)
            .writeMode(WriteMode.WRITE_THROUGH)   // 추가될 때마다 즉시 DB에 저장된다.
            .codec(RedissonCodecs.LZ4ForyComposite)

        // DB에 5개의 record가 있고, Redis에는 아무 것도 없다
        val map = redisson.getMapCache(options)

        // write through 로 redis -> db 로 저장한다
        repeat(ACTOR_SIZE) {
            val id = 100_000 + it
            map[id] = newActor(id)
        }

        map.keys.size shouldBeGreaterOrEqualTo ACTOR_SIZE

        // 메모리에서 가져온다
        repeat(ACTOR_SIZE) {
            val id = 100_000 + it
            map[id].shouldNotBeNull()
        }

        map.delete()
    }

    @Test
    @Order(2)
    fun `write behind by redisson map`() {
        val name = randomName()
        val options = MapCacheOptions.name<Int, Actor>(name)
            .loader(actorLoader)
            .writer(actorWriter)
            .writeMode(WriteMode.WRITE_BEHIND)   // delay를 두고, batch로 insert 한다
            .writeBehindBatchSize(20)           // batch size (기본 50)
            .writeBehindDelay(100)  // 기본 delay 는 1초이다

        // DB에 5개의 record가 있고, Redis에는 아무 것도 없다
        val map = redisson.getMapCache(options)

        // write through 로 redis 에 저장하고, delay 후 batch 로 db에 저장한다
        val prevActorCount = getActorCountFromDB()

        repeat(ACTOR_SIZE) {
            val id = 200_000 + it
            map[id] = newActor(id)
        }
        // 메모리에서 가져온다 (아직 DB에 저장 안되었을 수도 있다)
        repeat(ACTOR_SIZE) {
            val id = 200_000 + it
            map[id].shouldNotBeNull()
        }

        // delay 되어 있던 item들이 DB에 저장될 때까지 대기한다
        await atMost Duration.ofSeconds(5) until { getActorCountFromDB() >= prevActorCount + ACTOR_SIZE }

        map.delete()
    }

    @Test
    @Order(3)
    fun `get actor count from db`() {
        val actorCount = getActorCountFromDB()
        actorCount shouldBeGreaterThan 3
    }

    private fun getActorCountFromDB(): Int {
        return dataSource.runQuery(SELECT_ACTOR_COUNT) { rs ->
            if (rs.next()) rs.getInt("cnt")
            else 0
        }
    }

    @Test
    @Order(4)
    fun `read write through with coroutines`() = runSuspendIO {
        val name = randomName()
        val options = MapCacheOptions.name<Int, Actor>(name)
            .loader(actorLoader)
            .writer(actorWriter)
            .writeMode(WriteMode.WRITE_THROUGH)   // 추가될 때마다 즉시 DB에 저장된다.

        // DB에 5개의 record가 있고, Redis에는 아무 것도 없다
        val map = redisson.getMapCache(options)

        // write through 로 redis -> db 로 저장한다
        val insertJobs = List(ACTOR_SIZE) {
            launch {
                val id = 300_000 + it
                val actor = newActor(id)
                map.fastPutAsync(id, actor).awaitSuspending().shouldBeTrue()
            }
        }
        insertJobs.joinAll()

        map.keys.size shouldBeGreaterOrEqualTo ACTOR_SIZE

        // 메모리에서 가져온다
        val checkJob = List(ACTOR_SIZE) {
            launch {
                val id = 300_000 + it
                map.getAsync(id).awaitSuspending().shouldNotBeNull()
            }
        }
        checkJob.joinAll()

        map.deleteAsync().awaitSuspending()
    }

    @Test
    @Order(4)
    fun `read write behind with coroutines`() = runSuspendIO {
        val name = randomName()
        val options = MapCacheOptions.name<Int, Actor>(name)
            .loader(actorLoader)
            .writer(actorWriter)
            .writeMode(WriteMode.WRITE_BEHIND)   // delay를 두고, batch로 insert 한다
            .writeBehindBatchSize(20)           // batch size (기본 50)
            .writeBehindDelay(100)  // 기본 delay 는 1초이다

        // DB에 5개의 record가 있고, Redis에는 아무 것도 없다
        val map = redisson.getMapCache(options)

        // write through 로 redis 에 저장하고, delay 후 batch 로 db에 저장한다
        val prevActorCount = getActorCountFromDB()
        // write through 로 redis -> db 로 저장한다
        val insertJobs = List(ACTOR_SIZE) {
            launch {
                val id = 400_000 + it
                val actor = newActor(id)
                map.fastPutAsync(id, actor).awaitSuspending().shouldBeTrue()
            }
        }
        insertJobs.joinAll()

        // delay 되어 있던 item들이 DB에 저장될 때까지 대기한다
        await atMost Duration.ofSeconds(5) until { getActorCountFromDB() >= prevActorCount + ACTOR_SIZE }

        map.keys.size shouldBeGreaterOrEqualTo ACTOR_SIZE

        // 메모리에서 가져온다
        val checkJob = List(ACTOR_SIZE) {
            launch {
                val id = 400_000 + it
                map.getAsync(id).awaitSuspending().shouldNotBeNull()
            }
        }
        checkJob.joinAll()

        map.deleteAsync().awaitSuspending()
    }
}
