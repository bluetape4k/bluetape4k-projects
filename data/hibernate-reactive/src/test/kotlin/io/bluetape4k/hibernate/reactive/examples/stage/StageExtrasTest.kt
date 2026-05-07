package io.bluetape4k.hibernate.reactive.examples.stage

import io.bluetape4k.hibernate.reactive.examples.model.Author
import io.bluetape4k.hibernate.reactive.examples.model.Book
import io.bluetape4k.hibernate.reactive.stage.asStageSessionFactory
import io.bluetape4k.hibernate.reactive.stage.createQueryAs
import io.bluetape4k.hibernate.reactive.stage.createSelectionQueryAs
import io.bluetape4k.hibernate.reactive.stage.getAs
import io.bluetape4k.hibernate.reactive.stage.withSessionSuspending
import io.bluetape4k.hibernate.reactive.stage.withStatelessSessionSuspending
import io.bluetape4k.hibernate.reactive.stage.withStatelessTransactionSuspending
import io.bluetape4k.hibernate.reactive.stage.withTransactionSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.future.await
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.LocalDate
import java.time.Month
import io.bluetape4k.assertions.assertFailsWith

/**
 * Stage API 추가 커버리지 테스트
 *
 * - [asStageSessionFactory] unwrap 검증
 * - exception propagation 검증 (예외가 세션 경계 밖으로 올바르게 전파되는지)
 * - [createQueryAs], [createSelectionQueryAs], [getAs] 추가 커버리지
 * - [withStatelessTransactionSuspending] 단독 세션 조회 검증
 * - count 쿼리 반복 검증
 */
@Execution(ExecutionMode.SAME_THREAD)
class StageExtrasTest: AbstractStageTest() {

    companion object: KLoggingChannel()

    private val author1 = Author(faker.name().name())
    private val author2 = Author(faker.name().name())
    private val book1 = Book(
        faker.numerify("#-#####-###-#"),
        faker.book().title(),
        LocalDate.of(1985, Month.APRIL, 1)
    )
    private val book2 = Book(
        faker.numerify("#-#####-###-#"),
        faker.book().title(),
        LocalDate.of(2005, Month.OCTOBER, 1)
    )

    @BeforeAll
    fun beforeAll() {
        author1.addBook(book1)
        author2.addBook(book2)

        runSuspendIO {
            sf.withTransactionSuspending { session ->
                session.persist(author1, author2).await()
            }
        }
    }

    /**
     * [asStageSessionFactory] 가 유효한 SessionFactory 를 반환하는지 검증합니다.
     * unwrap 이후 세션을 열어 쿼리를 실행할 수 있어야 합니다.
     */
    @Test
    fun `asStageSessionFactory 는 유효한 SessionFactory 를 반환한다`() = runSuspendIO {
        sf.shouldNotBeNull()
        sf.isOpen shouldBeEqualTo true

        val count = sf.withSessionSuspending { session ->
            session.createSelectionQueryAs<Long>("select count(a) from Author a")
                .singleResult
                .await()
        }

        // 최소 2명의 author 가 존재해야 합니다 (BeforeAll 에서 저장)
        count shouldBeGreaterOrEqualTo 2L
    }

    /**
     * [withSessionSuspending] 내부에서 발생한 예외가 호출자로 올바르게 전파되는지 검증합니다.
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
                .await()
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
            session.getAs<Author>(author1.id).await()
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
                .await()
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
            session.find(Author::class.java, nonExistentId).await()
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
                .singleResult.await()
            val bc = session.createSelectionQueryAs<Long>("select count(b) from Book b")
                .singleResult.await()
            ac to bc
        }

        authorCount shouldBeGreaterOrEqualTo 1L
        bookCount shouldBeGreaterOrEqualTo 1L
        authorCount shouldBeLessOrEqualTo bookCount
    }
}
