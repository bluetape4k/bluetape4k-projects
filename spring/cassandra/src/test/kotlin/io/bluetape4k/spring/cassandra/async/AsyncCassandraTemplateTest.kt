package io.bluetape4k.spring.cassandra.async

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.metadata.schema.ClusteringOrder
import com.datastax.oss.driver.api.core.uuid.Uuids
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import io.bluetape4k.cassandra.querybuilder.eq
import io.bluetape4k.cassandra.querybuilder.literal
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.spring.cassandra.cql.deleteOptions
import io.bluetape4k.spring.cassandra.cql.updateOptions
import io.bluetape4k.spring.cassandra.domain.DomainTestConfiguration
import io.bluetape4k.spring.cassandra.domain.model.User
import io.bluetape4k.spring.cassandra.domain.model.UserToken
import io.bluetape4k.spring.cassandra.query.eq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.core.AsyncCassandraTemplate
import org.springframework.data.cassandra.core.EntityWriteResult
import org.springframework.data.cassandra.core.InsertOptions
import org.springframework.data.cassandra.core.count
import org.springframework.data.cassandra.core.delete
import org.springframework.data.cassandra.core.deleteById
import org.springframework.data.cassandra.core.exists
import org.springframework.data.cassandra.core.query.CassandraPageRequest
import org.springframework.data.cassandra.core.query.Columns
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.cassandra.core.query.query
import org.springframework.data.cassandra.core.query.where
import org.springframework.data.cassandra.core.select
import org.springframework.data.cassandra.core.selectOne
import org.springframework.data.cassandra.core.selectOneById
import org.springframework.data.cassandra.core.slice
import org.springframework.data.cassandra.core.truncate
import org.springframework.data.domain.Sort

