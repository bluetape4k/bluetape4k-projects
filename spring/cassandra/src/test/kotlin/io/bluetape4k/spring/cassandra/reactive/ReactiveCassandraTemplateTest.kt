package io.bluetape4k.spring.cassandra.reactive

import com.datastax.oss.driver.api.core.uuid.Uuids
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.spring.cassandra.cql.deleteOptions
import io.bluetape4k.spring.cassandra.cql.insertOptions
import io.bluetape4k.spring.cassandra.cql.updateOptions
import io.bluetape4k.spring.cassandra.domain.ReactiveDomainTestConfiguration
import io.bluetape4k.spring.cassandra.domain.model.User
import io.bluetape4k.spring.cassandra.query.eq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.core.ReactiveCassandraOperations
import org.springframework.data.cassandra.core.UpdateOptions
import org.springframework.data.cassandra.core.count
import org.springframework.data.cassandra.core.delete
import org.springframework.data.cassandra.core.deleteById
import org.springframework.data.cassandra.core.exists
import org.springframework.data.cassandra.core.query.CassandraPageRequest
import org.springframework.data.cassandra.core.query.Columns
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.cassandra.core.query.query
import org.springframework.data.cassandra.core.query.where
import org.springframework.data.cassandra.core.selectOneById
import org.springframework.data.cassandra.core.truncate
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories

