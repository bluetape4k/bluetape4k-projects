package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.ScalarAttributeType
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class DynamoDbAttributeDefinitionTest {

    companion object : KLogging()

    @Test
    fun `attributeDefinitionOf는 attributeName과 attributeType으로 정의를 생성한다`() {
        val def = attributeDefinitionOf("id", ScalarAttributeType.S)

        def.attributeName shouldBeEqualTo "id"
        def.attributeType shouldBeEqualTo ScalarAttributeType.S
    }

    @Test
    fun `attributeDefinitionOf는 Number 타입을 설정할 수 있다`() {
        val def = attributeDefinitionOf("score", ScalarAttributeType.N)

        def.attributeType shouldBeEqualTo ScalarAttributeType.N
    }

    @Test
    fun `attributeDefinitionOf는 Binary 타입을 설정할 수 있다`() {
        val def = attributeDefinitionOf("data", ScalarAttributeType.B)

        def.attributeType shouldBeEqualTo ScalarAttributeType.B
    }

    @Test
    fun `attributeDefinitionOf는 빈 attributeName을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            attributeDefinitionOf("", ScalarAttributeType.S)
        }
    }

    @Test
    fun `attributeDefinitionOf 인스턴스는 null이 아니다`() {
        val def = attributeDefinitionOf("pk", ScalarAttributeType.S)
        def.shouldNotBeNull()
    }
}
