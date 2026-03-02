package io.bluetape4k.spring.mongodb.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.mongodb.AbstractReactiveMongoCoroutineTest
import io.bluetape4k.spring.mongodb.model.User
import io.bluetape4k.spring.mongodb.query.criteria
import io.bluetape4k.spring.mongodb.query.eq
import io.bluetape4k.spring.mongodb.query.gt
import io.bluetape4k.spring.mongodb.query.paginate
import io.bluetape4k.spring.mongodb.query.queryOf
import io.bluetape4k.spring.mongodb.query.andSet
import io.bluetape4k.spring.mongodb.query.setTo
import io.bluetape4k.spring.mongodb.query.sortAscBy
import io.bluetape4k.spring.mongodb.query.sortDescBy
import io.bluetape4k.spring.mongodb.query.toQuery
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

@DataMongoTest
class ReactiveMongoOperationsCoroutinesTest: AbstractReactiveMongoCoroutineTest() {

    /** 도시별 사용자 수 집계 결과 모델 */
    data class CityCount(@Id val id: String, val count: Int)

    companion object: KLoggingChannel() {
        private val testUsers = listOf(
            User(name = "Alice", email = "alice@test.com", age = 30, city = "Seoul"),
            User(name = "Bob", email = "bob@test.com", age = 25, city = "Busan"),
            User(name = "Charlie", email = "charlie@test.com", age = 35, city = "Seoul"),
            User(name = "Diana", email = "diana@test.com", age = 28, city = "Incheon"),
            User(name = "Eve", email = "eve@test.com", age = 22, city = "Seoul"),
        )
    }

    @BeforeEach
    fun setUp(): Unit = runTest {
        mongoOperations.dropCollectionSuspending<User>()
        mongoOperations.insertAllAsFlow(testUsers).toList()
        log.debug { "Test data inserted: ${testUsers.size} users" }
    }

    // ====================================================
    // Insert / Save
    // ====================================================

    @Test
    fun `insertSuspending - 새 사용자를 삽입한다`() = runTest {
        val newUser = User(name = "Frank", email = "frank@test.com", age = 40, city = "Daejeon")
        val saved = mongoOperations.insertSuspending(newUser)

        saved.id.shouldNotBeNull()
        saved.name shouldBeEqualTo "Frank"
        saved.email shouldBeEqualTo "frank@test.com"
    }

    @Test
    fun `saveSuspending - 기존 사용자를 업데이트한다`() = runTest {
        val original = mongoOperations.findOneOrNullSuspending<User>(
            Criteria.where("name").`is`("Alice").toQuery()
        )
        original.shouldNotBeNull()

        val updated = mongoOperations.saveSuspending(original.copy(age = 31))
        updated.id shouldBeEqualTo original.id
        updated.age shouldBeEqualTo 31
    }

    // ====================================================
    // Find
    // ====================================================

    @Test
    fun `findAsFlow - 조건에 맞는 사용자를 Flow로 조회한다`() = runTest {
        val users = mongoOperations.findAsFlow<User>(
            Query(Criteria.where("city").`is`("Seoul"))
        ).toList()

        users shouldHaveSize 3
        users.all { it.city == "Seoul" }.shouldBeTrue()
    }

    @Test
    fun `findAllAsFlow - 전체 사용자를 Flow로 조회한다`() = runTest {
        val users = mongoOperations.findAllAsFlow<User>().toList()
        users shouldHaveSize testUsers.size
    }

    @Test
    fun `findOneOrNullSuspending - 단건을 조회한다`() = runTest {
        val user = mongoOperations.findOneOrNullSuspending<User>(
            Criteria.where("name").`is`("Bob").toQuery()
        )

        user.shouldNotBeNull()
        user.name shouldBeEqualTo "Bob"
    }

    @Test
    fun `findOneOrNullSuspending - 결과가 없으면 null을 반환한다`() = runTest {
        val user = mongoOperations.findOneOrNullSuspending<User>(
            Criteria.where("name").`is`("Unknown").toQuery()
        )
        user.shouldBeNull()
    }

