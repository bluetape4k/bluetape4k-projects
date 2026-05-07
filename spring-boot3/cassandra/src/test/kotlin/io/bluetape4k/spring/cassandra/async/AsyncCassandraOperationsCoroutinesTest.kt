package io.bluetape4k.spring.cassandra.async

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.uuid.Uuids
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.spring.cassandra.countSuspending
import io.bluetape4k.spring.cassandra.deleteByIdSuspending
import io.bluetape4k.spring.cassandra.deleteSuspending
import io.bluetape4k.spring.cassandra.domain.DomainTestConfiguration
import io.bluetape4k.spring.cassandra.domain.model.User
import io.bluetape4k.spring.cassandra.domain.model.UserToken
import io.bluetape4k.spring.cassandra.existsSuspending
import io.bluetape4k.spring.cassandra.insertSuspending
import io.bluetape4k.spring.cassandra.selectOneByIdSuspending
import io.bluetape4k.spring.cassandra.selectOneOrNullSuspending
import io.bluetape4k.spring.cassandra.selectSuspending
import io.bluetape4k.spring.cassandra.sliceSuspending
import io.bluetape4k.spring.cassandra.truncateSuspending
import io.bluetape4k.spring.cassandra.updateSuspending
import io.bluetape4k.spring.cassandra.cql.deleteOptions
import io.bluetape4k.spring.cassandra.cql.insertOptions
import io.bluetape4k.spring.cassandra.cql.updateOptions
import io.bluetape4k.spring.cassandra.query.eq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.core.AsyncCassandraTemplate
import org.springframework.data.cassandra.core.query.CassandraPageRequest
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.cassandra.core.query.query
import org.springframework.data.cassandra.core.query.where
import org.springframework.data.cassandra.core.query.Update

