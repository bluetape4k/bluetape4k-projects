package io.bluetape4k.aws.kotlin.s3.model

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class S3CopyObjectTest {

    companion object : KLogging()

    @Test
    fun `copyObjectRequestOf srcBucket과 srcKey, destBucket, destKey로 요청을 생성한다`() {
        val req = copyObjectRequestOf(
            srcBucket = "src-bucket",
            srcKey = "path/to/src.txt",
            destBucket = "dest-bucket",
            destKey = "path/to/dest.txt"
        )

        req.bucket shouldBeEqualTo "dest-bucket"
        req.key shouldBeEqualTo "path/to/dest.txt"
        req.copySource.shouldNotBeNull()
    }

    @Test
    fun `copyObjectRequestOf copySource 문자열로 요청을 생성한다`() {
        val req = copyObjectRequestOf(
            copySource = "src-bucket/path/to/src.txt",
            destBucket = "dest-bucket",
            destKey = "path/to/dest.txt"
        )

        req.copySource shouldBeEqualTo "src-bucket/path/to/src.txt"
        req.bucket shouldBeEqualTo "dest-bucket"
        req.key shouldBeEqualTo "path/to/dest.txt"
    }

    @Test
    fun `copyObjectRequestOf는 빈 srcBucket을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            copyObjectRequestOf(
                srcBucket = "",
                srcKey = "key",
                destBucket = "dest",
                destKey = "dest-key"
            )
        }
    }

    @Test
    fun `copyObjectRequestOf는 빈 destBucket을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            copyObjectRequestOf(
                copySource = "src/key",
                destBucket = "",
                destKey = "dest-key"
            )
        }
    }

    @Test
    fun `copyObjectRequestOf는 빈 destKey를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            copyObjectRequestOf(
                copySource = "src/key",
                destBucket = "dest-bucket",
                destKey = "  "
            )
        }
    }
}
