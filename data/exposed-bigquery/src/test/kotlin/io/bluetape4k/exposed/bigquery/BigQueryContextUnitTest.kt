package io.bluetape4k.exposed.bigquery

import com.google.api.services.bigquery.Bigquery
import com.google.api.services.bigquery.Bigquery.Jobs
import com.google.api.services.bigquery.model.ErrorProto
import com.google.api.services.bigquery.model.QueryResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

/**
 * [BigQueryContext] 단위 테스트 — 에뮬레이터 없이 MockK로 동작 검증.
 */
class BigQueryContextUnitTest {

    private val sqlGenDb: Database = Database.connect(
        url = "jdbc:h2:mem:bq_unit_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
    )

    /**
     * BigQuery REST API mock 헬퍼.
     * [responseSupplier]가 반환하는 [QueryResponse]를 `jobs().query().execute()` 호출 시 반환합니다.
     */
    private fun mockBigquery(responseSupplier: () -> QueryResponse): Bigquery {
        val bq = mockk<Bigquery>(relaxed = true)
        val jobs = mockk<Jobs>(relaxed = true)
        val queryCall = mockk<Jobs.Query>(relaxed = true)
        every { bq.jobs() } returns jobs
        every { jobs.query(any(), any()) } returns queryCall
        every { queryCall.execute() } answers { responseSupplier() }
        return bq
    }

    @Test
    fun `runRawQuery - 성공 응답은 QueryResponse를 반환한다`() {
        val expected = QueryResponse().setJobComplete(true)
        val bq = mockBigquery { expected }

        val context = BigQueryContext(bq, "proj", "ds", sqlGenDb)
        val result = context.runRawQuery("SELECT 1")

        result.shouldNotBeNull()
        verify(exactly = 1) { bq.jobs() }
    }

    @Test
    fun `runRawQuery - 오류 응답은 BigQueryQueryException을 던진다`() {
        val errorResponse = QueryResponse()
            .setJobComplete(true)
            .setErrors(listOf(ErrorProto().setMessage("테이블을 찾을 수 없습니다").setReason("notFound")))
        val bq = mockBigquery { errorResponse }

        val context = BigQueryContext(bq, "proj", "ds", sqlGenDb)

        val ex = assertFailsWith<BigQueryQueryException> {
            context.runRawQuery("SELECT * FROM missing_table")
        }
        ex.message.shouldNotBeNull()
        ex.message!! shouldContain "테이블을 찾을 수 없습니다"
    }

    @Test
    fun `runRawQuery - 예외 타입은 BigQueryQueryException이다`() {
        val errorResponse = QueryResponse()
            .setJobComplete(true)
            .setErrors(listOf(ErrorProto().setMessage("권한 없음").setReason("accessDenied")))
        val bq = mockBigquery { errorResponse }

        val context = BigQueryContext(bq, "proj", "ds", sqlGenDb)

        val ex = assertFailsWith<BigQueryQueryException> {
            context.runRawQuery("SELECT 1")
        }
        ex.shouldBeInstanceOf<BigQueryQueryException>()
    }

    @Test
    fun `runRawQuery - 오류 메시지에 SQL 앞부분이 포함된다`() {
        val sql = "SELECT * FROM bad_table_xyz"
        val errorResponse = QueryResponse()
            .setJobComplete(true)
            .setErrors(listOf(ErrorProto().setMessage("오류").setReason("badRequest")))
        val bq = mockBigquery { errorResponse }

        val context = BigQueryContext(bq, "proj", "ds", sqlGenDb)

        val ex = assertFailsWith<BigQueryQueryException> {
            context.runRawQuery(sql)
        }
        ex.message!! shouldContain "bad_table_xyz"
    }

    @Test
    fun `create 팩토리 - BigQueryContext 인스턴스를 올바르게 생성한다`() {
        val bq = mockk<Bigquery>(relaxed = true)

        val context = BigQueryContext.create(bq, projectId = "my-project", datasetId = "my-dataset")

        context.shouldNotBeNull()
        context.projectId shouldContain "my-project"
        context.datasetId shouldContain "my-dataset"
    }
}
