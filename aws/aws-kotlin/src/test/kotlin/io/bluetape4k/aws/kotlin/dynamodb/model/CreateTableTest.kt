package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.TableClass
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class CreateTableTest {

    companion object : KLogging()

    @Test
    fun `createTableRequestOf는 tableName으로 요청을 생성한다`() {
        val req = createTableRequestOf("my-table")

        req.tableName shouldBeEqualTo "my-table"
        req.tableClass shouldBeEqualTo TableClass.Standard
    }

    @Test
    fun `createTableRequestOf는 tableClass를 설정할 수 있다`() {
        val req = createTableRequestOf("my-table", tableClass = TableClass.StandardInfrequentAccess)

        req.tableClass shouldBeEqualTo TableClass.StandardInfrequentAccess
    }

    @Test
    fun `createTableRequestOf는 builder 블록으로 추가 설정이 가능하다`() {
        val req = createTableRequestOf("orders") {
            billingMode = aws.sdk.kotlin.services.dynamodb.model.BillingMode.PayPerRequest
        }

        req.shouldNotBeNull()
        req.tableName shouldBeEqualTo "orders"
    }

    @Test
    fun `createTableRequestOf는 빈 tableName을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            createTableRequestOf("")
        }
    }

    @Test
    fun `createTableRequestOf는 공백 tableName을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            createTableRequestOf("   ")
        }
    }
}