    @Test
    fun `findByIdOrNullSuspending - ID로 사용자를 조회한다`() = runTest {
        val alice = mongoOperations.findOneOrNullSuspending<User>(
            Criteria.where("name").`is`("Alice").toQuery()
        )
        alice.shouldNotBeNull()

        val found = mongoOperations.findByIdOrNullSuspending<User>(alice.id!!)
        found.shouldNotBeNull()
        found.name shouldBeEqualTo "Alice"
    }

    @Test
    fun `findByIdOrNullSuspending - 존재하지 않는 ID이면 null을 반환한다`() = runTest {
        val found = mongoOperations.findByIdOrNullSuspending<User>("non-existent-id")
        found.shouldBeNull()
    }

    // ====================================================
    // Sort / Paging
    // ====================================================

    @Test
    fun `findAsFlow - 오름차순으로 정렬하여 조회한다`() = runTest {
        val query = Query().sortAscBy("age")
        val users = mongoOperations.findAsFlow<User>(query).toList()

        users shouldHaveSize testUsers.size
        users.first().name shouldBeEqualTo "Eve"    // age 22
        users.last().name shouldBeEqualTo "Charlie" // age 35
    }

    @Test
    fun `findAsFlow - 내림차순으로 정렬하여 조회한다`() = runTest {
        val query = Query().sortDescBy("age")
        val users = mongoOperations.findAsFlow<User>(query).toList()

        users.first().name shouldBeEqualTo "Charlie" // age 35
        users.last().name shouldBeEqualTo "Eve"      // age 22
    }

    @Test
    fun `findAsFlow - 페이지네이션을 적용하여 조회한다`() = runTest {
        val query = Query().sortAscBy("age").paginate(page = 0, size = 2)
        val users = mongoOperations.findAsFlow<User>(query).toList()

        users shouldHaveSize 2
        users[0].name shouldBeEqualTo "Eve"   // age 22
        users[1].name shouldBeEqualTo "Bob"   // age 25
    }

    // ====================================================
    // Count / Exists
    // ====================================================

    @Test
    fun `countSuspending - 전체 사용자 수를 반환한다`() = runTest {
        val count = mongoOperations.countSuspending<User>()
        count shouldBeEqualTo testUsers.size.toLong()
    }

    @Test
    fun `countSuspending - 조건에 맞는 사용자 수를 반환한다`() = runTest {
        val count = mongoOperations.countSuspending<User>(
            Query(Criteria.where("city").`is`("Seoul"))
        )
        count shouldBeEqualTo 3L
    }

    @Test
    fun `existsSuspending - 조건에 맞는 문서가 존재하면 true를 반환한다`() = runTest {
        val exists = mongoOperations.existsSuspending<User>(
            Query(Criteria.where("name").`is`("Alice"))
        )
        exists.shouldBeTrue()
    }

    @Test
    fun `existsSuspending - 조건에 맞는 문서가 없으면 false를 반환한다`() = runTest {
        val exists = mongoOperations.existsSuspending<User>(
            Query(Criteria.where("name").`is`("Unknown"))
        )
        exists.shouldBeFalse()
    }

    // ====================================================
    // Update
    // ====================================================

    @Test
    fun `updateFirstSuspending - 첫 번째 문서를 업데이트한다`() = runTest {
        val result = mongoOperations.updateFirstSuspending<User>(
            Query(Criteria.where("city").`is`("Seoul")),
            Update().set("city", "Suwon")
        )
        result.modifiedCount shouldBeEqualTo 1L

        val suwonCount = mongoOperations.countSuspending<User>(
            Query(Criteria.where("city").`is`("Suwon"))
        )
        suwonCount shouldBeEqualTo 1L
    }