@SpringBootTest(classes = [DomainTestConfiguration::class])
class AsyncCassandraOperationsCoroutinesTest(
    @param:Autowired private val cqlSession: CqlSession,
) : AbstractCassandraCoroutineTest("async-ops-coroutines") {

    companion object : KLoggingChannel()

    private val operations: AsyncCassandraTemplate by lazy {
        AsyncCassandraTemplate(cqlSession).apply {
            isUsePreparedStatements = false
        }
    }

    private fun newUser(): User =
        User(Uuids.timeBased().toString(), faker.name().firstName(), faker.name().lastName())

    @BeforeEach
    fun beforeEach() {
        runBlocking {
            operations.truncateSuspending<User>()
            operations.truncateSuspending<UserToken>()
        }
    }

    @Test
    fun `insertSuspending - 엔티티 저장`() = runSuspendIO {
        val user = newUser()
        val saved = operations.insertSuspending(user)
        saved shouldBeEqualTo user
        operations.selectOneByIdSuspending<User>(user.id) shouldBeEqualTo user
    }

    @Test
    fun `insertSuspending with options - LWT insert`() = runSuspendIO {
        val user = newUser()
        val lwtOptions = insertOptions { withIfNotExists() }

        val result = operations.insertSuspending(user, lwtOptions)
        result.wasApplied().shouldBeTrue()

        // 이미 존재하면 insert 안됨
        val result2 = operations.insertSuspending(user.copy(firstname = "Another"), lwtOptions)
        result2.wasApplied().shouldBeFalse()
    }

    @Test
    fun `selectOneByIdSuspending - id로 단건 조회`() = runSuspendIO {
        val user = newUser()
        operations.insertSuspending(user)

        operations.selectOneByIdSuspending<User>(user.id) shouldBeEqualTo user
        operations.selectOneByIdSuspending<User>("nonexistent-id").shouldBeNull()
    }

    @Test
    fun `selectSuspending by Statement - 리스트 조회`() = runSuspendIO {
        val user = newUser()
        operations.insertSuspending(user)

        val results = operations.selectSuspending<User>("SELECT * FROM users")
        results.any { it.id == user.id }.shouldBeTrue()
    }

    @Test
    fun `selectSuspending by Query - 조건 조회`() = runSuspendIO {
        val user = newUser()
        operations.insertSuspending(user)

        val q = query(where("id").eq(user.id))
        val results = operations.selectSuspending<User>(q)
        results shouldBeEqualTo listOf(user)
    }

    @Test
    fun `selectSuspending by CQL - consumer 버전`() = runSuspendIO {
        val user = newUser()
        operations.insertSuspending(user)

        val collected = mutableListOf<User>()
        operations.selectSuspending<User>("SELECT * FROM users") { collected.add(it) }
        collected.any { it.id == user.id }.shouldBeTrue()
    }

    @Test
    fun `selectOneOrNullSuspending by CQL - 단건 조회`() = runSuspendIO {
        val user = newUser()
        operations.insertSuspending(user)

        val found = operations.selectOneOrNullSuspending<User>("SELECT * FROM users WHERE id = '${user.id}'")
        found shouldBeEqualTo user
    }

    @Test
    fun `selectOneOrNullSuspending by Query - 단건 조회`() = runSuspendIO {
        val user1 = newUser()
        val user2 = newUser()
        operations.insertSuspending(user1)
        operations.insertSuspending(user2)

        val q = query(where("id").eq(user1.id))
        operations.selectOneOrNullSuspending<User>(q) shouldBeEqualTo user1
    }

    @Test
    fun `countSuspending - 전체 건수`() = runSuspendIO {
        val user1 = newUser()
        val user2 = newUser()
        operations.insertSuspending(user1)
        operations.insertSuspending(user2)

        operations.countSuspending<User>() shouldBeEqualTo 2L
    }

    @Test
    fun `countSuspending by Query - 조건 건수`() = runSuspendIO {
        val user = newUser()
        operations.insertSuspending(user)

        val q = query(where("id").eq(user.id))
        operations.countSuspending<User>(q) shouldBeEqualTo 1L
        operations.countSuspending<User>(query(where("id").eq("nonexistent"))) shouldBeEqualTo 0L
    }

    @Test
    fun `existsSuspending by id`() = runSuspendIO {
        val user = newUser()
        operations.insertSuspending(user)

        operations.existsSuspending<User>(user.id)!!.shouldBeTrue()
        operations.existsSuspending<User>("nonexistent-id")!!.shouldBeFalse()
    }

    @Test
    fun `existsSuspending by Query`() = runSuspendIO {
        val user = newUser()
        operations.insertSuspending(user)

        val q = query(where("id").eq(user.id))
        operations.existsSuspending<User>(q)!!.shouldBeTrue()
        operations.existsSuspending<User>(query(where("id").eq("nonexistent")))!!.shouldBeFalse()
    }

    @Test
    fun `updateSuspending entity - 엔티티 갱신`() = runSuspendIO {
        val user = newUser()
        operations.insertSuspending(user)

        user.firstname = "갱신된이름"
        val updated = operations.updateSuspending(user)
        updated.shouldNotBeNull()
        operations.selectOneByIdSuspending<User>(user.id)!!.firstname shouldBeEqualTo "갱신된이름"
    }

    @Test
    fun `updateSuspending entity with options - 옵션 갱신`() = runSuspendIO {
        val user = newUser()
        operations.insertSuspending(user)

        user.firstname = "변경됨"
        val opts = updateOptions { withTracing() }
        val result = operations.updateSuspending(user, opts)
        result.wasApplied().shouldBeTrue()
    }

    @Test
    fun `updateSuspending by Query - 조건 갱신`() = runSuspendIO {
        val user = newUser()
        operations.insertSuspending(user)

        val q = query(where("id").eq(user.id))
        val update = Update.empty().set("firstname", "수정됨")
        operations.updateSuspending<User>(q, update)!!.shouldBeTrue()

        operations.selectOneByIdSuspending<User>(user.id)!!.firstname shouldBeEqualTo "수정됨"
    }

    @Test
    fun `deleteSuspending entity - 엔티티 삭제`() = runSuspendIO {
        val user = newUser()
        operations.insertSuspending(user)

        operations.deleteSuspending(user)
        operations.selectOneByIdSuspending<User>(user.id).shouldBeNull()
    }

    @Test
    fun `deleteSuspending entity with options - 옵션 삭제`() = runSuspendIO {
        val user = newUser()
        operations.insertSuspending(user)

        val opts = deleteOptions { withTracing() }
        operations.deleteSuspending(user, opts).wasApplied().shouldBeTrue()

        operations.selectOneByIdSuspending<User>(user.id).shouldBeNull()
    }

    @Test
    fun `deleteSuspending by Query - 조건 삭제`() = runSuspendIO {
        val user = newUser()
        operations.insertSuspending(user)

        val q = query(where("id").eq(user.id))
        operations.deleteSuspending<User>(q)!!.shouldBeTrue()

        operations.selectOneByIdSuspending<User>(user.id).shouldBeNull()
    }

    @Test
    fun `deleteByIdSuspending - id로 삭제`() = runSuspendIO {
        val user = newUser()
        operations.insertSuspending(user)

        operations.deleteByIdSuspending<User>(user.id).shouldBeTrue()
        operations.selectOneByIdSuspending<User>(user.id).shouldBeNull()
    }

    @Test
    fun `sliceSuspending by Query - 페이지 조회`() = runSuspendIO {
        val entitySize = 20
        val pageSize = 5

        List(entitySize) {
            async(Dispatchers.IO) {
                val user = newUser()
                operations.insertSuspending(user)
                user.id
            }
        }.awaitAll().toSet()

        val q = Query.empty()
        val slice = operations.sliceSuspending<User>(q.pageRequest(CassandraPageRequest.first(pageSize)))
        slice.size shouldBeEqualTo pageSize
    }

    @Test
    fun `truncateSuspending - 테이블 초기화`() = runSuspendIO {
        val user = newUser()
        operations.insertSuspending(user)
        operations.countSuspending<User>() shouldBeEqualTo 1L

        operations.truncateSuspending<User>()
        operations.countSuspending<User>() shouldBeEqualTo 0L
    }

    @Test
    fun `selectSuspending by Query with consumer`() = runSuspendIO {
        val user1 = newUser()
        val user2 = newUser()
        operations.insertSuspending(user1)
        operations.insertSuspending(user2)

        val collected = mutableListOf<User>()
        val q = Query.empty()
        operations.selectSuspending<User>(q) { collected.add(it) }
        collected.size shouldBeEqualTo 2
        collected.map { it.id } shouldContainSame listOf(user1.id, user2.id)
    }
}
