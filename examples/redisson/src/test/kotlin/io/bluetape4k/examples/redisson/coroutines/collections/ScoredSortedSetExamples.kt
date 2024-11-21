package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.coroutines.coAwait
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.redisson.api.RScoredSortedSet

/**
 * Scored sorted set examples
 *
 * 참고
 * * [Redisson SortedSet](https://redisson.org/glossary/java-sortedset.html)
 * * [SortedSortedSet](https://github.com/redisson/redisson/wiki/7.-distributed-collections/#75-scoredsortedset)
 */
class ScoredSortedSetExamples: AbstractRedissonCoroutineTest() {

    companion object: KLogging()

    private suspend fun getSampleScoredSortedSet(): RScoredSortedSet<String> {
        val zsetName = randomName()

        val batch = redisson.createBatch()
        with(batch.getScoredSortedSet<String>(zsetName)) {
            addAsync(10.0, "1")
            addAsync(20.0, "2")
            addAsync(30.0, "3")

            val objects = mapOf(
                "4" to 40.0,
                "5" to 50.0,
                "6" to 60.0
            )
            addAllAsync(objects)
        }
        batch.executeAsync().coAwait()

        return redisson.getScoredSortedSet(zsetName)
    }

    @Test
    fun `RScoredSortedSet 조작하기`() = runTest {
        val zset: RScoredSortedSet<String> = getSampleScoredSortedSet()

        zset.sizeAsync().coAwait() shouldBeEqualTo 6
        zset.toSortedSet() shouldBeEqualTo setOf("1", "2", "3", "4", "5", "6")

        zset.containsAsync("2").coAwait().shouldBeTrue()
        zset.containsAllAsync(setOf("2", "3", "4")).coAwait().shouldBeTrue()

        zset.deleteAsync().coAwait()
    }

    @Test
    fun `Score 변경하기`() = runTest {
        val zset: RScoredSortedSet<String> = getSampleScoredSortedSet()

        // "2" 의 score 값을 추가한다
        zset.addScoreAsync("2", 15.0).coAwait() shouldBeEqualTo 35.0

        // "1" 의 score를 추가하고, 올림차순 Rank 를 얻는다
        zset.addScoreAndGetRankAsync("1", 1.0).coAwait() shouldBeEqualTo 0

        zset.deleteAsync().coAwait()
    }

    @Test
    fun `Top N 조회하기`() = runTest {
        val zset: RScoredSortedSet<String> = getSampleScoredSortedSet()

        // 올림차순으로 Top 4 조회
        val top4 = zset.entryRangeAsync(0, 3).coAwait()
        top4.forEach { entry ->
            log.debug { "member=${entry.value}, score=${entry.score}" }
        }
        top4.map { it.value } shouldBeEqualTo listOf("1", "2", "3", "4")

        // 내림차순으로 Top 4 조회
        val top4Rev = zset.entryRangeReversedAsync(0, 3).coAwait()
        top4Rev.forEach { entry ->
            log.debug { "member=${entry.value}, score=${entry.score}" }
        }
        top4Rev.map { it.value } shouldBeEqualTo listOf("6", "5", "4", "3")

        // 올림차순으로 첫번째 요소
        zset.firstAsync().coAwait() shouldBeEqualTo "1"
        // 올림차순으로 마지막 요소
        zset.lastAsync().coAwait() shouldBeEqualTo "6"

        // 올림차순으로 Top 2 요소를 가져오고, 요소는 제거합니다. (Rank 갯수를 제한할 때 사용)
        zset.pollFirstAsync(2).coAwait() shouldBeEqualTo listOf("1", "2")

        // 특정 Ranking에 해당하는 요소를 제거합니다. (Rank 갯수를 제한할 때 사용)
        zset.removeRangeByRankAsync(-2, -1).coAwait() shouldBeEqualTo 2

        zset.deleteAsync().coAwait()
    }

    @Test
    fun `take and poll for waiting`() = runTest {
        val zset = redisson.getScoredSortedSet<String>(randomName())
        zset.clear()

        val job = scope.launch {
            zset.takeFirstAsync().coAwait() shouldBeEqualTo "1"
        }
        delay(1)

        zset.sizeAsync().coAwait() shouldBeEqualTo 0

        zset.addAsync(10.0, "1").coAwait()
        zset.addAsync(20.0, "2").coAwait()
        zset.addAsync(30.0, "3").coAwait()

        job.join()

        zset.readAll() shouldBeEqualTo listOf("2", "3")

        zset.deleteAsync().coAwait()
    }

    @Test
    fun `union multiple zset`() = runTest {
        val zset1 = redisson.getScoredSortedSet<String>(randomName())
        val zset2 = redisson.getScoredSortedSet<String>(randomName())
        val zset3 = redisson.getScoredSortedSet<String>(randomName())

        val map1 = mapOf("1" to 10.0, "2" to 20.0)
        val map2 = mapOf("2" to 10.0, "3" to 20.0)
        val map3 = mapOf("3" to 30.0, "4" to 40.0)

        zset1.addAllAsync(map1).coAwait()
        zset2.addAllAsync(map2).coAwait()
        zset3.addAllAsync(map3).coAwait()

        val union = redisson.getScoredSortedSet<String>(randomName())

        union.unionAsync(RScoredSortedSet.Aggregate.SUM, zset1.name, zset3.name).coAwait() shouldBeEqualTo 4
        union.readAll() shouldBeEqualTo listOf("1", "2", "3", "4")

        val intersection = redisson.getScoredSortedSet<String>(randomName())
        intersection.intersectionAsync(RScoredSortedSet.Aggregate.MAX, zset1.name, zset2.name)
            .coAwait() shouldBeEqualTo 1
        intersection.readAll() shouldBeEqualTo listOf("2")
        intersection.getScore("2") shouldBeEqualTo 20.0

        zset1.deleteAsync().coAwait()
        zset2.deleteAsync().coAwait()
        zset3.deleteAsync().coAwait()

        union.deleteAsync().coAwait()
        intersection.deleteAsync().coAwait()
    }
}
