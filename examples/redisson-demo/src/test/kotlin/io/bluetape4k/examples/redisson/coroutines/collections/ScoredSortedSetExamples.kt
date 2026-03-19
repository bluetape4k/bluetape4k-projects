package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.redisson.api.RScoredSortedSet
import org.redisson.api.SetIntersectionArgs
import org.redisson.api.SetUnionArgs

/**
 * Scored sorted set examples
 *
 * 참고
 * * [Redisson SortedSet](https://redisson.org/glossary/java-sortedset.html)
 * * [SortedSortedSet](https://github.com/redisson/redisson/wiki/7.-distributed-collections/#75-scoredsortedset)
 */
class ScoredSortedSetExamples: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

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
        batch.executeAsync().awaitSuspending()

        return redisson.getScoredSortedSet(zsetName)
    }

    @Test
    fun `RScoredSortedSet 조작하기`() = runTest {
        val zset: RScoredSortedSet<String> = getSampleScoredSortedSet()

        zset.sizeAsync().awaitSuspending() shouldBeEqualTo 6
        zset.toSortedSet() shouldBeEqualTo setOf("1", "2", "3", "4", "5", "6")

        zset.containsAsync("2").awaitSuspending().shouldBeTrue()
        zset.containsAllAsync(setOf("2", "3", "4")).awaitSuspending().shouldBeTrue()

        zset.deleteAsync().awaitSuspending()
    }

    @Test
    fun `Score 변경하기`() = runTest {
        val zset: RScoredSortedSet<String> = getSampleScoredSortedSet()

        // "2" 의 score 값을 추가한다
        zset.addScoreAsync("2", 15.0).awaitSuspending() shouldBeEqualTo 35.0

        // "1" 의 score를 추가하고, 올림차순 Rank 를 얻는다
        zset.addScoreAndGetRankAsync("1", 1.0).awaitSuspending() shouldBeEqualTo 0

        zset.deleteAsync().awaitSuspending()
    }

    @Test
    fun `Top N 조회하기`() = runTest {
        val zset: RScoredSortedSet<String> = getSampleScoredSortedSet()

        // 올림차순으로 Top 4 조회
        val top4 = zset.entryRangeAsync(0, 3).awaitSuspending()
        top4.forEach { entry ->
            log.debug { "member=${entry.value}, score=${entry.score}" }
        }
        top4.map { it.value } shouldBeEqualTo listOf("1", "2", "3", "4")

        // 내림차순으로 Top 4 조회
        val top4Rev = zset.entryRangeReversedAsync(0, 3).awaitSuspending()
        top4Rev.forEach { entry ->
            log.debug { "member=${entry.value}, score=${entry.score}" }
        }
        top4Rev.map { it.value } shouldBeEqualTo listOf("6", "5", "4", "3")

        // 올림차순으로 첫번째 요소
        zset.firstAsync().awaitSuspending() shouldBeEqualTo "1"
        // 올림차순으로 마지막 요소
        zset.lastAsync().awaitSuspending() shouldBeEqualTo "6"

        // 올림차순으로 Top 2 요소를 가져오고, 요소는 제거합니다. (Rank 갯수를 제한할 때 사용)
        zset.pollFirstAsync(2).awaitSuspending() shouldBeEqualTo listOf("1", "2")

        // 특정 Ranking에 해당하는 요소를 제거합니다. (Rank 갯수를 제한할 때 사용)
        zset.removeRangeByRankAsync(-2, -1).awaitSuspending() shouldBeEqualTo 2

        zset.deleteAsync().awaitSuspending()
    }

    @Test
    fun `take and poll for waiting`() = runTest {
        val zset = redisson.getScoredSortedSet<String>(randomName())
        zset.clear()

        val job = scope.launch {
            zset.takeFirstAsync().awaitSuspending() shouldBeEqualTo "1"
        }
        delay(1)

        zset.sizeAsync().awaitSuspending() shouldBeEqualTo 0

        zset.addAsync(10.0, "1").awaitSuspending()
        zset.addAsync(20.0, "2").awaitSuspending()
        zset.addAsync(30.0, "3").awaitSuspending()

        job.join()

        zset.readAll() shouldBeEqualTo listOf("2", "3")

        zset.deleteAsync().awaitSuspending()
    }

    @Test
    fun `union multiple zset`() = runTest {
        val zset1 = redisson.getScoredSortedSet<String>(randomName())
        val zset2 = redisson.getScoredSortedSet<String>(randomName())
        val zset3 = redisson.getScoredSortedSet<String>(randomName())

        val map1 = mapOf("1" to 10.0, "2" to 20.0)
        val map2 = mapOf("2" to 10.0, "3" to 20.0)
        val map3 = mapOf("3" to 30.0, "4" to 40.0)

        zset1.addAllAsync(map1).awaitSuspending()
        zset2.addAllAsync(map2).awaitSuspending()
        zset3.addAllAsync(map3).awaitSuspending()

        val union = redisson.getScoredSortedSet<String>(randomName())
        val unionArgs = SetUnionArgs.names(zset1.name, zset3.name).apply {
            aggregate(RScoredSortedSet.Aggregate.SUM)
        }
        union.unionAsync(unionArgs).awaitSuspending() shouldBeEqualTo 4
        // union.unionAsync(RScoredSortedSet.Aggregate.SUM, zset1.name, zset3.name).awaitSuspending() shouldBeEqualTo 4
        union.readAll() shouldBeEqualTo listOf("1", "2", "3", "4")

        val intersection = redisson.getScoredSortedSet<String>(randomName())
        val intersectionArgs = SetIntersectionArgs.names(zset1.name, zset2.name).apply {
            aggregate(RScoredSortedSet.Aggregate.MAX)
        }
        intersection.intersectionAsync(intersectionArgs).awaitSuspending() shouldBeEqualTo 1
        intersection.readAll() shouldBeEqualTo listOf("2")
        intersection.getScore("2") shouldBeEqualTo 20.0

        zset1.deleteAsync().awaitSuspending()
        zset2.deleteAsync().awaitSuspending()
        zset3.deleteAsync().awaitSuspending()

        union.deleteAsync().awaitSuspending()
        intersection.deleteAsync().awaitSuspending()
    }
}
