package io.bluetape4k.aws.kotlin.s3.model

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class S3ModelTest {

    companion object : KLogging()

    // -------- ObjectIdentifier --------

    @Test
    fun `objectIdentifierOf는 key로 ObjectIdentifier를 생성한다`() {
        val id = objectIdentifierOf("my-object-key")

        id.key shouldBeEqualTo "my-object-key"
    }

    @Test
    fun `objectIdentifierOf는 key와 versionId로 ObjectIdentifier를 생성한다`() {
        val id = objectIdentifierOf("my-key", versionId = "v1")

        id.key shouldBeEqualTo "my-key"
        id.versionId shouldBeEqualTo "v1"
    }

    @Test
    fun `String toObjectIdentifier 확장으로 ObjectIdentifier를 생성한다`() {
        val id = "my-key".toObjectIdentifier()

        id.key shouldBeEqualTo "my-key"
    }

    @Test
    fun `objectIdentifierOf는 빈 key를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            objectIdentifierOf("")
        }
    }

    // -------- Delete --------

    @Test
    fun `deleteOf vararg String으로 Delete를 생성한다`() {
        val delete = deleteOf("key-1", "key-2", "key-3")

        delete.objects.shouldNotBeNull()
        delete.objects!!.size shouldBeEqualTo 3
    }

    @Test
    fun `deleteOf Collection String으로 Delete를 생성한다`() {
        val keys = listOf("key-1", "key-2")
        val delete = deleteOf(keys)

        delete.objects!!.size shouldBeEqualTo 2
    }

    @Test
    fun `deleteOf는 quiet 옵션을 설정할 수 있다`() {
        val delete = deleteOf("key-1", quiet = true)

        delete.quiet shouldBeEqualTo true
    }

    @Test
    fun `deleteOf는 빈 keys를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            deleteOf(emptyList<String>())
        }
    }

    // -------- GetObject --------

    @Test
    fun `getObjectRequestOf는 bucket과 key로 요청을 생성한다`() {
        val req = getObjectRequestOf(bucket = "my-bucket", key = "path/to/object.txt")

        req.bucket shouldBeEqualTo "my-bucket"
        req.key shouldBeEqualTo "path/to/object.txt"
    }

    @Test
    fun `getObjectRequestOf는 versionId를 설정할 수 있다`() {
        val req = getObjectRequestOf(bucket = "my-bucket", key = "obj.txt", versionId = "v1")

        req.versionId shouldBeEqualTo "v1"
    }

    @Test
    fun `getObjectRequestOf는 빈 bucket을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            getObjectRequestOf(bucket = "", key = "key")
        }
    }

    @Test
    fun `getObjectRequestOf는 빈 key를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            getObjectRequestOf(bucket = "my-bucket", key = "  ")
        }
    }

    // -------- HeadObject --------

    @Test
    fun `headObjectRequestOf는 bucket과 key로 요청을 생성한다`() {
        val req = headObjectRequestOf(bucket = "my-bucket", key = "path/to/object.txt")

        req.bucket shouldBeEqualTo "my-bucket"
        req.key shouldBeEqualTo "path/to/object.txt"
    }

    @Test
    fun `headObjectRequestOf는 빈 bucket을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            headObjectRequestOf(bucket = "", key = "key")
        }
    }
}
