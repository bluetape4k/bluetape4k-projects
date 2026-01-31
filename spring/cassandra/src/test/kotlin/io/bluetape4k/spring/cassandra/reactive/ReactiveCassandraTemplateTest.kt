package io.bluetape4k.spring.cassandra.reactive

import com.datastax.oss.driver.api.core.uuid.Uuids
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.spring.cassandra.cql.deleteOptions
import io.bluetape4k.spring.cassandra.cql.insertOptions
import io.bluetape4k.spring.cassandra.cql.updateOptions
import io.bluetape4k.spring.cassandra.domain.ReactiveDomainTestConfiguration
import io.bluetape4k.spring.cassandra.domain.model.User
import io.bluetape4k.spring.cassandra.query.eq
import io.bluetape4k.spring.cassandra.suspendCount
import io.bluetape4k.spring.cassandra.suspendDelete
import io.bluetape4k.spring.cassandra.suspendDeleteById
import io.bluetape4k.spring.cassandra.suspendExists
import io.bluetape4k.spring.cassandra.suspendInsert
import io.bluetape4k.spring.cassandra.suspendSelectOneOrNullById
import io.bluetape4k.spring.cassandra.suspendSlice
import io.bluetape4k.spring.cassandra.suspendTruncate
import io.bluetape4k.spring.cassandra.suspendUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
import org.springframework.data.cassandra.core.query.CassandraPageRequest
import org.springframework.data.cassandra.core.query.Columns
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.cassandra.core.query.query
import org.springframework.data.cassandra.core.query.where
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories

