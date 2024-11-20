package io.bluetape4k.aws.s3.examples

import io.bluetape4k.aws.s3.AbstractS3Test
import io.bluetape4k.aws.s3.model.deleteObjectsRequestOf
import io.bluetape4k.aws.s3.model.deleteOf
import io.bluetape4k.aws.s3.model.listBucketsRequestOf
import io.bluetape4k.aws.s3.model.objectIdentifierOf
import io.bluetape4k.aws.s3.putAsByteArray
import io.bluetape4k.aws.s3.putAsString
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.s3.model.Permission
import software.amazon.awssdk.services.s3.model.RequestPayer
import software.amazon.awssdk.services.s3.model.S3Exception
import java.util.*
import kotlin.test.assertFailsWith

class BasicExamples: AbstractS3Test() {

    companion object: KLogging()

    @Test
    fun `모든 bucket 을 조회합니다`() {
        val response = s3Client.listBuckets(listBucketsRequestOf())

        val buckets = response.buckets()
        buckets.forEach { bucket ->
            log.debug { "Bucket=${bucket.name()}" }
        }
        buckets.shouldNotBeEmpty()
    }

    @Test
    fun `bucket의 모든 object를 조회합니다`() {
        val bucket = UUID.randomUUID().encodeBase62().lowercase()
        createBucketsIfNotExists(bucket)

        val keys = List(5) { UUID.randomUUID().encodeBase62() }.sorted()
        keys.forEach { key ->
            s3Client.putAsString(bucket, key, randomString()) {
                it.requestPayer(RequestPayer.REQUESTER)
            }
        }

        val response = s3Client.listObjectsV2 { it.bucket(bucket) }
        val objects = response.contents()

        objects.forEach {
            log.debug { "key=${it.key()}, size=${it.size()}, owner=${it.owner()}" }
        }
        objects.map { it.key() }.sorted() shouldBeEqualTo keys
    }

    @Test
    fun `delete multi objects`() {
        // Batch 로 삭제하기 위해서는 key name 으로 만든 [ObjectIdentifier] 정보가 필요합니다.
        val objectSize = 10
        val objectIds = List(objectSize) {
            val key = UUID.randomUUID().toString()
            s3Client.putAsByteArray(BUCKET_NAME, key, randomString().toUtf8Bytes())
            objectIdentifierOf(key)
        }

        log.debug { "${objectIds.size} objects successfully created." }

        // 삭제할 Object 들의 object identifier 로 [Delete] 를 구성한다
        val delete = deleteOf(objectIds)
        val response = s3Client.deleteObjects(deleteObjectsRequestOf(BUCKET_NAME, delete))
        val deletedObjects = response.deleted()
        deletedObjects.forEach {
            log.debug { "Deleted object. key=${it.key()}" }
        }
        response.deleted() shouldHaveSize objectSize
    }

    @Test
    fun `get bucket acl`() {
        val key = UUID.randomUUID().toString()
        s3Client.putAsByteArray(BUCKET_NAME, key, "acl-content".toUtf8Bytes())

        val aclResponse = s3Client.getObjectAcl { it.bucket(BUCKET_NAME).key(key) }
        val grants = aclResponse.grants()

        grants.forEach {
            log.debug { "Gratee id=${it.grantee()}, permission=${it.permission()}" }
        }

        grants.shouldNotBeEmpty()
        val grant = grants.first()
        grant.grantee().id().shouldNotBeEmpty()
        grant.permission() shouldBeEqualTo Permission.FULL_CONTROL
    }

    @Test
    fun `get bucket policy`() {
        // Bucket Policy 를 지정하지 않았습니다. 없으면 예외가 발생하네요 ...
        assertFailsWith<S3Exception> {
            val response = s3Client.getBucketPolicy { it.bucket(BUCKET_NAME) }
            val policy = response.policy()
            log.debug { "Policy=$policy" }
            policy.shouldNotBeEmpty()
        }
    }
}
