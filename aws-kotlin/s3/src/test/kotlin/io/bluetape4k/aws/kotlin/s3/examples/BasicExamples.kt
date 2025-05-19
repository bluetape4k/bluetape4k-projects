package io.bluetape4k.aws.kotlin.s3.examples

import aws.sdk.kotlin.services.s3.getBucketPolicy
import aws.sdk.kotlin.services.s3.listBuckets
import aws.sdk.kotlin.services.s3.listObjectsV2
import aws.sdk.kotlin.services.s3.model.Permission
import aws.sdk.kotlin.services.s3.model.S3Exception
import io.bluetape4k.aws.kotlin.s3.AbstractKotlinS3Test
import io.bluetape4k.aws.kotlin.s3.ensureBucketExists
import io.bluetape4k.aws.kotlin.s3.forceDeleteBucket
import io.bluetape4k.aws.kotlin.s3.getAsByteArray
import io.bluetape4k.aws.kotlin.s3.getAsFile
import io.bluetape4k.aws.kotlin.s3.getAsString
import io.bluetape4k.aws.kotlin.s3.getObjectAcl
import io.bluetape4k.aws.kotlin.s3.putFromByteArray
import io.bluetape4k.aws.kotlin.s3.putFromFile
import io.bluetape4k.aws.kotlin.s3.putFromString
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

@TempFolderTest
class BasicExamples: AbstractKotlinS3Test() {

    companion object: KLoggingChannel()

    @Test
    fun `launch S3 Server`() {
        s3Server.shouldNotBeNull()
        s3Client.shouldNotBeNull()
    }

    @Test
    fun `모든 Bucket을 조회합니다`() = runSuspendIO {
        log.debug { "모든 Bucket을 조회합니다 ..." }

        val response = s3Client.listBuckets { }

        response.buckets.shouldNotBeNull()
        response.buckets!!.forEach {
            log.debug { "Bucket=${it.name}" }
        }
    }

    @Test
    fun `Bucket의 모든 Object를 조회합니다`() = runSuspendIO {

        log.debug { "Bucket의 모든 Object를 조회합니다 ..." }

        // 테스트용 Bucket 생성
        val bucketName = TimebasedUuid.nextBase62String().lowercase()
        s3Client.ensureBucketExists(bucketName)

        val keys = List(5) { TimebasedUuid.nextBase62String().lowercase() }
        val uploadTasks = keys.map { key ->
            async(Dispatchers.IO) {
                s3Client.putFromString(bucketName, key, randomString())
            }
        }
        uploadTasks.awaitAll()

        val response = s3Client.listObjectsV2 { bucket = bucketName }
        val objects = response.contents ?: emptyList()

        objects.forEach {
            log.debug { "Object info. key=${it.key}, size=${it.size}, owner=${it.owner}" }
        }
        objects.map { it.key!! } shouldContainSame keys

        s3Client.forceDeleteBucket(bucketName)
    }

    @Test
    fun `put get object as ByteArray`() = runSuspendIO {
        val key = TimebasedUuid.nextBase62String().lowercase()
        val contents = randomString().toUtf8Bytes()

        // Put Object
        val putRes = s3Client.putFromByteArray(BUCKET_NAME, key, contents)
        putRes.eTag.shouldNotBeNull()

        // Get Object
        val downloadContents = s3Client.getAsByteArray(BUCKET_NAME, key)
        downloadContents.shouldNotBeNull()
        downloadContents shouldBeEqualTo contents
    }

    @Test
    fun `put get object as String`() = runSuspendIO {
        val key = TimebasedUuid.nextBase62String().lowercase()
        val contents = randomString()

        // Put Object
        val putRes = s3Client.putFromString(BUCKET_NAME, key, contents)
        putRes.eTag.shouldNotBeNull()

        // Get Object
        val downloadContents = s3Client.getAsString(BUCKET_NAME, key)
        downloadContents shouldBeEqualTo contents
    }

    @Test
    fun `put get object as File`(temp: TempFolder) = runSuspendIO {
        val key = TimebasedUuid.nextBase62String().lowercase()
        val content = randomString()

        val file = temp.createFile()
        file.writeText(content)

        // Put Object
        val putRes = s3Client.putFromFile(BUCKET_NAME, key, file)
        putRes.eTag.shouldNotBeNull()

        // Get Object
        val downloadFile = temp.createFile()
        val getRes = s3Client.getAsFile(BUCKET_NAME, key, downloadFile)
        getRes shouldBeGreaterThan 0L

        downloadFile.readText() shouldBeEqualTo content
    }

    @Test
    fun `get bucket acl`() = runSuspendIO {
        val key = TimebasedUuid.nextBase62String().lowercase()
        s3Client.putFromByteArray(BUCKET_NAME, key, "acl-content".toUtf8Bytes())

        val aclResponse = s3Client.getObjectAcl(BUCKET_NAME, key)
        val grants = aclResponse.grants ?: emptyList()

        grants.forEach {
            log.debug { "Grantee=${it.grantee}, Permission=${it.permission}" }
        }
        grants.shouldNotBeEmpty()

        val grant = grants.first()
        grant.grantee?.id.shouldNotBeNull().shouldNotBeEmpty()
        grant.permission shouldBeEqualTo Permission.FullControl
    }

    @Test
    fun `get bucket policy`() = runSuspendIO {
        assertFailsWith<S3Exception> {
            // Bucket Policy 를 지정하지 않았습니다. Policy가 없으면 예외가 발생합니다.
            s3Client.getBucketPolicy { bucket = BUCKET_NAME }
        }
    }
}
