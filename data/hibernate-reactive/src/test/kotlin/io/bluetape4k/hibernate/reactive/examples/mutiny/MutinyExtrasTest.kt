package io.bluetape4k.hibernate.reactive.examples.mutiny

import io.bluetape4k.hibernate.reactive.examples.model.Author
import io.bluetape4k.hibernate.reactive.examples.model.Book
import io.bluetape4k.hibernate.reactive.mutiny.asMutinySessionFactory
import io.bluetape4k.hibernate.reactive.mutiny.createQueryAs
import io.bluetape4k.hibernate.reactive.mutiny.createSelectionQueryAs
import io.bluetape4k.hibernate.reactive.mutiny.findAs
import io.bluetape4k.hibernate.reactive.mutiny.getAs
import io.bluetape4k.hibernate.reactive.mutiny.withSessionSuspending
import io.bluetape4k.hibernate.reactive.mutiny.withStatelessSessionSuspending
import io.bluetape4k.hibernate.reactive.mutiny.withStatelessTransactionSuspending
import io.bluetape4k.hibernate.reactive.mutiny.withTransactionSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.LocalDate
import java.time.Month
import kotlin.test.assertFailsWith

/**
 * Mutiny API 추가 커버리지 테스트
 *
 * - [asMutinySessionFactory] unwrap 검증
 * - exception propagation 검증 (예외가 세션 경계 밖으로 올바르게 전파되는지)
 * - [createQueryAs], [createSelectionQueryAs], [getAs] 추가 커버리지
 * - [withStatelessTransactionSuspending] 단독 세션 조회 검증
 * - count 쿼리 반복 검증
 */
@Execution(ExecutionMode.SAME_THREAD)
class MutinyExtrasTest: AbstractMutinyTest() {

    companion object: KLoggingChannel()

    private val author1 = Author(faker.name().name())
    private val author2 = Author(faker.name().name())
    private val book1 = Book(
        faker.numerify("#-#####-###-#"),
        faker.book().title(),
        LocalDate.of(1990, Month.MARCH, 1)
    )
    private val book2 = Book(
        faker.numerify("#-#####-###-#"),
        faker.book().title(),
        LocalDate.of(2000, Month.JULY, 1)
    )

    @BeforeAll
    fun beforeAll() = runSuspendIO {
        author1.addBook(book1)
        author2.addBook(book2)

        sf.withTransactionSuspending { session ->
            session.persistAll(author1, author2).awaitSuspending()
        }
    }

    /**
     * [asMutinySessionFactory] 이 유효한 SessionFactory 를 반환하는지 검증합니다.
     * unwrap 이후 세션을 열어 쿼리를 실행할 수 있어야 합니다.
     */
    @Test
    fun `asMutinySessionFactory 는 유효한 SessionFactory 를 반환한다`() = runSuspendIO {
        sf.shouldNotBeNull()
        sf.isOpen shouldBeEqualTo true

        val count = sf.withSessionSuspending { session ->
            session.createSelectionQueryAs<Long>("select count(a) from Author a")
                .singleResult
                .awaitSuspending()
        }

        // 최소 2명의 author 가 존재해야 합니다 (BeforeAll 에서 저장)
        count shouldBeGreaterOrEqualTo 2L
    }

    /**
     * [withSessionSuspending] 내부에서 발생한 예외가 호출자로 올바르게 전파되는지 검증합니다.
     * - 예외 타입이 보존되어야 합니다.
     */
    @Test
    fun `withSessionSuspending 내부 예외가 호출자로 전파된다`() = runSuspendIO {
        assertFailsWith<IllegalStateException> {
            sf.withSessionSuspending { _ ->
                error("의도적인 테스트 예외")
            }
        }
    }

    /**
     * [withTransactionSuspending] 내부에서 발생한 예외가 호출자로 전파되는지 검증합니다.
     * - 롤백 후 예외가 전파되어야 합니다.
     */
    @Test
    fun `withTransactionSuspending 내부 예외가 호출자로 전파된다`() = runSuspendIO {
        assertFailsWith<RuntimeException> {
            sf.withTransactionSuspending { _ ->
                throw RuntimeException("트랜잭션 내 의도적 예외")
            }
        }
    }

    /**
     * [withStatelessSessionSuspending] 에서 [createQueryAs] 로 엔티티를 조회합니다.
     */
    @Test
    fun `withStatelessSessionSuspending 에서 createQueryAs 로 Authors 를 조회한다`() = runSuspendIO {
        val authors = sf.withStatelessSessionSuspending { session ->
            session.createQueryAs<Author>("select a from Author a")
                .resultList
                .awaitSuspending()
        }

        // BeforeAll 에서 최소 2명 저장
        authors.size shouldBeGreaterOrEqualTo 2
    }

    /**
     * [withStatelessTransactionSuspending] 에서 단건 엔티티를 [getAs] 로 조회합니다.
     */
    @Test
    fun `withStatelessTransactionSuspending 에서 getAs 로 엔티티를 조회한다`() = runSuspendIO {
        val found = sf.withStatelessTransactionSuspending { session ->
            session.getAs<Author>(author1.id).awaitSuspending()
        }

        found.shouldNotBeNull()
        found.id shouldBeEqualTo author1.id
        found.name shouldBeEqualTo author1.name
    }

    /**
     * [withStatelessSessionSuspending] 에서 [createSelectionQueryAs] 로 count 를 조회합니다.
     */
    @Test
    fun `withStatelessSessionSuspending 에서 createSelectionQueryAs 로 count 를 조회한다`() = runSuspendIO {
        val count = sf.withStatelessSessionSuspending { session ->
            session.createSelectionQueryAs<Long>("select count(b) from Book b")
                .singleResult
                .awaitSuspending()
        }

        count shouldBeGreaterOrEqualTo 2L
    }

    /**
     * [withSessionSuspending] 에서 존재하지 않는 엔티티 조회 시 null 이 반환되는지 검증합니다.
     */
    @Test
    fun `존재하지 않는 id 로 findAs 를 호출하면 null 이 반환된다`() = runSuspendIO {
        val nonExistentId = Long.MAX_VALUE
        val result = sf.withSessionSuspending { session ->
            session.findAs<Author>(nonExistentId).awaitSuspending()
        }

        result.shouldBeNull()
    }

    /**
     * [withStatelessTransactionSuspending] 에서 여러 쿼리를 순차 실행해도 결과가 올바른지 검증합니다.
     */
    @Test
    fun `withStatelessTransactionSuspending 에서 순차 쿼리 결과가 일관성 있다`() = runSuspendIO {
        val (authorCount, bookCount) = sf.withStatelessTransactionSuspending { session ->
            val ac = session.createSelectionQueryAs<Long>("select count(a) from Author a")
                .singleResult.awaitSuspending()
            val bc = session.createSelectionQueryAs<Long>("select count(b) from Book b")
                .singleResult.awaitSuspending()
            ac to bc
        }

        authorCount shouldBeGreaterOrEqualTo 1L
        bookCount shouldBeGreaterOrEqualTo 1L
        // books 수는 authors 수보다 크거나 같을 수 없습니다 (각 author 가 최소 1권)
        // 이 모듈의 데이터에서는 author 당 book 이 1권이므로 동일하거나 초과할 수 있음
        authorCount shouldBeLessOrEqualTo bookCount
    }
}
