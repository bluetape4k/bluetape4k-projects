package io.bluetape4k.aws.kotlin.s3.model

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class S3ListObjectsTest {

    companion object : KLogging()

    @Test
    fun `listObjectsRequestOf는 bucket으로 요청을 생성한다`() {
        val req = listObjectsRequestOf(bucket = "my-bucket")

        req.bucket shouldBeEqualTo "my-bucket"
    }

    @Test
    fun `listObjectsRequestOf는 prefix를 설정할 수 있다`() {
        val req = listObjectsRequestOf(bucket = "my-bucket", prefix = "path/to/")

        req.prefix shouldBeEqualTo "path/to/"
    }

    @Test
    fun `listObjectsRequestOf는 maxKeys를 설정할 수 있다`() {
        val req = listObjectsRequestOf(bucket = "my-bucket", maxKeys = 50)

        req.maxKeys shouldBeEqualTo 50
    }

    @Test
    fun `listObjectsRequestOf는 delimiter를 설정할 수 있다`() {
        val req = listObjectsRequestOf(bucket = "my-bucket", delimiter = "/")

        req.delimiter shouldBeEqualTo "/"
    }

    @Test
    fun `listObjectsRequestOf는 빈 bucket을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            listObjectsRequestOf(bucket = "")
        }
    }

    @Test
    fun `listObjectsRequestOf builder 블록으로 추가 설정이 가능하다`() {
        val req = listObjectsRequestOf(bucket = "my-bucket") {
            // additional settings
        }
        req.shouldNotBeNull()
        req.bucket shouldBeEqualTo "my-bucket"
    }
}