    @Test
    fun `updateMultiSuspending - 조건에 맞는 모든 문서를 업데이트한다`() = runTest {
        val result = mongoOperations.updateMultiSuspending<User>(
            Query(Criteria.where("city").`is`("Seoul")),
            Update().set("city", "Suwon")
        )
        result.modifiedCount shouldBeEqualTo 3L

        val suwonCount = mongoOperations.countSuspending<User>(
            Query(Criteria.where("city").`is`("Suwon"))
        )
        suwonCount shouldBeEqualTo 3L
    }

    @Test
    fun `upsertSuspending - 문서가 없으면 삽입한다`() = runTest {
        val update = ("name" setTo "NewUser")
            .andSet("email", "newuser@test.com")
            .andSet("age", 20)
            .andSet("city", "Ulsan")

        val result = mongoOperations.upsertSuspending<User>(
            Query(Criteria.where("name").`is`("NewUser")),
            update
        )
        result.upsertedId.shouldNotBeNull()

        val count = mongoOperations.countSuspending<User>()
        count shouldBeEqualTo (testUsers.size + 1).toLong()
    }

    // ====================================================
    // Remove
    // ====================================================

    @Test
    fun `removeSuspending(query) - 조건에 맞는 문서를 삭제한다`() = runTest {
        val result = mongoOperations.removeSuspending<User>(
            Query(Criteria.where("name").`is`("Alice"))
        )
        result.deletedCount shouldBeEqualTo 1L

        val count = mongoOperations.countSuspending<User>()
        count shouldBeEqualTo (testUsers.size - 1).toLong()
    }

    // ====================================================
    // Find And Modify / Remove
    // ====================================================

    @Test
    fun `findAndModifySuspending - 문서를 수정하고 수정 전 문서를 반환한다`() = runTest {
        val original = mongoOperations.findAndModifySuspending<User>(
            Query(Criteria.where("name").`is`("Alice")),
            Update().set("age", 31)
        )
        original.shouldNotBeNull()
        original.age shouldBeEqualTo 30  // 수정 전 값

        val updated = mongoOperations.findOneOrNullSuspending<User>(
            Query(Criteria.where("name").`is`("Alice"))
        )
        updated.shouldNotBeNull()
        updated.age shouldBeEqualTo 31
    }

    @Test
    fun `findAndRemoveSuspending - 문서를 삭제하고 삭제된 문서를 반환한다`() = runTest {
        val removed = mongoOperations.findAndRemoveSuspending<User>(
            Query(Criteria.where("name").`is`("Alice"))
        )
        removed.shouldNotBeNull()
        removed.name shouldBeEqualTo "Alice"

        val count = mongoOperations.countSuspending<User>()
        count shouldBeEqualTo (testUsers.size - 1).toLong()
    }

    // ====================================================
    // Aggregation
    // ====================================================

    @Test
    fun `aggregateAsFlow - 도시별 사용자 수를 집계한다`() = runTest {
        val aggregation = Aggregation.newAggregation(
            Aggregation.group("city").count().`as`("count"),
            Aggregation.sort(Sort.by(Sort.Direction.DESC, "count"))
        )

        val results = mongoOperations.aggregateAsFlow<User, CityCount>(aggregation).toList()

        results.isNotEmpty().shouldBeTrue()
        val seoulCount = results.find { it.id == "Seoul" }
        seoulCount.shouldNotBeNull()
        seoulCount.count shouldBeEqualTo 3
    }

    // ====================================================
    // Criteria infix DSL 통합 테스트
    // ====================================================

    @Test
    fun `criteria infix DSL - gt를 이용한 나이 조건 조회`() = runTest {
        val users = mongoOperations.findAsFlow<User>(
            queryOf("age".criteria() gt 28)
        ).toList()

        users.all { it.age > 28 }.shouldBeTrue()
        users shouldHaveSize 2  // Alice(30), Charlie(35)
    }

    @Test
    fun `criteria infix DSL - eq를 이용한 이름 조건 조회`() = runTest {
        val user = mongoOperations.findOneOrNullSuspending<User>(
            queryOf("name".criteria() eq "Alice")
        )
        user.shouldNotBeNull()
        user.name shouldBeEqualTo "Alice"
    }
}