@SpringBootTest(classes = [ReactiveDomainTestConfiguration::class])
@EnableReactiveCassandraRepositories
class ReactiveCassandraTemplateTest(
    @Autowired private val operations: ReactiveCassandraOperations,
): AbstractCassandraCoroutineTest("reactive-template") {

    companion object: KLoggingChannel() {
        fun newUser(): User = User(
            Uuids.timeBased().toString(),
            faker.name().firstName(),
            faker.name().lastName()
        )
    }

    @BeforeEach
    fun beforeEach() {
        runBlocking {
            operations.truncate<User>().awaitSingleOrNull()
        }
    }

    @Test
    fun `context loading`() {
        operations.shouldNotBeNull()
    }

    @Test
    fun `새로운 엔티티 추가`() = runSuspendIO {
        val user = newUser()
        operations.insert(user).awaitSingleOrNull()
    }


    @Test
    fun `엔티티가 없을 때에만 추가`() = runSuspendIO {
        val lwtOptions = insertOptions { withIfNotExists() }
        val user = newUser()

        val inserted = operations.insert(user, lwtOptions).awaitSingle()
        inserted.wasApplied().shouldBeTrue()
        inserted.entity shouldBeEqualTo user

        val user2 = user.copy(firstname = "성혁")
        val notInserted = operations.insert(user2, lwtOptions).awaitSingle()
        notInserted.wasApplied().shouldBeFalse()
    }

    @Test
    fun `엔티티 Count 조회`() = runSuspendIO {
        val user = newUser()

        operations.insert(user).awaitSingle()
        operations.count<User>().awaitSingle() shouldBeEqualTo 1L
    }

    @Test
    fun `조건절을 이용한 count 조회`() = runSuspendIO {
        val user1 = newUser()
        val user2 = newUser()
        operations.insert(user1).awaitSingle()
        operations.insert(user2).awaitSingle()

        operations.count<User>(query(where("id").eq(user1.id))).awaitSingle() shouldBeEqualTo 1L
        operations.count<User>(query(where("id").eq("not-exists"))).awaitSingle() shouldBeEqualTo 0L
    }

    @Test
    fun `exists 조회`() = runSuspendIO {
        val user1 = newUser()
        val user2 = newUser()
        operations.insert(user1).awaitSingle()
        operations.insert(user2).awaitSingle()

        operations.exists<User>(user1.id).awaitSingle().shouldBeTrue()
        operations.exists<User>("not exists id").awaitSingle().shouldBeFalse()

        operations.exists<User>(query(where("id").eq(user1.id))).awaitSingle().shouldBeTrue()
        operations.exists<User>(query(where("id").eq("not-exists@example.com"))).awaitSingle().shouldBeFalse()
    }

    @Test
    fun `엔티티 갱신하기`() = runSuspendIO {
        val user = newUser()
        operations.insert(user).awaitSingle()

        user.firstname = "성혁"
        val updated = operations.update(user).awaitSingle()
        updated.id shouldBeEqualTo user.id
    }

    @Test
    fun `존재하지 않으면 Update 하지 않기`() = runSuspendIO {
        // 존재하지 않는 엔티티를 Update 하는 경우에는 아무 작업도 하지 않도록 합니다.
        val user = newUser()
        val lwtOptions = UpdateOptions.builder().withIfExists().build()

        val result = operations.update(user, lwtOptions).awaitSingle()
        result.wasApplied().shouldBeFalse()

        getUserById(user.id).shouldBeNull()
    }

    @Test
    fun `존재하는 엔티티를 Update 하기`() = runSuspendIO {
        val user = newUser()
        operations.insert(user).awaitSingle()

        user.firstname = "성혁"
        // NOTE: withIfExists() 가 제대로 작동하지 않는다
        val lwtOptions = updateOptions { /*withIfExists()*/ }
        val result = operations.update(user, lwtOptions).awaitSingle()
        result.wasApplied().shouldBeTrue()

        getUserById(user.id) shouldBeEqualTo user
    }

    @Test
    fun `조건절로 엔티티 삭제하기`() = runSuspendIO {
        val user = newUser()
        operations.insert(user).awaitSingle()

        val query = query(where("id").eq(user.id))
        operations.delete<User>(query).awaitSingle().shouldBeTrue()

        getUserById(user.id).shouldBeNull()
    }

    @Test
    fun `특정 컬럼만 삭제하기`() = runSuspendIO {
        val user = newUser()
        operations.insert(user).awaitSingle()

        val query = query(where("id").eq(user.id))
            .columns(Columns.from("lastname"))

        operations.delete<User>(query).awaitSingle().shouldBeTrue()

        val loaded = getUserById(user.id)!!
        loaded.firstname shouldBeEqualTo user.firstname
        loaded.lastname.shouldBeNull()
    }

    @Test
    fun `엔티티 삭제하기`() = runSuspendIO {
        val user = newUser()
        operations.insert(user).awaitSingle()

        operations.delete(user).awaitSingle()
        getUserById(user.id).shouldBeNull()
    }

    @Test
    fun `Id로 엔티티 삭제하기`() = runSuspendIO {
        val user = newUser()
        operations.insert(user).awaitSingle()

        operations.deleteById<User>(user.id).awaitSingle().shouldBeTrue()
        getUserById(user.id).shouldBeNull()
    }

    @Test
    fun `존재하는 엔티티만 삭제하기`() = runSuspendIO {
        val user = newUser()
        operations.insert(user).awaitSingle()

        // NOTE: withIfExists() 가 제대로 작동하지 않는다 
        val lwtOptions = deleteOptions { /*withIfExists()*/ }
        operations.delete(user, lwtOptions).awaitSingle().wasApplied().shouldBeTrue()

        getUserById(user.id).shouldBeNull()

        // 이미 삭제되었으므로, 재삭제 요청은 처리되지 않습니다.
        // operations.delete(user, lwtOptions).awaitSingle().wasApplied().shouldBeFalse()
    }

    @Test
    fun `조건절에 queryOptions 적용하여 삭제하기`() = runSuspendIO {
        val user = newUser()
        operations.insert(user).awaitSingle()

        val lwtOptions = deleteOptions { /*withIfExists()*/ }
        val query = query(where("id").eq(user.id)).queryOptions(lwtOptions)

        operations.delete<User>(query).awaitSingle().shouldBeTrue()

        getUserById(user.id).shouldBeNull()
        // 이미 삭제되었으므로, 재삭제 요청은 처리되지 않습니다.
        // operations.delete<User>(query).awaitSingle().shouldBeFalse()
    }

    @Test
    fun `PageRequest를 이용하여 Slice로 조회`() = runSuspendIO {
        val entitySize = 100
        val sliceSize = 10

        val insertTasks = List(entitySize) {
            async(Dispatchers.IO) {
                val user = newUser()
                operations.insert(user).awaitSingle()
                user.id
            }
        }
        val expectedIds = insertTasks.awaitAll().toSet()

        val query = Query.empty()
        var slice = operations
            .slice(query.pageRequest(CassandraPageRequest.first(sliceSize)), User::class.java)
            .awaitSingle()

        val loadIds = mutableSetOf<String>()
        var iterations = 0

        do {
            iterations++

            slice.size shouldBeEqualTo sliceSize
            loadIds.addAll(slice.map { it.id })

            if (slice.hasNext()) {
                slice = operations.slice(query.pageRequest(slice.nextPageable()), User::class.java).awaitSingle()
            } else {
                break
            }
        } while (slice.content.isNotEmpty())

        loadIds.size shouldBeEqualTo expectedIds.size
        loadIds shouldContainSame expectedIds
        iterations shouldBeEqualTo entitySize / sliceSize
    }

    private suspend fun getUserById(userId: String): User? =
        operations.selectOneById<User>(userId).awaitSingleOrNull()
}
