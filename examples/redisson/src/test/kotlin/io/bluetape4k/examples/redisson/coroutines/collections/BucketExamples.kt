package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.coroutines.coAwait
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.redisson.api.RBucket
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * [RBucket] 예제
 *
 * 참고: [RBucket](https://github.com/redisson/redisson/wiki/6.-distributed-objects/#61-object-holder)
 *
 * @constructor Create empty Bucket examples
 */
class BucketExamples: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

    @Test
    fun `use bucket`() = runTest {
        val bucket: RBucket<String> = redisson.getBucket(randomName())

        // bucket에 object를 설정한다
        bucket.setAsync("000", 60, TimeUnit.SECONDS).coAwait()
        delay(100)

        // 기존 TTL을 유지하면서 object 를 set 한다
        bucket.setAndKeepTTLAsync("123").coAwait()

        // TTL 정보
        bucket.remainTimeToLiveAsync().coAwait() shouldBeInRange (0L until 60 * 1000L)

        // "123" 값을 가지고 있으면 "2032" 로 변경한다
        bucket.compareAndSetAsync("123", "2032").coAwait().shouldBeTrue()

        // 기존 값을 가져오고, 새로운 값으로 설정
        bucket.getAndSetAsync("5081").coAwait() shouldBeEqualTo "2032"

        // bucket에 object가 없을 때에 set을 수행한다
        bucket.setIfAbsentAsync("7777", Duration.ofSeconds(60)).coAwait().shouldBeFalse()

        // 기존 값이 있을 때에만 새로 설정한다
        bucket.setIfExistsAsync("9999").coAwait().shouldBeTrue()

        // object size
        bucket.size() shouldBeGreaterThan 0L

        bucket.deleteAsync().coAwait()
    }

    @Test
    fun `use bucket in coroutines`() = runTest {
        val bucket: RBucket<String> = redisson.getBucket(randomName())

        // bucket에 object를 설정한다
        bucket.setAsync("000", 60, TimeUnit.SECONDS).coAwait()

        val job = scope.launch {
            bucket.compareAndSetAsync("000", "111").coAwait().shouldBeTrue()
        }

        delay(100)
        job.join()

        bucket.getAndDeleteAsync().coAwait() shouldBeEqualTo "111"

        bucket.deleteAsync().coAwait()
    }

    @Test
    fun `multiple buckets example`() = runTest {
        val buckets = redisson.buckets

        val bucketName1 = randomName()
        val bucketName2 = randomName()
        val bucketName3 = randomName()

        // 기존에 데이터를 가진 bucket 이 없다
        val existBuckets1 = buckets.getAsync<String>(bucketName1, bucketName2, bucketName3).coAwait()
        existBuckets1.size shouldBeEqualTo 0


        val map = mutableMapOf(
            bucketName1 to "object1",
            bucketName2 to "object2"
        )
        // 복수의 bucket에 한번에 데이터를 저장한다 (기존에 데이터가 있는 bucket 이 하나라도 있다면 실패한다)
        buckets.trySetAsync(map).coAwait().shouldBeTrue()

        // object를 가진 bucket 은 2개이다.
        val values = buckets.getAsync<String>(bucketName1, bucketName2, bucketName3).coAwait()
        values.size shouldBeEqualTo 2

        val bucket1 = redisson.getBucket<String>(bucketName1)
        bucket1.get() shouldBeEqualTo "object1"

        redisson.getBucket<String>(bucketName1).deleteAsync().coAwait().shouldBeTrue()
        redisson.getBucket<String>(bucketName2).deleteAsync().coAwait().shouldBeTrue()
        redisson.getBucket<String>(bucketName3).deleteAsync().coAwait().shouldBeFalse()
    }
}
