package io.bluetape4k.exposed.jdbc.repository

import io.bluetape4k.exposed.jdbc.repository.EdgeCaseSchema.EdgeCaseRecord
import io.bluetape4k.exposed.jdbc.repository.EdgeCaseSchema.EdgeCaseRepository
import io.bluetape4k.exposed.jdbc.repository.EdgeCaseSchema.EdgeCaseTable
import io.bluetape4k.exposed.jdbc.repository.EdgeCaseSchema.withEdgeCaseTable
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * JdbcRepository의 존재 여부 확인 메서드 (isEmpty, isNotEmpty, existsBy, existsById, exists, countBy) 테스트.
 */
class JdbcRepositoryExistenceTest : AbstractExposedTest() {

    companion object : KLogging()

    /**
     * isEmpty 는 데이터가 없는 빈 테이블에서 true 를 반환해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `isEmpty 는 빈 테이블에서 true 를 반환한다`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            EdgeCaseRepository.isEmpty().shouldBeTrue()
        }
    }

    /**
     * isEmpty 는 테이블에 데이터가 존재할 경우 false 를 반환해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `isEmpty 는 데이터 존재 시 false 를 반환한다`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Alice", age = 30))
            EdgeCaseRepository.isEmpty().shouldBeFalse()
        }
    }

    /**
     * isNotEmpty 는 isEmpty 의 부정값과 동일하게 동작해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `isNotEmpty 는 isEmpty 의 부정값을 반환한다`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            EdgeCaseRepository.isNotEmpty() shouldBeEqualTo !EdgeCaseRepository.isEmpty()

            EdgeCaseRepository.save(EdgeCaseRecord(name = "Bob", age = 25))

            EdgeCaseRepository.isNotEmpty() shouldBeEqualTo !EdgeCaseRepository.isEmpty()
        }
    }

    /**
     * existsBy 는 조건에 맞는 row 가 존재할 때 true 를 반환해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `existsBy 는 조건에 맞는 row 가 있으면 true`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Charlie", age = 40))
            EdgeCaseRepository.existsBy { EdgeCaseTable.name eq "Charlie" }.shouldBeTrue()
        }
    }

    /**
     * existsBy 는 조건에 맞는 row 가 없을 때 false 를 반환해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `existsBy 는 조건에 맞지 않으면 false`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Dave", age = 20))
            EdgeCaseRepository.existsBy { EdgeCaseTable.name eq "NotExist" }.shouldBeFalse()
        }
    }

    /**
     * existsById 는 존재하는 id 에 대해 true 를 반환해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `existsById 는 존재하는 id 에 true`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            val saved = EdgeCaseRepository.save(EdgeCaseRecord(name = "Eve", age = 35))
            EdgeCaseRepository.existsById(saved.id).shouldBeTrue()
        }
    }

    /**
     * existsById 는 존재하지 않는 id 에 대해 false 를 반환해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `existsById 는 없는 id 에 false`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            EdgeCaseRepository.existsById(Long.MAX_VALUE).shouldBeFalse()
        }
    }

    /**
     * exists(query) 는 서브쿼리 결과가 존재할 때 true, 없을 때 false 를 반환해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `exists(query) 는 서브쿼리 결과 존재 여부 반환`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Frank", age = 28))

            val existingQuery = EdgeCaseTable.selectAll()
                .where { EdgeCaseTable.name eq "Frank" }
                .limit(1)
            EdgeCaseRepository.exists(existingQuery).shouldBeTrue()

            val missingQuery = EdgeCaseTable.selectAll()
                .where { EdgeCaseTable.name eq "Ghost" }
                .limit(1)
            EdgeCaseRepository.exists(missingQuery).shouldBeFalse()
        }
    }

    /**
     * countBy(op) 는 Op 조건에 맞는 row 수를 정확하게 반환해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `countBy(op) 는 Op 조건에 맞는 row 수를 반환`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Grace", age = 22))
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Henry", age = 22))
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Iris", age = 30))

            val op = EdgeCaseTable.age eq 22
            EdgeCaseRepository.countBy(op) shouldBeEqualTo 2L
        }
    }

    /**
     * countBy(predicate) 람다 오버로드도 동일하게 동작해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `countBy(predicate) 람다 오버로드 동작`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Jack", age = 50, isActive = true))
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Kate", age = 50, isActive = false))
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Leo", age = 50, isActive = true))

            val count = EdgeCaseRepository.countBy { EdgeCaseTable.isActive eq true }
            count shouldBeEqualTo 2L
        }
    }

    /**
     * countBy 는 조건에 맞는 row 가 없을 때 0 을 반환해야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `countBy 결과 0 일 때도 정상 동작`(testDB: TestDB) {
        withEdgeCaseTable(testDB) {
            EdgeCaseRepository.save(EdgeCaseRecord(name = "Mia", age = 18))

            val op = EdgeCaseTable.age eq 99
            EdgeCaseRepository.countBy(op) shouldBeEqualTo 0L

            val countByPredicate = EdgeCaseRepository.countBy { EdgeCaseTable.age eq 99 }
            countByPredicate shouldBeEqualTo 0L
        }
    }
}
