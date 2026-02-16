package io.bluetape4k.examples.redisson.coroutines.objects

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.toUtf8Bytes
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.io.Serializable


/**
 * BloomFilter 예제
 *
 * Redis based distributed RBloomFilter bloom filters for Java.
 * Number of contained bits is limited to 2^32.
 *
 * 특정 요소가 포함되었는지는 불확실하지만, 포함되지 않았음은 확률적으로 보장하는 방법입니다.
 *
 * Must be initialized with capacity size by `tryInit(expectedInsertions, falseProbability)` method before usage.
 *
 * 참고:
 * * [BloomFilter](https://github.com/redisson/redisson/wiki/6.-distributed-objects/#68-bloom-filters)
 * * [블룸 필터 - Wiki](https://ko.wikipedia.org/wiki/%EB%B8%94%EB%A3%B8_%ED%95%84%ED%84%B0)
 *
 * 블룸 필터(Bloom filters)는 원소가 집합에 속하는지 여부를 검사하는데 사용되는 확률적 자료 구조이다.
 * 1970년 Burton Howard Bloom에 의해 고안되었다.
 * 블룸 필터에 의해 어떤 원소가 집합에 속한다고 판단된 경우 실제로는 원소가 집합에 속하지 않는 긍정 오류가 발생하는 것이 가능하지만,
 * 반대로 원소가 집합에 속하지 않는 것으로 판단되었는데 실제로는 원소가 집합에 속하는 부정 오류는 절대로 발생하지 않는다는 특성이 있다.
 * 집합에 원소를 추가하는 것은 가능하나, 집합에서 원소를 삭제하는 것은 불가능하다.
 * 집합 내 원소의 숫자가 증가할수록 긍정 오류 발생 확률도 증가한다.
 */
@RandomizedTest
class BloomFilterExamples: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

    data class Message(
        val id: Long,
        val content: String,
    ): Serializable

    @Test
    fun `use redisson bloomfilter`(
        @RandomValue(type = Message::class, size = 100) messages: List<Message>,
        @RandomValue excludedMessage: Message,
    ) = runSuspendIO {
        val bloomFilter = redisson.getBloomFilter<Message>(randomName())
        bloomFilter.tryInit(100_000L, 0.01).shouldBeTrue()

        // BloomFilter에 요소를 추가한다
        messages.forEach { bloomFilter.add(it) }

        // bloomFilter.count() shouldBeEqualTo messages.size
        val messageTotalSize = messages.sumOf { it.content.toUtf8Bytes().size }
        // BloomFilter가 차지하는 메모리가 예상 입력 데이터의 크기보다 더 작다 (예상 입력 갯수를 키우면 메모리를 더 차지하지만 정확도가 높아진다)
        // Redis used memory size=95,850, messages total size=127,401   : expected insertions: 10,000, message size = 1,000
        // Redis used memory size=958,505, messages total size=128,271  : expected insertions: 100,000, message size = 1,000
        println("Redis used memory size=${bloomFilter.size}, messages total size=$messageTotalSize")

        // BloomFilter로 요소가 존재하는지 판단한다
        messages.forEach {
            bloomFilter.contains(it).shouldBeTrue()
        }

        // 존재하지 않는 요소는 false 를 반환한다
        bloomFilter.contains(excludedMessage).shouldBeFalse()

        bloomFilter.deleteAsync().awaitSuspending().shouldBeTrue()
    }
}