@SpringBootTest(classes = [DomainTestConfiguration::class])
class AsyncCassandraTemplateTest(
    @Autowired private val cqlSession: CqlSession,
): AbstractCassandraCoroutineTest("coroutines-template") {

    companion object: KLogging()

    // NOTE: AsyncCassandraTemplate 는 직접 Injection 받을 수 없고, 이렇게 생성해야 한다.
    private val operations: AsyncCassandraTemplate by lazy {
        AsyncCassandraTemplate(cqlSession)
    }

    private fun newUser(): User =
        User(Uuids.timeBased().toString(), faker.name().firstName(), faker.name().lastName())

    @BeforeEach
    fun beforeEach() {
        runBlocking {
            operations.isUsePreparedStatements = false
            operations.truncate<User>().await()
            operations.truncate<UserToken>().await()
        }
    }

    @Test
    fun `조회 시 정렬 적용`() = runSuspendIO {
        val token1 = UserToken(
            userId = Uuids.timeBased(),
            token = Uuids.timeBased(),
            userComment = faker.community().character()
        )
        val token2 = UserToken(
            userId = token1.userId,
            token = Uuids.timeBased(),
            userComment = faker.community().character()
        )

        operations.insert(token1).await()
        operations.insert(token2).await()

        // DataStax QueryBuilder 로 조회 Query 생성
        val stmt = QueryBuilder.selectFrom("user_tokens").all()
            .whereColumn("user_id").eq(token1.userId.literal())
            .orderBy("auth_token", ClusteringOrder.ASC)
            .build()
        operations.select<UserToken>(stmt).await() shouldBeEqualTo listOf(token1, token2)

        // Spring Data Query로 조회
        val query = query(where("user_id").eq(token1.userId))
            .sort(Sort.by("auth_token"))
        operations.select<UserToken>(query).await() shouldBeEqualTo listOf(token1, token2)
    }

    @Test
    fun `단건 조회`() = runSuspendIO {
        val token1 = UserToken(
            userId = Uuids.timeBased(),
            token = Uuids.timeBased(),
            userComment = faker.community().character()
        )
        val token2 = UserToken(
            userId = token1.userId,
            token = Uuids.timeBased(),
            userComment = faker.community().character()
        )

        operations.insert(token1).await()
        operations.insert(token2).await()

        val query = query(where("user_id").eq(token1.userId))
        operations.selectOne<UserToken>(query).await() shouldBeEqualTo token1
    }


    @Test
    fun `insert entity`() = runSuspendIO {
        val user = newUser()
        getUserById(user.id).shouldBeNull()

        operations.insert(user).await() shouldBeEqualTo user
        getUserById(user.id) shouldBeEqualTo user
    }

    @Test
    fun `존재하지 않을 시에만 신규로 저장`() = runSuspendIO {
        val lwtOption = InsertOptions.builder().withIfNotExists().build()
        val user = newUser()

        val inserted = operations.insert(user, lwtOption).await()
        inserted.wasApplied().shouldBeTrue()

        // 이미 있기 때문에 insert 안된다
        val user2 = user.copy(firstname = "Sunghyouk", lastname = "Bae")
        operations.insert(user2, lwtOption).await().wasApplied().shouldBeFalse()

        getUserById(user.id) shouldBeEqualTo user
    }

    @Test
    fun `레코드 count 조회`() = runSuspendIO {
        val user = newUser()
        val saved = operations.insert(user).await()!!
        saved shouldBeEqualTo user

        operations.count<User>().await() shouldBeEqualTo 1L
    }

    @Test
    fun `조건절을 이용한 count 조회`() = runSuspendIO {
        val user1 = newUser()
        val user2 = newUser()
        operations.insert(user1).await()
        operations.insert(user2).await()

        operations.count<User>(query(where("id").eq(user1.id))).await() shouldBeEqualTo 1L
        operations.count<User>(query(where("id").eq("not exists"))).await() shouldBeEqualTo 0L
    }

    @Test
    fun `exists 조회`() = runSuspendIO {
        val user1 = newUser()
        val user2 = newUser()
        operations.insert(user1).await()
        operations.insert(user2).await()

        operations.exists<User>(user1.id).await().shouldBeTrue()
        operations.exists<User>("not exists id").await().shouldBeFalse()

        operations.exists<User>(query(where("id").eq(user1.id))).await().shouldBeTrue()
        operations.exists<User>(query(where("id").eq("not exists"))).await().shouldBeFalse()
    }

    @Test
    fun `엔티티 갱신하기`() = runSuspendIO {
        val user = newUser()
        operations.insert(user).await()

        user.firstname = "성혁"
        val updated = operations.update(user).await()
        updated.id shouldBeEqualTo user.id
    }

    @Test
    fun `존재하지 않으면 Update 하지 않기`() = runSuspendIO {
        // 존재하지 않는 엔티티를 Update 하는 경우에는 아무 작업도 하지 않도록 합니다.
        val user = newUser()

        val lwtOptions = updateOptions { withIfExists() }
        val result: EntityWriteResult<User> = operations.update(user, lwtOptions).await()
        result.wasApplied().shouldBeFalse()

        getUserById(user.id).shouldBeNull()
    }

    @Test
    fun `존재하는 엔티티를 Update 하기`() = runSuspendIO {
        val user = newUser()
        operations.insert(user).await()

        user.firstname = "성혁"
        // NOTE: withIfExists() 가 제대로 작동하지 않는다
        val options = updateOptions {
            // withIfExists()
            withTracing()
        }
        val result = operations.update(user, options).await()
        result.wasApplied().shouldBeTrue()

        getUserById(user.id) shouldBeEqualTo user
    }

    @Test
    fun `조건절로 엔티티 삭제하기`() = runSuspendIO {
        val user = newUser()
        operations.insert(user).await()

        val query = query(where("id").eq(user.id))
        operations.delete<User>(query).await().shouldBeTrue()

        getUserById(user.id).shouldBeNull()
    }

    @Test
    fun `특정 컬럼만 삭제하기`() = runSuspendIO {
        val user = newUser()
        operations.insert(user).await()!!

        val query = query(where("id").eq(user.id))
            .columns(Columns.from("lastname"))

        operations.delete<User>(query).await().shouldBeTrue()

        val loaded = getUserById(user.id)!!
        loaded.firstname shouldBeEqualTo user.firstname
        loaded.lastname.shouldBeNull()
    }

    @Test
    fun `엔티티 삭제하기`() = runSuspendIO {
        val user = newUser()
        operations.insert(user).await()!!

        operations.delete(user).await()

        getUserById(user.id).shouldBeNull()
    }

    @Test
    fun `Id로 엔티티 삭제하기`() = runSuspendIO {
        val user = newUser()
        operations.insert(user).await()!!

        operations.deleteById<User>(user.id).await().shouldBeTrue()
        getUserById(user.id).shouldBeNull()
    }

    @Test
    fun `존재하는 엔티티만 삭제하기`() = runSuspendIO {
        val user = newUser()
        operations.insert(user).await()!!

        // NOTE: withIfExists() 가 제대로 작동하지 않는다
        val lwtOptions = deleteOptions {
            // withIfExists()
        }
        operations.delete(user, lwtOptions).await().wasApplied().shouldBeTrue()

        getUserById(user.id).shouldBeNull()
        // 이미 삭제되었으므로, 재삭제 요청은 처리되지 않습니다.
        // operations.delete(user, lwtOptions).await().wasApplied().shouldBeFalse()
    }

    @Test
    fun `조건절에 queryOptions 적용하여 삭제하기`() = runSuspendIO {
        val user = newUser()
        operations.insert(user).await()!!

        // NOTE: withIfExists() 가 제대로 작동하지 않는다 
        val lwtOptions = deleteOptions {
            // withIfExists() // 존재하는 엔티티만 삭제합니다.
        }
        val query = query(where("id").eq(user.id)).queryOptions(lwtOptions)
        operations.delete<User>(query).await().shouldBeTrue()

        getUserById(user.id).shouldBeNull()
        // 이미 삭제되었으므로, 재삭제 요청은 처리되지 않습니다.
        // operations.delete<User>(query).await().shouldBeFalse()
    }

    @Test
    fun `PageRequest를 이용하여 Slice로 조회`() = runSuspendIO {
        val entitySize = 100
        val sliceSize = 10

        val insertTasks = List(entitySize) {
            async(Dispatchers.IO) {
                val user = newUser()
                operations.insert(user).await()
                user.id
            }
        }
        val expectedIds = insertTasks.awaitAll().toSet()

        val query = Query.empty()
        var slice = operations.slice<User>(query.pageRequest(CassandraPageRequest.first(sliceSize))).await()

        val loadIds = mutableSetOf<String>()
        var iterations = 0

        do {
            iterations++

            slice.size shouldBeEqualTo sliceSize
            loadIds.addAll(slice.map { it.id })

            if (slice.hasNext()) {
                slice = operations.slice<User>(query.pageRequest(slice.nextPageable())).await()
            } else {
                break
            }
        } while (slice.content.isNotEmpty())

        loadIds.size shouldBeEqualTo entitySize
        loadIds shouldContainSame expectedIds
        iterations shouldBeEqualTo entitySize / sliceSize
    }

    private suspend fun getUserById(id: String): User? =
        operations.selectOneById<User>(id).await()
}