@SpringBootTest(classes = [ReactiveDomainTestConfiguration::class])
@EnableReactiveCassandraRepositories
class ReactiveCassandraTemplateTest(
    @param:Autowired private val reactiveOps: ReactiveCassandraOperations,
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
            reactiveOps.suspendTruncate<User>()
        }
    }

    @Test
    fun `context loading`() {
        reactiveOps.shouldNotBeNull()
    }

    @Test
    fun `새로운 엔티티 추가`() = runSuspendIO {
        val user = newUser()
        reactiveOps.suspendInsert(user)
    }


    @Test
    fun `엔티티가 없을 때에만 추가`() = runSuspendIO {
        val lwtOptions = insertOptions { withIfNotExists() }
        val user = newUser()

        val inserted = reactiveOps.suspendInsert(user, lwtOptions)
        inserted.wasApplied().shouldBeTrue()
        inserted.entity shouldBeEqualTo user

        val user2 = user.copy(firstname = "성혁")
        val notInserted = reactiveOps.suspendInsert(user2, lwtOptions)
        notInserted.wasApplied().shouldBeFalse()
    }

    @Test
    fun `엔티티 Count 조회`() = runSuspendIO {
        val user = newUser()

        reactiveOps.suspendInsert(user)
        reactiveOps.suspendCount<User>() shouldBeEqualTo 1L
    }

    @Test
    fun `조건절을 이용한 count 조회`() = runSuspendIO {
        val user1 = newUser()
        val user2 = newUser()
        reactiveOps.suspendInsert(user1)
        reactiveOps.suspendInsert(user2)

        reactiveOps.suspendCount<User>(query(where("id").eq(user1.id))) shouldBeEqualTo 1L
        reactiveOps.suspendCount<User>(query(where("id").eq("not-exists"))) shouldBeEqualTo 0L
    }

    @Test
    fun `exists 조회`() = runSuspendIO {
        val user1 = newUser()
        val user2 = newUser()
        reactiveOps.suspendInsert(user1)
        reactiveOps.suspendInsert(user2)

        reactiveOps.suspendExists<User>(user1.id).shouldBeTrue()
        reactiveOps.suspendExists<User>("not exists id").shouldBeFalse()

        reactiveOps.suspendExists<User>(query(where("id").eq(user1.id))).shouldBeTrue()
        reactiveOps.suspendExists<User>(query(where("id").eq("not-exists@example.com"))).shouldBeFalse()
    }

    @Test
    fun `엔티티 갱신하기`() = runSuspendIO {
        val user = newUser()
        reactiveOps.suspendInsert(user)

        user.firstname = "성혁"
        val updated = reactiveOps.suspendUpdate(user)
        updated.id shouldBeEqualTo user.id
    }

    @Test
    fun `존재하지 않으면 Update 하지 않기`() = runSuspendIO {
        // 존재하지 않는 엔티티를 Update 하는 경우에는 아무 작업도 하지 않도록 합니다.
        val user = newUser()
        val lwtOptions = UpdateOptions.builder().withIfExists().build()

        val result = reactiveOps.suspendUpdate(user, lwtOptions)
        result.wasApplied().shouldBeFalse()

        getUserById(user.id).shouldBeNull()
    }

    @Test
    fun `존재하는 엔티티를 Update 하기`() = runSuspendIO {
        val user = newUser()
        reactiveOps.suspendInsert(user)

        user.firstname = "성혁"
        // NOTE: withIfExists() 가 제대로 작동하지 않는다
        val lwtOptions = updateOptions { /*withIfExists()*/ }
        val result = reactiveOps.suspendUpdate(user, lwtOptions)
        result.wasApplied().shouldBeTrue()

        getUserById(user.id) shouldBeEqualTo user
    }

    @Test
    fun `조건절로 엔티티 삭제하기`() = runSuspendIO {
        val user = newUser()
        reactiveOps.suspendInsert(user)

        val query = query(where("id").eq(user.id))
        reactiveOps.suspendDelete<User>(query).shouldBeTrue()

        getUserById(user.id).shouldBeNull()
    }

    @Test
    fun `특정 컬럼만 삭제하기`() = runSuspendIO {
        val user = newUser()
        reactiveOps.suspendInsert(user)

        val query = query(where("id").eq(user.id))
            .columns(Columns.from("lastname"))

        reactiveOps.suspendDelete<User>(query).shouldBeTrue()

        val loaded = getUserById(user.id)!!
        loaded.firstname shouldBeEqualTo user.firstname
        loaded.lastname.shouldBeNull()
    }

    @Test
    fun `엔티티 삭제하기`() = runSuspendIO {
        val user = newUser()
        reactiveOps.suspendInsert(user)

        reactiveOps.suspendDelete(user)
        getUserById(user.id).shouldBeNull()
    }

    @Test
    fun `Id로 엔티티 삭제하기`() = runSuspendIO {
        val user = newUser()
        reactiveOps.suspendInsert(user)

        reactiveOps.suspendDeleteById<User>(user.id).shouldBeTrue()
        getUserById(user.id).shouldBeNull()
    }

    @Test
    fun `존재하는 엔티티만 삭제하기`() = runSuspendIO {
        val user = newUser()
        reactiveOps.suspendInsert(user)

        // NOTE: withIfExists() 가 제대로 작동하지 않는다 
        val lwtOptions = deleteOptions { /*withIfExists()*/ }
        reactiveOps.suspendDelete(user, lwtOptions).wasApplied().shouldBeTrue()

        getUserById(user.id).shouldBeNull()

        // 이미 삭제되었으므로, 재삭제 요청은 처리되지 않습니다.
        // operations.delete(user, lwtOptions).awaitSingle().wasApplied().shouldBeFalse()
    }

    @Test
    fun `조건절에 queryOptions 적용하여 삭제하기`() = runSuspendIO {
        val user = newUser()
        reactiveOps.suspendInsert(user)

        val lwtOptions = deleteOptions { /*withIfExists()*/ }
        val query = query(where("id").eq(user.id)).queryOptions(lwtOptions)

        reactiveOps.suspendDelete<User>(query).shouldBeTrue()

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
                reactiveOps.suspendInsert(user)
                user.id
            }
        }
        val expectedIds = insertTasks.awaitAll().toUnifiedSet()

        val query = Query.empty()
        var slice = reactiveOps
            .suspendSlice<User>(query.pageRequest(CassandraPageRequest.first(sliceSize)))

        val loadIds = mutableSetOf<String>()
        var iterations = 0

        do {
            iterations++

            slice.size shouldBeEqualTo sliceSize
            loadIds.addAll(slice.map { it.id })

            if (slice.hasNext()) {
                slice = reactiveOps.suspendSlice<User>(query.pageRequest(slice.nextPageable()))
            } else {
                break
            }
        } while (slice.content.isNotEmpty())

        loadIds.size shouldBeEqualTo expectedIds.size
        loadIds shouldContainSame expectedIds
        iterations shouldBeEqualTo entitySize / sliceSize
    }

    private suspend fun getUserById(userId: String): User? =
        reactiveOps.suspendSelectOneOrNullById<User>(userId)
}
